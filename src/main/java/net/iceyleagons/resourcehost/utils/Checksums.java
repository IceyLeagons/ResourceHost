package net.iceyleagons.resourcehost.utils;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.security.crypto.codec.Hex;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;

/**
 * @author TOTHTOMI
 * @version 1.0.0
 * @since Nov. 23, 2021
 */
@AllArgsConstructor
public enum Checksums {

    MD5("MD5"),
    SHA1("SHA1"),
    SHA256("SHA-256"),
    SHA512("SHA-512");

    private String name;


    @SneakyThrows
    public byte[] digest(File file) {
        try (InputStream in = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance(this.name);
            byte[] buffer = new byte[1024];
            int length;

            while ((length = in.read(buffer, 0, buffer.length)) > 0) {
                digest.update(buffer, 0, length);
            }

            return digest.digest();
        }
    }

    public String asHex(File file) {
        return new String(Hex.encode(digest(file)));
    }
}
