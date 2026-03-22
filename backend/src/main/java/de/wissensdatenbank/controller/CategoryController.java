package de.wissensdatenbank.controller;

import de.wissensdatenbank.dto.CategoryDto;
import de.wissensdatenbank.service.CategoryService;
import de.wissensdatenbank.service.PermissionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/kategorien")
public class CategoryController {

    private final CategoryService categoryService;
    private final PermissionService permissionService;

    public CategoryController(CategoryService categoryService, PermissionService permissionService) {
        this.categoryService = categoryService;
        this.permissionService = permissionService;
    }

    @GetMapping
    public List<CategoryDto> list() {
        permissionService.requireLesen();
        return categoryService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto create(@RequestBody Map<String, String> body) {
        permissionService.requireAdmin();
        return categoryService.create(
                body.get("name"),
                body.get("description"),
                body.get("parentId"));
    }

    @PutMapping("/{id}")
    public CategoryDto update(@PathVariable String id, @RequestBody Map<String, String> body) {
        permissionService.requireAdmin();
        return categoryService.update(id, body.get("name"), body.get("description"));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        permissionService.requireAdmin();
        categoryService.delete(id);
    }
}
