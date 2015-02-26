package alien4cloud.paas.cloudify2.events;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.GridServiceAgents;
import org.openspaces.admin.gsa.GridServiceContainerOptions;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnitDeployment;

public class GigaSpacesPUDeployer {

    private static final String CONTEXT_PROPERTY_APPLICATION_NAME = "com.gs.application";
    private static final String DEFAULT_LOCATORS = "localhost:4174";
    private static final String DEFAULT_ZONE = "events";

    private static final Options COMMAND_LINE_OPTIONS = new Options();

    private static final String HELP_OPTION = "h";
    private static final String LOCATORS_OPTION = "locators";
    private static final String PU_NAME_OPTION = "pu";
    private static final String DEPLOYMENT_NAME_OPTION = "name";

    static {
        COMMAND_LINE_OPTIONS.addOption(HELP_OPTION, false, "Print help");
        COMMAND_LINE_OPTIONS.addOption(DEPLOYMENT_NAME_OPTION, true, "The deployment name");
        COMMAND_LINE_OPTIONS.addOption(LOCATORS_OPTION, true, "GigaSpaces locators");
        COMMAND_LINE_OPTIONS.addOption(PU_NAME_OPTION, true, "Path to the process unit to deploy");
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        String name = null;
        String locators = null;
        File processingUnit = null;

        CommandLineParser cmdLineParser = new PosixParser();
        try {
            CommandLine cmd = cmdLineParser.parse(COMMAND_LINE_OPTIONS, args);

            if (cmd.hasOption(HELP_OPTION)) {
                printUsage();
                System.exit(0);
            }

            if (cmd.hasOption(DEPLOYMENT_NAME_OPTION)) {
                name = cmd.getOptionValue(DEPLOYMENT_NAME_OPTION);
            }

            if (cmd.hasOption(LOCATORS_OPTION)) {
                locators = cmd.getOptionValue(LOCATORS_OPTION);
            } else {
                locators = DEFAULT_LOCATORS;
            }

            if (!cmd.hasOption(PU_NAME_OPTION)) {
                printUsage();
                quitWithError("Option -" + PU_NAME_OPTION + " is mandatory : The path to the processing unit must be specified");
            } else {
                // String puPath = "src/main/resources/alien4cloud-cloudify-events-rest.war";
                String puPath = cmd.getOptionValue(PU_NAME_OPTION);
                processingUnit = new File(puPath);
                if (!processingUnit.exists()) {
                    printUsage();
                    quitWithError("The file '" + processingUnit.getAbsolutePath() + "' does not exists");
                    System.exit(1);
                }
            }

        } catch (ParseException e) {
            quitWithError(e);
        }

        deploy(name, locators, processingUnit);
    }

    private static void quitWithError(String message) {
        System.err.println(message);
        System.exit(1);
    }

    private static void quitWithWarning(String message) {
        System.out.println("WARNING: \t " + message);
        System.exit(0);
    }

    private static void quitWithError(Exception e) {
        quitWithError("Exception happened : [" + e.getMessage() + "], see log for more details");
    }

    private static void printUsage() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("gsDeploy [-h] [-locators {host:port,host:port,..}] [-pu path_to_the_pu_file]", COMMAND_LINE_OPTIONS);
    }

    private static void deploy(String name, String locators, File processingUnit) throws InterruptedException, IOException {
        AdminFactory adminFactory = new AdminFactory();
        adminFactory.useGsLogging(false);
        adminFactory.addLocators(locators);
        Admin admin = null;
        try {

            admin = adminFactory.create();

            while (!checkGSAAvailability(admin)) {
                Thread.sleep(1000L);
            }

            while (!checkGSMAvailability(admin)) {
                Thread.sleep(1000L);
            }

            GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
            for (GridServiceAgent gsa : agents) {
                if (!gsa.getMachine().getGridServiceManagers().isEmpty()) {
                    startEventGSC(gsa);
                }
            }

            if (isDeployed(admin, name)) {
                System.out.println("WARNING: \t PU " + name + " already deployed. Skipping...");
                return;
            }

            ProcessingUnitDeployment deployment = new ProcessingUnitDeployment(processingUnit);
            deployment.name(name);
            deployment.setContextProperty(CONTEXT_PROPERTY_APPLICATION_NAME, "management");
            deployment.addZone(DEFAULT_ZONE);

            GridServiceManager electedManager = admin.getGridServiceManagers().waitForAtLeastOne();
            if (electedManager == null) {
                System.out.println("ERROR: \t Failed to find an available GSM to deploy.");
                return;
            }
            electedManager.deploy(deployment);
        } finally {
            if (admin != null) {
                admin.close();
            }
        }
    }

    private static void startEventGSC(GridServiceAgent agent) throws IOException {
        if (isLocalAgent(agent.getMachine().getHostAddress())) {
            System.out.println("Start a GSC for events on <" + agent.getMachine().getHostAddress() + ">");
            GridServiceContainerOptions options = new GridServiceContainerOptions();
            options.vmInputArgument("-Xmx128m");
            options.vmInputArgument("-Xms128m");
            options.vmInputArgument("-Dcom.gs.zones=" + DEFAULT_ZONE);
            options.vmInputArgument("-Dcom.gs.transport_protocol.lrmi.bind-port=7010-7110");
            agent.startGridService(options);
        }
    }

    private static boolean isLocalAgent(String agentIp) throws IOException {
        Enumeration e = NetworkInterface.getNetworkInterfaces();
        while (e.hasMoreElements()) {
            NetworkInterface n = (NetworkInterface) e.nextElement();
            Enumeration ee = n.getInetAddresses();
            while (ee.hasMoreElements()) {
                InetAddress i = (InetAddress) ee.nextElement();
                System.out.println("is agent <" + agentIp + "> on <" + i.getHostAddress() + ">");
                if (i.getHostAddress().equals(agentIp)) {
                    System.out.println("YES");
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if the given pu is deployed on an available GSM.
     *
     * @param admin
     * @param name
     * @return The first manager on which the PU is not yet deployed
     */
    private static boolean isDeployed(Admin admin, String name) {
        GridServiceManager[] gsms = admin.getGridServiceManagers().getManagers();
        for (GridServiceManager manager : gsms) {
            if (manager.isDeployed(name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean checkGSMAvailability(Admin admin) {
        GridServiceManager[] gsms = admin.getGridServiceManagers().getManagers();
        if (gsms != null && gsms.length > 0) {
            return true;
        }
        return false;
    }

    private static boolean checkGSAAvailability(Admin admin) {
        GridServiceAgents gridServiceAgents = admin.getGridServiceAgents();
        GridServiceAgent[] agents = gridServiceAgents.getAgents();
        if (agents != null && agents.length > 0) {
            return true;
        }
        return false;
    }
}
