package com.newpohone.web;

import jakarta.servlet.http.HttpSession;
import java.util.Map;

public final class SessionUsers {

    private SessionUsers() {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> current(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object user = session.getAttribute(AuthInterceptor.USER_SESSION_KEY);
        if (user instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }

    public static Integer customerCedula(HttpSession session) {
        Map<String, Object> user = current(session);
        if (user == null || !"CLIENTE".equals(String.valueOf(user.get("rol")))) {
            return null;
        }
        Object cedula = user.get("cedula");
        return cedula instanceof Number number ? number.intValue() : null;
    }
}
