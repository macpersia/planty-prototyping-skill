package be.planty.skills.prototyping.handlers;

import be.planty.models.prototyping.ActionRequest;
import be.planty.skills.assistant.handlers.AssistantUtils;
import be.planty.skills.prototyping.handlers.agent.AgentClient;
import com.amazon.ask.attributes.AttributesManager;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.*;
import com.amazon.ask.model.services.ServiceException;
import com.amazon.ask.model.services.directive.DirectiveServiceClient;
import com.amazon.ask.model.services.directive.Header;
import com.amazon.ask.model.services.directive.SendDirectiveRequest;
import com.amazon.ask.model.services.directive.SpeakDirective;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.auth.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.*;

import static be.planty.skills.prototyping.handlers.ChangePhoneNoIntentHandler.SLOT_PHONE_NO;
import static com.amazon.ask.model.DialogState.IN_PROGRESS;
import static com.amazon.ask.request.Predicates.intentName;
import static java.util.Optional.ofNullable;
import static org.springframework.util.StringUtils.isEmpty;

public class NewWebAppIntentHandler implements RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(NewWebAppIntentHandler.class);

    public static final String INTENT_NEW_WEB_APP = "NewWebAppIntent";

    public static final String SLOT_APP_NAME = "appName";

    private final AgentClient agentClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ExecutorService cachedThreadPool = Executors.newCachedThreadPool();

    public NewWebAppIntentHandler() {
        agentClient = new AgentClient();
    }

    @Override
    public boolean canHandle(HandlerInput input) {
        return input.matches(intentName(INTENT_NEW_WEB_APP));
    }

    @Override
    public Optional<Response> handle(HandlerInput input) {
        try {
            // Since it  could add too much latency to fetch the email address and to create necessary factories,
            // we try to prepare as soon as possible, so that by the time we need them, they'll be already cached.
            cachedThreadPool.execute(() -> AssistantUtils.getEmailAddress(input));
            cachedThreadPool.execute(() -> input.getServiceClientFactory().getDirectiveService());

            final RequestEnvelope reqEnvelope = input.getRequestEnvelope();
            final IntentRequest request = (IntentRequest) reqEnvelope.getRequest();
            logger.info(">>>> request.getDialogState(): " + request.getDialogState());
            final Slot appNameSlot = request.getIntent().getSlots().get(SLOT_APP_NAME);
            logger.info(">>>> appNameSlot: " + appNameSlot);
            if (isEmpty(appNameSlot.getValue())) {
                logger.info(">>>> Delegating the dialog to Alexa, to get the web app name...");
                return input.getResponseBuilder().addDelegateDirective(null).build();
            }

            final Session session = reqEnvelope.getSession();
            logger.info(">>>> new session? " + session.getNew());

            final String progressReply =
                    (request.getDialogState() == IN_PROGRESS ? "Alright!" : "Sure!")
                            + " Please wait while I instruct the agent to create the app…";
            final SpeakDirective.Builder speakDirBuilder = SpeakDirective.builder().withSpeech(progressReply);
            final SpeakDirective speakDirective = speakDirBuilder.build();
            //final String apiAccessToken = requestEnvelope.getContext().getSystem().getApiAccessToken();
            final String requestId = request.getRequestId();
            final Header header = Header.builder().withRequestId(requestId).build();
            final SendDirectiveRequest directiveRequest = SendDirectiveRequest.builder()
                    .withHeader(header)
                    .withDirective(speakDirective)
                    .build();
            final DirectiveServiceClient directiveSvc = input.getServiceClientFactory().getDirectiveService();
            directiveSvc.enqueue(directiveRequest);
            logger.info(">>>> Progressive response is enqueued.");

            //logger.info(">>>> Proceeding with app creation...");
            //final String actionReq = createMessage(input);
            //final CompletableFuture<Optional<Response>> futureResponse = agentClient.messageAgent(input, actionReq);
            final ActionRequest actionReq = createRequest(input);
            final CompletableFuture<Optional<Response>> futureResponse = agentClient.messageAgent(input, actionReq);
            final Optional<Response> response = futureResponse.get(45, TimeUnit.SECONDS);
            logger.info(">>>> Final response is ready.");
            return response;

        } catch (ServiceException | InterruptedException | ExecutionException | AuthenticationException e) {
            logger.error(e.getMessage(), e);
            final String speechText = "Sorry! Something went wrong, and I couldn't fulfill your request.";
            return input.getResponseBuilder()
                    .withSpeech(speechText)
                    .build();
        } catch (TimeoutException e) {
            logger.error(e.getMessage(), e);
            final String speechText = "Sorry! I did not receive a response from the agent in a timely manner.";
            return input.getResponseBuilder()
                    .withSpeech(speechText)
                    .build();
        }
    }

    private ActionRequest createRequest(HandlerInput input) {
        final IntentRequest intentRequest = (IntentRequest) input.getRequestEnvelope().getRequest();
        final String appName = intentRequest.getIntent().getSlots().get(SLOT_APP_NAME).getValue();

        final AttributesManager attsManager = input.getAttributesManager();
        final String userId = input.getRequestEnvelope().getSession().getUser().getUserId();
        //final Optional<String> phoneNo = ofNullable(attsManager.getSessionAttributes().get(SLOT_PHONE_NO))
        final Optional<String> phoneNo = ofNullable(attsManager.getPersistentAttributes().get(userId + "-" + SLOT_PHONE_NO))
                .map(Object::toString);

        //return "Create an app named '" + appName + "'";
        return new ActionRequest(INTENT_NEW_WEB_APP, new HashMap() {{
            put(SLOT_APP_NAME, appName);
            put(SLOT_PHONE_NO, phoneNo.orElse(null));
        }});
    }

}

