package be.planty.skills.prototyping;

import be.planty.skills.prototyping.handlers.*;
import com.amazon.ask.Skill;
import com.amazon.ask.SkillStreamHandler;
import com.amazon.ask.builder.CustomSkillBuilder;

public class HelloWorldStreamHandler extends SkillStreamHandler {

    private static Skill getSkill() {
        try {
            final CustomSkillBuilder skillBuilder = new CustomSkillBuilder();
            return skillBuilder.addRequestHandlers(
                    new CancelandStopIntentHandler(),
                    new HelloWorldIntentHandler(),
                    new HelpIntentHandler(),
                    new LaunchRequestHandler(),
                    new SessionEndedRequestHandler())
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public HelloWorldStreamHandler() {
        super(getSkill());
    }

 }