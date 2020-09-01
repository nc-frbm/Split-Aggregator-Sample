package dk.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.router.HeaderValueRouter;
import org.springframework.messaging.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Configuration
@EnableIntegration
public class SplitAggregateConfiguration {

    public static final int SIZE = 5;
    public static final String ROUTE_HEADER = "x_route";

    @Autowired
    private EventGateway eventGateway;

    @Bean
    public IntegrationFlow splitAggregateFlow() {
        return IntegrationFlows.from("splitInput")
                // Here we convert the event into multiple events
                // In DMS this would be multiple event types, one for each external service IOSS, Taric etc.
                .transform(Message.class, message -> {
                    List<Event> eventList = createList((Event) message.getPayload(), SIZE);
                    for (int i = 0; i < eventList.size(); i++) {
                        Event event = eventList.get(i);
                        if (i == 0) {
                            event.type = EventType.TARIC_X;
                        } else if (i == 1) {
                            event.type = EventType.TARIC_Y;
                        } else {
                            event.type = EventType.IOSS;
                        }
                        event.totalEvents = eventList.size();
                        event.text += " " + i;
                    }
                    return eventList;
                })
                .split()
                .enrichHeaders(e -> e.headerFunction(ROUTE_HEADER, (Function<Message<Event>, Object>) message -> message.getPayload().type.name()))
                .route(router())
//                .route() // Here we can route the message to different services (kafka producers)
                .get();
    }


    @Bean
    public IntegrationFlow aggregate() {
        // Aggregating the messages
        return IntegrationFlows.from("aggregateInput")
                .aggregate(aggregatorSpec ->
                                // Define correlation strategy. Use correlation id from the event
                                aggregatorSpec.correlationStrategy(message -> {
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

    public HeaderValueRouter router() {
        HeaderValueRouter router = new HeaderValueRouter(ROUTE_HEADER);
        router.setChannelMapping(EventType.IOSS.name(), "iossChannel");
        router.setChannelMapping(EventType.TARIC_X.name(), "taricXChannel");
        router.setChannelMapping(EventType.TARIC_Y.name(), "taricYChannel");
        return router;
    }

    @Bean
    public IntegrationFlow iossFlow() {
        return IntegrationFlows.from("iossChannel")
                .handle(message -> {
                    Event event = (Event) message.getPayload();
                    System.out.println("Handling IOSS event " + event);
                    eventGateway.receive(event); // Produce event for validation reply
                })
                .get();
    }
    @Bean
    public IntegrationFlow taricXFlow() {
        return IntegrationFlows.from("taricXChannel")
                .handle(message -> {
                    Event event = (Event) message.getPayload();
                    System.out.println("Handling TaricX event " + event);
                    eventGateway.receive(event); // Produce event for validation reply
                })
                .get();
    }
    @Bean
    public IntegrationFlow taricYFlow() {
        return IntegrationFlows.from("taricYChannel")
                .handle(message -> {
                    Event event = (Event) message.getPayload();
                    System.out.println("Handling TaricY event " + event);
                    eventGateway.receive(event); // Produce event for validation reply
                })
                .get();
    }

//    @Bean
//    public IntegrationFlow processEventFlow() {
//        // Dummy flow for "processing" the event
//        return IntegrationFlows.from("eventProcessInput")
//                .handle(message -> {
//                    Event event = (Event) message.getPayload();
//                    System.out.println("Handling event " + event.text);
//                    eventGateway.receive(event); // Produce event for validation reply
//                }).get();
//    }

    private Event duplicate(Event event) {
        return new Event(event.correlationId, event.text);
    }

    private List<Event> createList(Event event, int size) {
        return IntStream.range(0, size)
                .mapToObj(i -> duplicate(event))
                .collect(Collectors.toList());
    }
}

