package be.planty.skills.prototyping.interceptors;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.interceptor.ResponseInterceptor;
import com.amazon.ask.model.Response;

import java.util.Optional;

public class MyResponseInterceptor implements ResponseInterceptor {

    @Override
    public void process(HandlerInput handlerInput, Optional<Response> optional) {
        // handlerInput.getAttributesManager().savePersistentAttributes();
    }
}
