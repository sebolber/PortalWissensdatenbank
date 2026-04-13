package de.wissensdatenbank.service;

import de.wissensdatenbank.dto.KnowledgeItemCreateRequest;
import de.wissensdatenbank.dto.KnowledgeItemDto;
import de.wissensdatenbank.dto.KnowledgeSubArticleTreeDto;
import de.wissensdatenbank.entity.KnowledgeItem;
import de.wissensdatenbank.entity.KnowledgeSubArticle;
import de.wissensdatenbank.entity.ProductVersion;
import de.wissensdatenbank.entity.Tag;
import de.wissensdatenbank.enums.KnowledgeType;
import de.wissensdatenbank.repository.KnowledgeItemRepository;
import de.wissensdatenbank.repository.KnowledgeSubArticleRepository;
import de.wissensdatenbank.repository.ProductVersionRepository;
import de.wissensdatenbank.repository.TagRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * CRUD-Service für KnowledgeItems mit Mandantenfilter.
 */
@Service
@Transactional(readOnly = true)
public class KnowledgeService {

    private final KnowledgeItemRepository repository;
    private final KnowledgeSubArticleRepository subArticleRepository;
    private final TagRepository tagRepository;
    private final ProductVersionRepository productVersionRepository;

    public KnowledgeService(KnowledgeItemRepository repository,
                            KnowledgeSubArticleRepository subArticleRepository,
                            TagRepository tagRepository,
                            ProductVersionRepository productVersionRepository) {
        this.repository = repository;
        this.subArticleRepository = subArticleRepository;
        this.tagRepository = tagRepository;
        this.productVersionRepository = productVersionRepository;
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

    /**
     * Erstellt ein neues KnowledgeItem mit optionalen hierarchischen SubArticles.
     */
    @Transactional
    public KnowledgeItemDto create(String tenantId, String userId, KnowledgeItemCreateRequest request) {
        KnowledgeItem item = new KnowledgeItem();
        item.setTenantId(tenantId);
        item.setCreatedBy(userId);
        item.setUpdatedBy(userId);
        applyFields(item, request, tenantId);

        KnowledgeItem saved = repository.save(item);

        if (request.subArticles() != null && !request.subArticles().isEmpty()) {
            createSubArticles(saved, null, request.subArticles(), 0, "");
        }

        return toDto(repository.findById(saved.getId()).orElseThrow());
    }

    /**
     * Aktualisiert ein bestehendes KnowledgeItem. SubArticles werden komplett ersetzt.
     */
    @Transactional
    public KnowledgeItemDto update(String tenantId, String userId, Long id, KnowledgeItemCreateRequest request) {
        KnowledgeItem item = findById(tenantId, id);
        item.setUpdatedBy(userId);
        applyFields(item, request, tenantId);

        // SubArticles komplett ersetzen
        item.getSubArticles().clear();
        repository.saveAndFlush(item);

        if (request.subArticles() != null && !request.subArticles().isEmpty()) {
            createSubArticles(item, null, request.subArticles(), 0, "");
        }

        return toDto(repository.findById(id).orElseThrow());
    }

    @Transactional
    public void delete(String tenantId, Long id) {
        KnowledgeItem item = findById(tenantId, id);
        repository.delete(item);
    }

    /**
     * Gibt den SubArticle-Baum eines KnowledgeItems als verschachtelte Struktur zurueck.
     */
    public List<KnowledgeSubArticleTreeDto> getSubArticleTree(String tenantId, Long knowledgeItemId) {
        findById(tenantId, knowledgeItemId); // Zugriffspruefung
        List<KnowledgeSubArticle> roots =
                subArticleRepository.findByKnowledgeItemIdAndParentIsNullOrderByOrderIndexAsc(knowledgeItemId);
        return roots.stream().map(this::toTreeDto).collect(Collectors.toList());
    }

    // --- Private Hilfsmethoden ---

    private void applyFields(KnowledgeItem item, KnowledgeItemCreateRequest request, String tenantId) {
        item.setTitle(request.title());
        item.setSummary(request.summary());
        item.setKnowledgeType(request.knowledgeType());
        item.setBindingLevel(request.bindingLevel());
        item.setKeywords(request.keywords());
        item.setValidFrom(request.validFrom());
        item.setValidUntil(request.validUntil());
        item.setSourceReference(request.sourceReference());

        // Produktversion zuordnen
        if (request.productVersionId() != null) {
            ProductVersion pv = productVersionRepository.findById(request.productVersionId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Produktversion nicht gefunden: " + request.productVersionId()));
            item.setProductVersion(pv);
        } else {
            item.setProductVersion(null);
        }

        // Tags zuordnen (anhand Name)
        if (request.tags() != null && !request.tags().isEmpty()) {
            Set<Tag> tags = new HashSet<>(tagRepository.findByTenantIdAndNameIn(tenantId, request.tags()));
            item.setTags(tags);
        } else {
            item.setTags(new HashSet<>());
        }
    }

    /**
     * Erstellt hierarchische SubArticles rekursiv.
     */
    private void createSubArticles(KnowledgeItem item, KnowledgeSubArticle parent,
                                    List<KnowledgeItemCreateRequest.SubArticleRequest> requests,
                                    int depth, String parentPath) {
        for (var req : requests) {
            KnowledgeSubArticle sa = new KnowledgeSubArticle();
            sa.setKnowledgeItem(item);
            sa.setParent(parent);
            sa.setHeading(req.heading());
            sa.setContent(req.content());
            sa.setSectionNumber(req.sectionNumber());
            sa.setOrderIndex(req.orderIndex());
            sa.setDepth(depth);

            KnowledgeSubArticle saved = subArticleRepository.save(sa);

            // Materialized Path setzen
            String path = parentPath + "/" + saved.getId();
            saved.setPath(path + "/");
            subArticleRepository.save(saved);

            if (req.children() != null && !req.children().isEmpty()) {
                createSubArticles(item, saved, req.children(), depth + 1, path);
            }
        }
    }

    private KnowledgeSubArticleTreeDto toTreeDto(KnowledgeSubArticle sa) {
        List<KnowledgeSubArticleTreeDto> childDtos = sa.getChildren().stream()
                .sorted(Comparator.comparingInt(KnowledgeSubArticle::getOrderIndex))
                .map(this::toTreeDto)
                .collect(Collectors.toList());

        String preview = sa.getContent() != null && sa.getContent().length() > 200
                ? sa.getContent().substring(0, 200) + "..."
                : sa.getContent();

        return new KnowledgeSubArticleTreeDto(
                sa.getId(),
                sa.getHeading(),
                sa.getSectionNumber(),
                sa.getDepth(),
                sa.getOrderIndex(),
                preview,
                childDtos
        );
    }

    private KnowledgeItemDto toDto(KnowledgeItem item) {
        var pv = item.getProductVersion();
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
                item.getSeg4Recommendations() != null ? item.getSeg4Recommendations().size() : 0,
                pv != null ? pv.getId() : null,
                pv != null ? pv.getVersionLabel() : null,
                pv != null ? pv.getProduct().getName() : null
        );
    }
}
