// NotificaMapper.java
package org.elis.progettoesempio.mapper;

import org.elis.progettoesempio.dto.response.NotificaResponse;
import org.elis.progettoesempio.entity.Notifica;
import org.springframework.stereotype.Component;

@Component
public class NotificaMapper {

    public NotificaResponse aResponse(Notifica notifica) {
        return new NotificaResponse(
                notifica.getId(),
                notifica.getTitolo(),
                notifica.getMessaggio(),
                notifica.getDataCreazione(),
                notifica.isLetta());
    }
}