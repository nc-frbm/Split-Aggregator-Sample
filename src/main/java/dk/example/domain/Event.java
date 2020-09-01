package dk.example.domain;

public class Event {

    private EventType type;
    private String correlationId;
    private String text;
    private int totalEvents;

    public Event(String correlationId, String text) {
        this.correlationId = correlationId;
        this.text = text;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getTotalEvents() {
        return totalEvents;
    }

    public void setTotalEvents(int totalEvents) {
        this.totalEvents = totalEvents;
    }


    public Event copy() {
        return new Event(correlationId, text);
    }

    @Override
    public String toString() {
        return "Event{" +
                "type=" + type +
                ", correlationId='" + correlationId + '\'' +
                ", text='" + text + '\'' +
                ", totalEvents=" + totalEvents +
                '}';
    }
}

