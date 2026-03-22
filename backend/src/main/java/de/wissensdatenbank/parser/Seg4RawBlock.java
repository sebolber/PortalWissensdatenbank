package de.wissensdatenbank.parser;

/**
 * Ein einzelner erkannter SEG4-Block (Rohtext zwischen zwei Kodierempfehlungs-Markern).
 */
public record Seg4RawBlock(
        String header,
        String body
) {
}
