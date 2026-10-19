// service/impl/GeneratorePdfResoconto.java
package org.elis.progettoesempio.service.impl;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

import org.elis.progettoesempio.dto.response.BudgetResponse;
import org.elis.progettoesempio.dto.response.TransazioneResponse;
import org.elis.progettoesempio.entity.enumerazioni.MetodoPagamento;
import org.elis.progettoesempio.entity.enumerazioni.StatoBudget;
import org.elis.progettoesempio.entity.enumerazioni.TipoTransazione;
import org.elis.progettoesempio.service.impl.DatiResoconto.RigaCategoria;
import org.elis.progettoesempio.service.impl.DatiResoconto.RigaStipendio;
import org.springframework.stereotype.Component;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

/** Disegna il PDF del resoconto mensile. Non accede al database: lavora solo sui dati ricevuti. */
@Component
public class GeneratorePdfResoconto {

    private static final Color VERDE = new Color(25, 135, 84);
    private static final Color ROSSO = new Color(220, 53, 69);
    private static final Color GRIGIO = new Color(108, 117, 125);
    private static final Color SFONDO_INTESTAZIONE = new Color(231, 240, 255);
    private static final Color SFONDO_TOTALE = new Color(246, 247, 249);
    private static final Color BORDO = new Color(222, 226, 230);

    private static final Font TITOLO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
    private static final Font SOTTOTITOLO = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.NORMAL, GRIGIO);
    private static final Font SEZIONE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
    private static final Font TESTO = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private static final Font TESTO_GRASSETTO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    private static final Font PIEDE = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, GRIGIO);

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_ORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'alle' HH:mm");

    public byte[] genera(DatiResoconto dati) {
        ByteArrayOutputStream flusso = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 40, 40, 48, 56);
        try {
            PdfWriter scrittore = PdfWriter.getInstance(documento, flusso);
            scrittore.setPageEvent(new NumeroPagina());
            documento.addTitle("Resoconto mensile - " + nomePeriodo(dati));
            documento.addAuthor("Personal Finance");
            documento.open();

            aggiungiIntestazione(documento, dati);
            aggiungiRiepilogo(documento, dati);
            aggiungiCategorie(documento, "Entrate per categoria", dati.entratePerCategoria(), dati.valuta());
            aggiungiCategorie(documento, "Uscite per categoria", dati.uscitePerCategoria(), dati.valuta());
            aggiungiBudget(documento, dati);
            aggiungiStipendi(documento, dati);
            aggiungiTransazioni(documento, dati);

            documento.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Impossibile generare il PDF del resoconto", e);
        }
        return flusso.toByteArray();
    }

    private void aggiungiIntestazione(Document documento, DatiResoconto dati) throws DocumentException {
        documento.add(new Paragraph("Resoconto mensile", TITOLO));
        documento.add(new Paragraph(dati.nomeSpazio() + "  |  " + nomePeriodo(dati), SOTTOTITOLO));
        Paragraph generato = new Paragraph("Generato il " + dati.generatoIl().format(DATA_ORA), PIEDE);
        generato.setSpacingAfter(8);
        documento.add(generato);
    }

    private void aggiungiRiepilogo(Document documento, DatiResoconto dati) throws DocumentException {
        documento.add(titoloSezione("Riepilogo"));
        PdfPTable tabella = nuovaTabella(60, 40);
        rigaRiepilogo(tabella, "Entrate", formatta(dati.entrate(), dati.valuta()), VERDE);
        rigaRiepilogo(tabella, "Uscite", formatta(dati.uscite(), dati.valuta()), ROSSO);
        rigaRiepilogo(tabella, "Saldo del mese", formatta(dati.saldo(), dati.valuta()),
                dati.saldo().signum() >= 0 ? VERDE : ROSSO);
        rigaRiepilogo(tabella, "Numero di transazioni", String.valueOf(dati.transazioni().size()), null);
        documento.add(tabella);
    }

    private void aggiungiCategorie(Document documento, String titolo, List<RigaCategoria> righe, String valuta)
            throws DocumentException {
        if (righe.isEmpty()) {
            return;
        }
        BigDecimal somma = righe.stream().map(RigaCategoria::totale).reduce(BigDecimal.ZERO, BigDecimal::add);

        documento.add(titoloSezione(titolo));
        PdfPTable tabella = nuovaTabella(55, 30, 15);
        tabella.addCell(cellaIntestazione("Categoria", Element.ALIGN_LEFT));
        tabella.addCell(cellaIntestazione("Importo", Element.ALIGN_RIGHT));
        tabella.addCell(cellaIntestazione("%", Element.ALIGN_RIGHT));
        tabella.setHeaderRows(1);

        for (RigaCategoria riga : righe) {
            tabella.addCell(cella(riga.nome(), TESTO, Element.ALIGN_LEFT));
            tabella.addCell(cella(formatta(riga.totale(), valuta), TESTO, Element.ALIGN_RIGHT));
            tabella.addCell(cella(percentuale(riga.totale(), somma) + "%", TESTO, Element.ALIGN_RIGHT));
        }
        tabella.addCell(cellaTotale("Totale", Element.ALIGN_LEFT));
        tabella.addCell(cellaTotale(formatta(somma, valuta), Element.ALIGN_RIGHT));
        tabella.addCell(cellaTotale("", Element.ALIGN_RIGHT));
        documento.add(tabella);
    }

    private void aggiungiBudget(Document documento, DatiResoconto dati) throws DocumentException {
        if (dati.budget().isEmpty()) {
            return;
        }
        documento.add(titoloSezione("Budget del mese"));
        PdfPTable tabella = nuovaTabella(26, 16, 16, 16, 12, 14);
        tabella.addCell(cellaIntestazione("Categoria", Element.ALIGN_LEFT));
        tabella.addCell(cellaIntestazione("Budget", Element.ALIGN_RIGHT));
        tabella.addCell(cellaIntestazione("Speso", Element.ALIGN_RIGHT));
        tabella.addCell(cellaIntestazione("Disponibile", Element.ALIGN_RIGHT));
        tabella.addCell(cellaIntestazione("Utilizzo", Element.ALIGN_RIGHT));
        tabella.addCell(cellaIntestazione("Stato", Element.ALIGN_LEFT));
        tabella.setHeaderRows(1);

        for (BudgetResponse voce : dati.budget()) {
            tabella.addCell(cella(voce.categoriaNome(), TESTO, Element.ALIGN_LEFT));
            tabella.addCell(cella(formatta(voce.importo(), dati.valuta()), TESTO, Element.ALIGN_RIGHT));
            tabella.addCell(cella(formatta(voce.speso(), dati.valuta()), TESTO, Element.ALIGN_RIGHT));
            tabella.addCell(cella(formatta(voce.disponibile(), dati.valuta()),
                    colorato(voce.disponibile().signum() < 0 ? ROSSO : null), Element.ALIGN_RIGHT));
            tabella.addCell(cella(voce.percentuale().setScale(1, RoundingMode.DOWN) + "%", TESTO, Element.ALIGN_RIGHT));
            tabella.addCell(cella(etichettaStato(voce.stato()), colorato(coloreStato(voce.stato())), Element.ALIGN_LEFT));
        }
        documento.add(tabella);
    }

    private void aggiungiStipendi(Document documento, DatiResoconto dati) throws DocumentException {
        if (dati.stipendi().isEmpty()) {
            return;
        }
        documento.add(titoloSezione("Stipendi pagati nel mese"));
        PdfPTable tabella = nuovaTabella(50, 25, 25);
        tabella.addCell(cellaIntestazione("Dipendente", Element.ALIGN_LEFT));
        tabella.addCell(cellaIntestazione("Data", Element.ALIGN_LEFT));
        tabella.addCell(cellaIntestazione("Importo", Element.ALIGN_RIGHT));
        tabella.setHeaderRows(1);

        for (RigaStipendio riga : dati.stipendi()) {
            tabella.addCell(cella(riga.dipendente(), TESTO, Element.ALIGN_LEFT));
            tabella.addCell(cella(riga.data().format(DATA), TESTO, Element.ALIGN_LEFT));
            tabella.addCell(cella(formatta(riga.importo(), dati.valuta()), TESTO, Element.ALIGN_RIGHT));
        }
        tabella.addCell(cellaTotale("Totale stipendi", Element.ALIGN_LEFT));
        tabella.addCell(cellaTotale("", Element.ALIGN_LEFT));
        tabella.addCell(cellaTotale(formatta(dati.totaleStipendi(), dati.valuta()), Element.ALIGN_RIGHT));
        documento.add(tabella);
    }

    private void aggiungiTransazioni(Document documento, DatiResoconto dati) throws DocumentException {
        documento.add(titoloSezione("Elenco delle transazioni"));
        if (dati.transazioni().isEmpty()) {
            documento.add(new Paragraph("Nessuna transazione in questo mese.", TESTO));
            return;
        }
        PdfPTable tabella = nuovaTabella(14, 34, 20, 14, 18);
        tabella.addCell(cellaIntestazione("Data", Element.ALIGN_LEFT));
        tabella.addCell(cellaIntestazione("Descrizione", Element.ALIGN_LEFT));
        tabella.addCell(cellaIntestazione("Categoria", Element.ALIGN_LEFT));
        tabella.addCell(cellaIntestazione("Metodo", Element.ALIGN_LEFT));
        tabella.addCell(cellaIntestazione("Importo", Element.ALIGN_RIGHT));
        tabella.setHeaderRows(1); // l'intestazione si ripete a ogni pagina

        for (TransazioneResponse t : dati.transazioni()) {
            boolean uscita = t.tipo() == TipoTransazione.USCITA;
            tabella.addCell(cella(t.data().format(DATA), TESTO, Element.ALIGN_LEFT));
            tabella.addCell(cella(t.descrizione(), TESTO, Element.ALIGN_LEFT));
            tabella.addCell(cella(t.categoriaNome(), TESTO, Element.ALIGN_LEFT));
            tabella.addCell(cella(etichettaMetodo(t.metodoPagamento()), TESTO, Element.ALIGN_LEFT));
            tabella.addCell(cella((uscita ? "-" : "+") + formatta(t.importo(), dati.valuta()),
                    colorato(uscita ? ROSSO : VERDE), Element.ALIGN_RIGHT));
        }
        documento.add(tabella);
    }

    // ---------- Piccoli strumenti per costruire le tabelle ----------

    private Paragraph titoloSezione(String testo) {
        Paragraph paragrafo = new Paragraph(testo, SEZIONE);
        paragrafo.setSpacingBefore(14);
        paragrafo.setSpacingAfter(6);
        return paragrafo;
    }

    private PdfPTable nuovaTabella(float... larghezzeRelative) {
        PdfPTable tabella = new PdfPTable(larghezzeRelative);
        tabella.setWidthPercentage(100);
        return tabella;
    }

    private PdfPCell cella(String testo, Font font, int allineamento) {
        PdfPCell cella = new PdfPCell(new Phrase(testo, font));
        cella.setHorizontalAlignment(allineamento);
        cella.setPadding(5);
        cella.setBorderColor(BORDO);
        return cella;
    }

    private PdfPCell cellaIntestazione(String testo, int allineamento) {
        PdfPCell cella = cella(testo, TESTO_GRASSETTO, allineamento);
        cella.setBackgroundColor(SFONDO_INTESTAZIONE);
        return cella;
    }

    private PdfPCell cellaTotale(String testo, int allineamento) {
        PdfPCell cella = cella(testo, TESTO_GRASSETTO, allineamento);
        cella.setBackgroundColor(SFONDO_TOTALE);
        return cella;
    }

    private void rigaRiepilogo(PdfPTable tabella, String etichetta, String valore, Color colore) {
        tabella.addCell(cella(etichetta, TESTO, Element.ALIGN_LEFT));
        tabella.addCell(cella(valore, colorato(colore), Element.ALIGN_RIGHT));
    }

    /** Testo in grassetto del colore indicato; senza colore, grassetto nero */
    private Font colorato(Color colore) {
        return colore == null
                ? TESTO_GRASSETTO
                : FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Font.NORMAL, colore);
    }

    // ---------- Formati ----------

    private String nomePeriodo(DatiResoconto dati) {
        String mese = dati.periodo().getMonth().getDisplayName(TextStyle.FULL, Locale.ITALIAN);
        return mese.substring(0, 1).toUpperCase(Locale.ITALIAN) + mese.substring(1) + " " + dati.periodo().getYear();
    }

    /** Un importo scritto all'italiana, ad esempio "1.234,50 €" */
    private String formatta(BigDecimal importo, String valuta) {
        try {
            NumberFormat formato = NumberFormat.getCurrencyInstance(Locale.ITALY);
            formato.setCurrency(Currency.getInstance(valuta));
            // Gli spazi "speciali" del formato italiano si sostituiscono con spazi normali
            return formato.format(importo).replace('\u00a0', ' ').replace('\u202f', ' ');
        } catch (IllegalArgumentException e) {
            // Codice valuta non riconosciuto: si scrive l'importo con il codice
            return String.format(Locale.ITALY, "%,.2f %s", importo, valuta);
        }
    }

    private String percentuale(BigDecimal parte, BigDecimal totale) {
        if (totale.signum() == 0) {
            return "0,0";
        }
        BigDecimal valore = parte.multiply(BigDecimal.valueOf(100)).divide(totale, 1, RoundingMode.HALF_UP);
        return String.format(Locale.ITALY, "%.1f", valore);
    }

    private String etichettaStato(StatoBudget stato) {
        return switch (stato) {
            case SUPERATO -> "Superato";
            case ATTENZIONE -> "Attenzione";
            case OK -> "Nei limiti";
        };
    }

    private Color coloreStato(StatoBudget stato) {
        return switch (stato) {
            case SUPERATO -> ROSSO;
            case ATTENZIONE -> new Color(204, 153, 0);
            case OK -> VERDE;
        };
    }

    private String etichettaMetodo(MetodoPagamento metodo) {
        return switch (metodo) {
            case CARTA -> "Carta";
            case CONTANTI -> "Contanti";
            case BONIFICO -> "Bonifico";
            case ALTRO -> "Altro";
        };
    }

    /** Scrive "Pagina N" in fondo a ogni pagina */
    private static class NumeroPagina extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter scrittore, Document documento) {
            PdfContentByte tela = scrittore.getDirectContent();
            Phrase testo = new Phrase("Pagina " + scrittore.getPageNumber(), PIEDE);
            ColumnText.showTextAligned(tela, Element.ALIGN_CENTER, testo,
                    (documento.right() + documento.left()) / 2, documento.bottom() - 20, 0);
        }
    }
}