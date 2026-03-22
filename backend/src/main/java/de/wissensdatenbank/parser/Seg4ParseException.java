package de.wissensdatenbank.parser;

/**
 * Wird bei Fehlern im SEG4-Parsing geworfen.
 */
public class Seg4ParseException extends RuntimeException {

    public Seg4ParseException(String message) {
        super(message);
    }

    public Seg4ParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
