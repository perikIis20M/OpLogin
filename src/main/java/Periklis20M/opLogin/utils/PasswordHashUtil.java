package Periklis20M.opLogin.utils;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordHashUtil {

    private PasswordHashUtil() {
    }

    public static String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public static boolean verify(String password, String hash) {
        if (password == null || hash == null || hash.isEmpty()) {
            return false;
        }

        try {
            return BCrypt.checkpw(password, hash);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public static boolean isHash(String value) {
        if (value == null) {
            return false;
        }
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }
}
