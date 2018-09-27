package be.planty.skills.prototyping.handlers.agent;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Response;
import org.apache.http.auth.AuthenticationException;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class AgentClient extends be.planty.skills.assistant.handlers.agent.AgentClient {

    @Override
    public CompletableFuture<Optional<Response>> messageAgent(HandlerInput input, Object payload) throws AuthenticationException {
        return super.messageAgent(input, payload);
    }

    @Override
    protected AgentSessionHandler createSessionHandler(HandlerInput input, CompletableFuture<Optional<Response>> futureResponse, String messageId) {
        return new AgentSessionHandler(input, messageId, futureResponse);
    }
}