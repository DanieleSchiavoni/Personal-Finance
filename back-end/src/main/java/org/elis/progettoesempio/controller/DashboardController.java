// DashboardController.java
package org.elis.progettoesempio.controller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.elis.progettoesempio.dto.response.PuntoTrendResponse;
import org.elis.progettoesempio.dto.response.RiepilogoDashboardResponse;
import org.elis.progettoesempio.dto.response.TotaleCategoriaResponse;
import org.elis.progettoesempio.entity.enumerazioni.Granularita;
import org.elis.progettoesempio.security.UtenteCorrente;
import org.elis.progettoesempio.service.DashboardService;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/spazi-lavoro/{spazioId}/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /** Se mese e anno mancano si usa il mese corrente */
    @GetMapping("/riepilogo")
    public RiepilogoDashboardResponse riepilogo(@PathVariable Long spazioId, @UtenteCorrente Long utenteId,
            @RequestParam(required = false) Integer mese, @RequestParam(required = false) Integer anno) {
        YearMonth oggi = YearMonth.now();
        return dashboardService.riepilogo(spazioId, utenteId,
                mese != null ? mese : oggi.getMonthValue(),
                anno != null ? anno : oggi.getYear());
    }

    /** ?granularita=MESE|SETTIMANA&dal=2026-01-01&al=2026-10-11: tutti i parametri sono facoltativi */
    @GetMapping("/trend")
    public List<PuntoTrendResponse> trend(@PathVariable Long spazioId, @UtenteCorrente Long utenteId,
            @RequestParam(required = false) Granularita granularita,
            @RequestParam(required = false) LocalDate dal, @RequestParam(required = false) LocalDate al) {
        return dashboardService.trend(spazioId, utenteId, granularita, dal, al);
    }

    @GetMapping("/spese-per-categoria")
    public List<TotaleCategoriaResponse> spesePerCategoria(@PathVariable Long spazioId,
            @UtenteCorrente Long utenteId,
            @RequestParam(required = false) LocalDate dal, @RequestParam(required = false) LocalDate al) {
        return dashboardService.spesePerCategoria(spazioId, utenteId, dal, al);
    }
}