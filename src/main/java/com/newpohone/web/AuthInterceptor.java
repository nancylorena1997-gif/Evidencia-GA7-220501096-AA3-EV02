package com.newpohone.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String USER_SESSION_KEY = "authenticatedUser";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HttpSession session = request.getSession(false);
        Object user = session == null ? null : session.getAttribute(USER_SESSION_KEY);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }
        request.setAttribute("currentUser", user);
        if (user instanceof Map<?, ?> map) {
            Object role = map.get("rol");
            request.setAttribute("userName", map.get("nombre"));
            request.setAttribute("userRole", role);
            request.setAttribute("userEmail", map.get("email"));
            if ("CLIENTE".equals(String.valueOf(role)) && isAdminPath(request)) {
                response.sendRedirect(request.getContextPath() + "/catalog");
                return false;
            }
        }
        return true;
    }

    private boolean isAdminPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        String path = context != null && !context.isEmpty() && uri.startsWith(context)
                ? uri.substring(context.length())
                : uri;
        return path.startsWith("/dashboard")
                || path.startsWith("/modules")
                || path.startsWith("/inventario")
                || path.startsWith("/pedidos");
    }
}
