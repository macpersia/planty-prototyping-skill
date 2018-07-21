package be.planty.skills.prototyping.handlers;

import org.apache.http.auth.AuthenticationException;
import org.junit.AfterClass;
import org.junit.Test;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for the {@link NewWebAppIntentHandler}.
 */
//@RunWith(SpringRunner.class)
public class NewWebAppIntentHandlerTest {

    @Test
    public void messageAgent() throws ExecutionException, InterruptedException, TimeoutException, AuthenticationException {
        final ListenableFuture<StompSession> futureSession = NewWebAppIntentHandler.messageAgent(null);
        assertNotNull(futureSession);
        final StompSession session = futureSession.completable().get(5, SECONDS);
        assertNotNull(session);
        assertTrue(session.isConnected());
    }

    @AfterClass
    public static void tearDown() throws InterruptedException {
        Thread.sleep(2000);
    }
}
