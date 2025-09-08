package by.bonenaut7.uebus;

public abstract class Event {
	private boolean cancelled = false;
	
	/**
	 * Indicates whether event is cancellable (supports the change of the
	 * cancellation value).
	 * 
	 * @return <code>true</code> whether cancellation is supported,
	 *         <code>false</code> otherwise.
	 */
	public boolean isCancellable() {
		return false;
	}
	
	/**
	 * Indicates whether event is cancelled.
	 * 
	 * @return The event cancellation value
	 */
	public boolean isCancelled() {
		return this.cancelled;
	}
	
	/**
	 * Changes the cancellation value of the event if it's supported by the
	 * {@link #isCancellable()}. Otherwise throws an exception.
	 * 
	 * @param cancelled The cancellation value
	 */
	public void setCancelled(boolean cancelled) {
		if (!isCancellable()) {
			throw new IllegalStateException(String.format("Event cancellation is not supported for the event \"%s\".", getClass().getSimpleName()));
		}
		
		this.cancelled = onCancellation(cancelled);
	}
	
	/**
	 * Returns modified cancellation value if so required.
	 * 
	 * @param cancelled The cancellation value
	 * 
	 * @return modified cancellation value
	 */
	protected boolean onCancellation(boolean cancelled) {
		return cancelled; // NO-OP
	}
}
