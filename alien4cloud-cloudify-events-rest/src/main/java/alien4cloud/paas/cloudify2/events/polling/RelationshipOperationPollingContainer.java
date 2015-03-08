package alien4cloud.paas.cloudify2.events.polling;

import java.util.Collection;
import java.util.logging.Logger;

import javax.annotation.Resource;

import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.openspaces.events.polling.ReceiveHandler;
import org.openspaces.events.polling.receive.ReceiveOperationHandler;
import org.openspaces.events.polling.receive.SingleTakeReceiveOperationHandler;

import alien4cloud.paas.cloudify2.events.RelationshipOperationEvent;
import alien4cloud.paas.cloudify2.events.polling.handler.IEventHandler;

@EventDriven
@Polling(concurrentConsumers = 2)
@SuppressWarnings("unchecked")
public class RelationshipOperationPollingContainer {
    private static final Logger log = Logger.getLogger(RelationshipOperationPollingContainer.class.getName());
    @Resource
    @SuppressWarnings("rawtypes")
    private Collection<IEventHandler> handlers;

    @ReceiveHandler
    ReceiveOperationHandler receiveHandler() {
        SingleTakeReceiveOperationHandler receiveHandler = new SingleTakeReceiveOperationHandler();
        receiveHandler.setNonBlocking(true);
        receiveHandler.setNonBlockingFactor(60);
        return receiveHandler;
    }

    @EventTemplate
    RelationshipOperationEvent newRelationshipOperationTemplate() {
        log.info("Register template for relationship event...");
        RelationshipOperationEvent template = new RelationshipOperationEvent();
        template.setProcessed(false);
        return template;
    }

    @SpaceDataEvent
    @SuppressWarnings("rawtypes")
    public RelationshipOperationEvent eventListener(RelationshipOperationEvent event) {
        log.info("Got event: " + event.resume() + "\n\t full event:" + event);
        for (IEventHandler handler : handlers) {
            if (handler.canHandle(event)) {
                log.info("Handler found. delegating...");
                handler.eventHappened(event);
            }
        }
        event.setProcessed(true);

        return event;
    }
}
