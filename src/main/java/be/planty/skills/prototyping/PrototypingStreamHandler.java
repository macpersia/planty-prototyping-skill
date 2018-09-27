package be.planty.skills.prototyping;

import be.planty.skills.assistant.AssistantStreamHandler;
import be.planty.skills.assistant.handlers.*;
import be.planty.skills.prototyping.handlers.ChangePhoneNoIntentHandler;
import be.planty.skills.prototyping.handlers.GetPhoneNoIntentHandler;
import be.planty.skills.prototyping.handlers.LaunchRequestHandler;
import be.planty.skills.prototyping.handlers.NewWebAppIntentHandler;
import com.amazon.ask.Skills;
import com.amazon.ask.builder.SkillBuilder;

public class PrototypingStreamHandler extends AssistantStreamHandler {

    public static final String ATTRIBUTES_TABLENAME = "prototyping-skill-attributes";

    protected static SkillBuilder getSkillBuilder() {
        return Skills.standard()
                .addRequestHandlers(
                        new HelpIntentHandler(),
                        new LaunchRequestHandler(),
                        new HelloWorldIntentHandler(),
                        new EmailAddressIntentHandler(),
                        new FallbackIntentHandler(),
                        new CancelandStopIntentHandler(),
                        new SessionEndedRequestHandler(),
                        new NewWebAppIntentHandler(),
                        new ChangePhoneNoIntentHandler(),
                        new GetPhoneNoIntentHandler())
//                .addRequestInterceptor(new MyRequestInterceptor())
//                .addResponseInterceptor(new MyResponseInterceptor())
                .addExceptionHandler(new MyExecptionHandler())
                .withTableName(ATTRIBUTES_TABLENAME);
    }

    public PrototypingStreamHandler() {
        super(getSkillBuilder().build());
    }
 }
