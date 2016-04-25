package de.jwin;

import com.google.common.util.concurrent.MoreExecutors;
import org.apache.camel.Message;
import org.apache.camel.rx.ReactiveCamel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.observables.ConnectableObservable;
import rx.schedulers.Schedulers;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RouteBuilder extends org.apache.camel.builder.RouteBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(RouteBuilder.class);

    //    private Executor executor = Executors.newFixedThreadPool(3);
/*
    private Executor executor = Executors.newFixedThreadPool(3,
        r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        });
*/
    private Executor executor = MoreExecutors.getExitingExecutorService(
            (ThreadPoolExecutor) Executors.newFixedThreadPool(4),
            3, TimeUnit.SECONDS //period after which executor will be automatically closed
    );

    private int counter;

    public void configure() throws Exception {

        from("timer:fast-timer?period=1")
                .process(exchange -> {
                    exchange.getOut().setBody(counter++);
                })
                .to("direct:rx-in");

        getContext().addStartupListener((camelContext, b) -> {

            ReactiveCamel rx = new ReactiveCamel(camelContext);
            final Observable<Message> messageObservable = rx.toObservable("direct:rx-in");
            ConnectableObservable<Message> connectableObservable = messageObservable.publish();
            connectableObservable.connect();

            connectableObservable
                    .onBackpressureDrop(message -> LOG.warn("DROPPED {}", message.getBody()))
//                    .onBackpressureDrop()
                    .observeOn(Schedulers.from(executor)).forEach(message -> {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                LOG.info("counter {} thread {}", message.getBody(), Thread.currentThread().getName());
            });
        });
    }

}
