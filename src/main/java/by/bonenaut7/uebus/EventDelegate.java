package by.bonenaut7.uebus;

@FunctionalInterface
public interface EventDelegate<T> {
	void handle(T event) throws Throwable;
}
