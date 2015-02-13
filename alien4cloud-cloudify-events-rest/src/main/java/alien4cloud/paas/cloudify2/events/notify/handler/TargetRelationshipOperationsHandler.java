package alien4cloud.paas.cloudify2.events.notify.handler;

import org.springframework.stereotype.Component;

import alien4cloud.paas.cloudify2.events.RelationshipOperationEvent;
import alien4cloud.paas.cloudify2.events.RelationshipOperationsContants;

@Component
public class TargetRelationshipOperationsHandler extends AbstractRelationshipOperationHandler {

    @Override
    public void configureHandler() {
        handledOperations.add(RelationshipOperationsContants.ADD_TARGET);
    }

    @Override
    protected RelationshipMember getTriggeredMember(RelationshipOperationEvent event) {
        return new RelationshipMember(event.getSource(), event.getSourceService());
    }

}
