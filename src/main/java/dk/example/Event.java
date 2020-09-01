package dk.example;

public class Event {

    public EventType type;
    public String correlationId;
    public String text;
    public int totalEvents;

    public Event(String correlationId, String text) {
        this.correlationId = correlationId;
        this.text = text;
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

