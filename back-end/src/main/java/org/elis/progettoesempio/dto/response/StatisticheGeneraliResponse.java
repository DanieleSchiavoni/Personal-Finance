// StatisticheGeneraliResponse.java
package org.elis.progettoesempio.dto.response;

/** Solo conteggi: gli admin non vedono mai i dati finanziari degli utenti */
public record StatisticheGeneraliResponse(
        long utentiTotali,
        long utentiAbilitati,
        long utentiBloccati,
        long amministratori,
        long spaziPersonali,
        long spaziAziendali,
        long transazioniTotali,
        long categorieGlobali) {
}