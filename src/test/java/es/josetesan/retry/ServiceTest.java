package es.josetesan.retry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.StopWatch;

import java.util.Collections;

import static java.lang.System.out;

public class ServiceTest {

    private RetryTemplate retryTemplate;
    private Service service;

    @BeforeEach
    public void setup() {

        service = new Service();

        ExponentialBackOffPolicy exponentialBackOffPolicy = new ExponentialBackOffPolicy();
        exponentialBackOffPolicy.setInitialInterval(1_000);
        exponentialBackOffPolicy.setMultiplier(1.2d);
        exponentialBackOffPolicy.setMaxInterval(5_000);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(10, Collections.singletonMap(NullPointerException.class, true));

        retryTemplate = new RetryTemplate();
        retryTemplate.setBackOffPolicy(exponentialBackOffPolicy);
        retryTemplate.setRetryPolicy(retryPolicy);


        retryTemplate.registerListener(new RetryListener() {
            final StopWatch stopWatch = new StopWatch();
            public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
                stopWatch.start("Opening");
                out.println("Open");
                return true;
            }

            public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                stopWatch.stop();
                out.format("Closing %s%n",stopWatch.prettyPrint());
            }

            public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                stopWatch.stop();
                out.format("onError, waiting %d times, time %s %n",context.getRetryCount(),stopWatch.shortSummary());
                stopWatch.start("onError:"+context.getRetryCount());
            }
        });



    }

    @Test
    public void testRetry() {
        Integer result = retryTemplate.execute(context -> service.serviceMethod(2));
        out.format("Final result is %d%n",result);
        Assertions.assertEquals(1, result);
    }

    @Test
    public void testRetryFails() {
        Assertions.assertThrows(NullPointerException.class,() -> retryTemplate.execute(context -> service.serviceMethod(null)));
    }


    @Test
    public void testRetryFailsAndRecover() {
        Integer result = retryTemplate.execute(context ->  service.serviceMethod(null),
                              context -> {
                                           out.println("Recovered");
                                           return service.serviceMethod(2);
                                         }
                            );

        out.format("Final result is %d%n",result);
        Assertions.assertEquals(1, result);
    }

}
