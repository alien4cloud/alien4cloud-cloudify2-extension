package alien4cloud.paas.cloudify2.events;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.pu.ProcessingUnit;

public class TestGigaSpacesPUDeployer {

    public static void main(String[] args) throws InterruptedException, IOException {
        AdminFactory adminFactory = new AdminFactory();
        adminFactory.useGsLogging(false);
        adminFactory.addLocators("129.185.67.65");
        Admin admin = null;
        try {
            admin = adminFactory.create();
            // ProcessingUnit eventPU = admin.getProcessingUnits().waitFor("events");
            // System.out.println("Events PU have " + eventPU.getPlannedNumberOfInstances() + " instances");
            // System.out.println("Events PU have " + eventPU.getPlannedNumberOfPartitions() + " partitions");
            // eventPU.undeployAndWait();

            long timeout = 30L;
            // Wait for management space PU
            ProcessingUnit cloudifyManagementSpacePU = admin.getProcessingUnits().waitFor("cloudifyManagementSpace", timeout, TimeUnit.MINUTES);
            if (cloudifyManagementSpacePU == null) {
                System.out.println("ERROR: \t Could not retrieve cloudifyManagementSpace PU after " + timeout + " minutes");
                return;
            }
            int plannedInstancesNumberOfEventsPU = cloudifyManagementSpacePU.getPlannedNumberOfInstances();
            // Wait for the same number of agent as the planned number of instances
            if (!admin.getGridServiceAgents().waitFor(plannedInstancesNumberOfEventsPU, timeout, TimeUnit.MINUTES)) {
                System.out.println("ERROR: \t Could not have " + plannedInstancesNumberOfEventsPU + " GSAs after " + timeout + " minutes");
            }
            GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
            for (GridServiceAgent gsa : agents) {
                System.out.println("Agent found on " + gsa.getMachine().getHostAddress());
            }
        } finally {
            if (admin != null)
                admin.close();
        }
    }
}
