// TotaleCategoria.java  (proiezione per i totali per categoria)
package org.elis.progettoesempio.repository;

import java.math.BigDecimal;

public interface TotaleCategoria {
    Long getCategoriaId();
    String getCategoriaNome();
    String getColore();
    BigDecimal getTotale();
}