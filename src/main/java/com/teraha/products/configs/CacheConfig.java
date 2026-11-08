package com.teraha.products.configs;

import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import com.teraha.commons.dtos.ProductDTO;
import com.teraha.commons.dtos.CategoryDTO;

import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper,
            @Value("${app.cache.ttl.default-seconds}") long defaultTtl,
            @Value("${app.cache.ttl.products-seconds}") long productsTtl,
            @Value("${app.cache.ttl.categories-seconds}") long categoriesTtl) {

        RedisCacheConfiguration defaults =
            RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(defaultTtl))
                .disableCachingNullValues();

        RedisCacheConfiguration productsCache =
            defaults
                .entryTtl(Duration.ofSeconds(productsTtl))
                .serializeValuesWith(
                    SerializationPair.fromSerializer(
                        new JacksonJsonRedisSerializer<>(
                            objectMapper, ProductDTO.class)));

        RedisCacheConfiguration categoriesCache =
            defaults
                .entryTtl(Duration.ofSeconds(categoriesTtl))
                .serializeValuesWith(
                    SerializationPair.fromSerializer(
                        new JacksonJsonRedisSerializer<>(
                            objectMapper, CategoryDTO.class)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaults)
            .withInitialCacheConfigurations(
                Map.of(
                    "products:products", productsCache,
                    "products:categories", categoriesCache
                ))
            .transactionAware()
            .build();
    }
}
