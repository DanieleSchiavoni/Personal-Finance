// service/ScaricatoreFeed.java
package org.elis.progettoesempio.service;

import java.io.IOException;

/** Scarica il contenuto di un feed. Separato dal resto per poterlo sostituire nei test. */
public interface ScaricatoreFeed {

    byte[] scarica(String url) throws IOException, InterruptedException;
}