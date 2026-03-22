package de.wissensdatenbank.service;

import de.wissensdatenbank.config.SecurityHelper;
import de.wissensdatenbank.dto.CategoryDto;
import de.wissensdatenbank.entity.Category;
import de.wissensdatenbank.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

    private final CategoryRepository categoryRepository;
    private final SecurityHelper securityHelper;

    public CategoryService(CategoryRepository categoryRepository, SecurityHelper securityHelper) {
        this.categoryRepository = categoryRepository;
        this.securityHelper = securityHelper;
    }

    public List<CategoryDto> findAll() {
        String tenantId = securityHelper.getCurrentTenantId();
        return categoryRepository.findByTenantIdOrderByOrderIndexAsc(tenantId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public CategoryDto create(String name, String description, String parentId) {
        String tenantId = securityHelper.getCurrentTenantId();

        if (categoryRepository.existsByTenantIdAndNameAndParentId(tenantId, name, parentId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Kategorie existiert bereits");
        }

        Category cat = new Category();
        cat.setId(UUID.randomUUID().toString());
        cat.setTenantId(tenantId);
        cat.setName(name);
        cat.setDescription(description);
        cat.setParentId(parentId);

        Category saved = categoryRepository.save(cat);
        log.info("Kategorie erstellt: id={}, name={}", saved.getId(), saved.getName());
        return toDto(saved);
    }

    public CategoryDto update(String id, String name, String description) {
        String tenantId = securityHelper.getCurrentTenantId();
        Category cat = categoryRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kategorie nicht gefunden"));

        cat.setName(name);
        cat.setDescription(description);
        Category saved = categoryRepository.save(cat);
        log.info("Kategorie aktualisiert: id={}, name={}", saved.getId(), saved.getName());
        return toDto(saved);
    }

    public void delete(String id) {
        String tenantId = securityHelper.getCurrentTenantId();
        Category cat = categoryRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kategorie nicht gefunden"));
        categoryRepository.delete(cat);
        log.info("Kategorie geloescht: id={}", id);
    }

    private CategoryDto toDto(Category cat) {
        return new CategoryDto(cat.getId(), cat.getName(), cat.getDescription(),
                cat.getParentId(), cat.getOrderIndex(), cat.getCreatedAt());
    }
}
