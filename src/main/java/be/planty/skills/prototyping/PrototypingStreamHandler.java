package be.planty.skills.prototyping;

import be.planty.skills.assistant.AssistantStreamHandler;
import be.planty.skills.prototyping.handlers.NewWebAppIntentHandler;
import com.amazon.ask.Skill;

public class PrototypingStreamHandler extends AssistantStreamHandler {

    private static Skill getSkill() {
        return getSkillBuilder()
                .addRequestHandler(
                        new NewWebAppIntentHandler()
                ).build();
    }

    public PrototypingStreamHandler() {
        super(getSkill());
    }
 }
