package dk.example.gateway;

import dk.example.domain.Event;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.stereotype.Component;

@SuppressWarnings("UnresolvedMessageChannel")
@Component
@MessagingGateway
public interface EventGateway {

    @Gateway(requestChannel = "splitInput")
    void send(Event event);


    @Gateway(requestChannel = "aggregateInput")
    void receive(Event event);

}
