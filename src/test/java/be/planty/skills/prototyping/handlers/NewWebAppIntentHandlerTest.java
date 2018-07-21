package be.planty.skills.prototyping.handlers;

import com.amazon.ask.model.Response;
import org.apache.http.auth.AuthenticationException;
import org.junit.AfterClass;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.*;

/**
 * Test class for the {@link NewWebAppIntentHandler}.
 */
//@RunWith(SpringRunner.class)
public class NewWebAppIntentHandlerTest {

    @Test
    public void messageAgent() throws ExecutionException, InterruptedException, TimeoutException, AuthenticationException {
        final CompletableFuture<Optional<Response>> futureSession = NewWebAppIntentHandler.messageAgent(null);
        assertNotNull(futureSession);
        final Optional<Response> optResponse = futureSession.get(5, SECONDS);
        assertTrue("No response is present!", optResponse.isPresent());
        assertEquals("Some speech!", optResponse.get().getOutputSpeech());
    }

    @AfterClass
    public static void tearDown() throws InterruptedException {
        Thread.sleep(2000);
    }
}
