package by.bonenaut7.uebus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventListener {
	/**
	 * Default priority for the listeners.
	 */
	public static final int DEFAULT_PRIORITY = 0;
	
	/**
	 * Default value for the {@link #ignoreCancellation()} method, indicates that by
	 * default listeners will ignore cancelled events.
	 */
	public static final boolean DEFAULT_IGNORE_CANCELLATION = false;
	
	/**
	 * Listener's priority, lesser value will lead to later execution.
	 * 
	 * @return priority value
	 */
	int priority() default DEFAULT_PRIORITY;

	/**
	 * Indicates whether listener ignores events cancelled beforehand.
	 * 
	 * @return <true>true</code> whether event should pass even if it's cancelled,
	 *         <code>false</code> otherwise.
	 */
	boolean ignoreCancellation() default DEFAULT_IGNORE_CANCELLATION;
}
