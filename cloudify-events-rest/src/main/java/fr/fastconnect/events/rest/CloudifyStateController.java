package fr.fastconnect.events.rest;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.j_spaces.core.client.SQLQuery;

@Controller
public class CloudifyStateController {

    private static final Logger LOGGER = Logger.getLogger(CloudifyStateController.class.getName());

    private static final long DEFAULT_LEASE = 1000l * 60l * 60l; // 60 MINUTES

    private static final byte[] MUTEX = new byte[0];

    private ObjectMapper serializer = new ObjectMapper();

    @Autowired
    private GigaSpace gigaSpace;

    @RequestMapping(value = "/test", method = RequestMethod.GET)
    @ResponseBody
    public String test() {
        return "is running";
    }

    @PostConstruct
    public void afterPropertiesSet() {
        gigaSpace.getTypeManager().registerTypeDescriptor(AlienEvent.class);
        gigaSpace.getTypeManager().registerTypeDescriptor(BlockStorageEvent.class);
        gigaSpace.getTypeManager().registerTypeDescriptor(NodeInstanceState.class);
    }

    @RequestMapping(value = "/getEventsSince", method = RequestMethod.GET)
    @ResponseBody
    public AlienEvent[] getEventsSince(@RequestParam(required = true) long dateAsLong, @RequestParam(required = true) int maxEvents) {
        Date date = new Date(dateAsLong);
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest(String.format("Requesting getEventsSince(date=%s, maxEvents=%s)...", date, maxEvents));
        }
        SQLQuery<AlienEvent> template = new SQLQuery<AlienEvent>(AlienEvent.class, "dateTimestamp > ?", date);
        return gigaSpace.readMultiple(template, maxEvents);
    }

    @RequestMapping(value = "/getEvents", method = RequestMethod.GET)
    @ResponseBody
    public AlienEvent[] getEvents(@RequestParam(required = true) String application, @RequestParam(required = false) String service,
            @RequestParam(required = false) String instanceId, @RequestParam(required = false) Integer lastIndex) {

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest(String.format("Requesting getEvents(application=%s, service=%s, instanceId=%s, lastIndex=%s)...", application, service, instanceId,
                    lastIndex));
        }

        StringBuilder sb = new StringBuilder();
        sb.append("applicationName='").append(application).append("'");
        if (StringUtils.isNotEmpty(service)) {
            sb.append(" and serviceName='").append(service).append("'");
        }

        if (StringUtils.isNotEmpty(instanceId)) {
            sb.append(" and instanceId='").append(instanceId).append("'");
        }

        if (lastIndex != null) {
            sb.append(" and eventIndex >= ").append(lastIndex == null ? 0 : lastIndex);
        }
        sb.append(" ORDER BY dateTimestamp, eventIndex");

        SQLQuery<AlienEvent> template = new SQLQuery<AlienEvent>(AlienEvent.class, sb.toString());
        return gigaSpace.readMultiple(template);
    }

    @RequestMapping(value = "/putEvent", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void putEvent(@RequestParam(required = true) String application, @RequestParam(required = true) String service,
            @RequestParam(required = true) String instanceId, @RequestParam(required = true) String event) {
        synchronized (MUTEX) {

            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(String.format("Requesting putEvent(application=%s, service=%s, instanceId=%s, event=%s)...", application, service, instanceId,
                        event));
            }

            AlienEvent latestEvent = this.getLatestEvent(application, service, instanceId);
            int lastIndex = latestEvent == null ? 0 : latestEvent.getEventIndex();
            AlienEvent entry = new AlienEvent(application, service, event);
            entry.setEventIndex(lastIndex + 1);
            // entry.setDeploymentId(deploymentId);
            entry.setInstanceId(instanceId);
            entry.setDateTimestamp(new Date());
            gigaSpace.write(entry, DEFAULT_LEASE);

            NodeInstanceState instanceState = new NodeInstanceState();
            instanceState.setTopologyId(application);
            instanceState.setNodeTemplateId(service);
            instanceState.setInstanceState(event);
            instanceState.setInstanceId(instanceId);
            gigaSpace.write(instanceState);
        }
    }

    @RequestMapping(value = "/postEvent", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void postEvent(@RequestParam(required = true) String application, @RequestParam(required = true) String service,
            @RequestParam(required = true) String instanceId, @RequestParam(required = false) EventType eventType, @RequestBody(required = true) Object event) {
        synchronized (MUTEX) {

            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(String.format("Requesting postEvent(application=%s, service=%s, instanceId=%s, eventType=%s, event=%s)...", application, service, instanceId,
                        eventType, event));
            }
            AlienEvent formatedEvent;
            EventType type = eventType == null ? EventType.INSTANCE_STATE : eventType;
            try {
                formatedEvent = getFormatedEvent(application, service, instanceId, type, event);
            } catch (IOException e) {
                return;
            }
            if (formatedEvent == null) {
                return;
            }
            gigaSpace.write(formatedEvent, DEFAULT_LEASE);

            if (EventType.INSTANCE_STATE.equals(type)) {
                NodeInstanceState instanceState = new NodeInstanceState();
                instanceState.setTopologyId(application);
                instanceState.setNodeTemplateId(service);
                instanceState.setInstanceState(formatedEvent.getEvent());
                instanceState.setInstanceId(instanceId);
                gigaSpace.write(instanceState);
            }
        }
    }

    private AlienEvent getFormatedEvent(String applicationName, String serviceName, String instanceId, EventType eventType, Object event) throws IOException {
        String eventSTring;
        if (event instanceof String) {
            eventSTring = (String) event;
        } else {
            eventSTring =  serializer.writeValueAsString(event);
        }
        AlienEvent finalEvent;
        switch (eventType) {
        case BLOCKSTORAGE:
            finalEvent = serializer.readValue(eventSTring, BlockStorageEvent.class);
            break;
        case INSTANCE_STATE:
            finalEvent = new AlienEvent();
            finalEvent.setEvent(eventSTring);
            break;
        default:
            return null;
        }

        AlienEvent latestEvent = this.getLatestEvent(applicationName, serviceName, instanceId);
        int lastIndex = latestEvent == null ? 0 : latestEvent.getEventIndex();
        finalEvent.setEventIndex(lastIndex + 1);
        finalEvent.setApplicationName(applicationName);
        finalEvent.setServiceName(serviceName);
        finalEvent.setInstanceId(instanceId);
        finalEvent.setDateTimestamp(new Date());
        return finalEvent;
    }

    @RequestMapping(value = "/getInstanceStates", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public NodeInstanceState[] getInstanceStates(@RequestParam(required = true) String application) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest(String.format("Requesting getInstanceStates(application=%s)...", application));
        }
        NodeInstanceState template = new NodeInstanceState();
        template.setTopologyId(application);
        return gigaSpace.readMultiple(template);
    }

    @RequestMapping(value = "/deleteInstanceStates", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String deleteInstanceStates(@RequestParam(required = true) String application) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest(String.format("Requesting getInstanceStates(application=%s)...", application));
        }
        NodeInstanceState template = new NodeInstanceState();
        template.setTopologyId(application);
        gigaSpace.clear(template);
        return "ok";
    }

    @RequestMapping(value = "/getLatestEvent", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public AlienEvent getLatestEvent(@RequestParam(required = true) String application, @RequestParam(required = true) String service,
            @RequestParam(required = true) String instanceId) {

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest(String.format("Requesting getLatestEvent(application=%s, service=%s, instanceId=%s)...", application, service, instanceId));
        }

        SQLQuery<AlienEvent> template = new SQLQuery<AlienEvent>(AlienEvent.class, String.format(
                "applicationName='%s' and serviceName='%s' and instanceId='%s' ORDER BY eventIndex DESC", application, service, instanceId));
        AlienEvent[] readMultiple = gigaSpace.readMultiple(template, 1);

        if (readMultiple != null && readMultiple.length > 0) {
            return readMultiple[0];
        } else {
            return null;
        }
    }

    @RequestMapping(value = "/getAllEvents", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public AlienEvent[] getAllEvents() {

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("Requesting getAllEvents()...");
        }

        SQLQuery<AlienEvent> template = new SQLQuery<AlienEvent>(AlienEvent.class, String.format("ORDER BY dateTimestamp, eventIndex"));
        AlienEvent[] read = gigaSpace.readMultiple(template);
        return read;
    }
}
