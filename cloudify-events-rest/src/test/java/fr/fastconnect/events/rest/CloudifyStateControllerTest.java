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
    private static final String VOLUME_ID = "abcde";

    @Autowired
    CloudifyStateController cloudifyStateController = new CloudifyStateController();

    @Autowired
    GigaSpace gigaSpace;

    @Before
    public void before() {
        String[] events = {
                "PRE_SERVICE_START"
                , "PRE_INSTALL"
                , "INSTALL"
                , "POST_INSTALL"
        };

        int eventIndex = 1;

        for (String event : events) {
            AlienEvent entry = new AlienEvent(APPLI_NAME, SERVICE_NAME, event);
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
        this.doTestGetEvents(APPLI_NAME, SERVICE_NAME, INSTANCE_ID, 0
                , Arrays.asList("PRE_SERVICE_START"
                        , "PRE_INSTALL"
                        , "INSTALL"
                        , "POST_INSTALL"));

        // Test request without specify the service
        this.doTestGetEvents(APPLI_NAME, null, INSTANCE_ID, 0
                , Arrays.asList("PRE_SERVICE_START"
                        , "PRE_INSTALL"
                        , "INSTALL"
                        , "POST_INSTALL"));

        // Test request with an eventIndex
        this.doTestGetEvents(APPLI_NAME, SERVICE_NAME, INSTANCE_ID, 3
                , Arrays.asList("INSTALL"
                        , "POST_INSTALL"));

        // Test request with an eventIndex without specifying the service
        this.doTestGetEvents(APPLI_NAME, null, INSTANCE_ID, 3
                , Arrays.asList("INSTALL"
                        , "POST_INSTALL"));

        // Test request with a wrong application name
        this.doTestGetEvents("unknownAppli", null, INSTANCE_ID, 0, null);

        // Test request with a wrong service name
        this.doTestGetEvents(APPLI_NAME, "unknownService", INSTANCE_ID, 0, null);
    }

    @Test
    public void testPutEvent() throws Exception {

        // Test put request
        cloudifyStateController.putEvent(APPLI_NAME, SERVICE_NAME, INSTANCE_ID, "PRE_START");
        cloudifyStateController.putEvent(APPLI_NAME, SERVICE_NAME, INSTANCE_ID, "START");
        this.doTestGetEvents(APPLI_NAME, SERVICE_NAME, INSTANCE_ID, 0, Arrays.asList(
                "PRE_SERVICE_START"
                , "PRE_INSTALL"
                , "INSTALL"
                , "POST_INSTALL"
                , "PRE_START"
                , "START"
                ));

        // Test with new application
        String newApplication = "newApplication";
        cloudifyStateController.putEvent(newApplication, SERVICE_NAME, INSTANCE_ID, "PRE_SERVICE_START");
        cloudifyStateController.putEvent(newApplication, SERVICE_NAME, INSTANCE_ID, "PRE_INSTALL");
        this.doTestGetEvents(newApplication, SERVICE_NAME, INSTANCE_ID, 0, Arrays.asList(
                "PRE_SERVICE_START"
                , "PRE_INSTALL"
                ));

        // Test with new service
        String newService = "newService";
        cloudifyStateController.putEvent(APPLI_NAME, newService, INSTANCE_ID, "PRE_SERVICE_START");
        cloudifyStateController.putEvent(APPLI_NAME, newService, INSTANCE_ID, "PRE_INSTALL");
        this.doTestGetEvents(APPLI_NAME, newService, INSTANCE_ID, 0, Arrays.asList(
                "PRE_SERVICE_START"
                , "PRE_INSTALL"
                ));

        // Test with new instance
        // /!\ The event index
        cloudifyStateController.putEvent(APPLI_NAME, SERVICE_NAME, "2", "PRE_SERVICE_START");
        cloudifyStateController.putEvent(APPLI_NAME, SERVICE_NAME, "2", "PRE_INSTALL");
        this.doTestGetEvents(APPLI_NAME, SERVICE_NAME, "2", 0, Arrays.asList(
                "PRE_SERVICE_START"
                , "PRE_INSTALL"
                ));
    }

    @Test
    public void testPostEvent() throws Exception {

        BlockStorageEvent bsEvent = new BlockStorageEvent();
        bsEvent.setEvent("FORMATING");
        bsEvent.setVolumeId(VOLUME_ID);

        // Test post request
        cloudifyStateController.postEvent(APPLI_NAME, SERVICE_NAME, INSTANCE_ID, EventType.INSTANCE_STATE, "PRE_START");
        cloudifyStateController.postEvent(APPLI_NAME, SERVICE_NAME, INSTANCE_ID, null, "START");
        cloudifyStateController.postEvent(APPLI_NAME, SERVICE_NAME, INSTANCE_ID, EventType.BLOCKSTORAGE, bsEvent);
        this.doTestGetEvents(APPLI_NAME, SERVICE_NAME, INSTANCE_ID, 0, Arrays.asList(
                "PRE_SERVICE_START"
                , "PRE_INSTALL"
                , "INSTALL"
                , "POST_INSTALL"
                , "PRE_START"
                , "START"
                , "FORMATING"
                ));

        // Test with new application
        String newApplication = "newApplication";
        cloudifyStateController.postEvent(newApplication, SERVICE_NAME, INSTANCE_ID, null, "PRE_INSTALL");
        cloudifyStateController.postEvent(newApplication, SERVICE_NAME, INSTANCE_ID, EventType.BLOCKSTORAGE, bsEvent);
        this.doTestGetEvents(newApplication, SERVICE_NAME, INSTANCE_ID, 0, Arrays.asList(
                "PRE_INSTALL"
                , "FORMATING"
                ));

        // Test with new service
        String newService = "newService";
        cloudifyStateController.postEvent(APPLI_NAME, newService, INSTANCE_ID, null, "PRE_SERVICE_START");
        cloudifyStateController.postEvent(APPLI_NAME, newService, INSTANCE_ID, EventType.BLOCKSTORAGE, bsEvent);
        this.doTestGetEvents(APPLI_NAME, newService, INSTANCE_ID, 0, Arrays.asList(
                "PRE_SERVICE_START"
                , "FORMATING"
                ));

        // Test with new instance
        // /!\ The event index
        cloudifyStateController.postEvent(APPLI_NAME, SERVICE_NAME, "2", EventType.INSTANCE_STATE, "PRE_SERVICE_START");
        cloudifyStateController.postEvent(APPLI_NAME, SERVICE_NAME, "2", EventType.BLOCKSTORAGE, bsEvent);
        this.doTestGetEvents(APPLI_NAME, SERVICE_NAME, "2", 0, Arrays.asList(
                "PRE_SERVICE_START"
                , "FORMATING"
                ));

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
