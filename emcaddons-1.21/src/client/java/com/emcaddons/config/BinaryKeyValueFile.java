package com.emcaddons.config;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class BinaryKeyValueFile {
    private static final int MAGIC = 0x43424346; // CBCF
    private static final short VERSION = 1;
    private static final String CIPHER_ALGO = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int NONCE_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final byte[] KEY_BYTES = deriveKey();

    private BinaryKeyValueFile() {}

    public static Properties load(File file) throws IOException {
        Properties properties = new Properties();
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(file.toPath())))) {
            int magic = in.readInt();
            if (magic != MAGIC) throw new IOException("Invalid config file magic for " + file.getName());
            short version = in.readShort();
            if (version != VERSION) throw new IOException("Unsupported config version " + version + " for " + file.getName());

            byte[] nonce = in.readNBytes(NONCE_LENGTH);
            if (nonce.length != NONCE_LENGTH) throw new IOException("Corrupt config nonce for " + file.getName());
            int encryptedLength = in.readInt();
            if (encryptedLength <= 0) throw new IOException("Invalid encrypted payload length for " + file.getName());
            byte[] encryptedPayload = in.readNBytes(encryptedLength);
            if (encryptedPayload.length != encryptedLength) throw new IOException("Unexpected end of file while reading encrypted payload");
            if (in.read() != -1) throw new IOException("Unexpected trailing bytes in " + file.getName());

            byte[] payload = decrypt(encryptedPayload, nonce);
            try (DataInputStream payloadIn = new DataInputStream(new ByteArrayInputStream(payload))) {
                int entryCount = payloadIn.readInt();
                if (entryCount < 0) throw new IOException("Invalid entry count for " + file.getName());
                for (int i = 0; i < entryCount; i++) {
                    String key = readString(payloadIn);
                    String value = readString(payloadIn);
                    properties.setProperty(key, value);
                }
                if (payloadIn.read() != -1) throw new IOException("Unexpected trailing payload bytes in " + file.getName());
            }
        } catch (EOFException e) {
            throw new IOException("Corrupt binary config file: " + file.getName(), e);
        }
        return properties;
    }

    public static void save(Properties properties, File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) Files.createDirectories(parent.toPath());

        Map<String, String> ordered = new TreeMap<>();
        for (String key : properties.stringPropertyNames()) {
            ordered.put(key, properties.getProperty(key, ""));
        }

        ByteArrayOutputStream payloadBuffer = new ByteArrayOutputStream();
        try (DataOutputStream payloadOut = new DataOutputStream(payloadBuffer)) {
            payloadOut.writeInt(ordered.size());
            for (Map.Entry<String, String> entry : ordered.entrySet()) {
                writeString(payloadOut, entry.getKey());
                writeString(payloadOut, entry.getValue());
            }
        }
        byte[] payload = payloadBuffer.toByteArray();
        byte[] nonce = new byte[NONCE_LENGTH];
        RANDOM.nextBytes(nonce);
        byte[] encryptedPayload = encrypt(payload, nonce);

        File temp = parent != null ? new File(parent, file.getName() + ".tmp") : new File(file.getName() + ".tmp");
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(temp.toPath())))) {
            out.writeInt(MAGIC);
            out.writeShort(VERSION);
            out.write(nonce);
            out.writeInt(encryptedPayload.length);
            out.write(encryptedPayload);
        }

        try {
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static String readString(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length < 0) throw new IOException("Negative string length");
        byte[] bytes = in.readNBytes(length);
        if (bytes.length != length) throw new EOFException("Unexpected end of file while reading string");
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static byte[] deriveKey() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] full = digest.digest("complex-bundle:binary-config:v1".getBytes(StandardCharsets.UTF_8));
            byte[] key = new byte[16];
            System.arraycopy(full, 0, key, 0, key.length);
            return key;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not derive encryption key", e);
        }
    }

    private static byte[] encrypt(byte[] plaintext, byte[] nonce) throws IOException {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY_BYTES, "AES"), new GCMParameterSpec(GCM_TAG_BITS, nonce));
            return cipher.doFinal(plaintext);
        } catch (GeneralSecurityException e) {
            throw new IOException("Failed to encrypt config payload", e);
        }
    }

    private static byte[] decrypt(byte[] ciphertext, byte[] nonce) throws IOException {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KEY_BYTES, "AES"), new GCMParameterSpec(GCM_TAG_BITS, nonce));
            return cipher.doFinal(ciphertext);
        } catch (GeneralSecurityException e) {
            throw new IOException("Failed to decrypt config payload", e);
        }
    }
}
