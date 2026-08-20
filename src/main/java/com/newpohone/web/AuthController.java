package com.newpohone.web;

import com.newpohone.security.AuthRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    private final AuthRepository authRepository;

    public AuthController(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String next,
            HttpSession session,
            Model model) {
        String safeNext = safeNext(next);
        model.addAttribute("next", safeNext);
        Map<String, Object> user = SessionUsers.current(session);
        if (user != null) {
            return redirectAfterAuth(user, safeNext);
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(@RequestParam(required = false) String next,
            HttpSession session,
            Model model) {
        String safeNext = safeNext(next);
        model.addAttribute("next", safeNext);
        Map<String, Object> user = SessionUsers.current(session);
        if (user != null) {
            return redirectAfterAuth(user, safeNext);
        }
        return "auth/register";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
            @RequestParam String password,
            @RequestParam(required = false) String next,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Ingresa el correo y la contraseña.");
            return loginRedirect(next);
        }

        try {
            Map<String, Object> user = authRepository.authenticate(email, password);
            if (user == null) {
                redirectAttributes.addFlashAttribute("error",
                        "El correo o la contraseña son incorrectos.");
                return loginRedirect(next);
            }
            HttpSession session = request.getSession(true);
            session.setAttribute(AuthInterceptor.USER_SESSION_KEY, user);
            return redirectAfterAuth(user, safeNext(next));
        } catch (DataAccessException exception) {
            redirectAttributes.addFlashAttribute("error",
                    "No fue posible iniciar sesión en este momento.");
            return loginRedirect(next);
        }
    }

    @PostMapping("/register")
    public String register(@RequestParam String nombre,
            @RequestParam String telefono,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmation,
            @RequestParam String cedula,
            @RequestParam(required = false) String next,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        if (nombre == null || nombre.trim().length() < 3 || telefono == null || telefono.trim().length() < 7) {
            redirectAttributes.addFlashAttribute("error", "Ingresa un nombre y un teléfono válidos.");
            return "redirect:/register";
        }
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            redirectAttributes.addFlashAttribute("error", "El correo electrónico no es válido.");
            return "redirect:/register";
        }
        if (password == null || password.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "La contraseña debe tener al menos 6 caracteres.");
            return "redirect:/register";
        }
        if (!password.equals(confirmation)) {
            redirectAttributes.addFlashAttribute("error", "Las contraseñas no coinciden.");
            return "redirect:/register";
        }

        int document;
        try {
            document = Integer.parseInt(cedula.trim());
        } catch (NumberFormatException exception) {
            redirectAttributes.addFlashAttribute("error", "La cédula no es válida.");
            return "redirect:/register";
        }

        try {
            Map<String, Object> user = authRepository.register(document, nombre, telefono, email, password);
            HttpSession session = request.getSession(true);
            session.setAttribute(AuthInterceptor.USER_SESSION_KEY, user);
            redirectAttributes.addFlashAttribute("success", "Cuenta creada. Bienvenido a Newphone.");
            return redirectAfterAuth(user, safeNext(next));
        } catch (DataAccessException exception) {
            redirectAttributes.addFlashAttribute("error",
                    "No fue posible registrar la cuenta. Verifica que el correo o la cédula no existan.");
            return "redirect:/register";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logoutGet(HttpServletRequest request) {
        return logout(request);
    }

    private String redirectAfterAuth(Map<String, Object> user, String next) {
        if (next != null) {
            return "redirect:" + next;
        }
        if ("CLIENTE".equals(String.valueOf(user.get("rol")))) {
            return "redirect:/catalog";
        }
        return "redirect:/dashboard";
    }

    private String loginRedirect(String next) {
        String safe = safeNext(next);
        return safe == null ? "redirect:/login" : "redirect:/login?next=" + safe;
    }

    private String safeNext(String next) {
        if (next == null || next.isBlank()) {
            return null;
        }
        String value = next.trim();
        if (!value.startsWith("/") || value.startsWith("//") || value.contains("://") || value.contains("\\")) {
            return null;
        }
        return value;
    }
}
