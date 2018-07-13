package be.planty.skills.prototyping.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.services.ServiceException;
import com.amazon.ask.model.services.directive.SendDirectiveRequest;
import com.amazon.ask.request.Predicates;

import java.util.Optional;

public class NewWebAppIntentHandler implements RequestHandler {

     @Override
     public boolean canHandle(HandlerInput input) {
        return input.matches(Predicates.intentName("NewWebAppIntent"));
     }

     @Override
     public Optional<Response> handle(HandlerInput input) {
//         try {
//             input.getServiceClientFactory().getDirectiveService()
//                     .enqueue(SendDirectiveRequest.builder().build());
//              // other handler logic goes here
//
//         } catch (ServiceException e) {
//             e.printStackTrace();
//         }
         String speechText = "Sure, I'll initiate an app creation, right away!";
         return input.getResponseBuilder()
                 .withSpeech(speechText)
                 //.withSimpleCard("HelloWorld", speechText)
                 .build();
     }

}