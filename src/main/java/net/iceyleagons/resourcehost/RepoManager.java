package net.iceyleagons.resourcehost;

import com.google.common.io.BaseEncoding;
import lombok.Getter;
import lombok.SneakyThrows;
import net.iceyleagons.resourcehost.utils.AdvancedFile;
import net.iceyleagons.resourcehost.utils.Checksums;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.MultipartConfigElement;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author TOTHTOMI
 * @version 1.0.0
 * @since Nov. 23, 2021
 */
@Getter
@Service
public class RepoManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepoManager.class);

    private final Set<String> files = new HashSet<>();
    private final AdvancedFile repoFolder;
    private final long retainTime; //in minutes

    private final long mbRaw;
    private final long mbLimit;

    public RepoManager(@Value("${repo.retain-time}") long retainTime, @Value("${repo.mb-limit}") long mbLimit) {
        this.repoFolder = new AdvancedFile(new File("resourcepacks"), true);
        this.retainTime = retainTime;

        this.mbRaw = mbLimit;
        this.mbLimit = mbLimit * 1000000L;
        clearResourcePacks();
        startCleanUpThread();
    }

    private void clearResourcePacks() {
        this.files.clear();
        for (File file : Objects.requireNonNull(repoFolder.file().listFiles())) {
            if (!file.delete()) {
                LOGGER.warn("Could not delete file: " + file.getAbsolutePath());
            }
        }
    }

    public boolean upload(JSONObject jsonObject, MultipartFile file)  {
        try {
            String id = getId();
            files.add(id);
            File result = writeToFile(id, file);

            jsonObject.put("download", getDownloadUrl(id));
            jsonObject.put("available", System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(retainTime));
            jsonObject.put("key", id);
            jsonObject.put("hashes", getChecksums(result));
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    private JSONObject getChecksums(File file) {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("md5", Checksums.MD5.asHex(file));
        jsonObject.put("sha1", Checksums.SHA1.asHex(file));
        jsonObject.put("sha256", Checksums.SHA256.asHex(file));
        jsonObject.put("sha512", Checksums.SHA512.asHex(file));

        return jsonObject;
    }

    public void cleanUpFiles() {
        for (File file : Objects.requireNonNull(repoFolder.file().listFiles())) {
            deleteFileIfExpired(file);
        }
    }

    public String getDownloadUrl(String id) {
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString() + "/get/" + id;
    }

    public boolean contains(String id) {
        return this.files.contains(id);
    }

    public long getAvailability(String id) {
        File file = repoFolder.getChild(id + ".zip");
        return file.lastModified() + TimeUnit.MINUTES.toMillis(retainTime);
    }

    public byte[] readFile(String id) throws IOException {
        File file = repoFolder.getChild(id + ".zip");
        if (!file.exists()) return null;
        try (InputStream inputStream = new FileInputStream(file)) {
            return inputStream.readAllBytes();
        }
    }

    private File writeToFile(String id, MultipartFile file) throws IOException {
        File toWrite = this.repoFolder.getChild(id + ".zip");
        try (OutputStream os = new FileOutputStream(toWrite)) {
            os.write(file.getBytes());
        }

        return toWrite;
    }

    private String getId() {
        UUID uuid = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        String id = StringUtils.trimTrailingCharacter(BaseEncoding.base64Url().encode(bb.array()), '=');

        return contains(id) ? getId() : id;
    }

    private void startCleanUpThread() {
        final long sleepMillis = TimeUnit.MINUTES.toMillis(1);
        Thread thread = new Thread(() -> {
            cleanUpFiles();
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void deleteFileIfExpired(File file) {
        // Some fail safes, so we don't wreck our system accidentally.
        if (!file.getParentFile().getName().contains("packs") && !file.getParentFile().equals(this.repoFolder.file())) return;
        if (!file.getName().contains("zip")) return;

        long diffMs = System.currentTimeMillis() - file.lastModified();
        long retain = TimeUnit.MINUTES.toMillis(retainTime);

        if (diffMs > retain) {
            files.remove(file.getName().split("\\.")[0]);
            if (!file.delete()) {
                LOGGER.warn("Could not delete file: " + file.getAbsolutePath());
            }
        }
    }

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.ofMegabytes(mbRaw));
        factory.setMaxRequestSize(DataSize.ofMegabytes(mbRaw));
        return factory.createMultipartConfig();
    }
}
