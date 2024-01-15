package eu.nebulous.utilityevaluator.communication.sal;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class ProactiveClientProperties {
    
    @Value("${spring.pa-config.rest-url}")
    public String url;
    @Value("${spring.pa-config.login}")
    public String login;
    @Value("${spring.pa-config.password}")
    public String password;

     
    public ProactiveClientProperties(@Value("${spring.pa-config.rest-url}") String url,
                                              @Value("${spring.pa-config.login}") String login,
                                              @Value("${spring.pa-config.password}") String password) {
        this.url = url;
        this.login = login;
        this.password = password;  
    }
} 

