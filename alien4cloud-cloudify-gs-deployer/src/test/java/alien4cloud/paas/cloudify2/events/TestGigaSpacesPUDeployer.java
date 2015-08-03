package alien4cloud.paas.cloudify2.events;

import java.io.IOException;

import org.openspaces.admin.Admin;

public class TestGigaSpacesPUDeployer {

    public static void main(String[] args) throws InterruptedException, IOException {
        Admin admin = null;
        try {
            admin = GigaSpacesPUDeployer.createAdmin("129.185.67.65");
            GigaSpacesPUDeployer.undeploy(admin, "event");
        } finally {
            if (admin != null)
                admin.close();
        }
    }
}
