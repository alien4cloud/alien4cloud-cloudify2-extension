package alien4cloud.paas.cloudify2.events.polling.handler;

import org.cloudifysource.dsl.internal.CloudifyConstants.DeploymentState;
import org.cloudifysource.dsl.rest.response.ServiceDescription;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.springframework.stereotype.Component;

import alien4cloud.paas.cloudify2.events.RelationshipOperationEvent;
import alien4cloud.paas.cloudify2.events.RelationshipOperationsContants;
import alien4cloud.paas.cloudify2.events.Execption.EventHandlingException;

@Component
public class RemoveMemberRelationshipOperationsHandler extends AbstractRelationshipOperationHandler {

    @Override
    public void configureHandler() {
        handledOperations.add(RelationshipOperationsContants.REMOVE_SOURCE);
        handledOperations.add(RelationshipOperationsContants.REMOVE_TARGET);
    }

    @Override
    protected RelationshipMember getTriggeredMember(RelationshipOperationEvent event) {
        if (event.getEvent().equals(RelationshipOperationsContants.REMOVE_SOURCE)) {
            return new RelationshipMember(event.getTarget(), event.getTargetService());
        } else {
            return new RelationshipMember(event.getSource(), event.getSourceService());
        }
    }

    @Override
    protected boolean isConditionMetToProcessEvent(RelationshipOperationEvent event, RelationshipMember triggeredMember) throws EventHandlingException,
            RestClientException {
        RestClient restClient = restClientManager.getRestClient();
        String mainMemberServiceName = triggeredMember.cdfyServiceName.equals(event.getSourceService()) ? event.getTargetService() : event.getServiceName();
        ServiceDescription mainServiceDescription = restClient.getServiceDescription(event.getApplicationName(), mainMemberServiceName);
        boolean isNotUndeploying = !mainServiceDescription.getServiceState().equals(DeploymentState.IN_PROGRESS);
        if (!isNotUndeploying) {
            log.info("Service " + mainMemberServiceName + " is in undeployment state... We do not invoque remove_target/remove_node in this case.");
        }
        return isNotUndeploying;
    }
}
