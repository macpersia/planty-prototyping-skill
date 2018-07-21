package be.planty.skills.prototyping.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Slot;
import com.amazon.ask.model.dialog.DelegateDirective;
import com.amazon.ask.model.services.ServiceException;
import com.amazon.ask.model.services.directive.DirectiveServiceClient;
import com.amazon.ask.model.services.directive.Header;
import com.amazon.ask.model.services.directive.SendDirectiveRequest;
import com.amazon.ask.model.services.directive.SpeakDirective;
import com.amazon.ask.request.Predicates;
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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.amazon.ask.model.DialogState.IN_PROGRESS;
import static java.util.Arrays.asList;
import static org.springframework.util.StringUtils.isEmpty;

public class NewWebAppIntentHandler implements RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(NewWebAppIntentHandler.class);

    private final String baseUrl = System.getProperty("be.planty.assistant.login.url");
    private final String username = System.getProperty("be.planty.assistant.access.id");
    private final String password = System.getProperty("be.planty.assistant.access.key");

    @Override
    public boolean canHandle(HandlerInput input) {
        return input.matches(Predicates.intentName("NewWebAppIntent"));
    }

    @Override
    public Optional<Response> handle(HandlerInput input) {
        try {
            final IntentRequest request = (IntentRequest) input.getRequestEnvelope().getRequest();
            logger.info(">>>> request.getDialogState(): " + request.getDialogState());
            final Slot appNameSlot = request.getIntent().getSlots().get("WebAppName");
            logger.info(">>>> appNameSlot: " + appNameSlot);
            if (isEmpty(appNameSlot.getValue())) {
                final DelegateDirective delegateDirective = DelegateDirective.builder().build();
                //webAppName = delegateDirective.getUpdatedIntent().getSlots().get("WebAppName").getValue();
                logger.info(">>>> Delegating the dialog to Alexa, to get the web app name...");
                return input.getResponseBuilder().addDelegateDirective(null).build();
            } else {
                final String progressReply =
                        (request.getDialogState() == IN_PROGRESS ? "Alright!" : "Sure!")
                        + " Please wait while I instruct the agent to create the app…";
                final SpeakDirective speakDirective = SpeakDirective.builder().withSpeech(progressReply).build();
                final RequestEnvelope requestEnvelope = input.getRequestEnvelope();
                //final String apiAccessToken = requestEnvelope.getContext().getSystem().getApiAccessToken();
                final String requestId = requestEnvelope.getRequest().getRequestId();
                final Header header = Header.builder().withRequestId(requestId).build();
                final SendDirectiveRequest directiveRequest = SendDirectiveRequest.builder()
                        .withHeader(header)
                        .withDirective(speakDirective)
                        .build();
                final DirectiveServiceClient directiveSvc = input.getServiceClientFactory().getDirectiveService();
                directiveSvc.enqueue(directiveRequest);
            }
            logger.info(">>>> Proceeding with app creation...");
            final CompletableFuture<Optional<Response>> futureResponse = messageAgent(input);
            return futureResponse.get();

        } catch (ServiceException | InterruptedException | ExecutionException | AuthenticationException e) {
            logger.error(e.getMessage(), e);
            final String speechText = "Sorry! Something went wrong, and I couldn't fulfill your request.";
            return input.getResponseBuilder()
                    .withSpeech(speechText)
                    .build();
        }
    }

    CompletableFuture<Optional<Response>> messageAgent(HandlerInput input) throws AuthenticationException {

        final CompletableFuture<Optional<Response>> futureResponse = new CompletableFuture<>();

        final String accessToken = login(baseUrl, username, password);
        final String wsUrl = System.getProperty("be.planty.assistant.ws.url");
        final String url = wsUrl + "/action?access_token=" + accessToken;
        final WebSocketStompClient stompClient = createStompClient();
        final StompSessionHandler handler = new AgentSessionHandler(input, futureResponse);

        logger.info("Connecting to: " + url + " ...");
        final ListenableFuture<StompSession> futureSession = stompClient.connect(url, handler);
        futureSession.addCallback(
                session -> {
                    logger.info("Connected!");
                    session.subscribe("/topic/action.res", handler);
                    //final HashMap message = new HashMap() {{
                    //    put("to", "Agent X");
                    //    put("message", "A message to myself!");
                    //}};
                    final IntentRequest intentRequest = (IntentRequest) input.getRequestEnvelope().getRequest();
                    final String appName = intentRequest.getIntent().getSlots().get("WebAppName").getValue();
                    final String message = "Create an app named '" + appName + "'";
                    logger.info("Sending a message to /topic/action.req...");
                    session.send("/topic/action.req", message);
                },
                err -> logger.error(err.getMessage(), err));
        return futureResponse;
    }

    private WebSocketStompClient createStompClient() {
        final WebSocketClient socketClient = new StandardWebSocketClient();

        //final WebSocketStompClient stompClient = new WebSocketStompClient(socketClient);
        final SockJsClient sockJsClient = new SockJsClient(asList(
                new WebSocketTransport(socketClient)
        ));
        final WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);

        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        return stompClient;
    }

    private String login(String baseUrl, String username, String password) throws AuthenticationException {
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
        return authHeader.substring(7);
    }
}

class AgentSessionHandler extends StompSessionHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(AgentSessionHandler.class);
    private final HandlerInput input;
    private final CompletableFuture<Optional<Response>> futureResponse;

    public AgentSessionHandler(HandlerInput input, CompletableFuture<Optional<Response>> futureResponse) {
        this.input = input;
        this.futureResponse = futureResponse;
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {

        if (headers == null || headers.getDestination() == null) {
            futureResponse.complete(Optional.empty());

        } else if (headers.getDestination().equals("/topic/action.res")) {
            final String response = String.valueOf(payload);
            logger.info("Here's the action response: " + response);
            futureResponse.complete(
                    input.getResponseBuilder()
                            .withSpeech("I'm done! Your app is ready.")
                            .build());
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
