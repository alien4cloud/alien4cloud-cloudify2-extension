package alien4cloud.paas.cloudify2.events;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.GridServiceContainerOptions;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.zone.Zone;

public class GigaSpacesPUDeployer {

    private static final String CONTEXT_PROPERTY_APPLICATION_NAME = "com.gs.application";
    private static final String CDFY_USERNAME = "cdfy.username";
    private static final String CDFY_PASSWORD = "cdfy.password";
    private static final String DEFAULT_LOCATORS = "localhost:4174";
    private static final String DEFAULT_ZONE = "events";

    private static final Options COMMAND_LINE_OPTIONS = new Options();

    private static final String HELP_OPTION = "h";
    private static final String LOCATORS_OPTION = "locators";
    private static final String PU_PATH_OPTION = "pu";
    private static final String DEPLOYMENT_NAME_OPTION = "name";
    private static final String CDFY_USERNAME_OPTION = "username";
    private static final String CDFY_PASSWORD_OPTION = "password";

    static {
        COMMAND_LINE_OPTIONS.addOption(HELP_OPTION, false, "Print help");
        COMMAND_LINE_OPTIONS.addOption(DEPLOYMENT_NAME_OPTION, true, "The deployment name");
        COMMAND_LINE_OPTIONS.addOption(LOCATORS_OPTION, true, "GigaSpaces locators");
        COMMAND_LINE_OPTIONS.addOption(PU_PATH_OPTION, true, "Path to the process unit to deploy");
        COMMAND_LINE_OPTIONS.addOption(CDFY_USERNAME_OPTION, true, "Username to usr for cloudify connexion");
        COMMAND_LINE_OPTIONS.addOption(CDFY_PASSWORD_OPTION, true, "Password to usr for cloudify connexion");
    }

    // Wait for each action 2 minutes
    public static final long TIME_OUT = 2L;
    public static final TimeUnit TIME_UNIT = TimeUnit.MINUTES;

    public static void main(String[] args) throws InterruptedException, IOException {
        String name = null;
        String locators = null;
        File processingUnit = null;
        String username = null;
        String password = null;

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

            if (!cmd.hasOption(PU_PATH_OPTION)) {
                printUsage();
                quitWithError("Option -" + PU_PATH_OPTION + " is mandatory : The path to the processing unit must be specified");
            } else {
                String puPath = cmd.getOptionValue(PU_PATH_OPTION);
                processingUnit = new File(puPath);
                if (!processingUnit.exists()) {
                    printUsage();
                    quitWithError("The file '" + processingUnit.getAbsolutePath() + "' does not exists");
                    System.exit(1);
                }
            }
            username = cmd.getOptionValue(CDFY_USERNAME_OPTION);
            password = cmd.getOptionValue(CDFY_PASSWORD_OPTION);

        } catch (ParseException e) {
            quitWithError(e);
        }
        deploy(name, locators, processingUnit, username, password);
    }

    private static void quitWithError(String message) {
        System.err.println(message);
        System.exit(1);
    }

    private static void quitWithError(Exception e) {
        quitWithError("Exception happened : [" + e.getMessage() + "], see log for more details");
    }

    private static void printUsage() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("gsDeploy [-h] [-locators {host:port,host:port,..}] [-pu path_to_the_pu_file]", COMMAND_LINE_OPTIONS);
    }

    private static boolean isLocalAgent(String agentIp) throws IOException {
        Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
        while (e.hasMoreElements()) {
            NetworkInterface n = (NetworkInterface) e.nextElement();
            Enumeration<InetAddress> ee = n.getInetAddresses();
            while (ee.hasMoreElements()) {
                InetAddress i = (InetAddress) ee.nextElement();
                // System.out.println("is agent <" + agentIp + "> on <" + i.getHostAddress() + ">");
                if (i.getHostAddress().equals(agentIp)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int getPlannedNumberOfInstances(Admin admin) {
        // Wait for management space PU
        ProcessingUnit cloudifyManagementSpacePU = admin.getProcessingUnits().waitFor("cloudifyManagementSpace", TIME_OUT, TIME_UNIT);
        if (cloudifyManagementSpacePU == null) {
            quitWithError("ERROR: \t Could not retrieve cloudifyManagementSpace PU after " + TIME_OUT + " " + TIME_UNIT);
        }
        return cloudifyManagementSpacePU.getPlannedNumberOfInstances();
    }

    private static void ensureGSCsForEventPU(Admin admin, int plannedInstancesNumberOfEventsPU, String cdfyUsername, String cdfyPassword) throws IOException {
        // Wait for the same number of agent as the planned number of instances
        if (!admin.getGridServiceAgents().waitFor(plannedInstancesNumberOfEventsPU, TIME_OUT, TIME_UNIT)) {
            System.out.println("ERROR: \t Could not have " + plannedInstancesNumberOfEventsPU + " GSAs after " + TIME_OUT + " " + TIME_UNIT);
        }
        GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
        for (GridServiceAgent gsa : agents) {
            if (isLocalAgent(gsa.getMachine().getHostAddress())) {
                startEventGSC(gsa, cdfyUsername, cdfyPassword);
            }
        }
    }

    public static void deploy(Admin admin, String name, File processingUnit, String cdfyUsername, String cdfyPassword) throws InterruptedException, IOException {
        // Wait for at least 1 manager in order to be able to query for processing unit
        GridServiceManager gsm = admin.getGridServiceManagers().waitForAtLeastOne(TIME_OUT, TIME_UNIT);
        if (gsm == null) {
            quitWithError("ERROR: \t Could not get GSM for deployment after " + TIME_OUT + " " + TIME_UNIT);
        }
        int plannedInstancesNumberOfEventsPU = getPlannedNumberOfInstances(admin);
        ensureGSCsForEventPU(admin, plannedInstancesNumberOfEventsPU, cdfyUsername, cdfyPassword);
        if (isDeployed(admin, name)) {
            System.out.println("INFO: \t PU " + name + " already deployed. Skipping...");
            return;
        }
        ProcessingUnitDeployment deployment = new ProcessingUnitDeployment(processingUnit);
        deployment.name(name);
        deployment.setContextProperty(CONTEXT_PROPERTY_APPLICATION_NAME, "management");
        deployment.addZone(DEFAULT_ZONE);
        deployment.numberOfInstances(plannedInstancesNumberOfEventsPU);
        gsm.deploy(deployment);
    }

    public static void deploy(String name, String locators, File processingUnit, String cdfyUsername, String cdfyPassword) throws InterruptedException,
            IOException {
        Admin admin = null;
        try {
            admin = createAdmin(locators);
            deploy(admin, name, processingUnit, cdfyUsername, cdfyPassword);
        } finally {
            if (admin != null) {
                admin.close();
            }
        }
    }

    public static void undeploy(Admin admin, String name) {
        ProcessingUnit eventPU = admin.getProcessingUnits().waitFor(name, TIME_OUT, TIME_UNIT);
        if (eventPU == null) {
            quitWithError("ERROR: \t Could not retrieve event PU after " + TIME_OUT + " " + TIME_UNIT);
        }
        eventPU.undeployAndWait(TIME_OUT, TIME_UNIT);
        int plannedInstancesNumberOfEventsPU = getPlannedNumberOfInstances(admin);
        Zone zone = admin.getZones().waitFor(DEFAULT_ZONE, 5, TimeUnit.SECONDS);
        if (zone != null && zone.getGridServiceContainers().waitFor(1, 5, TimeUnit.SECONDS)) {
            zone.getGridServiceContainers().waitFor(plannedInstancesNumberOfEventsPU, 5, TimeUnit.SECONDS);
            for (GridServiceContainer container : zone.getGridServiceContainers().getContainers()) {
                container.kill();
            }
        }
    }

    public static void undeploy(String name, String locators) {
        Admin admin = null;
        try {
            admin = createAdmin(locators);
            undeploy(admin, name);
        } finally {
            if (admin != null) {
                admin.close();
            }
        }
    }

    public static Admin createAdmin(String locators) {
        AdminFactory adminFactory = new AdminFactory();
        adminFactory.useGsLogging(false);
        adminFactory.addLocators(locators);
        return adminFactory.create();
    }

    private static void startEventGSC(GridServiceAgent agent, String cdfyUsername, String cdfyPassword) throws IOException {
        System.out.println("Start a GSC for events on <" + agent.getMachine().getHostAddress() + ">");
        GridServiceContainerOptions options = new GridServiceContainerOptions();
        options.vmInputArgument("-Xmx128m");
        options.vmInputArgument("-Xms128m");
        options.vmInputArgument("-Dcom.gs.zones=" + DEFAULT_ZONE);
        options.vmInputArgument("-Dcom.gs.transport_protocol.lrmi.bind-port=7010-7110");
        if (cdfyUsername != null) {
            options.vmInputArgument("-D" + CDFY_USERNAME + "=" + cdfyUsername.trim());
        }
        if (cdfyPassword != null) {
            options.vmInputArgument("-D" + CDFY_PASSWORD + "=" + cdfyPassword.trim());
        }
        agent.startGridService(options);
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
}
