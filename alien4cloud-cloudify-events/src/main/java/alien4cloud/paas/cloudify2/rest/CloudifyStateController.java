package alien4cloud.paas.cloudify2.rest;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import alien4cloud.paas.cloudify2.events.AlienEvent;
import alien4cloud.paas.cloudify2.events.BlockStorageEvent;
import alien4cloud.paas.cloudify2.events.NodeInstanceState;
import alien4cloud.paas.cloudify2.events.RelationshipOperationEvent;

import com.j_spaces.core.client.SQLQuery;

@Controller
public class CloudifyStateController {

    private static final Logger LOGGER = Logger.getLogger(CloudifyStateController.class.getName());

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
        gigaSpace.getTypeManager().registerTypeDescriptor(RelationshipOperationEvent.class);
    }

    @RequestMapping(value = "/getEventsSince", method = RequestMethod.GET)
    @ResponseBody
    public AlienEvent[] getEventsSince(@RequestParam(required = true) long dateAsLong, @RequestParam(required = true) int maxEvents) {
        Date date = new Date(dateAsLong);
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest(String.format("Requesting getEventsSince(date=%s, maxEvents=%s)...", date, maxEvents));
        }
        SQLQuery<AlienEvent> template = new SQLQuery<AlienEvent>(AlienEvent.class, "dateTimestamp > ? ORDER BY dateTimestamp, eventIndex", date);
        return gigaSpace.readMultiple(template, maxEvents);
    }

    @RequestMapping(value = "/getEvents", method = RequestMethod.GET)
    @ResponseBody
    public AlienEvent[] getEvents(@RequestParam(required = true) String deploymentId, @RequestParam(required = false) String service,
            @RequestParam(required = false) String instanceId, @RequestParam(required = false) Integer lastIndex) {

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest(String.format("Requesting getEvents(deploymentId=%s, service=%s, instanceId=%s, lastIndex=%s)...", deploymentId, service, instanceId,
                    lastIndex));
        }

        StringBuilder sb = new StringBuilder();
        sb.append("deploymentId='").append(deploymentId).append("'");
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

    @RequestMapping(value = "/getInstanceStates", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public NodeInstanceState[] getInstanceStates(@RequestParam(required = true) String deploymentId) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest(String.format("Requesting getInstanceStates(deploymentId=%s)...", deploymentId));
        }
        NodeInstanceState template = new NodeInstanceState();
        template.setDeploymentId(deploymentId);
        return gigaSpace.readMultiple(template);
    }

    @RequestMapping(value = "/deleteInstanceStates", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String deleteInstanceStates(@RequestParam(required = true) String deploymentId) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest(String.format("Requesting deleteInstanceStates(deploymentId=%s)...", deploymentId));
        }
        NodeInstanceState template = new NodeInstanceState();
        template.setDeploymentId(deploymentId);
        ;
        gigaSpace.clear(template);
        return "ok";
    }

    @RequestMapping(value = "/deleteAllInstanceStates", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String deleteAllInstanceStates() {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest(String.format("Deleteting All InstanceStates..."));
        }
        NodeInstanceState template = new NodeInstanceState();
        gigaSpace.clear(template);
        return "ok";
    }

    @RequestMapping(value = "/deleteAllEvents", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String deleteAllEvents(@RequestParam(required = true) String deploymentId) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest(String.format("RequestingdeleteAllEvents(deploymentId=%s)...", deploymentId));
        }
        AlienEvent template = new AlienEvent();
        template.setDeploymentId(deploymentId);
        ;
        gigaSpace.clear(template);
        RelationshipOperationEvent template2 = new RelationshipOperationEvent();
        template2.setDeploymentId(deploymentId);
        gigaSpace.clear(template2);
        return "ok";
    }

    @RequestMapping(value = "/getLatestEvent", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public AlienEvent getLatestEvent(@RequestParam(required = true) String deploymentId, @RequestParam(required = true) String service,
            @RequestParam(required = true) String instanceId) {

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest(String.format("Requesting getLatestEvent(deploymentId=%s, service=%s, instanceId=%s)...", deploymentId, service, instanceId));
        }

        SQLQuery<AlienEvent> template = new SQLQuery<AlienEvent>(AlienEvent.class, String.format(
                "deploymentId='%s' and serviceName='%s' and instanceId='%s' ORDER BY eventIndex DESC", deploymentId, service, instanceId));
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

    @RequestMapping(value = "/getAllRelEvents", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public RelationshipOperationEvent[] getAllRelationshipEvents() {

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("Requesting getAllRelationsihpEvents()...");
        }

        SQLQuery<RelationshipOperationEvent> template = new SQLQuery<RelationshipOperationEvent>(RelationshipOperationEvent.class,
                String.format("ORDER BY dateTimestamp, eventIndex"));
        RelationshipOperationEvent[] read = gigaSpace.readMultiple(template);
        return read;
    }

    @RequestMapping(value = "/getRelEvents", method = RequestMethod.GET)
    @ResponseBody
    public RelationshipOperationEvent[] getRelEvents(@RequestParam(required = true) String deploymentId, @RequestParam(required = false) String service,
            @RequestParam(required = false) String instanceId, @RequestParam(required = false) Integer lastIndex) {

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest(String.format("Requesting getRelEvents(deploymentId=%s, service=%s, instanceId=%s, lastIndex=%s)...", deploymentId, service,
                    instanceId, lastIndex));
        }

        StringBuilder sb = new StringBuilder();
        sb.append("deploymentId='").append(deploymentId).append("'");
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

        SQLQuery<RelationshipOperationEvent> template = new SQLQuery<RelationshipOperationEvent>(RelationshipOperationEvent.class, sb.toString());
        return gigaSpace.readMultiple(template);
    }

    @RequestMapping(value = "/putInstanceStates", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public void putNodeInstanceState(@RequestBody NodeInstanceState[] nodeInstanceState) {
        if (ArrayUtils.isNotEmpty(nodeInstanceState)) {
            gigaSpace.writeMultiple(nodeInstanceState);
        }
    }
}
