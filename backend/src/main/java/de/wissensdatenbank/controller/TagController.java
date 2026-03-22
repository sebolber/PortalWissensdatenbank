package de.wissensdatenbank.controller;

import de.wissensdatenbank.dto.TagDto;
import de.wissensdatenbank.service.PermissionService;
import de.wissensdatenbank.service.TagService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;
    private final PermissionService permissionService;

    public TagController(TagService tagService, PermissionService permissionService) {
        this.tagService = tagService;
        this.permissionService = permissionService;
    }

    @GetMapping
    public List<TagDto> list() {
        permissionService.requireLesen();
        return tagService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TagDto create(@RequestBody Map<String, String> body) {
        permissionService.requireAdmin();
        return tagService.create(body.get("name"));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        permissionService.requireAdmin();
        tagService.delete(id);
    }
}
