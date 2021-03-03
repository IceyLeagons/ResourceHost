package net.iceyleagons.resourcepackhost.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.BaseEncoding;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author TOTHTOMI
 */
@Service
public class HostService {

    //files will expire after 4 hours
    private final Cache<String, HostedFile> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(4, TimeUnit.HOURS)
            .initialCapacity(10)
            .maximumSize(500)
            .build();

    public String upload(HostedFile multipartFile) throws IOException {
        String id = getID();
        cache.put(id, multipartFile);
        return id;
    }

    public HostedFile retrieve(String id) {
        return cache.getIfPresent(id);
    }

    public static String getID() {
        UUID uuid = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return StringUtils.trimTrailingCharacter(BaseEncoding.base64Url().encode(bb.array()), '=');
    }

    @AllArgsConstructor
    @Getter
    public static class HostedFile {
        private final String type;
        private final byte[] data;
    }
}
