package fr.fastconnect.events.rest;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.j_spaces.core.client.SQLQuery;

@Controller
public class CloudifyStateController {

    private static final long DEFAULT_LEASE = 1000l * 60l * 60l; // 60 MINUTES

    private static final byte[] MUTEX = new byte[0];

    @Autowired
    private GigaSpace gigaSpace;

    @RequestMapping(value = "/test", method = RequestMethod.GET)
    @ResponseBody
    public String test() {
        return "is running";
    }

    // @PostConstruct
    // public void afterPropertiesSet() {
    // gigaSpace.getTypeManager().registerTypeDescriptor(CloudifyEvent.class);
    // }

    @RequestMapping(value = "/getEvents", method = RequestMethod.GET)
    @ResponseBody
    public CloudifyEvent[] getEvents(
            @RequestParam(required = true) String application,
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String instanceId,
            @RequestParam(required = false) Integer lastIndex) {

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

        SQLQuery<CloudifyEvent> template = new SQLQuery<CloudifyEvent>(CloudifyEvent.class, sb.toString());
        CloudifyEvent[] read = gigaSpace.readMultiple(template);
        return read;
    }

    @RequestMapping(value = "/putEvent", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void putEvent(
            @RequestParam(required = true) String application,
            @RequestParam(required = true) String service,
            @RequestParam(required = true) String instanceId,
            @RequestParam(required = true) String event) {
        synchronized (MUTEX) {
            SQLQuery<CloudifyEvent> template = new SQLQuery<CloudifyEvent>(CloudifyEvent.class,
                    String.format("applicationName='%s' and serviceName='%s' and instanceId='%s' ORDER BY eventIndex DESC", application, service, instanceId));
            CloudifyEvent[] readMultiple = gigaSpace.readMultiple(template, 1);

            int lastIndex = 0;
            if (readMultiple != null && readMultiple.length > 0) {
                lastIndex = readMultiple[0].getEventIndex();
            }
            CloudifyEvent entry = new CloudifyEvent(application, service, event);
            entry.setEventIndex(lastIndex + 1);
            // entry.setDeploymentId(deploymentId);
            entry.setInstanceId(instanceId);
            entry.setDateTimestamp(new Date());
            gigaSpace.write(entry, DEFAULT_LEASE);
        }
    }

    @RequestMapping(value = "/getAllEvents", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public CloudifyEvent[] getAllEvents() {
        SQLQuery<CloudifyEvent> template = new SQLQuery<CloudifyEvent>(CloudifyEvent.class, String.format("ORDER BY dateTimestamp, eventIndex"));
        CloudifyEvent[] read = gigaSpace.readMultiple(template);
        return read;
    }
}
