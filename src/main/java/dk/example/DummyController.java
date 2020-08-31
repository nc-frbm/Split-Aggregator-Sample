package dk.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DummyController {

    private final EventGateway gateway;

    @Autowired
    public DummyController(EventGateway gateway) {
        this.gateway = gateway;
    }

    @RequestMapping("/{correlationId}")
    public String index(@PathVariable("correlationId") String correlationId) {

        gateway.send(new Event(correlationId, "HelloWorld"));
        return "Greetings from Spring Boot!";
    }

}
