package es.josetesan.retry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Collections;

import static java.lang.System.out;

public class ServiceTest {

    private RetryTemplate retryTemplate;
    private Service service;

    @BeforeEach
    public void setup() {

        service = new Service();

        retryTemplate = new RetryTemplate();

        ExponentialBackOffPolicy exponentialBackOffPolicy = new ExponentialBackOffPolicy();
        exponentialBackOffPolicy.setInitialInterval(1000);
        exponentialBackOffPolicy.setMultiplier(1.2d);
        exponentialBackOffPolicy.setMaxInterval(5000);
        retryTemplate.setBackOffPolicy(exponentialBackOffPolicy);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(5, Collections.singletonMap(NullPointerException.class, true));
        retryPolicy.setMaxAttempts(5);

        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.registerListener(new RetryListener() {
            public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
                out.println("Open");
                return true;
            }

            public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                out.println("Closing");
                System.exit(-1);
            }

            public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                out.format("onError, waiting %d times%n",context.getRetryCount());
            }
        });



    }

    @Test
    public void testRetry() {

        Integer value = 1;
        retryTemplate.execute(context -> {
            service.serviceMethod(value);
            return null;
        });
    }

    @Test
    public void testRetryFails() {

        Integer value = null;
        retryTemplate.execute(context -> {
            service.serviceMethod(value);
            return null;
        });
    }


    @Test
    public void testRetryFailsAndRecover() {
        Integer value = null;
        retryTemplate.execute(context -> { service.serviceMethod(value);return null;},
                              context -> { out.println("Recovered");service.serviceMethod(1);return null;});
    }

}
