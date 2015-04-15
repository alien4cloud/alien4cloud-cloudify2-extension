package alien4cloud.paas.cloudify2.events.polling.handler;

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

import org.cloudifysource.dsl.rest.request.InvokeCustomCommandRequest;
import org.cloudifysource.dsl.rest.response.InvokeServiceCommandResponse;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.openspaces.core.GigaSpace;

import alien4cloud.paas.cloudify2.events.RelationshipOperationEvent;
import alien4cloud.paas.cloudify2.events.Execption.EventHandlingException;
import alien4cloud.paas.cloudify2.events.Execption.RestEventException;
import alien4cloud.paas.cloudify2.events.polling.RestClientManager;
import alien4cloud.paas.cloudify2.rest.RestUtils;

public abstract class AbstractRelationshipOperationHandler implements IEventHandler<RelationshipOperationEvent> {

    protected final Logger log = getLogger();

    @Resource
    protected RestClientManager restClientManager;
    @Resource
    protected GigaSpace gigaSpace;

    protected Set<String> handledOperations = new HashSet<String>();

    @Override
    public boolean canHandle(RelationshipOperationEvent event) {
        return handledOperations.contains(event.getEvent());
    }

    @Override
    public void eventHappened(RelationshipOperationEvent event) throws RestEventException {
        log.info("Handling event:" + event.resume());
        RestClient restClient = restClientManager.getRestClient();
        RelationshipMember triggeredMember = getTriggeredMember(event);

        try {
            // check if the condition to trigger the event
            if (!isConditionMetToProcessEvent(event, triggeredMember)) {
                log.warning("Condition not met to trigger event " + event.resume() + ". So we do nothing...");
                event.setExecuted(false);
                return;
            }

            // mark the event as executed
            event.setExecuted(true);
            InvokeCustomCommandRequest invokeRequest = new InvokeCustomCommandRequest();
            invokeRequest.setCommandName(event.getCommandName());
            List<String> params = new ArrayList<String>();
            params.add(event.getInstanceId());
            params.add(event.getIpAddress());
            invokeRequest.setParameters(params);
            InvokeServiceCommandResponse invokeResponse = restClient.invokeServiceCommand(event.getApplicationName(), triggeredMember.cdfyServiceName,
                    invokeRequest);
            Map<String, String> success = new HashMap<String, String>();
            Map<String, String> failures = new HashMap<String, String>();
            RestUtils.parseServiceInvokeResponse(success, failures, invokeResponse.getInvocationResultPerInstance());
            log.info("Command result: \n\tSUCCESS: " + success + "\n\tFAILLURES: " + failures);
            if (failures.isEmpty()) {
                event.setSucceeded(true);
            } else {
                log.warning("Errors when handling event " + event);
                log.warning("Errors on some instances when executing operation <" + event.getCommandName() + ">: " + failures);
                event.setSucceeded(false);
            }
        } catch (RestClientException e) {
            event.setSucceeded(false);
            log.severe("Fail to handle event " + event + ". \t" + e.getMessageFormattedText());
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

    /**
     * Implements the condition to satisfy to process the event and trigger its related command
     *
     * @param event
     * @param triggeredMember
     * @return
     * @throws RestClientException
     */
    protected abstract boolean isConditionMetToProcessEvent(RelationshipOperationEvent event, RelationshipMember triggeredMember)
            throws EventHandlingException, RestClientException;

}
