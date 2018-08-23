package be.planty.skills.prototyping;

import be.planty.skills.assistant.AssistantStreamHandler;
import be.planty.skills.assistant.handlers.*;
import be.planty.skills.assistant.handlers.interceptors.MyRequestInterceptor;
import be.planty.skills.assistant.handlers.interceptors.MyResponseInterceptor;
import be.planty.skills.prototyping.handlers.LaunchRequestHandler;
import be.planty.skills.prototyping.handlers.NewWebAppIntentHandler;
import com.amazon.ask.Skills;
import com.amazon.ask.builder.SkillBuilder;

public class PrototypingStreamHandler extends AssistantStreamHandler {

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
                        new NewWebAppIntentHandler())
                .addRequestInterceptor(new MyRequestInterceptor())
                .addResponseInterceptor(new MyResponseInterceptor())
                .addExceptionHandler(new MyExecptionHandler());
    }

    public PrototypingStreamHandler() {
        super(getSkillBuilder().build());
    }
 }
