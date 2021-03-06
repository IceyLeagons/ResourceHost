package net.iceyleagons.resourcepackhost.service;

import com.google.common.io.BaseEncoding;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author TOTHTOMI
 */
@Service
public class HostService {

    private final File directory;

    public HostService() {
        directory = new File("packs");
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                System.out.println("Could not create packs folder!");
            };
        }
        new Thread(() -> {
            while (true) {
                try {
                    System.out.println("TICK");
                    Arrays.stream(Objects.requireNonNull(directory.listFiles())).filter(this::shouldDelete).forEach(file -> {
                        if (file.getParentFile().getName().contains("packs")) {
                            if (file.getName().contains("zip")) {
                                System.out.println("Deleting a file");
                                if (!file.delete())
                                    System.out.println("Could not delete expired pack named: " + file.getName());
                            }
                        }
                    });
                    Thread.sleep(TimeUnit.MINUTES.toMillis(1));
                } catch (Exception ignored) {
                }
            }
        }).start();
    }

    public String upload(MultipartFile multipartFile) throws IOException {
        String id = getID();
        writeToFile(id, multipartFile);
        return id;
    }

    public String getChecksum(byte[] data) {
        return DigestUtils.md5DigestAsHex(data);
    }

    @SneakyThrows
    public byte[] retrieve(String id) {
        return readFile(id);
    }

    public void writeToFile(String id, MultipartFile hostedFile) throws IOException {
        try (OutputStream outputStream = new FileOutputStream(new File(directory, id+".zip"))) {
            outputStream.write(hostedFile.getBytes());
        }
    }

    public byte[] readFile(String id) throws IOException {
        File file = new File(directory, id+".zip");
        if (!file.exists()) return null;
        try (InputStream inputStream = new FileInputStream(file)) {
            return inputStream.readAllBytes();
        }
    }

    public boolean shouldDelete(File file) {
        return (file.lastModified() + TimeUnit.HOURS.toMillis(4)) <= System.currentTimeMillis();
    }

    public static String getID() {
        UUID uuid = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return StringUtils.trimTrailingCharacter(BaseEncoding.base64Url().encode(bb.array()), '=');
    }
}
