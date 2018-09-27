package be.planty.skills.prototyping.handlers;

import com.amazon.ask.attributes.AttributesManager;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static be.planty.skills.prototyping.handlers.ChangePhoneNoIntentHandler.SLOT_PHONE_NO;
import static com.amazon.ask.request.Predicates.intentName;
import static org.springframework.util.StringUtils.isEmpty;

public class GetPhoneNoIntentHandler implements RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(GetPhoneNoIntentHandler.class);

    public static final String INTENT_GET_PHONE_NO = "GetPhoneNoIntent";

    @Override
    public boolean canHandle(HandlerInput input) {
        return input.matches(intentName(INTENT_GET_PHONE_NO));
    }

    @Override
    public Optional<Response> handle(HandlerInput input) {
        final AttributesManager attsManager = input.getAttributesManager();
        final String userId = input.getRequestEnvelope().getSession().getUser().getUserId();
        final Object phoneNoAtt = attsManager.getPersistentAttributes().get(userId  + "-" + SLOT_PHONE_NO);
        logger.info(">>>> phoneNoAtt: " + phoneNoAtt);
        final String phoneNo = String.valueOf(phoneNoAtt);
        final String response =
                isEmpty(phoneNoAtt) ?
                        "You haven't registered any phone number yet!"
                        : "The phone number is "
                        + phoneNo.substring(0, 2) + " "
                        + phoneNo.substring(2, 4) + " "
                        + phoneNo.substring(4, 6) + " "
                        + phoneNo.substring(6, 8) + " "
                        + phoneNo.substring(8);
        return input.getResponseBuilder()
                .withSpeech(response)
                .withSimpleCard("Agent Response", response)
                .build();
    }
}
