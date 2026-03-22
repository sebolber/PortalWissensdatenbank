package de.wissensdatenbank.service;

import de.wissensdatenbank.config.SecurityHelper;
import de.wissensdatenbank.dto.TagDto;
import de.wissensdatenbank.entity.Tag;
import de.wissensdatenbank.repository.TagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TagService {

    private static final Logger log = LoggerFactory.getLogger(TagService.class);

    private final TagRepository tagRepository;
    private final SecurityHelper securityHelper;

    public TagService(TagRepository tagRepository, SecurityHelper securityHelper) {
        this.tagRepository = tagRepository;
        this.securityHelper = securityHelper;
    }

    public List<TagDto> findAll() {
        String tenantId = securityHelper.getCurrentTenantId();
        return tagRepository.findByTenantIdOrderByNameAsc(tenantId).stream()
                .map(t -> new TagDto(t.getId(), t.getName()))
                .collect(Collectors.toList());
    }

    public TagDto create(String name) {
        String tenantId = securityHelper.getCurrentTenantId();

        if (tagRepository.existsByTenantIdAndName(tenantId, name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tag existiert bereits");
        }

        Tag tag = new Tag();
        tag.setId(UUID.randomUUID().toString());
        tag.setTenantId(tenantId);
        tag.setName(name);

        Tag saved = tagRepository.save(tag);
        log.info("Tag erstellt: id={}, name={}", saved.getId(), saved.getName());
        return new TagDto(saved.getId(), saved.getName());
    }

    public void delete(String id) {
        String tenantId = securityHelper.getCurrentTenantId();
        Tag tag = tagRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag nicht gefunden"));
        tagRepository.delete(tag);
        log.info("Tag geloescht: id={}", id);
    }
}
