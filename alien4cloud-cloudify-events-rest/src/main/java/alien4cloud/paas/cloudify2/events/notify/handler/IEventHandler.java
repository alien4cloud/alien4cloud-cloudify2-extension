package alien4cloud.paas.cloudify2.events.notify.handler;

import alien4cloud.paas.cloudify2.events.AlienEventDescription;
import alien4cloud.paas.cloudify2.events.Execption.EventHandlingException;

/**
 * Interface implemented by all event handlers
 *
 * @author 'Igor Ngouagna'
 *
 */
public interface IEventHandler<T extends AlienEventDescription> {

    /**
     * Whether this handler can handle or not the happened event
     *
     * @return true if this handler can handle this event, false if not
     */
    boolean canHandle(T event);

    /**
     * Called when an event happens
     *
     * @param event the event to be handled
     */
    void eventHappened(T event) throws EventHandlingException;
}
