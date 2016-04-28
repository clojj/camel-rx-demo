package de.jwin;

import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.metrics.routepolicy.MetricsRoutePolicyFactory;
import org.apache.camel.rx.ObservableBody;
import org.apache.camel.rx.ReactiveCamel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

class RxDemoRouteBuilder extends org.apache.camel.builder.RouteBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(RxDemoRouteBuilder.class);

    private Executor executor = Executors.newFixedThreadPool(4);
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

        ProducerTemplate template = getContext().createProducerTemplate();

        from("timer:slow-timer?period=1000").id("rx-in-slow")
                .process(exchange -> {
                    exchange.getOut().setBody("from rx-in-slow " + exchange.getIn().getHeader("firedTime"));
                })
                .to("direct:rx-in-slow")
                .to("direct:rx-in-slow'");

        from("timer:fast-timer?period=1").id("rx-in")
//                .to("metrics:timer:simple.timer?action=start")
                .process(exchange -> {
                    exchange.getOut().setBody(counter++);
                })
//                .to("metrics:timer:simple.timer?action=stop")
                .to("direct:rx-in");

        from("direct:rx-out").id("rx-out")
                .process(exchange -> LOG.info("@rx-out " + exchange.getIn().getBody()))
                // elasticsearch
                .process(exchange -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("content", (String) exchange.getIn().getBody());
                    String indexId = template.requestBody("direct:es-index", map, String.class);
                    LOG.info("indexId {}", indexId);
                });

        class RxRouteInline extends ObservableBody<String> {
            private RxRouteInline() {
                super(String.class);
            }

            protected void configure(Observable<String> observable) {
                // lets process the messages using the RX API
                observable
                        .map(body -> "Hello " + body)
                        .subscribe(body -> {
                            template.sendBody("direct:rx-out", body);
                        });
            }
        }
        from("direct:rx-in-slow'").process(new RxRouteInline());

        // elasticsearch
        from("direct:es-index")
                .log(LoggingLevel.INFO, "indexing")
                .to("elasticsearch://my-es?operation=INDEX&indexName=test&indexType=string&ip=127.0.0.1&port=9300");

        // setup JMX
        final MetricsRoutePolicyFactory routePolicyFactory = new MetricsRoutePolicyFactory();
        routePolicyFactory.setUseJmx(true);
        getContext().addRoutePolicyFactory(routePolicyFactory);


        getContext().addStartupListener((camelContext, b) -> {

            ReactiveCamel rx = new ReactiveCamel(camelContext);

            final Observable<Message> rxIn = rx.toObservable("direct:rx-in");
            Observable<Message> rxInSlow = rx.toObservable("direct:rx-in-slow");
/*
            ConnectableObservable<Message> connectableObservable = rxIn.publish();
            connectableObservable.connect();
			connectableObservable
*/

            Subscription subscription = rxIn
//					.onBackpressureDrop()
                    .onBackpressureDrop(message -> LOG.debug("DROPPED {}", message.getBody()))
//                    .subscribeOn(Schedulers.from(executor))
                    .observeOn(Schedulers.from(executor))
                    .mergeWith(rxInSlow)
                    .subscribe(message -> {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        LOG.info("counter {} on thread {}", message.getBody(), Thread.currentThread().getName());
//                        rx.sendTo(Observable.just(message.getBody()), "direct:rx-out");
                    });
        });
    }

}
