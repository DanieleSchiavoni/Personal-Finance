// config/NotizieConfig.java
package org.elis.progettoesempio.config;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(NotizieProprieta.class)
public class NotizieConfig {

    /** L'orologio si inietta, così nei test si può controllare il passare del tempo */
    @Bean
    public Clock orologio() {
        return Clock.systemUTC();
    }
}