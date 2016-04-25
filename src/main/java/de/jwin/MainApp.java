package de.jwin;

import org.apache.camel.main.Main;

public class MainApp {

    public static void main(String... args) throws Exception {
        Main main = new Main();
        main.addRouteBuilder(new RouteBuilder());
        main.run(args);
    }

}

