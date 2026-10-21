package com.campushub.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 手写 HS256 JWT。
 * 签名密钥与有效期来自配置（campushub.jwt.secret / expire-ms），源码中不再保存密钥。
 */
@Component
public class JwtUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final byte[] secret;
    private final long expireMillis;

    public JwtUtil(@Value("${campushub.jwt.secret}") String secret,
                   @Value("${campushub.jwt.expire-ms:604800000}") long expireMillis) {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("campushub.jwt.secret 不能为空");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expireMillis = expireMillis;
    }

    public String create(Long userId, String studentNo, String nickname, String role) {
        try {
            Map<String, Object> header = new HashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");
            Map<String, Object> payload = new HashMap<>();
            payload.put("sub", String.valueOf(userId));
            payload.put("studentNo", studentNo);
            payload.put("nickname", nickname);
            payload.put("role", role);
            payload.put("exp", System.currentTimeMillis() + expireMillis);
            String h = encode(MAPPER.writeValueAsBytes(header));
            String p = encode(MAPPER.writeValueAsBytes(payload));
            return h + "." + p + "." + sign(h + "." + p);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> parse(String token) {
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

    private String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sign(String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return encode(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
