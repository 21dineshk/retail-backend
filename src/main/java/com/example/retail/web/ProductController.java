package com.example.retail.web;

import com.example.retail.repository.ProductRepository;
import com.example.retail.web.dto.CategoryCountDto;
import com.example.retail.web.dto.PageResponse;
import com.example.retail.web.dto.ProductDto;
import com.example.retail.web.error.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Product catalog: search, browse, and inventory checks.")
public class ProductController {

    private final ProductRepository repo;

    public ProductController(ProductRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    @Operation(summary = "Search the product catalog",
        description = "Returns a paginated list of products. All filters are optional and combine with AND.")
    public PageResponse<ProductDto> search(
        @Parameter(description = "Free-text query matched against name and description")
        @RequestParam(required = false) String q,
        @Parameter(description = "Exact category match (see /api/products/categories)")
        @RequestParam(required = false) String category,
        @Parameter(description = "Minimum price (inclusive)")
        @RequestParam(required = false) BigDecimal minPrice,
        @Parameter(description = "Maximum price (inclusive)")
        @RequestParam(required = false) BigDecimal maxPrice,
        @Parameter(description = "When true, only return products with stock > 0")
        @RequestParam(defaultValue = "false") boolean inStockOnly,
        @Parameter(description = "Zero-based page index")
        @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Page size (max 100)")
        @RequestParam(defaultValue = "20") int size,
        @Parameter(description = "Sort key. One of: name, price, rating, stock")
        @RequestParam(defaultValue = "name") String sortBy,
        @Parameter(description = "Sort direction: asc or desc")
        @RequestParam(defaultValue = "asc") String sortDir
    ) {
        if (size < 1 || size > 100) throw ApiException.badRequest("size must be 1..100");
        Sort.Direction dir = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(dir, sortBy);
        return PageResponse.from(
            repo.search(q, category, minPrice, maxPrice, inStockOnly, PageRequest.of(page, size, sort)),
            ProductDto::of);
    }

    @GetMapping("/categories")
    @Operation(summary = "List product categories with item counts")
    public List<CategoryCountDto> categories() {
        return repo.categoryCounts().stream()
            .map(c -> new CategoryCountDto(c.getCategory(), c.getCount()))
            .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single product by id")
    public ProductDto getById(@PathVariable Long id) {
        return repo.findById(id).map(ProductDto::of)
            .orElseThrow(() -> ApiException.notFound("Product " + id));
    }

    @GetMapping("/sku/{sku}")
    @Operation(summary = "Get a single product by SKU")
    public ProductDto getBySku(@PathVariable String sku) {
        return repo.findBySku(sku).map(ProductDto::of)
            .orElseThrow(() -> ApiException.notFound("Product SKU " + sku));
    }

    @GetMapping("/{id}/availability")
    @Operation(summary = "Check stock for a product",
        description = "Returns the current available units for the given product id.")
    public Availability availability(@PathVariable Long id) {
        var p = repo.findById(id).orElseThrow(() -> ApiException.notFound("Product " + id));
        return new Availability(p.getId(), p.getSku(), p.getStock(), p.getStock() > 0);
    }

    public record Availability(Long productId, String sku, int stock, boolean inStock) {}
}
