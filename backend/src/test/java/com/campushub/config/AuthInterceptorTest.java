package com.campushub.config;

import com.campushub.common.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("权限边界：管理员不参与用户交易")
class AuthInterceptorTest {

    private final AuthInterceptor interceptor =
            new AuthInterceptor(new JwtUtil("unit-test-secret", 3_600_000L));

    private MockHttpServletRequest request(String method, String uri) {
        return new MockHttpServletRequest(method, uri);
    }

    @Test
    @DisplayName("管理员可以只读浏览集市")
    void adminCanBrowse() throws Exception {
        MockHttpServletRequest request = request("GET", "/api/items");
        request.setAttribute("userId", 99L);
        request.setAttribute("role", "ADMIN");

        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("管理员调用用户侧写接口被拦截（4003）")
    void adminCannotWriteUserApi() throws Exception {
        MockHttpServletRequest request = request("POST", "/api/items");
        request.setAttribute("userId", 99L);
        request.setAttribute("role", "ADMIN");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).contains("\"code\":4003");
        assertThat(response.getContentAsString()).contains("不参与用户交易活动");
    }

    @Test
    @DisplayName("管理员可以调用管理接口")
    void adminCanUseAdminApi() throws Exception {
        MockHttpServletRequest request = request("PATCH", "/api/admin/items/1/off-shelf");
        request.setAttribute("userId", 99L);
        request.setAttribute("role", "ADMIN");

        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
    }

    @Test
    @DisplayName("管理员可以调用认证接口")
    void adminCanUseAuthApi() throws Exception {
        MockHttpServletRequest request = request("POST", "/api/auth/logout");
        request.setAttribute("userId", 99L);
        request.setAttribute("role", "ADMIN");

        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
    }

    @Test
    @DisplayName("普通用户写接口放行（由业务层校验角色）")
    void userCanWrite() throws Exception {
        MockHttpServletRequest request = request("POST", "/api/bargains");
        request.setAttribute("userId", 2L);
        request.setAttribute("role", "USER");

        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
    }

    @Test
    @DisplayName("未登录写接口返回 HTTP 401")
    void anonymousWriteRejected() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request("POST", "/api/items"), response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }
}
