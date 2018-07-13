package be.planty.skills.prototyping;

import be.planty.skills.prototyping.handlers.*;
import be.planty.skills.prototyping.interceptors.MyRequestInterceptor;
import be.planty.skills.prototyping.interceptors.MyResponseInterceptor;
import com.amazon.ask.Skill;
import com.amazon.ask.SkillStreamHandler;
import com.amazon.ask.Skills;

public class HelloWorldStreamHandler extends SkillStreamHandler {

    private static Skill getSkill() {
        return Skills.standard()
                .addRequestHandlers(
                        new CancelandStopIntentHandler(),
                        new HelloWorldIntentHandler(),
                        new HelpIntentHandler(),
                        new LaunchRequestHandler(),
                        new SessionEndedRequestHandler())
                .addRequestInterceptor(new MyRequestInterceptor())
                .addResponseInterceptor(new MyResponseInterceptor())
                .addExceptionHandler(new MyExecptionHandler())
                .build();
    }

    public HelloWorldStreamHandler() {
        super(getSkill());
    }
 }