package com.campushub.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("公共组件 JWT：签发、校验与防篡改")
class JwtUtilTest {

    private final JwtUtil jwtUtil = new JwtUtil("unit-test-secret", 3_600_000L);

    @Test
    @DisplayName("签发后能解析出原始声明")
    void createAndParse() {
        String token = jwtUtil.create(7L, "542412320201", "边皓东", "USER");

        assertThat(token.split("\\.")).hasSize(3);

        Map<String, Object> payload = jwtUtil.parse(token);
        assertThat(payload).isNotNull();
        assertThat(payload.get("sub")).isEqualTo("7");
        assertThat(payload.get("studentNo")).isEqualTo("542412320201");
        assertThat(payload.get("nickname")).isEqualTo("边皓东");
        assertThat(payload.get("role")).isEqualTo("USER");
        assertThat(((Number) payload.get("exp")).longValue()).isGreaterThan(System.currentTimeMillis());
    }

    @Test
    @DisplayName("篡改 payload 后签名校验失败，解析返回 null")
    void tamperedPayloadRejected() {
        String[] parts = jwtUtil.create(1L, "542412310217", "宋杰", "USER").split("\\.");
        String forged = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"999\",\"exp\":9999999999999}".getBytes(StandardCharsets.UTF_8));

        assertThat(jwtUtil.parse(parts[0] + "." + forged + "." + parts[2])).isNull();
    }

    @Test
    @DisplayName("篡改签名后解析返回 null")
    void tamperedSignatureRejected() {
        String[] parts = jwtUtil.create(1L, "542412310217", "宋杰", "USER").split("\\.");

        assertThat(jwtUtil.parse(parts[0] + "." + parts[1] + ".AAAA")).isNull();
    }

    @Test
    @DisplayName("分段数不足、空串或 null 时返回 null")
    void malformedTokenRejected() {
        assertThat(jwtUtil.parse("abc")).isNull();
        assertThat(jwtUtil.parse("a.b")).isNull();
        assertThat(jwtUtil.parse("")).isNull();
        assertThat(jwtUtil.parse(null)).isNull();
    }

    @Test
    @DisplayName("结构完整但内容伪造的 token 不会被接受")
    void forgedTokenRejected() {
        assertThat(jwtUtil.parse("e30.e30.e30")).isNull();
    }

    @Test
    @DisplayName("已过期的 token 解析返回 null")
    void expiredTokenRejected() {
        JwtUtil expired = new JwtUtil("unit-test-secret", -1_000L);

        String token = expired.create(1L, "542412310217", "宋杰", "USER");

        assertThat(jwtUtil.parse(token)).isNull();
    }

    @Test
    @DisplayName("使用不同密钥签发的 token 不能被验签通过")
    void tokenFromAnotherSecretRejected() {
        JwtUtil other = new JwtUtil("another-secret", 3_600_000L);

        assertThat(jwtUtil.parse(other.create(1L, "542412310217", "宋杰", "USER"))).isNull();
    }
}
