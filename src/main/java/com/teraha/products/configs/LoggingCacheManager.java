package com.teraha.products.configs;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

public class LoggingCacheManager implements CacheManager {

    private static final Logger log =
        LoggerFactory.getLogger(LoggingCacheManager.class);

    private final CacheManager delegate;
    private final Map<String, Cache> caches = new ConcurrentHashMap<>();

    public LoggingCacheManager(CacheManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public Cache getCache(String name) {
        Cache cache = delegate.getCache(name);

        if (cache == null) {
            return null;
        }

        return caches.computeIfAbsent(name, ignored ->
            new LoggingCache(cache));
    }

    @Override
    public Collection<String> getCacheNames() {
        return delegate.getCacheNames();
    }

    private static class LoggingCache implements Cache {

        private final Cache delegate;

        LoggingCache(Cache delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public Object getNativeCache() {
            return delegate.getNativeCache();
        }

        @Override
        public ValueWrapper get(Object key) {
            ValueWrapper value = delegate.get(key);
            log.info("Cache GET {}: cache={}, key={}",
                value == null ? "MISS" : "HIT", getName(), key);
            return value;
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            T value = delegate.get(key, type);
            log.info("Cache GET {}: cache={}, key={}",
                value == null ? "MISS" : "HIT", getName(), key);
            return value;
        }

        @Override
        public <T> T get(Object key, Callable<T> loader) {
            AtomicBoolean loaded = new AtomicBoolean(false);

            try {
                T value = delegate.get(key, () -> {
                    loaded.set(true);
                    return loader.call();
                });

                log.info("Cache GET {}: cache={}, key={}",
                    loaded.get() ? "MISS" : "HIT", getName(), key);

                if (loaded.get()) {
                    log.info("Cache SET: cache={}, key={}",
                        getName(), key);
                }

                return value;
            } catch (RuntimeException ex) {
                log.warn("Cache GET/LOAD failed: cache={}, key={}",
                    getName(), key, ex);
                throw ex;
            }
        }

        @Override
        public void put(Object key, Object value) {
            delegate.put(key, value);
            log.info("Cache SET: cache={}, key={}", getName(), key);
        }

        @Override
        public void evict(Object key) {
            delegate.evict(key);
            log.info("Cache DELETE: cache={}, key={}", getName(), key);
        }

        @Override
        public void clear() {
            delegate.clear();
            log.info("Cache CLEAR: cache={}", getName());
        }
    }
}

