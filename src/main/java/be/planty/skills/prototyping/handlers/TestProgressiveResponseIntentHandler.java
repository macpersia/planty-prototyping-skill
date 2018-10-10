package be.planty.skills.prototyping.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.services.directive.DirectiveServiceClient;
import com.amazon.ask.model.services.directive.Header;
import com.amazon.ask.model.services.directive.SendDirectiveRequest;
import com.amazon.ask.model.services.directive.SpeakDirective;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;

public class TestProgressiveResponseIntentHandler implements RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(TestProgressiveResponseIntentHandler.class);

    public static final String INTENT_TEST_PROGRESSIVE_RESP = "TestProgressiveResponseIntent";

    @Override
    public boolean canHandle(HandlerInput input) {
        return input.matches(intentName(INTENT_TEST_PROGRESSIVE_RESP));
    }

    @Override
    public Optional<Response> handle(HandlerInput input) {
        final String ackMessage = "This is the acknowledgement message.";
        final SpeakDirective.Builder speakDirBuilder = SpeakDirective.builder().withSpeech(ackMessage);
        final SpeakDirective speakDirective = speakDirBuilder.build();
        //final String apiAccessToken = requestEnvelope.getContext().getSystem().getApiAccessToken();
        final String requestId = input.getRequestEnvelope().getRequest().getRequestId();
        final Header header = Header.builder().withRequestId(requestId).build();
        final SendDirectiveRequest directiveRequest = SendDirectiveRequest.builder()
                .withHeader(header)
                .withDirective(speakDirective)
                .build();
        final DirectiveServiceClient directiveSvc = input.getServiceClientFactory().getDirectiveService();
        directiveSvc.enqueue(directiveRequest);
        logger.info(">>>> Progressive response is enqueued.");
        
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            logger.error(e.getMessage() ,e);
        }

        final String finalResponse = "This is the final message.";
        return input.getResponseBuilder()
                .withSpeech(finalResponse)
                .withSimpleCard("Final Response", finalResponse)
                .build();
    }
}
