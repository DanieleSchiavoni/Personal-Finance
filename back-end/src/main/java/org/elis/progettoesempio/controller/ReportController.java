// controller/ReportController.java
package org.elis.progettoesempio.controller;

import java.time.YearMonth;

import org.elis.progettoesempio.security.UtenteCorrente;
import org.elis.progettoesempio.service.ReportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/spazi-lavoro/{spazioId}/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /** Scarica il PDF del mese. Se mese e anno mancano si usa il mese corrente. */
    @GetMapping("/mensile")
    public ResponseEntity<byte[]> resocontoMensile(@PathVariable Long spazioId, @UtenteCorrente Long utenteId,
            @RequestParam(required = false) Integer mese, @RequestParam(required = false) Integer anno) {
        YearMonth oggi = YearMonth.now();
        int meseScelto = mese != null ? mese : oggi.getMonthValue();
        int annoScelto = anno != null ? anno : oggi.getYear();

        byte[] pdf = reportService.generaResocontoMensile(spazioId, utenteId, meseScelto, annoScelto);
        String nomeFile = String.format("resoconto-%d-%02d.pdf", annoScelto, meseScelto);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nomeFile).build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}