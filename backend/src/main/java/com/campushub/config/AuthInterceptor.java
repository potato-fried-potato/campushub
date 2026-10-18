package com.campushub.config;

import com.campushub.common.JwtUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * 登录拦截：先解析 token，再判断当前接口是否允许匿名
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            Map<String, Object> claims = JwtUtil.parse(token.substring(7));
            if (claims != null) {
                request.setAttribute("userId", Long.valueOf(String.valueOf(claims.get("sub"))));
                request.setAttribute("role", claims.get("role"));
                request.setAttribute("nickname", claims.get("nickname"));
                request.setAttribute("studentNo", claims.get("studentNo"));
            }
        }
        if (isAnonymous(request) || request.getAttribute("userId") != null) {
            return true;
        }
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已过期\",\"data\":null}");
        return false;
    }

    // 匿名接口：注册登录、分类统计、商品浏览、商品留言、用户评价
    private boolean isAnonymous(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        if ("/api/auth/login".equals(uri) || "/api/auth/register".equals(uri)) {
            return true;
        }
        if ("/api/categories".equals(uri) || "/api/system/stats".equals(uri)) {
            return true;
        }
        if (!"GET".equals(method)) {
            return false;
        }
        return "/api/items".equals(uri)
                || uri.matches("/api/items/\\d+")
                || uri.matches("/api/items/\\d+/messages")
                || uri.matches("/api/users/\\d+/reviews");
    }
}
