package alien4cloud.paas.cloudify2.events.Execption;

public class EventHandlingException extends RestEventException {
    /**
     *
     */
    private static final long serialVersionUID = 3771209867870589374L;

    public EventHandlingException() {
        super();
    }

    public EventHandlingException(String message, Throwable cause) {
        super(message, cause);
    }

    public EventHandlingException(String message) {
        super(message);
    }

    public EventHandlingException(Throwable cause) {
        super(cause);
    }

}
