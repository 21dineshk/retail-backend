package com.example.retail.repository;

import com.example.retail.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    @Query("""
        select p from Product p
        where (:q is null or lower(p.name) like lower(concat('%', :q, '%'))
                          or lower(p.description) like lower(concat('%', :q, '%')))
          and (:category is null or p.category = :category)
          and (:minPrice is null or p.price >= :minPrice)
          and (:maxPrice is null or p.price <= :maxPrice)
          and (:inStockOnly = false or p.stock > 0)
    """)
    Page<Product> search(@Param("q") String q,
                         @Param("category") String category,
                         @Param("minPrice") BigDecimal minPrice,
                         @Param("maxPrice") BigDecimal maxPrice,
                         @Param("inStockOnly") boolean inStockOnly,
                         Pageable pageable);

    @Query("select distinct p.category from Product p order by p.category")
    List<String> findAllCategories();

    @Query("select p.category as category, count(p) as count from Product p group by p.category order by p.category")
    List<CategoryCount> categoryCounts();

    interface CategoryCount {
        String getCategory();
        long getCount();
    }
}
