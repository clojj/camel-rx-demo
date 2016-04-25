package de.jwin;

import org.apache.camel.*;
import org.apache.camel.rx.ReactiveCamel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Action1;
import rx.observables.ConnectableObservable;
import rx.schedulers.Schedulers;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class RouteBuilder extends org.apache.camel.builder.RouteBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(RouteBuilder.class);

    private Executor executor = Executors.newFixedThreadPool(3);

    private int counter;

    public void configure() throws Exception {

        from("timer:fast-timer?period=1")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        exchange.getOut().setBody(counter++);
                    }
                })
                .to("direct:rx-in");

        getContext().addStartupListener(new StartupListener() {
            @Override
            public void onCamelContextStarted(CamelContext camelContext, boolean b) throws Exception {

                ReactiveCamel rx = new ReactiveCamel(getContext());
                final Observable<Message> messageObservable = rx.toObservable("direct:rx-in");
                ConnectableObservable<Message> connectableObservable = messageObservable.publish();
                connectableObservable.connect();

                connectableObservable.onBackpressureDrop(new Action1<Message>() {
                    @Override
                    public void call(Message message) {
                        LOG.warn("DROPPED {}", message.getBody());
                    }
                }).observeOn(Schedulers.from(executor)).forEach(new Action1<Message>() {
                    @Override
                    public void call(Message message) {
                        try {
                            Thread.sleep(2);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        LOG.info("counter {} thread {}", message.getBody(), Thread.currentThread().getName());
                    }
                });
            }
        });
    }

}
