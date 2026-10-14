// service/NotizieService.java
package org.elis.progettoesempio.service;

import java.util.List;

import org.elis.progettoesempio.dto.response.NotiziaResponse;

public interface NotizieService {

    /** Le ultime notizie di tutte le fonti, dalla più recente. Il limite va da 1 a 100. */
    List<NotiziaResponse> ultime(int limite);
}