package org.elis.progettoesempio.entity;

public enum RuoloSpazio {
    PROPRIETARIO, AMMINISTRATORE, MEMBRO, LETTORE;

    /** Può gestire lo spazio: membri, categorie, budget, dati dei dipendenti */
    public boolean puoGestire() {
        return this == PROPRIETARIO || this == AMMINISTRATORE;
    }
}