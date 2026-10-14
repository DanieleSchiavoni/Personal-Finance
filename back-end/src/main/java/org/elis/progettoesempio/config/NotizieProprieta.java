// config/NotizieProprieta.java
package org.elis.progettoesempio.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Le impostazioni "app.notizie.*" di application.properties */
@ConfigurationProperties(prefix = "app.notizie")
public record NotizieProprieta(
        @DefaultValue("true") boolean abilitato,
        @DefaultValue("15") int minutiCache,
        List<Fonte> fonti) {

    public NotizieProprieta {
        fonti = fonti == null ? List.of() : List.copyOf(fonti);
    }

    /** Una testata: nome da mostrare, indirizzo del feed RSS e lingua (en, it...) */
    public record Fonte(String nome, String url, String lingua) {
    }
}