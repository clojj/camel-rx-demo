package de.jwin;

import org.apache.camel.main.Main;

public class MainApp {

    public static void main(final String... args) throws Exception {

/*
        CamelContext context = new DefaultCamelContext();
        context.addRoutes(new RxDemoRouteBuilder());
        context.start();
        Thread.sleep(10000);
        context.stop();
*/
        final Main main = new Main();
//        main.addRouteBuilder(new RxDemoRouteBuilder());
        main.addRouteBuilder(new GroovyDemoRouteBuilder());
        main.run(args);
    }

}

