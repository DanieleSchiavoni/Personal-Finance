// service/DashboardService.java
package org.elis.progettoesempio.service;

import java.time.LocalDate;
import java.util.List;

import org.elis.progettoesempio.dto.response.PuntoTrendResponse;
import org.elis.progettoesempio.dto.response.RiepilogoDashboardResponse;
import org.elis.progettoesempio.dto.response.TotaleCategoriaResponse;
import org.elis.progettoesempio.entity.enumerazioni.Granularita;

/** Qualunque membro dello spazio può consultare la dashboard */
public interface DashboardService {

    /** Schede principali del mese: saldo, entrate, uscite, budget, obiettivi, ultime transazioni */
    RiepilogoDashboardResponse riepilogo(Long spazioLavoroId, Long utenteId, int mese, int anno);

    /**
     * Andamento per settimana o mese. Se mancano granularità o date: mesi, ultimi 12 periodi.
     * L'intervallo viene allineato a periodi interi (settimane da lunedì, mesi dal primo giorno).
     */
    List<PuntoTrendResponse> trend(Long spazioLavoroId, Long utenteId, Granularita granularita,
            LocalDate dal, LocalDate al);

    /** Distribuzione delle uscite per categoria. Se mancano le date: mese corrente. */
    List<TotaleCategoriaResponse> spesePerCategoria(Long spazioLavoroId, Long utenteId, LocalDate dal, LocalDate al);
}