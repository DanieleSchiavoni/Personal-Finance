// TransazioneMapper.java
package org.elis.progettoesempio.mapper;

import org.elis.progettoesempio.dto.response.TransazioneResponse;
import org.elis.progettoesempio.entity.Categoria;
import org.elis.progettoesempio.entity.Transazione;
import org.elis.progettoesempio.entity.Utente;
import org.springframework.stereotype.Component;

@Component
public class TransazioneMapper {

    public TransazioneResponse aResponse(Transazione transazione) {
        Categoria categoria = transazione.getCategoria();
        Utente autore = transazione.getCreataDa(); // può essere null se l'utente è stato eliminato

        return new TransazioneResponse(
                transazione.getId(),
                transazione.getImporto(),
                transazione.getDescrizione(),
                transazione.getDataTransazione(),
                transazione.getTipo(),
                transazione.getMetodoPagamento(),
                transazione.getNote(),
                categoria.getId(),
                categoria.getNome(),
                categoria.getColore(),
                autore != null ? autore.getId() : null,
                autore != null ? autore.getUsername() : null);
    }
}