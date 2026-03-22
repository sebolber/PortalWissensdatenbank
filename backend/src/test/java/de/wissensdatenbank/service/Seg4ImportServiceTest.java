package de.wissensdatenbank.service;

import de.wissensdatenbank.entity.KnowledgeItem;
import de.wissensdatenbank.parser.*;
import de.wissensdatenbank.repository.KnowledgeItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Seg4ImportServiceTest {

    @Mock private Seg4PdfParser pdfParser;
    @Mock private Seg4BlockDetector blockDetector;
    @Mock private Seg4FieldExtractor fieldExtractor;
    @Mock private Seg4Normalizer normalizer;
    @Mock private KnowledgeItemRepository repository;

    @InjectMocks
    private Seg4ImportService importService;

    @Test
    void importPdf_createsKnowledgeItemWithRecommendations() {
        // given
        String rawText = "Kodierempfehlung Nr. 001\nEmpfehlung: Test";
        Seg4RawBlock block = new Seg4RawBlock("Kodierempfehlung Nr. 001", "Empfehlung: Test");
        Seg4ParsedFields fields = new Seg4ParsedFields();
        fields.setRecommendationNumber("001");
        fields.setEmpfehlung("Test");

        when(pdfParser.extractText(any())).thenReturn(rawText);
        when(blockDetector.detectBlocks(rawText)).thenReturn(List.of(block));
        when(fieldExtractor.extract(block)).thenReturn(fields);
        when(normalizer.normalize(fields)).thenReturn(fields);
        when(repository.save(any(KnowledgeItem.class))).thenAnswer(inv -> {
            KnowledgeItem item = inv.getArgument(0);
            item.setId(1L);
            return item;
        });

        // when
        KnowledgeItem result = importService.importPdf(
                "tenant1", "user1", "test.pdf", new ByteArrayInputStream(new byte[0]));

        // then
        assertNotNull(result);
        assertEquals("SEG4 Import: test.pdf", result.getTitle());
        assertEquals(1, result.getSeg4Recommendations().size());
        assertEquals("001", result.getSeg4Recommendations().get(0).getRecommendationNumber());
        verify(repository).save(any(KnowledgeItem.class));
    }

    @Test
    void importPdf_throwsWhenNoBlocksFound() {
        when(pdfParser.extractText(any())).thenReturn("Kein SEG4 Inhalt");
        when(blockDetector.detectBlocks(any())).thenReturn(List.of());

        assertThrows(Seg4ParseException.class, () ->
                importService.importPdf("t1", "u1", "leer.pdf", new ByteArrayInputStream(new byte[0])));
    }
}
