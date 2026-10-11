// service/impl/NotificaServiceImpl.java
package org.elis.progettoesempio.service.impl;

import java.util.List;

import org.elis.progettoesempio.dto.response.NotificaResponse;
import org.elis.progettoesempio.entity.Notifica;
import org.elis.progettoesempio.entity.Utente;
import org.elis.progettoesempio.exception.RisorsaNonTrovataException;
import org.elis.progettoesempio.mapper.NotificaMapper;
import org.elis.progettoesempio.repository.NotificaRepository;
import org.elis.progettoesempio.service.NotificaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificaServiceImpl implements NotificaService {

    private final NotificaRepository notificaRepository;
    private final NotificaMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<NotificaResponse> elenca(Long utenteId) {
        return notificaRepository.findByUtenteIdOrderByDataCreazioneDesc(utenteId).stream()
                .map(mapper::aResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long contaNonLette(Long utenteId) {
        return notificaRepository.countByUtenteIdAndLettaFalse(utenteId);
    }

    @Override
    @Transactional
    public NotificaResponse segnaComeLetta(Long utenteId, Long notificaId) {
        Notifica notifica = trovaNotificaDellUtente(notificaId, utenteId);
        notifica.setLetta(true);
        return mapper.aResponse(notifica);
    }

    @Override
    @Transactional
    public void segnaTutteComeLette(Long utenteId) {
        notificaRepository.segnaTutteComeLette(utenteId);
    }

    @Override
    @Transactional
    public void elimina(Long utenteId, Long notificaId) {
        notificaRepository.delete(trovaNotificaDellUtente(notificaId, utenteId));
    }

    @Override
    @Transactional
    public void creaPerUtente(Utente destinatario, String titolo, String messaggio) {
        Notifica notifica = new Notifica();
        notifica.setUtente(destinatario);
        notifica.setTitolo(titolo);
        notifica.setMessaggio(messaggio);
        notificaRepository.save(notifica);
    }

    /** Le notifiche sono personali: si cerca sempre per id E per utente */
    private Notifica trovaNotificaDellUtente(Long notificaId, Long utenteId) {
        return notificaRepository.findByIdAndUtenteId(notificaId, utenteId)
                .orElseThrow(() -> new RisorsaNonTrovataException("Notifica non trovata"));
    }
}