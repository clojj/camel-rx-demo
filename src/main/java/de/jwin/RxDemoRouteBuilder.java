package de.jwin;

import org.apache.camel.Message;
import org.apache.camel.component.metrics.routepolicy.MetricsRoutePolicyFactory;
import org.apache.camel.rx.ReactiveCamel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class RxDemoRouteBuilder extends org.apache.camel.builder.RouteBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(RxDemoRouteBuilder.class);

    private Executor executor = Executors.newFixedThreadPool(3);
/*
    private Executor executor = Executors.newFixedThreadPool(3,
        r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        });
*/
/*
    private Executor executor = MoreExecutors.getExitingExecutorService(
			(ThreadPoolExecutor) Executors.newFixedThreadPool(4),
			3, TimeUnit.SECONDS //period after which executor will be automatically closed
	);
*/

    private int counter;

    public void configure() throws Exception {
        from("timer:fast-timer?period=1")
                .to("metrics:timer:simple.timer?action=start")
                .process(exchange -> {
                    Thread.sleep(500);
                    exchange.getOut().setBody(counter++);
                })
                .to("direct:rx-in")
                .to("metrics:timer:simple.timer?action=stop");


        final MetricsRoutePolicyFactory routePolicyFactory = new MetricsRoutePolicyFactory();
        routePolicyFactory.setUseJmx(true);
        getContext().addRoutePolicyFactory(routePolicyFactory);

        getContext().addStartupListener((camelContext, b) -> {

            ReactiveCamel rx = new ReactiveCamel(camelContext);
            final Observable<Message> messageObservable = rx.toObservable("direct:rx-in");
/*
            ConnectableObservable<Message> connectableObservable = messageObservable.publish();
            connectableObservable.connect();
			connectableObservable
*/

            messageObservable
//					.onBackpressureDrop()
                    .onBackpressureDrop(message -> LOG.warn("DROPPED {}", message.getBody()))
                    .observeOn(Schedulers.from(executor))
                    .forEach(message -> {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        LOG.info("counter {} thread {}", message.getBody(), Thread.currentThread().getName());
                    });
        });
    }

}
