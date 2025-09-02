package by.bonenaut7.uebus;

import java.util.List;

@FunctionalInterface
public interface EventRegistrationListFactory {
	List<EventRegistration> create();
}
