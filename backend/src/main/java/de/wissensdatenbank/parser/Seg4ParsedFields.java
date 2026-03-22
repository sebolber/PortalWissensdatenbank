package de.wissensdatenbank.parser;

import java.time.LocalDate;

/**
 * Strukturierte Felder einer einzelnen SEG4-Kodierempfehlung (nach Extraktion).
 */
public class Seg4ParsedFields {

    private String recommendationNumber;
    private String schlagworte;
    private LocalDate erstelltAm;
    private LocalDate aktualisiertAm;
    private String problemErlaeuterung;
    private String empfehlung;
    private String entscheidung;
    private String zusatzhinweis;
    private boolean arbitration;
    private String originalText;

    // --- Getters & Setters ---

    public String getRecommendationNumber() { return recommendationNumber; }
    public void setRecommendationNumber(String v) { this.recommendationNumber = v; }

    public String getSchlagworte() { return schlagworte; }
    public void setSchlagworte(String v) { this.schlagworte = v; }

    public LocalDate getErstelltAm() { return erstelltAm; }
    public void setErstelltAm(LocalDate v) { this.erstelltAm = v; }

    public LocalDate getAktualisiertAm() { return aktualisiertAm; }
    public void setAktualisiertAm(LocalDate v) { this.aktualisiertAm = v; }

    public String getProblemErlaeuterung() { return problemErlaeuterung; }
    public void setProblemErlaeuterung(String v) { this.problemErlaeuterung = v; }

    public String getEmpfehlung() { return empfehlung; }
    public void setEmpfehlung(String v) { this.empfehlung = v; }

    public String getEntscheidung() { return entscheidung; }
    public void setEntscheidung(String v) { this.entscheidung = v; }

    public String getZusatzhinweis() { return zusatzhinweis; }
    public void setZusatzhinweis(String v) { this.zusatzhinweis = v; }

    public boolean isArbitration() { return arbitration; }
    public void setArbitration(boolean v) { this.arbitration = v; }

    public String getOriginalText() { return originalText; }
    public void setOriginalText(String v) { this.originalText = v; }
}
