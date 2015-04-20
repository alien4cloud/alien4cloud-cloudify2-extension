package alien4cloud.paas.cloudify2.rest;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import alien4cloud.paas.cloudify2.events.AlienEvent;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-context.xml")
public class CloudifyStateControllerTest {

    private static final String APPLI_NAME = "applicationName";
    private static final String DEPLOYMENT_ID = UUID.randomUUID().toString();
    private static final String SERVICE_NAME = "serviceName";
    private static final String INSTANCE_ID = "1";

    @Autowired
    CloudifyStateController cloudifyStateController;

    @Autowired
    GigaSpace gigaSpace;

    @Before
    public void before() {
        String[] events = { "PRE_SERVICE_START", "PRE_INSTALL", "INSTALL", "POST_INSTALL" };

        int eventIndex = 1;

        for (String event : events) {
            AlienEvent entry = new AlienEvent();
            entry.setApplicationName(APPLI_NAME);
            entry.setDeploymentId(DEPLOYMENT_ID);
            entry.setEvent(event);
            entry.setServiceName(SERVICE_NAME);
            entry.setInstanceId(INSTANCE_ID);
            entry.setEventIndex(eventIndex++);
            entry.setDateTimestamp(new Date());
            gigaSpace.write(entry);
            try {
                Thread.sleep(10L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @After
    public void after() {
        gigaSpace.clear(new AlienEvent());
    }

    @Test
    public void testGetAllEvents() {
        AlienEvent[] allEvents = cloudifyStateController.getAllEvents();
        Assert.assertEquals(4, allEvents.length);

        Set<String> set = new HashSet<String>(4);
        for (AlienEvent events : allEvents) {
            set.add(events.getEvent());
        }
        Assert.assertTrue(set.contains("PRE_SERVICE_START"));
        Assert.assertTrue(set.contains("PRE_INSTALL"));
        Assert.assertTrue(set.contains("INSTALL"));
        Assert.assertTrue(set.contains("POST_INSTALL"));
    }

    @Test
    public void testGetEvents() throws Exception {
        // Test request with all parameters
        this.doTestGetEvents(DEPLOYMENT_ID, SERVICE_NAME, INSTANCE_ID, 0, Arrays.asList("PRE_SERVICE_START", "PRE_INSTALL", "INSTALL", "POST_INSTALL"));

        // Test request without specify the service
        this.doTestGetEvents(DEPLOYMENT_ID, null, INSTANCE_ID, 0, Arrays.asList("PRE_SERVICE_START", "PRE_INSTALL", "INSTALL", "POST_INSTALL"));

        // Test request with an eventIndex
        this.doTestGetEvents(DEPLOYMENT_ID, SERVICE_NAME, INSTANCE_ID, 3, Arrays.asList("INSTALL", "POST_INSTALL"));

        // Test request with an eventIndex without specifying the service
        this.doTestGetEvents(DEPLOYMENT_ID, null, INSTANCE_ID, 3, Arrays.asList("INSTALL", "POST_INSTALL"));

        // Test request with a wrong application name
        this.doTestGetEvents("unknownAppli", null, INSTANCE_ID, 0, null);

        // Test request with a wrong service name
        this.doTestGetEvents(DEPLOYMENT_ID, "unknownService", INSTANCE_ID, 0, null);
    }

    private void doTestGetEvents(String appliName, String serviceName, String instanceId, int index, List<String> expectedEvents) {

        AlienEvent[] allEvents = cloudifyStateController.getEvents(appliName, serviceName, instanceId, index);
        Assert.assertEquals(expectedEvents == null ? 0 : expectedEvents.size(), allEvents.length);
        if (expectedEvents != null && !expectedEvents.isEmpty()) {
            int currentIndex = index == 0 ? 1 : index;
            Set<String> set = new HashSet<String>();
            for (AlienEvent events : allEvents) {
                Assert.assertEquals("Wrong index order", new Integer(currentIndex++), events.getEventIndex());
                set.add(events.getEvent());
            }

            for (String event : expectedEvents) {
                if (!set.contains(event)) {
                    Assert.fail("Missing event " + event);
                }
            }
        }
    }

}
