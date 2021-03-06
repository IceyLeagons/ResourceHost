package net.iceyleagons.resourcepackhost.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author TOTHTOMI
 */
@Service
public class RateLimitService {

    //5 uploads/hour

    @Getter
    private final long uploadCost = 1;
    @Getter
    private final long downloadRateLimit = 1;

    private final LoadingCache<String, Bucket> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .initialCapacity(10)
            .maximumSize(250)
            .build(new CacheLoader<>() {
                @Override
                public Bucket load(String key) {
                    Bandwidth bandwidth = Bandwidth.classic(5, Refill.intervally(5, Duration.ofHours(1)));
                    return Bucket4j.builder().addLimit(bandwidth).build();
                }
            });

    private final LoadingCache<String, Bucket> downloadCache = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .initialCapacity(10)
            .maximumSize(250)
            .build(new CacheLoader<>() {
                @Override
                public Bucket load(String key) {
                    Bandwidth bandwidth = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)));
                    return Bucket4j.builder().addLimit(bandwidth).build();
                }
            });

    private final LoadingCache<String, Bucket> otherCache = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .initialCapacity(10)
            .maximumSize(250)
            .build(new CacheLoader<>() {
                @Override
                public Bucket load(String key) {
                    Bandwidth bandwidth = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)));
                    return Bucket4j.builder().addLimit(bandwidth).build();
                }
            });

    public Bucket get(String id) throws ExecutionException {
        return cache.get(id);
    }

    public Bucket getDownload(String id) throws ExecutionException {
        return downloadCache.get(id);
    }

    public Bucket getOther(String id) throws ExecutionException {
        return downloadCache.get(id);
    }
}
