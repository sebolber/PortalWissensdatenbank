package de.wissensdatenbank.controller;

import de.wissensdatenbank.dto.*;
import de.wissensdatenbank.enums.DocumentStatus;
import de.wissensdatenbank.service.DocumentService;
import de.wissensdatenbank.service.PermissionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dokumente")
public class DocumentController {

    private final DocumentService documentService;
    private final PermissionService permissionService;

    public DocumentController(DocumentService documentService, PermissionService permissionService) {
        this.documentService = documentService;
        this.permissionService = permissionService;
    }

    @GetMapping
    public Page<DocumentDto> list(
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        permissionService.requireLesen();
        return documentService.findAll(status, categoryId, q, page, size, sortBy, sortDir);
    }

    @GetMapping("/{id}")
    public DocumentDto getById(@PathVariable String id) {
        permissionService.requireLesen();
        return documentService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentDto create(@Valid @RequestBody DocumentCreateRequest request) {
        permissionService.requireSchreiben();
        return documentService.create(request);
    }

    @PutMapping("/{id}")
    public DocumentDto update(@PathVariable String id, @Valid @RequestBody DocumentUpdateRequest request) {
        permissionService.requireSchreiben();
        return documentService.update(id, request);
    }

    @PutMapping("/{id}/publish")
    public DocumentDto publish(@PathVariable String id) {
        permissionService.requireVeroeffentlichen();
        return documentService.publish(id);
    }

    @PutMapping("/{id}/archive")
    public DocumentDto archive(@PathVariable String id) {
        permissionService.requireVeroeffentlichen();
        return documentService.archive(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        permissionService.requireSchreiben();
        documentService.delete(id);
    }

    @GetMapping("/{id}/versionen")
    public List<DocumentVersionDto> getVersions(@PathVariable String id) {
        permissionService.requireLesen();
        return documentService.getVersions(id);
    }

    @PostMapping("/{id}/feedback")
    public void submitFeedback(@PathVariable String id, @Valid @RequestBody FeedbackRequest request) {
        permissionService.requireLesen();
        documentService.submitFeedback(id, request);
    }

    @GetMapping("/neueste")
    public List<DocumentDto> newest(@RequestParam(defaultValue = "5") int limit) {
        permissionService.requireLesen();
        return documentService.findNewest(limit);
    }

    @GetMapping("/beliebt")
    public List<DocumentDto> popular(@RequestParam(defaultValue = "5") int limit) {
        permissionService.requireLesen();
        return documentService.findMostViewed(limit);
    }

    @GetMapping("/statistik")
    public StatistikDto statistik() {
        return documentService.getStatistik();
    }
}
