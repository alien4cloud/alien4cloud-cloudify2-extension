package alien4cloud.paas.cloudify2.events.polling.handler;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Component;

import alien4cloud.paas.cloudify2.events.NodeInstanceState;
import alien4cloud.paas.cloudify2.events.RelationshipOperationEvent;
import alien4cloud.paas.cloudify2.events.RelationshipOperationsContants;

@Component
public class AddMemberRelationshipOperationsHandler extends AbstractRelationshipOperationHandler {

    private static Long DEFAULT_TIMEOUT_MILLIS = 60000L * 5L;

    @Override
    public void configureHandler() {
        handledOperations.add(RelationshipOperationsContants.ADD_SOURCE);
        handledOperations.add(RelationshipOperationsContants.ADD_TARGET);
    }

    @Override
    protected RelationshipMember getTriggeredMember(RelationshipOperationEvent event) {
        if (event.getEvent().equals(RelationshipOperationsContants.ADD_SOURCE)) {
            return new RelationshipMember(event.getTarget(), event.getTargetService());
        } else {
            return new RelationshipMember(event.getSource(), event.getSourceService());
        }
    }

    @Override
    protected boolean isConditionMetToProcessEvent(RelationshipOperationEvent event, RelationshipMember triggeredMember) {
        // wait for the triggered node to be at least started
        return waitFor(event.getDeploymentId(), triggeredMember.nodeId, triggeredMember.cdfyServiceName, "started", "available");
    }

    /**
     * wait for a service (node template) to reach a certain state
     *
     * @param deploymentId TODO
     * @param node
     * @param service
     * @param state
     */
    protected boolean waitFor(String deploymentId, String node, String service, String... states) {
        NodeInstanceState template = new NodeInstanceState();
        template.setDeploymentId(deploymentId);
        template.setNodeTemplateId(node);
        Long timeout = System.currentTimeMillis() + DEFAULT_TIMEOUT_MILLIS;
        boolean continueToCheck = true;
        log.info("Checking for node <" + node + "> state. Expects one of <" + ArrayUtils.toString(states) + ">.");
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
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    log.warning("Interrupted sleep");
                }
            }
        }

        if (continueToCheck) {
            log.severe("Fail to handle event. <" + node + "> node fails to reach one of the states <" + ArrayUtils.toString(states) + "> in time.");
        }

        return !continueToCheck;
    }
}
