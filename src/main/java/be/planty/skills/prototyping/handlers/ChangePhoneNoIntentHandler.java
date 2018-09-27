package be.planty.skills.prototyping.handlers;

import com.amazon.ask.attributes.AttributesManager;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Slot;
import com.amazon.ask.model.SlotConfirmationStatus;
import com.amazon.ask.request.Predicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.amazon.ask.model.DialogState.IN_PROGRESS;
import static java.util.Optional.empty;
import static org.springframework.util.StringUtils.isEmpty;

public class ChangePhoneNoIntentHandler implements RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChangePhoneNoIntentHandler.class);

    public static final String INTENT_CHANGE_PHONE_NO = "ChangePhoneNoIntent";

    public static final String SLOT_PHONE_NO = "phoneNo";

    @Override
    public boolean canHandle(HandlerInput input) {
        return input.matches(Predicates.intentName(INTENT_CHANGE_PHONE_NO));
    }

    @Override
    public Optional<Response> handle(HandlerInput input) {
        final IntentRequest request = (IntentRequest) input.getRequestEnvelope().getRequest();
        logger.info(">>>> request.getDialogState(): " + request.getDialogState());
        final Slot phoneNoSlot = request.getIntent().getSlots().get(SLOT_PHONE_NO);
        logger.info(">>>> phoneNoSlot: " + phoneNoSlot);
        final SlotConfirmationStatus confirmationStatus = phoneNoSlot.getConfirmationStatus();
        if (isEmpty(phoneNoSlot.getValue())) {
            logger.info(">>>> Delegating the dialog to Alexa, to get the phone number...");
            return input.getResponseBuilder().addDelegateDirective(null).build();

        } if (confirmationStatus.equals(SlotConfirmationStatus.NONE)) {
            logger.info(">>>> Delegating the dialog to Alexa, to confirm the phone number...");
            return input.getResponseBuilder().addDelegateDirective(null).build();

        } else if (confirmationStatus.equals(SlotConfirmationStatus.CONFIRMED)) {
            final AttributesManager attsManager = input.getAttributesManager();
            final String userId = input.getRequestEnvelope().getSession().getUser().getUserId();
            //attsManager.getSessionAttributes().put(SLOT_PHONE_NO, phoneNoSlot.getValue());
            attsManager.getPersistentAttributes().put(userId + "-" + SLOT_PHONE_NO, phoneNoSlot.getValue());
            attsManager.savePersistentAttributes();

            final String response =
                    (request.getDialogState() == IN_PROGRESS ? "Alright!" : "Sure!")
                            + " Consider it done!";
            return input.getResponseBuilder()
                    .withSpeech(response)
                    .withSimpleCard("Agent Response", response)
                    .build();
        }
        return empty();
    }
}
