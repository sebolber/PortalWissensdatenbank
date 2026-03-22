package de.wissensdatenbank.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Erkennt einzelne Kodierempfehlungs-Blöcke im extrahierten PDF-Text.
 * Ein Block beginnt mit einem Muster wie "Kodierempfehlung Nr. 123" oder "Kodierempfehlung: 123".
 */
@Component
public class Seg4BlockDetector {

    private static final Logger log = LoggerFactory.getLogger(Seg4BlockDetector.class);

    /**
     * Pattern erkennt Varianten:
     * - "Kodierempfehlung Nr. 123"
     * - "Kodierempfehlung: 123"
     * - "Kodierempfehlung Nr.123"
     * - "Kodierempfehlung 123"
     */
    private static final Pattern BLOCK_START = Pattern.compile(
            "(?i)(Kodierempfehlung\\s*(?:Nr\\.?\\s*|:\\s*|\\s+)(\\S+))",
            Pattern.UNICODE_CASE
    );

    /**
     * Zerlegt den Rohtext in einzelne SEG4-Blöcke.
     *
     * @param rawText kompletter PDF-Text
     * @return Liste erkannter Blöcke
     */
    public List<Seg4RawBlock> detectBlocks(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return List.of();
        }

        List<Seg4RawBlock> blocks = new ArrayList<>();
        Matcher matcher = BLOCK_START.matcher(rawText);

        List<int[]> starts = new ArrayList<>();
        while (matcher.find()) {
            starts.add(new int[]{matcher.start(), matcher.end()});
        }

        for (int i = 0; i < starts.size(); i++) {
            int blockStart = starts.get(i)[0];
            int headerEnd = starts.get(i)[1];
            int blockEnd = (i + 1 < starts.size()) ? starts.get(i + 1)[0] : rawText.length();

            String header = rawText.substring(blockStart, headerEnd).trim();
            String body = rawText.substring(headerEnd, blockEnd).trim();

            blocks.add(new Seg4RawBlock(header, body));
        }

        log.debug("{} SEG4-Bloecke erkannt", blocks.size());
        return blocks;
    }
}
