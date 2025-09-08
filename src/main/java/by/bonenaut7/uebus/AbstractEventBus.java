package by.bonenaut7.uebus;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class AbstractEventBus {
	protected static final Class<Event> EVENT_CLASS = Event.class;
	
	protected final ReentrantReadWriteLock reentrantLock = new ReentrantReadWriteLock();
	protected final Lock readLock = reentrantLock.readLock();
	protected final Lock writeLock = reentrantLock.writeLock();
	
	protected final Map<Class<? extends Event>, List<EventRegistration>> eventRegistry;
	protected EventRegistrationListFactory registryListFactory;
	protected ExceptionHandler exceptionHandler = null;
	
	protected AbstractEventBus() {
		this(new HashMap<>(), ArrayList::new);
	}
	
	protected AbstractEventBus(Map<Class<? extends Event>, List<EventRegistration>> map, EventRegistrationListFactory listFactory) {
		Objects.requireNonNull(map, "map cannot be null.");
		Objects.requireNonNull(listFactory, "listFactory cannot be null.");
		
		this.eventRegistry = map;
		this.registryListFactory = listFactory;
	}
	
	/**
	 * Sets an exception handler for the event bus. All exceptions will be passed
	 * through the specified exception handler, including registration ones.
	 * 
	 * @param exceptionHandler The exception handler
	 */
	public void setExceptionHandler(ExceptionHandler exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
	}
	
	/**
	 * Registers event listener.
	 * 
	 * @param <T>      Event type
	 * @param type     Event class object
	 * @param delegate Listener delegate
	 * 
	 * @return Event registration object if succeed, <code>null</code> otherwise.
	 */
	public <T extends Event> EventRegistration<T> register(Class<T> type, EventDelegate<T> delegate) {
		return register(type, delegate, EventListener.DEFAULT_PRIORITY, EventListener.DEFAULT_IGNORE_CANCELLATION);
	}
	
	/**
	 * Registers event listener.
	 * 
	 * @param <T>      Event type
	 * @param type     Event class object
	 * @param delegate Listener delegate
	 * @param priority Listener priority
	 * 
	 * @return Event registration object if succeed, <code>null</code> otherwise.
	 */
	public <T extends Event> EventRegistration<T> register(Class<T> type, EventDelegate<T> delegate, int priority) {
		return register(type, delegate, priority, EventListener.DEFAULT_IGNORE_CANCELLATION);
	}
	
	/**
	 * Registers event listener.
	 * 
	 * @param <T>                Event type
	 * @param type               Event class object
	 * @param delegate           Listener delegate
	 * @param ignoreCancellation The flag that indicates whether listener will
	 *                           ignore cancelled methods or not.
	 * 
	 * @return Event registration object if succeed, <code>null</code> otherwise.
	 */
	public <T extends Event> EventRegistration<T> register(Class<T> type, EventDelegate<T> delegate, boolean ignoreCancellation) {
		return register(type, delegate, EventListener.DEFAULT_PRIORITY, ignoreCancellation);
	}
	
	/**
	 * Registers event listener.
	 * 
	 * @param <T>                Event type
	 * @param type               Event class object
	 * @param delegate           Listener delegate
	 * @param priority           Listener priority
	 * @param ignoreCancellation The flag that indicates whether listener will
	 *                           ignore cancelled methods or not.
	 * 
	 * @return Event registration object if succeed, <code>null</code> otherwise.
	 */
	public <T extends Event> EventRegistration<T> register(Class<T> type, EventDelegate<T> delegate, int priority, boolean ignoreCancellation) {
		Objects.requireNonNull(type, "type cannot be null.");
		Objects.requireNonNull(delegate, "delegate cannot be null.");
		
		final EventRegistration<T> registration = new EventRegistration<>(type, delegate, priority, ignoreCancellation);
		
		this.writeLock.lock();
		try {
			final List<EventRegistration> registrations = getListenerRegistrations(type);
			
			if (registrations.add(registration)) {
				registrations.sort(null);
				return registration;
			}
		} finally {
			this.writeLock.unlock();
		}
		
		return null;
	}
	
	/**
	 * Looks for plain(non-static) annotated methods and registers them as an event
	 * listeners.
	 * 
	 * @param eventHandler The object with annotated methods.
	 */
	public void register(Object eventHandler) {
		register(eventHandler, null);
	}
	
	/**
	 * Looks for plain(non-static) annotated methods and registers them as an event
	 * listeners.
	 * 
	 * @param eventHandler The object with annotated methods
	 * @param out          The list to which listener registrations will be added
	 * 
	 * @return <code>out</code> parameter with event registrations added.
	 */
	public List<EventRegistration> register(Object eventHandler, List<EventRegistration> out) {
		Objects.requireNonNull(eventHandler, "eventHandler cannot be null.");
		registerAnnotated(eventHandler.getClass(), eventHandler, false, out);
		return out;
	}
	
	/**
	 * Looks for static annotated methods and registers them as an event listeners.
	 * 
	 * @param eventHandler The class object with annotated methods
	 */
	public void registerStatic(Class<?> eventHandler) {
		registerStatic(eventHandler, null);
	}
	
	/**
	 * Looks for static annotated methods and registers them as an event listeners.
	 * 
	 * @param eventHandler The class object with annotated methods
	 * @param out          The list to which listener registrations will be added
	 * 
	 * @return <code>out</code> parameter with event registrations added.
	 */
	public List<EventRegistration> registerStatic(Class<?> eventHandler, List<EventRegistration> out) {
		Objects.requireNonNull(eventHandler, "eventHandler cannot be null.");
		registerAnnotated(eventHandler, null, true, out);
		return out;
	}
	
	/**
	 * Unregisters event listener.
	 * 
	 * @param registration The listener registration
	 * 
	 * @return <code>true</code> if succeed, <code>false</code> otherwise.
	 */
	public boolean unregister(EventRegistration registration) {
		Objects.requireNonNull(registration, "registration cannot be null.");
		final List<EventRegistration> registrations = getListenerRegistrations(registration.getType());
		return registrations.remove(registration);
	}
	
	/**
	 * Posts an event and returns it.
	 * 
	 * @param <T> An event type.
	 * @param event An event to be posted.
	 * 
	 * @return Specified <code>event</code> parameter.
	 */
	public <T extends Event> T post(T event) {
		Objects.requireNonNull(event, "event cannot be null.");
		
		final Class<?> eventType = event.getClass();
		
		this.readLock.lock();
		try {
			final List<EventRegistration> registrations = this.eventRegistry.get(eventType);
			if (registrations != null) {
				try {
					if (event.isCancellable()) {
						for (final EventRegistration registration : registrations) {
							if (!event.isCancelled() || registration.isIgnoringCancellation()) {
								registration.getDelegate().handle(event);
							}
						}
					} else {
						for (final EventRegistration registration : registrations) {
							registration.getDelegate().handle(event);
						}
					}
				} catch (Throwable throwable) {
					handleException(throwable);
				}
			}
		} finally {
			this.readLock.unlock();
		}
		
		return event;
	}
	
	/**
	 * Posts an event and returns whether event has been cancelled or not.
	 * 
	 * @param event An event to be posted.
	 * 
	 * @return <code>true</code> if <code>event</code> has not been cancelled,
	 *         <code>false</code> otherwise.
	 */
	public boolean postIsCancelled(Event event) {
		return post(event).isCancelled();
	}
	
	protected void handleException(Throwable throwable) {
		Objects.requireNonNull(throwable, "throwable cannot be null.");
		
		if (this.exceptionHandler != null) {
			this.exceptionHandler.handle(throwable);
		} else {
			// If we don't have an exception handler, we should re-throw the exception
			throw new RuntimeException(throwable);
		}
	}
	
	protected List<EventRegistration> getListenerRegistrations(final Class eventType) {
		List<EventRegistration> registrations = this.eventRegistry.get(eventType);
		if (registrations == null) {
			registrations = this.registryListFactory.create();
			this.eventRegistry.put(eventType, registrations);
		}
		
		return registrations;
	}
	
	protected final void registerAnnotated(Class<?> type, Object instance, boolean registerStatic, List<EventRegistration> registrationsOut) {
		Objects.requireNonNull(type, "type cannot be null.");
		
		final Lookup lookup = MethodHandles.publicLookup();
		
		// Check class access (Not compliant with Java 8)
//		try {
//			if (lookup.accessClass(type) == null) {
//				handleException(new IllegalAccessException(String.format("Unable to access class \"%s\" to register event listeners.", type.getSimpleName())));
//			}
//		} catch (IllegalAccessException illegalAccessException) {
//			// We can't even check the access...
//			handleException(illegalAccessException);
//		}
		
		// Register the listener
		this.writeLock.lock();
		try {
			for (final Method method : type.getDeclaredMethods()) {
				try {
					final boolean isStatic = Modifier.isStatic(method.getModifiers());
					if (isStatic != registerStatic) {
						continue; // Skip if we don't register static methods
					}
					
					final EventListener annotation = checkIsListenerValid(method, !isStatic ? instance : null);
					if (annotation == null) {
						continue; // The method failed listener validity checks, skippin' it...
					}
					
					final Class eventType = method.getParameterTypes()[0];
					final List registrations = getListenerRegistrations(eventType);
					final EventDelegate delegate = isStatic ?
						new MethodHandleStaticInvoker<>( // Static methods invoker
							lookup.findStatic(type, method.getName(), MethodType.methodType(Void.TYPE, eventType))
						) :
						new MethodHandleVirtualInvoker<>( // Virtual methods invoker
							lookup.findVirtual(type, method.getName(), MethodType.methodType(Void.TYPE, eventType)),
							instance
						);
					
					final EventRegistration registration = new EventRegistration<>(eventType, delegate, annotation.priority(), annotation.ignoreCancellation());
					if (registrations.add(registration)) {
						registrations.sort(null);
						
						if (registrationsOut != null) {
							registrationsOut.add(registration);
						}
					}
				} catch (IllegalAccessException | NoSuchMethodException exception) {
					handleException(exception);
				}
			}
		} catch (SecurityException securityException) {
			handleException(securityException);
		} finally {
			this.writeLock.unlock();
		}
	}
	
	protected static final EventListener checkIsListenerValid(final Method method, final Object instance) {
		Objects.requireNonNull(method, "method cannot be null.");
		
		final EventListener annotation = method.getAnnotation(EventListener.class);
		if (annotation != null && // If annotation is present
			// method.canAccess(instance) && // If method can be accessed (Not compliant with Java 8)
			method.getReturnType() == Void.TYPE && // If method's return type is void (... void methodName(...))
			method.getParameterCount() == 1 && // If method's parameter count is 1 (... methodName(Parameter1))
			EVENT_CLASS.isAssignableFrom(method.getParameterTypes()[0]) // If parameter is child of an Event type (... methodName(<? extends Event> param))
		) {
			return annotation;
		}
		
		return null;
	}
	
	// Plain class > lambda calls
	private static final class MethodHandleStaticInvoker<T extends Event> implements EventDelegate<T> {
		private final MethodHandle handle;
		
		private MethodHandleStaticInvoker(MethodHandle handle) {
			this.handle = handle;
		}

		@Override
		public void handle(T event) throws Throwable {
			this.handle.invoke(event);
		}
	}
	
	private static final class MethodHandleVirtualInvoker<T extends Event> implements EventDelegate<T> {
		private final MethodHandle handle;
		private final Object instance;
		
		private MethodHandleVirtualInvoker(MethodHandle handle, Object instance) {
			this.handle = handle;
			this.instance = instance;
		}

		@Override
		public void handle(T event) throws Throwable {
			this.handle.invoke(this.instance, event);
		}
	}
}
