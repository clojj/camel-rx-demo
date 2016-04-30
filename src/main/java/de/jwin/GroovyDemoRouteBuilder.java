package de.jwin;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.GenericFile;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.Charset;

/**
 * Created by jwin on 30.04.16.
 */
public class GroovyDemoRouteBuilder extends RouteBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(GroovyDemoRouteBuilder.class);

    @Override
    public void configure() throws Exception {
        from("file:///home/jwin/camel-rx-demo/src/test/resources?noop=true&idempotent=true")
                .process(exchange -> {
                            GenericFile file = (GenericFile) exchange.getIn().getBody();
                            LOG.info(file.toString());
                            String html = FileUtils.readFileToString((File) file.getFile(), Charset.defaultCharset());
                            exchange.getOut().setBody(html);
                        }
                )
                .setHeader("").groovy("resource:classpath:processWithJsoup.groovy")
                .log(LoggingLevel.INFO, "Finish");
    }
}
