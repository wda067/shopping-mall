package com.shop.global.auth;

import com.shop.exception.Forbidden;
import com.shop.exception.Unauthorized;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        //현재 요청에 연관된 세션이 존재하는 경우에만 세션을 반환
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("email") == null) {
            throw new Unauthorized();
        }

        //관리자 페이지 접근 시 권한 확인
        if (isAdminPage(request)) {
            String role = session.getAttribute("role").toString();
            if (!role.equals("ADMIN")) {
                throw new Forbidden();
            }
        }

        return true;
    }

    private boolean isAdminPage(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/admin") ||
                request.getRequestURI().startsWith("/api/members") ||
                request.getRequestURI().startsWith("/api/payments") ||
                (request.getRequestURI().startsWith("/api/product") && request.getMethod().equals("POST")) ||
                (request.getRequestURI().startsWith("/api/product/**") && request.getMethod().equals("DELETE"));
    }
}
