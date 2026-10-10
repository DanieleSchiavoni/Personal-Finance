package org.elis.progettoesempio.entity;

public class enumerazioni {
	// RuoloUtente.java  (ruolo a livello di piattaforma)
	public enum RuoloUtente { UTENTE, ADMIN }

	// TipoSpazio.java
	public enum TipoSpazio { PERSONALE, AZIENDALE }

	// RuoloSpazio.java  (ruolo dentro uno spazio di lavoro)
	public enum RuoloSpazio { PROPRIETARIO, AMMINISTRATORE, MEMBRO, LETTORE }

	// TipoTransazione.java  (usato anche dalle categorie)
	public enum TipoTransazione { ENTRATA, USCITA }

	// MetodoPagamento.java
	public enum MetodoPagamento { CARTA, CONTANTI, BONIFICO, ALTRO }

	// StatoObiettivo.java
	public enum StatoObiettivo { IN_CORSO, COMPLETATO, SCADUTO }
}
