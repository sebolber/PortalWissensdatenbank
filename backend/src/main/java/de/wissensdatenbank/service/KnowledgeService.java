package de.wissensdatenbank.service;

import de.wissensdatenbank.dto.KnowledgeItemDto;
import de.wissensdatenbank.entity.KnowledgeItem;
import de.wissensdatenbank.entity.Tag;
import de.wissensdatenbank.enums.KnowledgeType;
import de.wissensdatenbank.repository.KnowledgeItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

/**
 * CRUD-Service für KnowledgeItems mit Mandantenfilter.
 */
@Service
@Transactional(readOnly = true)
public class KnowledgeService {

    private final KnowledgeItemRepository repository;

    public KnowledgeService(KnowledgeItemRepository repository) {
        this.repository = repository;
    }

    public Page<KnowledgeItemDto> findAll(String tenantId, Pageable pageable) {
        return repository.findByTenantId(tenantId, pageable).map(this::toDto);
    }

    public Page<KnowledgeItemDto> findByType(String tenantId, KnowledgeType type, Pageable pageable) {
        return repository.findByTenantIdAndKnowledgeType(tenantId, type, pageable).map(this::toDto);
    }

    public KnowledgeItem findById(String tenantId, Long id) {
        return repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Wissensobjekt nicht gefunden: " + id));
    }

    public KnowledgeItemDto findDtoById(String tenantId, Long id) {
        return toDto(findById(tenantId, id));
    }

    @Transactional
    public void delete(String tenantId, Long id) {
        KnowledgeItem item = findById(tenantId, id);
        repository.delete(item);
    }

    private KnowledgeItemDto toDto(KnowledgeItem item) {
        return new KnowledgeItemDto(
                item.getId(),
                item.getTitle(),
                item.getSummary(),
                item.getKnowledgeType(),
                item.getBindingLevel(),
                item.getKeywords(),
                item.getValidFrom(),
                item.getValidUntil(),
                item.getSourceReference(),
                item.getCreatedAt(),
                item.getUpdatedAt(),
                item.getCreatedBy(),
                item.getTags().stream().map(Tag::getName).collect(Collectors.toList()),
                item.getSeg4Recommendations() != null ? item.getSeg4Recommendations().size() : 0
        );
    }
}
