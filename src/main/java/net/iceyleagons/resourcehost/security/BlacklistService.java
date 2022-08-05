package net.iceyleagons.resourcehost.security;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import lombok.SneakyThrows;
import net.iceyleagons.resourcehost.utils.AdvancedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * A very crude blacklist implementation
 *
 * @author TOTHTOMI
 * @version 1.0.0
 * @since Nov. 23, 2021
 */
@Service
public class BlacklistService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlacklistService.class);

    private final AdvancedFile blacklistFile;
    private final Set<String> blacklisted = new HashSet<>();

    // You can only exceed the rate limit twice/hour to not get banned. The cache holds its values for 3 hours accordingly with a bit of breathing room in mind
    // TODO move out to config
    private final LoadingCache<String, Bucket> rateLimitExceed = CacheBuilder.newBuilder()
            .expireAfterAccess(3, TimeUnit.HOURS)
            .initialCapacity(10)
            .maximumSize(250)
            .build(new CacheLoader<>() {
                @Override
                public Bucket load(String key) {
                    Bandwidth bandwidth = Bandwidth.classic(2, Refill.intervally(2, Duration.ofHours(1)));
                    return Bucket4j.builder().addLimit(bandwidth).build();
                }
            });

    public BlacklistService() {
        this.blacklistFile = new AdvancedFile(new File("blacklist.txt"));

        final long sleepMillis = TimeUnit.HOURS.toMillis(1);
        Thread thread = new Thread(() -> {
            reloadBlacklistSet();
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    @SneakyThrows
    public void rateLimitExceed(String ip) {
        Bucket bucket = rateLimitExceed.get(ip);
        if (!bucket.tryConsume(1)) {
            LOGGER.info("Blacklisted IP: {}", ip);
            addToBlacklist(ip);
        }
    }

    public boolean isBlacklisted(String ip) {
        return blacklisted.contains(ip);
    }

    public void addToBlacklist(String ip) {
        blacklistFile.appendToFile(ip);
        reloadBlacklistSet();
    }

    private void reloadBlacklistSet() {
        blacklisted.clear();
        blacklisted.addAll(List.of(blacklistFile.getContent(true).split(System.lineSeparator())));
    }
}
