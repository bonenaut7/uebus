package by.bonenaut7.uebus;

import java.util.List;
import java.util.Map;

@SuppressWarnings("rawtypes")
public final class SimpleEventBus extends AbstractEventBus {
	public SimpleEventBus() {
		super();
	}
	
	public SimpleEventBus(Map<Class<? extends Event>, List<EventRegistration>> map, EventRegistrationListFactory listFactory) {
		super(map, listFactory);
	}
}
