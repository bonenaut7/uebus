package by.bonenaut7.uebus;

@FunctionalInterface
public interface EventDelegate<T extends Event> {
	void handle(T event) throws Throwable;
}
