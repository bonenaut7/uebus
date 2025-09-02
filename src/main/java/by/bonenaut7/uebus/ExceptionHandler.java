package by.bonenaut7.uebus;

public interface ExceptionHandler {
	
	/**
	 * Exception handler, should handle or re-throw supplied throwables.
	 * 
	 * @param throwable The throwable object
	 */
	void handle(Throwable throwable);
}
