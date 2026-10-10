// RefreshTokenRepository.java
package org.elis.progettoesempio.repository;

import java.util.Optional;
import org.elis.progettoesempio.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying(clearAutomatically = true)
    @Query("delete from RefreshToken r where r.utente.id = :utenteId")
    void eliminaTuttiPerUtente(@Param("utenteId") Long utenteId);
}