package net.iceyleagons.resourcehost.security;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author TOTHTOMI
 * @version 1.0.0
 * @since Nov. 23, 2021
 */
@Getter
@Service
public class RateLimitService {

    private final long uploadCost;
    private final long downloadCost;

    private final LoadingCache<String, Bucket> upload = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .initialCapacity(5)
            .maximumSize(250)
            .build(new CacheLoader<>() {
                @Override
                public Bucket load(String key) {
                    Bandwidth bandwidth = Bandwidth.classic(6, Refill.intervally(6, Duration.ofHours(1)));
                    return Bucket4j.builder().addLimit(bandwidth).build();
                }
            });

    private final LoadingCache<String, Bucket> download = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .initialCapacity(10)
            .maximumSize(250)
            .build(new CacheLoader<>() {
                @Override
                public Bucket load(String key) {
                    Bandwidth bandwidth = Bandwidth.classic(6, Refill.intervally(6, Duration.ofMinutes(1)));
                    return Bucket4j.builder().addLimit(bandwidth).build();
                }
            });

    private final LoadingCache<String, Bucket> general = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .initialCapacity(10)
            .maximumSize(250)
            .build(new CacheLoader<>() {
                @Override
                public Bucket load(String key) {
                    Bandwidth bandwidth = Bandwidth.classic(6, Refill.intervally(6, Duration.ofMinutes(1)));
                    return Bucket4j.builder().addLimit(bandwidth).build();
                }
            });

    public RateLimitService(@Value("${rate.upload-cost}") long uploadCost, @Value("${rate.download-cost}") long downloadCost) {
        this.uploadCost = uploadCost;
        this.downloadCost = downloadCost;
    }

    public Bucket getUpload(String id) throws ExecutionException {
        return this.upload.get(id);
    }

    public Bucket getDownload(String id) throws ExecutionException {
        return this.download.get(id);
    }

    public Bucket getGeneral(String id) throws ExecutionException {
        return this.general.get(id);
    }
}
