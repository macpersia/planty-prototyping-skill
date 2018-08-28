package be.planty.skills.prototyping.handlers;

import be.planty.models.prototyping.ActionRequest;
import be.planty.skills.assistant.handlers.agent.AgentClient;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Slot;
import com.amazon.ask.model.services.ServiceException;
import com.amazon.ask.model.services.directive.DirectiveServiceClient;
import com.amazon.ask.model.services.directive.Header;
import com.amazon.ask.model.services.directive.SendDirectiveRequest;
import com.amazon.ask.model.services.directive.SpeakDirective;
import com.amazon.ask.request.Predicates;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.auth.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.amazon.ask.model.DialogState.IN_PROGRESS;
import static org.springframework.util.StringUtils.isEmpty;

public class NewWebAppIntentHandler implements RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(NewWebAppIntentHandler.class);

    private final AgentClient agentSessionHelper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public NewWebAppIntentHandler() {
        agentSessionHelper = new AgentClient();
    }

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
                //final DelegateDirective delegateDirective = DelegateDirective.builder().build();
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
            final String message = createMessage(input);
            final CompletableFuture<Optional<Response>> futureResponse = agentSessionHelper.messageAgent(input, message);
            return futureResponse.get();

        } catch (ServiceException | InterruptedException | ExecutionException | AuthenticationException | JsonProcessingException e) {
            logger.error(e.getMessage(), e);
            final String speechText = "Sorry! Something went wrong, and I couldn't fulfill your request.";
            return input.getResponseBuilder()
                    .withSpeech(speechText)
                    .build();
        }
    }

    private String createMessage(HandlerInput input) throws JsonProcessingException {
        //final HashMap message = new HashMap() {{
        //    put("to", "Agent X");
        //    put("message", "A message to myself!");
        //}};
        final IntentRequest intentRequest = (IntentRequest) input.getRequestEnvelope().getRequest();
        final String appName = intentRequest.getIntent().getSlots().get("WebAppName").getValue();

        //return "Create an app named '" + appName + "'";
        final ActionRequest request = new ActionRequest("NewWebApp", new HashMap() {{
            put("WebAppName", appName);
        }});
        return objectMapper.writeValueAsString(request);
    }

}

