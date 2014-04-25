package fr.fastconnect.events.rest;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-context.xml")
public class CloudifyStateControllerTest {

    private static final String APPLI_NAME = "applicationName";
    private static final String SERVICE_NAME = "serviceName";
    private static final String INSTANCE_ID = "1";

    @Autowired
    CloudifyStateController cloudifyStateController = new CloudifyStateController();

    @Autowired
    GigaSpace gigaSpace;

    @Before
    public void before() {
        LifecycleEvents[] events = {
                LifecycleEvents.PRE_SERVICE_START
                , LifecycleEvents.PRE_INSTALL
                , LifecycleEvents.INSTALL
                , LifecycleEvents.POST_INSTALL
        };

        int eventIndex = 1;

        for (LifecycleEvents event : events) {
            CloudifyEvent entry = new CloudifyEvent(APPLI_NAME, SERVICE_NAME, event);
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
        gigaSpace.clear(new CloudifyEvent());
    }

    @Test
    public void testGetAllEvents() {
        CloudifyEvent[] allEvents = cloudifyStateController.getAllEvents();
        Assert.assertEquals(4, allEvents.length);

        Set<LifecycleEvents> set = new HashSet<LifecycleEvents>(4);
        for (CloudifyEvent events : allEvents) {
            set.add(events.getEvent());
        }
        Assert.assertTrue(set.contains(LifecycleEvents.PRE_SERVICE_START));
        Assert.assertTrue(set.contains(LifecycleEvents.PRE_INSTALL));
        Assert.assertTrue(set.contains(LifecycleEvents.INSTALL));
        Assert.assertTrue(set.contains(LifecycleEvents.POST_INSTALL));
    }

    @Test
    public void testGetEvents() throws Exception {
        // Test request with all parameters
        this.doTestGetEvents(APPLI_NAME, SERVICE_NAME, INSTANCE_ID, 0
                , Arrays.asList(LifecycleEvents.PRE_SERVICE_START
                        , LifecycleEvents.PRE_INSTALL
                        , LifecycleEvents.INSTALL
                        , LifecycleEvents.POST_INSTALL));

        // Test request without specify the service
        this.doTestGetEvents(APPLI_NAME, null, INSTANCE_ID, 0
                , Arrays.asList(LifecycleEvents.PRE_SERVICE_START
                        , LifecycleEvents.PRE_INSTALL
                        , LifecycleEvents.INSTALL
                        , LifecycleEvents.POST_INSTALL));

        // Test request with an eventIndex
        this.doTestGetEvents(APPLI_NAME, SERVICE_NAME, INSTANCE_ID, 3
                , Arrays.asList(LifecycleEvents.INSTALL
                        , LifecycleEvents.POST_INSTALL));

        // Test request with an eventIndex without specifying the service
        this.doTestGetEvents(APPLI_NAME, null, INSTANCE_ID, 3
                , Arrays.asList(LifecycleEvents.INSTALL
                        , LifecycleEvents.POST_INSTALL));

        // Test request with a wrong application name
        this.doTestGetEvents("unknownAppli", null, INSTANCE_ID, 0, null);

        // Test request with a wrong service name
        this.doTestGetEvents(APPLI_NAME, "unknownService", INSTANCE_ID, 0, null);
    }

    @Test
    public void testPutEvent() throws Exception {

        // Test put request
        cloudifyStateController.putEvent(APPLI_NAME, SERVICE_NAME, INSTANCE_ID, LifecycleEvents.PRE_START);
        cloudifyStateController.putEvent(APPLI_NAME, SERVICE_NAME, INSTANCE_ID, LifecycleEvents.START);
        this.doTestGetEvents(APPLI_NAME, SERVICE_NAME, INSTANCE_ID, 0, Arrays.asList(
                LifecycleEvents.PRE_SERVICE_START
                , LifecycleEvents.PRE_INSTALL
                , LifecycleEvents.INSTALL
                , LifecycleEvents.POST_INSTALL
                , LifecycleEvents.PRE_START
                , LifecycleEvents.START
                ));

        // Test with new application
        String newApplication = "newApplication";
        cloudifyStateController.putEvent(newApplication, SERVICE_NAME, INSTANCE_ID, LifecycleEvents.PRE_SERVICE_START);
        cloudifyStateController.putEvent(newApplication, SERVICE_NAME, INSTANCE_ID, LifecycleEvents.PRE_INSTALL);
        this.doTestGetEvents(newApplication, SERVICE_NAME, INSTANCE_ID, 0, Arrays.asList(
                LifecycleEvents.PRE_SERVICE_START
                , LifecycleEvents.PRE_INSTALL
                ));

        // Test with new service
        String newService = "newService";
        cloudifyStateController.putEvent(APPLI_NAME, newService, INSTANCE_ID, LifecycleEvents.PRE_SERVICE_START);
        cloudifyStateController.putEvent(APPLI_NAME, newService, INSTANCE_ID, LifecycleEvents.PRE_INSTALL);
        this.doTestGetEvents(APPLI_NAME, newService, INSTANCE_ID, 0, Arrays.asList(
                LifecycleEvents.PRE_SERVICE_START
                , LifecycleEvents.PRE_INSTALL
                ));

        // Test with new instance
        // /!\ The event index
        cloudifyStateController.putEvent(APPLI_NAME, SERVICE_NAME, "2", LifecycleEvents.PRE_SERVICE_START);
        cloudifyStateController.putEvent(APPLI_NAME, SERVICE_NAME, "2", LifecycleEvents.PRE_INSTALL);
        this.doTestGetEvents(APPLI_NAME, SERVICE_NAME, "2", 0, Arrays.asList(
                LifecycleEvents.PRE_SERVICE_START
                , LifecycleEvents.PRE_INSTALL
                // , LifecycleEvents.INSTALL
                // , LifecycleEvents.POST_INSTALL
                // , LifecycleEvents.PRE_START
                // , LifecycleEvents.START
                // , LifecycleEvents.PRE_SERVICE_START
                // , LifecycleEvents.PRE_INSTALL
                ));
    }

    private void doTestGetEvents(String appliName, String serviceName, String instanceId, int index, List<LifecycleEvents> expectedEvents) {

        CloudifyEvent[] allEvents = cloudifyStateController.getEvents(appliName, serviceName, instanceId, index);
        Assert.assertEquals(expectedEvents == null ? 0 : expectedEvents.size(), allEvents.length);

        if (expectedEvents != null && !expectedEvents.isEmpty()) {
            int currentIndex = index == 0 ? 1 : index;
            Set<LifecycleEvents> set = new HashSet<LifecycleEvents>(4);
            for (CloudifyEvent events : allEvents) {
                Assert.assertEquals("Wrong index order", new Integer(currentIndex++), events.getEventIndex());
                set.add(events.getEvent());
            }

            for (LifecycleEvents event : expectedEvents) {
                if (!set.contains(event)) {
                    Assert.fail("Missing event " + event);
                }
            }
        }
    }

}
