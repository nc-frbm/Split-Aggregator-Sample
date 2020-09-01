package dk.example.domain;

import java.util.List;

public class AggregatedEvent {

    private String correlationId;
    private List<Event> events;

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    @Override
    public String toString() {
        return "AggregatedEvent{" +
                "correlationId='" + correlationId + '\'' +
                ", events=" + events +
                '}';
    }
}
