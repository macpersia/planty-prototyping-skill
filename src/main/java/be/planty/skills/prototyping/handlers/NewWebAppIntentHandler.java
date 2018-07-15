package be.planty.skills.prototyping.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.services.ServiceException;
import com.amazon.ask.model.services.directive.DirectiveService;
import com.amazon.ask.model.services.directive.DirectiveServiceClient;
import com.amazon.ask.model.services.directive.SendDirectiveRequest;
import com.amazon.ask.model.services.directive.SpeakDirective;
import com.amazon.ask.request.Predicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.util.HashMap;
import java.util.Optional;

import static java.util.Arrays.asList;

public class NewWebAppIntentHandler implements RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(NewWebAppIntentHandler.class);

    @Override
    public boolean canHandle(HandlerInput input) {
        return input.matches(Predicates.intentName("NewWebAppIntent"));
    }

    @Override
    public Optional<Response> handle(HandlerInput input) {
        try {
            final SpeakDirective directive = SpeakDirective.builder()
                    .withSpeech("Sure, please wait while I instruct the agent to create the app…")
                    .build();
            final SendDirectiveRequest directiveRequest = SendDirectiveRequest.builder()
                    .withDirective(directive)
                    .build();
            final DirectiveServiceClient directiveSvc = input.getServiceClientFactory().getDirectiveService();
            directiveSvc.enqueue(directiveRequest);



            final HashMap message = new HashMap() {{
                put("to", "Agent X");
                put("message", "A message to myself!");
            }};



            final String url = "ws://localhost:8080/websocket/messages";
            WebSocketClient socketClient = new StandardWebSocketClient();

            //WebSocketStompClient stompClient = new WebSocketStompClient(socketClient);
            SockJsClient sockJsClient = new SockJsClient(asList(
                    new WebSocketTransport(socketClient)
            ));
            WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);

            stompClient.setMessageConverter(new MappingJackson2MessageConverter());
            final StompSessionHandler handler = new MySessionHandler(directiveSvc, message);
            logger.info("Connecting to: " + url + " ...");
            stompClient.connect(url, handler).addCallback(
                            session -> {
                                session.subscribe("/topic/responses", handler);
                                session.send("/topic/requests", message);
                            },
                            err -> {});
            return Optional.empty();

        } catch (ServiceException e) {
            final String speechText = "Sorry! Something went wrong, and I couldn't fulfill your request.";
            return input.getResponseBuilder()
                    .withSpeech(speechText)
                    .build();
        }
    }

}

class MySessionHandler extends StompSessionHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(MySessionHandler.class);

    private final DirectiveService directiveSvc;
    private final HashMap message;

    public MySessionHandler(DirectiveService directiveSvc, HashMap message) {
        this.directiveSvc = directiveSvc;
        this.message = message;
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        if (headers.getDestination().equals("/topic/responses")) {
            String response = String.valueOf(payload);
            final SpeakDirective directive = SpeakDirective.builder()
                    .withSpeech("I'm done! Your app is ready."
                            + "\nYou got a message from the app too! It says, " + response)
                    .build();
            final SendDirectiveRequest directiveRequest = SendDirectiveRequest.builder()
                    .withDirective(directive)
                    .build();

            try {
                directiveSvc.enqueue(directiveRequest);

            } catch (ServiceException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}
