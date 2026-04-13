package de.wissensdatenbank.service;

import de.wissensdatenbank.dto.HandbuchImportRequest;
import de.wissensdatenbank.dto.KnowledgeItemCreateRequest;
import de.wissensdatenbank.entity.KnowledgeItem;
import de.wissensdatenbank.entity.KnowledgeSubArticle;
import de.wissensdatenbank.entity.ProductVersion;
import de.wissensdatenbank.entity.SoftwareProduct;
import de.wissensdatenbank.entity.Tag;
import de.wissensdatenbank.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service fuer den Bulk-Import eines Benutzerhandbuchs.
 * Erstellt Produkt, Version und alle Kapitel mit hierarchischen Abschnitten.
 */
@Service
public class HandbuchImportService {

    private static final Logger log = LoggerFactory.getLogger(HandbuchImportService.class);

    private final SoftwareProductRepository productRepository;
    private final ProductVersionRepository versionRepository;
    private final KnowledgeItemRepository knowledgeItemRepository;
    private final KnowledgeSubArticleRepository subArticleRepository;
    private final TagRepository tagRepository;

    public HandbuchImportService(SoftwareProductRepository productRepository,
                                  ProductVersionRepository versionRepository,
                                  KnowledgeItemRepository knowledgeItemRepository,
                                  KnowledgeSubArticleRepository subArticleRepository,
                                  TagRepository tagRepository) {
        this.productRepository = productRepository;
        this.versionRepository = versionRepository;
        this.knowledgeItemRepository = knowledgeItemRepository;
        this.subArticleRepository = subArticleRepository;
        this.tagRepository = tagRepository;
    }

    @Transactional
    public ImportResult importHandbuch(String tenantId, String userId, HandbuchImportRequest request) {
        log.info("Starte Handbuch-Import fuer Mandant {} durch Benutzer {}", tenantId, userId);

        // 1. SoftwareProduct anlegen oder wiederverwenden
        var productInput = request.softwareProduct();
        SoftwareProduct product = productRepository
                .findByTenantIdAndName(tenantId, productInput.name())
                .orElseGet(() -> {
                    SoftwareProduct p = new SoftwareProduct();
                    p.setTenantId(tenantId);
                    p.setName(productInput.name());
                    p.setExecutableName(productInput.executableName());
                    p.setPublisher(productInput.publisher());
                    p.setDescription(productInput.description());
                    return productRepository.save(p);
                });
        log.info("Produkt: {} (id={})", product.getName(), product.getId());

        // 2. ProductVersion anlegen oder wiederverwenden
        var versionInput = request.productVersion();
        ProductVersion version = versionRepository
                .findByProductIdAndVersionLabel(product.getId(), versionInput.versionLabel())
                .orElseGet(() -> {
                    ProductVersion v = new ProductVersion();
                    v.setProduct(product);
                    v.setVersionLabel(versionInput.versionLabel());
                    v.setReleaseDate(versionInput.releaseDate());
                    v.setChangeSummary(versionInput.changeSummary());
                    return versionRepository.save(v);
                });
        log.info("Version: {} (id={})", version.getVersionLabel(), version.getId());

        // 3. Tags vorab laden/erstellen
        Set<String> allTagNames = request.knowledgeItems().stream()
                .filter(ki -> ki.tags() != null)
                .flatMap(ki -> ki.tags().stream())
                .collect(Collectors.toSet());

        Map<String, Tag> tagMap = new HashMap<>();
        if (!allTagNames.isEmpty()) {
            tagRepository.findByTenantIdAndNameIn(tenantId, new ArrayList<>(allTagNames))
                    .forEach(t -> tagMap.put(t.getName(), t));

            // Fehlende Tags erstellen
            for (String tagName : allTagNames) {
                if (!tagMap.containsKey(tagName)) {
                    Tag tag = new Tag();
                    tag.setId(UUID.randomUUID().toString());
                    tag.setTenantId(tenantId);
                    tag.setName(tagName);
                    tagMap.put(tagName, tagRepository.save(tag));
                }
            }
        }

        // 4. KnowledgeItems erstellen
        int totalItems = 0;
        int totalSubArticles = 0;

        for (var itemRequest : request.knowledgeItems()) {
            KnowledgeItem item = new KnowledgeItem();
            item.setTenantId(tenantId);
            item.setTitle(itemRequest.title());
            item.setSummary(itemRequest.summary());
            item.setKnowledgeType(itemRequest.knowledgeType());
            item.setBindingLevel(itemRequest.bindingLevel());
            item.setKeywords(itemRequest.keywords());
            item.setValidFrom(itemRequest.validFrom());
            item.setValidUntil(itemRequest.validUntil());
            item.setSourceReference(itemRequest.sourceReference());
            item.setProductVersion(version);
            item.setCreatedBy(userId);
            item.setUpdatedBy(userId);

            // Tags zuordnen
            if (itemRequest.tags() != null) {
                Set<Tag> tags = itemRequest.tags().stream()
                        .map(tagMap::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                item.setTags(tags);
            }

            KnowledgeItem savedItem = knowledgeItemRepository.save(item);

            // SubArticles hierarchisch erstellen
            if (itemRequest.subArticles() != null) {
                int count = createSubArticlesRecursive(savedItem, null,
                        itemRequest.subArticles(), 0, "");
                totalSubArticles += count;
            }

            totalItems++;
            log.debug("KnowledgeItem erstellt: {} (id={})", savedItem.getTitle(), savedItem.getId());
        }

        log.info("Handbuch-Import abgeschlossen: {} Kapitel, {} Abschnitte",
                totalItems, totalSubArticles);

        return new ImportResult(product.getId(), version.getId(),
                totalItems, totalSubArticles);
    }

    private int createSubArticlesRecursive(KnowledgeItem item, KnowledgeSubArticle parent,
                                            List<KnowledgeItemCreateRequest.SubArticleRequest> requests,
                                            int depth, String parentPath) {
        int count = 0;
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

            count++;

            if (req.children() != null && !req.children().isEmpty()) {
                count += createSubArticlesRecursive(item, saved,
                        req.children(), depth + 1, path);
            }
        }
        return count;
    }

    public record ImportResult(
            Long productId,
            Long productVersionId,
            int knowledgeItemCount,
            int subArticleCount
    ) {}
}
