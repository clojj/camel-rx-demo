package de.jwin;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;

public class MainApp {

    public static void main(final String... args) throws Exception {

        CamelContext context = new DefaultCamelContext();
        context.addRoutes(new RouteBuilder());
        context.start();
        Thread.sleep(10000);
        context.stop();
/*
        final Main main = new Main();
        main.addRouteBuilder(new RouteBuilder());
        main.run(args);
*/
    }

}

