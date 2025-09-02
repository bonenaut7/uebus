package by.bonenaut7.uebus;

import java.util.Objects;

public final class EventRegistration<T extends Event> implements Comparable<EventRegistration<T>> {
	private final Class<T> type;
	private final int priority;
	private final boolean ignoreCancellation;
	private final EventDelegate<T> delegate;
	
	public EventRegistration(Class<T> type, EventDelegate<T> delegate, int priority, boolean ignoreCancellation) {
		Objects.requireNonNull(type, "type cannot be null.");
		Objects.requireNonNull(delegate, "delegate cannot be null.");
		
		this.type = type;
		this.delegate = delegate;
		this.priority = priority;
		this.ignoreCancellation = ignoreCancellation;
	}
	
	public Class<T> getType() {
		return this.type;
	}
	
	public int getPriority() {
		return this.priority;
	}
	
	public boolean isIgnoringCancellation() {
		return this.ignoreCancellation;
	}
	
	public EventDelegate<T> getDelegate() {
		return this.delegate;
	}
	
	@Override
	public int compareTo(EventRegistration<T> o) {
		return (this.priority < o.priority) ? 1 : ((this.priority == o.priority) ? 0 : -1);
	}
}
