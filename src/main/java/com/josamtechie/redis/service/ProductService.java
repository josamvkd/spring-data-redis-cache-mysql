package com.josamtechie.redis.service;

import com.josamtechie.redis.entity.Product;
import com.josamtechie.redis.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService
{
    public static final String HASH_KEY = "Product";
    private final ProductRepository repository;
    private final RedisTemplate<String, Object> template;

    @Value("${spring.data.redis.cache.ttl}")
    private long cacheTtlMinutes;

    // ── CREATE ───────────────────────────────────────────────────────────────
    // Always write to MySQL → then cache the saved result in Redis

    public Product save(Product product)
    {
        product.setId(UUID.randomUUID().toString().split("-")[0]);
        Product saved = repository.save(product);

        String cacheKey = cacheKey(saved.getId());
        template.opsForValue().set(cacheKey, saved, cacheTtlMinutes, TimeUnit.MINUTES);
        log.info("CACHE SET  → key: {}", cacheKey);
        return saved;
    }

    // ── READ (all) ────────────────────────────────────────────────────────────
    // findAll() is not cached — list caches go stale easily; always hit MySQL

    public List<Product> findAll()
    {
        log.info("DB HIT → findAll()");
        return repository.findAll();
    }

    // ── READ (by id) ──────────────────────────────────────────────────────────
    // Cache-aside: check Redis first → on miss, load from MySQL → cache result
    public Product findProductById(String id)
    {
        String cacheKey = cacheKey(id);
        Product cachedProduct = (Product) template.opsForValue().get(cacheKey);
        if (cachedProduct != null) {
            log.info("CACHE HIT  → key: {}", cacheKey);
            return cachedProduct;
        }
        log.info("CACHE MISS → key: {}, fetching from DB", cacheKey);
        Product product = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + id));
        template.opsForValue().set(cacheKey, product, cacheTtlMinutes, TimeUnit.MINUTES);
        log.info("NEW CACHE SET  → key: {}", cacheKey);
        return product;
    }

    // ── READ (by name) ────────────────────────────────────────────────────────
    // Delegated to MySQL — JPA secondary index query
    public List<Product> findByName(String name)
    {
        log.info("DB HIT → findByName({})", name);
        return repository.findByName(name);
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────
    // Update MySQL → refresh the Redis cache entry for this id
    public Product updateProductById(String id, Product product)
    {
        Product existing = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + id));
        existing.setName(product.getName());
        existing.setQty(product.getQty());
        existing.setPrice(product.getPrice());
        Product updated = repository.save(existing);
        String cacheKey = cacheKey(id);
        template.opsForValue().set(cacheKey, updated, cacheTtlMinutes, TimeUnit.MINUTES);
        log.info("CACHE REFRESH → key: {}", cacheKey);
        return updated;
    }

    // ── DELETE ────────────────────────────────────────────────────────────────
    // Delete from MySQL → evict from Redis cache

    public String deleteProduct(String id)
    {
        repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + id));
        repository.deleteById(id);
        String cacheKey = cacheKey(id);
        template.delete(cacheKey);
        log.info("CACHE EVICT → key: {}", cacheKey);
        return "product removed !!";
    }


    // ── Helper ────────────────────────────────────────────────────────────────
    private String cacheKey(String id)
    {
        return HASH_KEY + ":" + id;     // e.g. "Product:1"
    }


}