// UtenteCorrente.java
package org.elis.progettoesempio.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Sul parametro di un controller: riceve l'id dell'utente che ha fatto la richiesta */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UtenteCorrente {
}