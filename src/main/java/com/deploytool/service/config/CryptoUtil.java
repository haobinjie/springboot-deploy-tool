package com.deploytool.service.config;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * AES 加解密工具（V1.1 起对 password/passphrase 加密存储）。
 * 密文以 {@code AES:} 前缀标识；无该前缀视为 V1.0 明文，保证平滑过渡。
 * 注意：V1.0 不加密，密钥来源为占位实现，V1.1 需替换为安全密钥管理。
 */
public final class CryptoUtil {

    public static final String PREFIX = "AES:";
    private static final String ALGORITHM = "AES";
    // 16 字节示例密钥，正式版本应从安全来源加载
    private static final byte[] KEY = "0123456789abcdef".getBytes(StandardCharsets.UTF_8);

    private CryptoUtil() {
    }

    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    /** 若已加密则原样返回，否则加密并加前缀。 */
    public static String encryptIfPlain(String plain) {
        if (plain == null || plain.isEmpty() || isEncrypted(plain)) {
            return plain;
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY, ALGORITHM));
            byte[] out = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return PREFIX + Base64.encodeBase64String(out);
        } catch (Exception e) {
            throw new IllegalStateException("加密失败", e);
        }
    }

    /** 若带前缀则解密返回明文，否则原样返回（兼容明文）。 */
    public static String decrypt(String stored) {
        if (!isEncrypted(stored)) {
            return stored;
        }
        try {
            String body = stored.substring(PREFIX.length());
            byte[] raw = Base64.decodeBase64(body);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KEY, ALGORITHM));
            return new String(cipher.doFinal(raw), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("解密失败", e);
        }
    }
}
