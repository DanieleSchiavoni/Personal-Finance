// repository/CategoriaRepositoryTest.java
package org.elis.progettoesempio.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.elis.progettoesempio.entity.Categoria;
import org.elis.progettoesempio.entity.SpazioLavoro;
import org.elis.progettoesempio.entity.enumerazioni.TipoSpazio;
import org.elis.progettoesempio.entity.enumerazioni.TipoTransazione;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class CategoriaRepositoryTest {

    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private SpazioLavoroRepository spazioLavoroRepository;

    private SpazioLavoro spazio;
    private Categoria esterna;
    private Categoria soloPersonale;
    private Categoria soloAziendale;

    @BeforeEach
    void preparaDati() {
        spazio = creaSpazio("Azienda A");
        SpazioLavoro altroSpazio = creaSpazio("Azienda B");

        creaCategoria("Interna", spazio, null);
        esterna = creaCategoria("Esterna", altroSpazio, null);
        // Categorie globali (senza spazio): visibili secondo il tipo di spazio
        soloPersonale = creaCategoria("Casa", null, TipoSpazio.PERSONALE);
        soloAziendale = creaCategoria("Fornitori", null, TipoSpazio.AZIENDALE);
        creaCategoria("Altro", null, null);
    }

    @Test
    void unoSpazioAziendaleVedeLeProprieCategorieEQuelleGlobaliAdatte() {
        assertThat(categoriaRepository.trovaVisibili(spazio.getId(), TipoSpazio.AZIENDALE))
                .extracting(Categoria::getNome)
                .containsExactlyInAnyOrder("Interna", "Fornitori", "Altro");
    }

    @Test
    void unaCategoriaNonVisibileNonSiTrovaPerId() {
        assertThat(categoriaRepository.trovaVisibilePerId(esterna.getId(), spazio.getId(), TipoSpazio.AZIENDALE)).isEmpty();
        assertThat(categoriaRepository.trovaVisibilePerId(soloPersonale.getId(), spazio.getId(), TipoSpazio.AZIENDALE)).isEmpty();
        assertThat(categoriaRepository.trovaVisibilePerId(soloAziendale.getId(), spazio.getId(), TipoSpazio.AZIENDALE)).isPresent();
    }

    @Test
    void leCategorieGlobaliSiCercanoPerNomeETipo() {
        assertThat(categoriaRepository.findBySpazioLavoroIsNullAndNomeAndTipo("Altro", TipoTransazione.USCITA)).isPresent();
        // "Interna" appartiene a uno spazio: non è globale
        assertThat(categoriaRepository.findBySpazioLavoroIsNullAndNomeAndTipo("Interna", TipoTransazione.USCITA)).isEmpty();
    }

    private SpazioLavoro creaSpazio(String nome) {
        SpazioLavoro nuovo = new SpazioLavoro();
        nuovo.setNome(nome);
        nuovo.setTipo(TipoSpazio.AZIENDALE);
        return spazioLavoroRepository.save(nuovo);
    }

    private Categoria creaCategoria(String nome, SpazioLavoro dove, TipoSpazio visibileA) {
        Categoria categoria = new Categoria();
        categoria.setNome(nome);
        categoria.setTipo(TipoTransazione.USCITA);
        categoria.setSpazioLavoro(dove);
        categoria.setTipoSpazio(visibileA);
        return categoriaRepository.save(categoria);
    }
}