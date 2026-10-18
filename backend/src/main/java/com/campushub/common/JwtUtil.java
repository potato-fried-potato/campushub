package com.campushub.common;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 手写 HS256 JWT，够用就行
 */
public class JwtUtil {

    private static final String SECRET = "campushub-jwt-secret-2026";
    private static final long EXPIRE = 7L * 24 * 60 * 60 * 1000;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String create(Long userId, String studentNo, String nickname, String role) {
        try {
            Map<String, Object> header = new HashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");
            Map<String, Object> payload = new HashMap<>();
            payload.put("sub", String.valueOf(userId));
            payload.put("studentNo", studentNo);
            payload.put("nickname", nickname);
            payload.put("role", role);
            payload.put("exp", System.currentTimeMillis() + EXPIRE);
            String h = encode(MAPPER.writeValueAsBytes(header));
            String p = encode(MAPPER.writeValueAsBytes(payload));
            return h + "." + p + "." + sign(h + "." + p);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parse(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            String expected = sign(parts[0] + "." + parts[1]);
            if (!expected.equals(parts[2])) {
                return null;
            }
            Map<String, Object> payload = MAPPER.readValue(Base64.getUrlDecoder().decode(parts[1]), Map.class);
            long exp = Long.parseLong(String.valueOf(payload.get("exp")));
            if (exp < System.currentTimeMillis()) {
                return null;
            }
            return payload;
        } catch (Exception e) {
            return null;
        }
    }

    private static String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sign(String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return encode(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
