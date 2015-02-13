package alien4cloud.paas.cloudify2.rest;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import alien4cloud.paas.cloudify2.events.AlienEvent;
import alien4cloud.paas.cloudify2.events.RelationshipOperationEvent;
import alien4cloud.paas.cloudify2.rest.CloudifyStateController;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-context.xml")
@Ignore
public class EventListenerTest {

    private static final String APPLI_NAME = "applicationName";
    private static final String SERVICE_NAME = "serviceName";
    private static final String INSTANCE_ID = "1";

    @Autowired
    CloudifyStateController cloudifyStateController;

    @Autowired
    GigaSpace gigaSpace;

    @Before
    public void before() {
        String[] events = { "add_source", "add_target", "remove_target" };

        int eventIndex = 1;

        for (String event : events) {
            RelationshipOperationEvent entry = new RelationshipOperationEvent();
            entry.setApplicationName(APPLI_NAME);
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
        RelationshipOperationEvent[] allEvents = cloudifyStateController.getAllRelationshipEvents();
        Assert.assertEquals(3, allEvents.length);

        Set<String> set = new HashSet<String>(4);
        for (RelationshipOperationEvent events : allEvents) {
            set.add(events.getEvent());
        }
        Assert.assertTrue(set.contains("add_target"));
        Assert.assertTrue(set.contains("add_source"));
        Assert.assertTrue(set.contains("remove_target"));

        for (RelationshipOperationEvent events : allEvents) {
            if (events.getEvent().equals("add_source") || events.getEvent().equals("add_target")) {
                Assert.assertTrue(events.isExecuted());
            }
        }
    }

}
