package be.planty.skills.prototyping.handlers.agent;

import be.planty.models.prototyping.ActionResponse;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Response;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.StompHeaders;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static be.planty.skills.prototyping.handlers.NewWebAppIntentHandler.INTENT_NEW_WEB_APP;
import static com.amazon.ask.request.Predicates.intentName;

public class AgentSessionHandler extends be.planty.skills.assistant.handlers.agent.AgentSessionHandler {

    private static final Logger logger = LoggerFactory.getLogger(AgentSessionHandler.class);

    private static final ObjectWriter objectWriter = new ObjectMapper().writerWithDefaultPrettyPrinter();

    public AgentSessionHandler(HandlerInput input, String messageId, CompletableFuture<Optional<Response>> futureResponse) {
        super(input, messageId, futureResponse);
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {

        final String destination = headers.getDestination();

        if (headers.getFirst("correlation-id").equals(messageId)
                && destination.startsWith("/user/queue/action-responses")
                //&& (!emailAddress.isPresent()
                //    || destination.endsWith(emailAddress.get()))
                && payload instanceof ActionResponse
                && this.input.matches(intentName(INTENT_NEW_WEB_APP))
        ) {
            try {
                final String prettyJson = objectWriter.writeValueAsString(payload);
                logger.info("Received action response: " + prettyJson);
                final ActionResponse actionResponse = (ActionResponse) payload;
                final Integer statusCode = actionResponse.statusCode;
                if ( statusCode / 100 == 2) {
                    final String appId = String.valueOf(actionResponse.body);
                    futureResponse.complete(
                            input.getResponseBuilder()
                                    .withSpeech("<speak>"
                                            + "I'm done with the app creation, and the app i.d. is"
                                            + " <say-as interpret-as=\"telephone\">" + appId + "</say-as>."
                                            + "</speak>")
                                    .build());
                } else{
                    futureResponse.complete(
                            input.getResponseBuilder()
                                    .withSpeech("The request failed with error code" +
                                            " <say-as interpret-as=\"telephone\">" + statusCode + "</say-as>.")
                                    .build());
                }
            } catch (JsonProcessingException e) {
                logger.error(e.getMessage());
            }
        } else {
            super.handleFrame(headers, payload);
        }
    }
}