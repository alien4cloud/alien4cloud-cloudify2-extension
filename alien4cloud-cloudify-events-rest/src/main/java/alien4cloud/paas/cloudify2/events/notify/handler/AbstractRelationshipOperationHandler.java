package alien4cloud.paas.cloudify2.events.notify.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import org.apache.commons.lang3.ArrayUtils;
import org.cloudifysource.dsl.rest.request.InvokeCustomCommandRequest;
import org.cloudifysource.dsl.rest.response.InvokeServiceCommandResponse;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.openspaces.core.GigaSpace;

import alien4cloud.paas.cloudify2.events.NodeInstanceState;
import alien4cloud.paas.cloudify2.events.RelationshipOperationEvent;
import alien4cloud.paas.cloudify2.events.Execption.EventHandlingException;
import alien4cloud.paas.cloudify2.events.notify.RestClientManager;

public abstract class AbstractRelationshipOperationHandler implements IEventHandler<RelationshipOperationEvent> {

    private final Logger log = getLogger();

    @Resource
    protected RestClientManager restClientManager;
    @Resource
    protected GigaSpace gigaSpace;

    protected Set<String> handledOperations = new HashSet<String>();

    private static Long DEFAULT_TIMEOUT_MILLIS = 60000L * 5L;

    /**
     * initialize the handler. Ex, initialize the handled operations
     */
    @PostConstruct
    public abstract void configureHandler();

    /**
     * Get the service on which to trigger the command
     *
     * @param event
     * @return
     */
    protected abstract RelationshipMember getTriggeredMember(RelationshipOperationEvent event);

    @Override
    public boolean canHandle(RelationshipOperationEvent event) {
        return handledOperations.contains(event.getEvent());
    }

    @Override
    public void eventHappened(RelationshipOperationEvent event) throws EventHandlingException {
        log.info("Handling event:" + event);
        RestClient restClient;
        RelationshipMember triggeredNodePair = getTriggeredMember(event);
        waitFor(event.getApplicationName(), triggeredNodePair.nodeId, triggeredNodePair.cdfyServiceName, "started", "available");
        try {
            restClient = restClientManager.getRestClient();
            InvokeCustomCommandRequest invokeRequest = new InvokeCustomCommandRequest();
            invokeRequest.setCommandName(event.getCommandName());
            List<String> params = new ArrayList<String>();
            params.add(event.getInstanceId());
            invokeRequest.setParameters(params);
            InvokeServiceCommandResponse invokeResponse = restClient.invokeServiceCommand(event.getApplicationName(), triggeredNodePair.cdfyServiceName,
                    invokeRequest);
            Map<String, String> success = new HashMap<String, String>();
            Map<String, String> failures = new HashMap<String, String>();
            RestUtils.parseServiceInvokeResponse(success, failures, invokeResponse.getInvocationResultPerInstance());
            log.info("Command result: \n\tSUCCESS: " + success + "\n\tFAILLURES: " + failures);
            if (failures.isEmpty()) {
                event.setSuccess(true);
            } else {
                log.warning("Errors when handling event " + event);
                log.warning("Errors on some instances when executing operation <" + event.getCommandName() + ">: " + failures);
                event.setSuccess(false);
            }
        } catch (RestClientException e) {
            event.setSuccess(false);
            String msg = "Fail to handle event " + event + ". \n" + e.getMessageFormattedText();
            log.severe(msg);
            throw new EventHandlingException(msg, e);
        } catch (IOException e) {
            event.setSuccess(false);
            String msg = "Fail to handle event " + event;
            log.severe(msg);
            throw new EventHandlingException(msg, e);
        }

    }

    /**
     * wait for a service (node template) to reach a certain state
     *
     * @param application
     * @param source
     * @param sourceService
     * @param state
     */
    protected void waitFor(String application, String source, String sourceService, String... states) {
        NodeInstanceState template = new NodeInstanceState();
        template.setTopologyId(application);
        template.setNodeTemplateId(source);
        Long timeout = System.currentTimeMillis() + DEFAULT_TIMEOUT_MILLIS;
        boolean continueToCheck = true;
        log.info("Checking for node <" + source + "> state. Expects one of <" + ArrayUtils.toString(states) + ">.");
        while (System.currentTimeMillis() < timeout && continueToCheck) {
            NodeInstanceState[] instanceStates = gigaSpace.readMultiple(template);
            if (instanceStates != null) {
                for (NodeInstanceState instanceState : instanceStates) {
                    if (!ArrayUtils.contains(states, instanceState.getInstanceState())) {
                        break;
                    }
                    continueToCheck = false;
                }
            }

            if (continueToCheck) {
                log.info("Waiting 1000millis for node <" + source + ">  to reach one of the states <" + ArrayUtils.toString(states) + ">...");
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    log.warning("Interrupted sleep");
                }
            }
        }

        if (continueToCheck) {
            String msg = "Fail to handle event. <" + source + "> node fails to reach one of the states <" + ArrayUtils.toString(states) + "> in time.";
            log.severe(msg);
            throw new EventHandlingException(msg);
        }
    }

    private Logger getLogger() {
        return Logger.getLogger(this.getClass().getName());
    };

    @NoArgsConstructor
    @AllArgsConstructor
    protected class RelationshipMember {
        String nodeId;
        String cdfyServiceName;
    }

}
