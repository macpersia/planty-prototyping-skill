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
import org.apache.commons.codec.binary.Base64;
import org.apache.http.auth.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.springframework.util.StringUtils.isEmpty;

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

            try {
                messageAgent(directiveSvc);
                return Optional.empty();

            } catch (AuthenticationException e) {
                logger.error(e.getMessage(), e);
                return Optional.empty();
            }


        } catch (ServiceException e) {
            final String speechText = "Sorry! Something went wrong, and I couldn't fulfill your request.";
            return input.getResponseBuilder()
                    .withSpeech(speechText)
                    .build();
        }
    }

    static ListenableFuture<StompSession> messageAgent(DirectiveServiceClient directiveSvc) throws AuthenticationException {

        final String baseUrl = System.getProperty("be.planty.assistant.login.url");
        final String username = System.getProperty("be.planty.assistant.access.id");
        final String password = System.getProperty("be.planty.assistant.access.key");
        final Map<String, String> request = new HashMap(){{
            put("username", username);
            put("password", password);
        }};
        final ResponseEntity<String> response = new RestTemplate()
                .postForEntity(baseUrl, request, String.class);

        if (response.getStatusCode().isError()) {
            logger.error(response.toString());
            throw new AuthenticationException(response.toString());
        }
        final HttpHeaders respHeaders = response.getHeaders();
        final String authHeader = respHeaders.getFirst("Authorization");
        if (isEmpty(authHeader)) {
            final String msg = "No 'Authorization header found!";
            logger.error(msg + " : " + response.toString());
            throw new AuthenticationException(msg);
        }
        if (!authHeader.startsWith("Bearer ")) {
            final String msg = "The 'Authorization header does not start with 'Bearer '!";
            logger.error(msg + " : " + authHeader);
            throw new AuthenticationException(msg);
        }
        final String accessToken = authHeader.substring(7);

//        final HashMap message = new HashMap() {{
//            put("to", "Agent X");
//            put("message", "A message to myself!");
//        }};
        final String message = "A message to myself!";

        final String url = "ws://localhost:8080/websocket/agent?access_token=" + accessToken;
        final WebSocketClient socketClient = new StandardWebSocketClient();

        //final WebSocketStompClient stompClient = new WebSocketStompClient(socketClient);
        final SockJsClient sockJsClient = new SockJsClient(asList(
                new WebSocketTransport(socketClient)
        ));
        final WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);

        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        final StompSessionHandler handler = new AgentSessionHandler(directiveSvc);

        logger.info("Connecting to: " + url + " ...");
        final ListenableFuture<StompSession> futureSession = stompClient.connect(url, handler);
        futureSession.addCallback(
                session -> {
                    logger.info("Connected!");
                    session.subscribe("/topic/agent.res", handler);
                    logger.info("Sending a message to /topic/agent.req...");
                    session.send("/topic/agent.req", message);
                },
                err -> logger.error(err.getMessage(), err));
        return futureSession;
    }
}

class AgentSessionHandler extends StompSessionHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(AgentSessionHandler.class);

    private final DirectiveService directiveSvc;

    public AgentSessionHandler(DirectiveService directiveSvc) {
        this.directiveSvc = directiveSvc;
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {

        if (headers == null || headers.getDestination() == null)
            return;
        
        if (headers.getDestination().equals("/topic/agent.res")) {
            String response = String.valueOf(payload);
            final SpeakDirective directive = SpeakDirective.builder()
                    .withSpeech("I'm done! Your app is ready."
                            + "\nYou got a message from the app too! It says, " + response)
                    .build();
            final SendDirectiveRequest directiveRequest = SendDirectiveRequest.builder()
                    .withDirective(directive)
                    .build();

            if (directiveSvc == null) {
                logger.warn("No DirectiveService set; Probably in test mode.");
                return;
            }
            try {
                directiveSvc.enqueue(directiveRequest);

            } catch (ServiceException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
        logger.error(exception.toString(), exception);
    }

    @Override
    public void handleTransportError(StompSession session, Throwable exception) {
        logger.error(exception.toString(), exception);
    }
}
