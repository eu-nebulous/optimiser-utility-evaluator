package eu.nebulous.utilityevaluator.communication.exnconnector;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@PropertySource("./application.properties")
@Component
@Slf4j
@Getter
public class ConnectionProperties {
    @Value("${spring.exn.broker-url}")
    String brokerUrl;
    @Value("${spring.exn.broker-port}")
    Integer brokerPort;
    @Value("${spring.exn.user}")
    String brokerUsername;
    @Value("${spring.exn.password}")
    String brokerPassword;

    public ConnectionProperties(@Value("${spring.exn.broker-url}") String url, @Value("${spring.exn.broker-port}") Integer port,
                                @Value("${spring.exn.user}") String user, @Value("${spring.exn.password}") String password) {
        this.brokerUrl = url;
        this.brokerPort = port;
        this.brokerUsername= user;
        this.brokerPassword = password;
        log.info("Got connection properties: BROKER_URL: {}, BROKER_PORT: {} BROKER_USERNAME: {}", url, port, user );
    }
}
