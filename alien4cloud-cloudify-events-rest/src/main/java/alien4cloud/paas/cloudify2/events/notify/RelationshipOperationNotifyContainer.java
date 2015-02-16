package alien4cloud.paas.cloudify2.events.notify;

import java.util.Collection;
import java.util.logging.Logger;

import javax.annotation.Resource;

import org.openspaces.core.GigaSpace;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.notify.Notify;

import alien4cloud.paas.cloudify2.events.RelationshipOperationEvent;
import alien4cloud.paas.cloudify2.events.notify.handler.IEventHandler;

@EventDriven
@Notify
@SuppressWarnings("unchecked")
public class RelationshipOperationNotifyContainer {
    private static final Logger log = Logger.getLogger(RelationshipOperationNotifyContainer.class.getName());
    @Resource
    @SuppressWarnings("rawtypes")
    private Collection<IEventHandler> handlers;

    @Resource
    private GigaSpace gigaSpace;

    @EventTemplate
    RelationshipOperationEvent template() {
        RelationshipOperationEvent template = new RelationshipOperationEvent();
        template.setExecuted(false);
        return template;
    }

    @SpaceDataEvent
    @SuppressWarnings("rawtypes")
    public void eventListener(RelationshipOperationEvent event) {
        log.info("\t Got event: " + event.toString());
        for (IEventHandler handler : handlers) {
            if (handler.canHandle(event)) {
                log.info("Handler found. delegating...");
                handler.eventHappened(event);
            }
        }
        event.setExecuted(true);
        gigaSpace.write(event);
        try {
            Thread.sleep(10L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
