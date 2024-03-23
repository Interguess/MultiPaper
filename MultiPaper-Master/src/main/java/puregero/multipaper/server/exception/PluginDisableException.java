package puregero.multipaper.server.exception;

public class PluginDisableException extends RuntimeException {

    public PluginDisableException(String message) {
        super(message);
    }

    public PluginDisableException(String message, Throwable cause) {
        super(message, cause);
    }
}
