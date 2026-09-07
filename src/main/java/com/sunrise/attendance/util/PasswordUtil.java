package com.sunrise.attendance.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * ユーザーごとにランダムなソルトを生成しSHA-256でハッシュ化する。
 * 本番運用では反復ストレッチングを行うBCrypt等への置き換えを推奨(READMEに記載)。
 */
public final class PasswordUtil {

    private PasswordUtil() {}

    public static String hash(String rawPassword) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] digest = digest(rawPassword, salt);
        return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(digest);
    }

    public static boolean matches(String rawPassword, String stored) {
        String[] parts = stored.split(":", 2);
        if (parts.length != 2) return false;
        byte[] salt = Base64.getDecoder().decode(parts[0]);
        byte[] expected = Base64.getDecoder().decode(parts[1]);
        byte[] actual = digest(rawPassword, salt);
        return MessageDigest.isEqual(expected, actual);
    }

    private static byte[] digest(String rawPassword, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            return md.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
