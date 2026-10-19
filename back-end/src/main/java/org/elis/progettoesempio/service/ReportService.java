// service/ReportService.java
package org.elis.progettoesempio.service;

public interface ReportService {

    /**
     * Genera il PDF del resoconto di un mese. Riservato a chi gestisce lo spazio
     * (PROPRIETARIO e AMMINISTRATORE): contiene anche gli stipendi dei singoli dipendenti.
     */
    byte[] generaResocontoMensile(Long spazioLavoroId, Long utenteId, int mese, int anno);
}