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

    private final JwtUtil jwtUtil;

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            Map<String, Object> claims = jwtUtil.parse(token.substring(7));
            if (claims != null) {
                request.setAttribute("userId", Long.valueOf(String.valueOf(claims.get("sub"))));
                request.setAttribute("role", claims.get("role"));
                request.setAttribute("nickname", claims.get("nickname"));
                request.setAttribute("studentNo", claims.get("studentNo"));
            }
        }
        // 管理端与用户端分开：管理员账号只做后台管理，不参与用户侧的交易活动
        if (isAdminBlocked(request)) {
            response.setStatus(200);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":4003,\"message\":\"管理员账号仅用于后台管理，不参与用户交易活动\",\"data\":null}");
            return false;
        }
        if (isAnonymous(request) || request.getAttribute("userId") != null) {
            return true;
        }
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已过期\",\"data\":null}");
        return false;
    }

    // 管理员可用：认证接口、管理接口，以及只读浏览；其余用户侧写操作一律拒绝
    private boolean isAdminBlocked(HttpServletRequest request) {
        if (!"ADMIN".equals(String.valueOf(request.getAttribute("role")))) {
            return false;
        }
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/admin") || uri.startsWith("/api/auth")) {
            return false;
        }
        String method = request.getMethod();
        return !("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method));
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
