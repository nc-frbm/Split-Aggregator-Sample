package dk.example;

public class Event {

    public String correlationId;
    public String text;
    public int totalEvents;

    public Event(String correlationId, String text) {
        this.correlationId = correlationId;
        this.text = text;
    }
}

