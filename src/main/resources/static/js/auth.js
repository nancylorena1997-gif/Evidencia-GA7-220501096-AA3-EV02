(() => {
    const form = document.getElementById("registerForm");
    if (!form) {
        return;
    }

    const emailPattern = /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i;

    const validators = {
        nombre(value) {
            if (!value.trim()) {
                return "Ingresa tu nombre completo.";
            }
            if (value.trim().length < 3) {
                return "El nombre debe tener al menos 3 caracteres.";
            }
            return "";
        },
        cedula(value) {
            if (!value.trim()) {
                return "Ingresa tu número de cédula.";
            }
            if (!/^\d+$/.test(value.trim())) {
                return "La cédula debe contener solo números.";
            }
            return "";
        },
        telefono(value) {
            const phone = value.replace(/\s+/g, "");
            if (!phone) {
                return "Ingresa tu número de teléfono.";
            }
            if (!/^\d{7,15}$/.test(phone)) {
                return "Ingresa un teléfono válido (mínimo 7 dígitos).";
            }
            return "";
        },
        email(value) {
            if (!value.trim()) {
                return "Ingresa tu correo electrónico.";
            }
            if (!emailPattern.test(value.trim())) {
                return "El correo electrónico no es válido.";
            }
            return "";
        },
        password(value) {
            if (!value) {
                return "Ingresa una contraseña.";
            }
            if (value.length < 6) {
                return "La contraseña debe tener al menos 6 caracteres.";
            }
            return "";
        },
        confirmation(value) {
            if (!value) {
                return "Confirma tu contraseña.";
            }
            if (value !== form.password.value) {
                return "Las contraseñas no coinciden.";
            }
            return "";
        }
    };

    const setFieldError = (name, message) => {
        const input = form.elements[name];
        const wrap = input?.closest(".input-wrap");
        const error = form.querySelector(`[data-error-for="${name}"]`);

        wrap?.classList.toggle("invalid", Boolean(message));
        input?.setAttribute("aria-invalid", message ? "true" : "false");

        if (error) {
            error.textContent = message;
            error.hidden = !message;
        }
    };

    const validateField = (name) => {
        const validator = validators[name];
        const input = form.elements[name];
        if (!validator || !input) {
            return true;
        }

        const message = validator(input.value);
        setFieldError(name, message);
        return !message;
    };

    const validateForm = () => {
        let isValid = true;
        Object.keys(validators).forEach((name) => {
            if (!validateField(name)) {
                isValid = false;
            }
        });
        return isValid;
    };

    form.addEventListener("submit", (event) => {
        if (!validateForm()) {
            event.preventDefault();
            form.querySelector(".input-wrap.invalid input")?.focus();
        }
    });

    Object.keys(validators).forEach((name) => {
        const input = form.elements[name];
        input?.addEventListener("input", () => {
            if (input.getAttribute("aria-invalid") === "true" || name === "confirmation") {
                validateField(name);
                if (name === "password" && form.confirmation.value) {
                    validateField("confirmation");
                }
            }
        });
        input?.addEventListener("blur", () => validateField(name));
    });
})();
