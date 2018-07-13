package be.planty.skills.prototyping.interceptors;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.interceptor.RequestInterceptor;

public class MyRequestInterceptor implements RequestInterceptor {

    @Override
    public void process(HandlerInput handlerInput) {
        // handlerInput.getAttributesManager().savePersistentAttributes();
    }
}
