package de.wissensdatenbank.service;

import de.wissensdatenbank.config.SecurityHelper;
import de.wissensdatenbank.dto.*;
import de.wissensdatenbank.entity.Category;
import de.wissensdatenbank.entity.Document;
import de.wissensdatenbank.entity.DocumentVersion;
import de.wissensdatenbank.entity.Feedback;
import de.wissensdatenbank.entity.Tag;
import de.wissensdatenbank.enums.DocumentStatus;
import de.wissensdatenbank.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository versionRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final FeedbackRepository feedbackRepository;
    private final SecurityHelper securityHelper;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentVersionRepository versionRepository,
                           CategoryRepository categoryRepository,
                           TagRepository tagRepository,
                           FeedbackRepository feedbackRepository,
                           SecurityHelper securityHelper) {
        this.documentRepository = documentRepository;
        this.versionRepository = versionRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.feedbackRepository = feedbackRepository;
        this.securityHelper = securityHelper;
    }

    public Page<DocumentDto> findAll(DocumentStatus status, String categoryId, String query,
                                     int page, int size, String sortBy, String sortDir) {
        String tenantId = securityHelper.getCurrentTenantId();
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC,
                sortBy != null ? sortBy : "createdAt");
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Document> documents;
        if (query != null && !query.isBlank()) {
            documents = documentRepository.search(tenantId, query.trim(), pageable);
        } else if (status != null && categoryId != null) {
            documents = documentRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        } else if (status != null) {
            documents = documentRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        } else if (categoryId != null) {
            documents = documentRepository.findByTenantIdAndCategoryId(tenantId, categoryId, pageable);
        } else {
            documents = documentRepository.findByTenantId(tenantId, pageable);
        }

        return documents.map(this::toDto);
    }

    public DocumentDto findById(String id) {
        String tenantId = securityHelper.getCurrentTenantId();
        Document doc = documentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dokument nicht gefunden"));
        doc.setViewCount(doc.getViewCount() + 1);
        documentRepository.save(doc);
        return toDto(doc);
    }

    @Transactional
    public DocumentDto create(DocumentCreateRequest request) {
        String tenantId = securityHelper.getCurrentTenantId();
        String userId = securityHelper.getCurrentUserId();

        Document doc = new Document();
        doc.setId(UUID.randomUUID().toString());
        doc.setTenantId(tenantId);
        doc.setTitle(request.title());
        doc.setContent(request.content());
        doc.setSummary(request.summary());
        doc.setCategoryId(request.categoryId());
        doc.setCreatedBy(userId);
        doc.setPublicWithinTenant(request.publicWithinTenant());
        doc.setStatus(DocumentStatus.DRAFT);

        if (request.tagIds() != null && !request.tagIds().isEmpty()) {
            Set<Tag> tags = new HashSet<>(tagRepository.findAllById(request.tagIds()));
            doc.setTags(tags);
        }

        Document saved = documentRepository.save(doc);
        log.info("Dokument erstellt: id={}, titel={}", saved.getId(), saved.getTitle());
        return toDto(saved);
    }

    @Transactional
    public DocumentDto update(String id, DocumentUpdateRequest request) {
        String tenantId = securityHelper.getCurrentTenantId();
        String userId = securityHelper.getCurrentUserId();

        Document doc = documentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dokument nicht gefunden"));

        // Version speichern
        DocumentVersion version = new DocumentVersion();
        version.setId(UUID.randomUUID().toString());
        version.setDocumentId(doc.getId());
        version.setVersion(doc.getVersion());
        version.setTitle(doc.getTitle());
        version.setContent(doc.getContent());
        version.setSummary(doc.getSummary());
        version.setChangedBy(userId);
        version.setChangeNote(request.changeNote());
        versionRepository.save(version);

        doc.setTitle(request.title());
        doc.setContent(request.content());
        doc.setSummary(request.summary());
        doc.setCategoryId(request.categoryId());
        doc.setUpdatedBy(userId);
        doc.setPublicWithinTenant(request.publicWithinTenant());
        doc.setVersion(doc.getVersion() + 1);

        if (request.tagIds() != null) {
            Set<Tag> tags = new HashSet<>(tagRepository.findAllById(request.tagIds()));
            doc.setTags(tags);
        }

        Document saved = documentRepository.save(doc);
        log.info("Dokument aktualisiert: id={}, version={}", saved.getId(), saved.getVersion());
        return toDto(saved);
    }

    @Transactional
    public DocumentDto publish(String id) {
        return changeStatus(id, DocumentStatus.PUBLISHED);
    }

    @Transactional
    public DocumentDto archive(String id) {
        return changeStatus(id, DocumentStatus.ARCHIVED);
    }

    @Transactional
    public void delete(String id) {
        String tenantId = securityHelper.getCurrentTenantId();
        Document doc = documentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dokument nicht gefunden"));
        documentRepository.delete(doc);
        log.info("Dokument geloescht: id={}", id);
    }

    public List<DocumentVersionDto> getVersions(String documentId) {
        return versionRepository.findByDocumentIdOrderByVersionDesc(documentId).stream()
                .map(v -> new DocumentVersionDto(v.getId(), v.getVersion(), v.getTitle(),
                        v.getSummary(), v.getChangedBy(), v.getChangedAt(), v.getChangeNote()))
                .collect(Collectors.toList());
    }

    public List<DocumentDto> findNewest(int limit) {
        String tenantId = securityHelper.getCurrentTenantId();
        return documentRepository.findNewest(tenantId, PageRequest.of(0, limit)).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<DocumentDto> findMostViewed(int limit) {
        String tenantId = securityHelper.getCurrentTenantId();
        return documentRepository.findMostViewed(tenantId, PageRequest.of(0, limit)).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public StatistikDto getStatistik() {
        String tenantId = securityHelper.getCurrentTenantId();
        return new StatistikDto(
                documentRepository.countByTenantId(tenantId),
                documentRepository.countByTenantIdAndStatus(tenantId, DocumentStatus.PUBLISHED),
                documentRepository.countByTenantIdAndStatus(tenantId, DocumentStatus.DRAFT),
                documentRepository.countByTenantIdAndStatus(tenantId, DocumentStatus.ARCHIVED),
                categoryRepository.countByTenantId(tenantId),
                tagRepository.countByTenantId(tenantId)
        );
    }

    @Transactional
    public void submitFeedback(String documentId, FeedbackRequest request) {
        String tenantId = securityHelper.getCurrentTenantId();
        String userId = securityHelper.getCurrentUserId();

        Document doc = documentRepository.findByIdAndTenantId(documentId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dokument nicht gefunden"));

        Feedback existing = feedbackRepository.findByDocumentIdAndUserId(documentId, userId).orElse(null);
        if (existing != null) {
            doc.setRatingSum(doc.getRatingSum() - existing.getRating() + request.rating());
            existing.setRating(request.rating());
            existing.setComment(request.comment());
            feedbackRepository.save(existing);
        } else {
            Feedback feedback = new Feedback();
            feedback.setId(UUID.randomUUID().toString());
            feedback.setDocumentId(documentId);
            feedback.setUserId(userId);
            feedback.setTenantId(tenantId);
            feedback.setRating(request.rating());
            feedback.setComment(request.comment());
            feedbackRepository.save(feedback);
            doc.setRatingSum(doc.getRatingSum() + request.rating());
            doc.setRatingCount(doc.getRatingCount() + 1);
        }
        documentRepository.save(doc);
    }

    private DocumentDto changeStatus(String id, DocumentStatus newStatus) {
        String tenantId = securityHelper.getCurrentTenantId();
        Document doc = documentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dokument nicht gefunden"));
        doc.setStatus(newStatus);
        doc.setUpdatedBy(securityHelper.getCurrentUserId());
        Document saved = documentRepository.save(doc);
        log.info("Dokument-Status geaendert: id={}, status={}", id, newStatus);
        return toDto(saved);
    }

    private DocumentDto toDto(Document doc) {
        String categoryName = null;
        if (doc.getCategoryId() != null) {
            categoryName = categoryRepository.findById(doc.getCategoryId())
                    .map(Category::getName).orElse(null);
        }

        List<TagDto> tagDtos = doc.getTags().stream()
                .map(t -> new TagDto(t.getId(), t.getName()))
                .collect(Collectors.toList());

        double avgRating = doc.getRatingCount() > 0
                ? doc.getRatingSum() / doc.getRatingCount() : 0;

        return new DocumentDto(
                doc.getId(), doc.getTenantId(), doc.getTitle(), doc.getContent(),
                doc.getSummary(), doc.getStatus(), doc.getCategoryId(), categoryName,
                tagDtos, doc.getCreatedBy(), doc.getUpdatedBy(),
                doc.getCreatedAt(), doc.getUpdatedAt(), doc.getVersion(),
                doc.isPublicWithinTenant(), doc.getViewCount(), avgRating, doc.getRatingCount()
        );
    }
}
