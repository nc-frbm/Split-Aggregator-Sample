package dk.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.messaging.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Configuration
@EnableIntegration
public class SplitAggregateConfiguration {

    public static final String CORRELATION_ID = "x_correlationId";
    public static final String TOTAL_REQUIRED_EVENTS = "x_totalRequiredEvents";
    public static final int SIZE = 5;

    @Autowired
    private EventGateway eventGateway;

    @Bean
    public AtomicInteger integerSource() {
        return new AtomicInteger();
    }

    @Bean
    public IntegrationFlow splitAggregateFlow() {
        return IntegrationFlows.from("splitInput")
                // Here we convert the event into multiple events
                // In DMS this would be multiple event types, one for each external service IOSS, Taric etc.
                .transform(Message.class, message -> {
                    List<Event> eventList = createList((Event) message.getPayload(), SIZE);
                    for (int i = 0; i < eventList.size(); i++) {
                        eventList.get(i).totalEvents = eventList.size();
                        eventList.get(i).text += " " + i;
                    }
                    return eventList;
                })
                .split()
//                .route() // Here we can route the message to different services (kafka producers)
                .channel("eventProcessInput") // This is just to simulate sending the events to external services
                .get();
    }


    @Bean
    public IntegrationFlow aggregate() {
        // Aggregating the messages
        return IntegrationFlows.from("aggregateInput")
                .aggregate(a ->
                                // Define correlation strategy. Use correlation id from the event
                                a.correlationStrategy(message -> {
                                    Event event = (Event) message.getPayload();
                                    return event.correlationId;
                                })
                                // Define release strategy. This strategy defines when we are done aggregating.
                                .releaseStrategy(group -> {
                                    Event event = (Event)group.getOne().getPayload();
                                    boolean isFinished = group.size() == event.totalEvents;
                                    System.out.println("Aggregation done? " + isFinished);
                                    return isFinished;
                                })
//                        .messageStore(messageStore()) // Here message store should be configured https://docs.spring.io/spring-integration/docs/5.3.0.M3/reference/html/jdbc.html#jdbc-message-store
//                        .lockRegistry(lockRegistry()) // Here the lock registry should be configured https://docs.spring.io/spring-integration/docs/5.3.0.M3/reference/html/jdbc.html#jdbc-lock-registry
                )
                .transform(Message.class, genericMessage -> {
                    Message<ArrayList<Event>> message = (Message<ArrayList<Event>>) genericMessage;
                    AggregatedEvent aggregatedEvent = new AggregatedEvent();
                    aggregatedEvent.events = message.getPayload();
                    aggregatedEvent.correlationId = message.getPayload().get(0).correlationId;
                    return aggregatedEvent;
                })
                .handle(message -> {
                    System.out.println("done");
                })
                .get();
    }

    @Bean
    public IntegrationFlow processEventFlow() {
        // Dummy flow for "processing" the event
        return IntegrationFlows.from("eventProcessInput")
                .handle(m -> {
                    Event e = (Event) m.getPayload();
                    System.out.println("Handling event " + e.text);
                    eventGateway.receive(e); // Produce event for validation reply
                }).get();
    }

    private Event duplicate(Event event) {
        return new Event(event.correlationId, event.text);
    }

    private List<Event> createList(Event event, int size) {
        return IntStream.range(0, size)
                .mapToObj(i -> duplicate(event))
                .collect(Collectors.toList());
    }
}

