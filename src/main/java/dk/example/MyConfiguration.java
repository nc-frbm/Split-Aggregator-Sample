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
public class MyConfiguration {

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
                .enrichHeaders(e -> e.headerFunction(CORRELATION_ID, (Function<Message<Event>, Object>) message -> message.getPayload().correlationId))
                .transform(Message.class, m -> createList((Event) m.getPayload(), SIZE))
                .enrichHeaders(e -> e.headerFunction(TOTAL_REQUIRED_EVENTS, (Function<Message<List<Event>>, Object>) message -> message.getPayload().size()))
                .split()
                .channel("eventProcessInput")
                .get();
    }


    @Bean
    public IntegrationFlow aggregate() {
        return IntegrationFlows.from("aggregateInput")
                .enrichHeaders(e -> e.headerFunction(CORRELATION_ID, (Function<Message<Event>, Object>) message -> message.getPayload().correlationId))
                .aggregate(a -> a.correlationStrategy(m -> m.getHeaders().get(CORRELATION_ID))
                .releaseStrategy(g -> {
                    boolean isFinished = g.size() == SIZE;
                    System.out.println("Aggregation done? " + isFinished);
                    return isFinished;
                }))
                .transform(Message.class, m -> {
                    Message<ArrayList<Event>> message = (Message<ArrayList<Event>>) m;
                    AggregatedEvent aggregatedEvent = new AggregatedEvent();
                    aggregatedEvent.events = message.getPayload();
                    aggregatedEvent.correlationId = message.getPayload().get(0).correlationId;
                    return aggregatedEvent;
                })
                .handle(m -> {
                    System.out.println("done");
                })
                .get();
    }

    @Bean
    public IntegrationFlow processEventFlow() {
        return IntegrationFlows.from("eventProcessInput")
                .handle(m -> {
                    Event e = (Event) m.getPayload();
                    System.out.println("Handling event " + e.text);
                    eventGateway.receive(e);
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

