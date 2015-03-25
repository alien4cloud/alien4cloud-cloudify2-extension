package alien4cloud.paas.cloudify2.events.Execption;

public class RestEventException extends RuntimeException {
    /**
     *
     */
    private static final long serialVersionUID = 3771209867870589374L;

    public RestEventException() {
        super();
    }

    public RestEventException(String message, Throwable cause) {
        super(message, cause);
    }

    public RestEventException(String message) {
        super(message);
    }

    public RestEventException(Throwable cause) {
        super(cause);
    }

}
