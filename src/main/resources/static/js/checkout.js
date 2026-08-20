(() => {
    const form = document.getElementById("checkoutForm");
    if (!form) {
        return;
    }

    const cardFields = document.getElementById("cardFields");
    const methodInputs = form.querySelectorAll("input[name='metodo']");

    const selectedMethod = () => form.querySelector("input[name='metodo']:checked")?.value || "";
    const isCard = (method) => method.startsWith("Tarjeta");

    const toggleCardFields = () => {
        const show = isCard(selectedMethod());
        if (cardFields) {
            cardFields.hidden = !show;
        }
        ["titular", "numero", "vencimiento", "cvv"].forEach((name) => {
            form.elements[name]?.toggleAttribute("required", show);
        });
    };

    const setError = (name, message) => {
        const error = document.getElementById(`error-${name}`);
        const field = form.querySelector(`#${name}`)?.closest(".field");
        if (error) {
            error.textContent = message;
            error.hidden = !message;
        }
        field?.classList.toggle("invalid", Boolean(message));
    };

    const cardDigits = () => String(form.numero?.value || "").replace(/\D/g, "");

    const validate = () => {
        let valid = true;
        const method = selectedMethod();
        if (!method) {
            setError("metodo", "Selecciona un método de pago.");
            valid = false;
        } else {
            setError("metodo", "");
        }

        if (!isCard(method)) {
            ["titular", "numero", "vencimiento", "cvv"].forEach((name) => setError(name, ""));
        } else {
            const titular = String(form.titular.value || "").trim();
            if (titular.length < 3) {
                setError("titular", "Ingresa el nombre que aparece en la tarjeta.");
                valid = false;
            } else {
                setError("titular", "");
            }

            if (cardDigits().length < 13 || cardDigits().length > 19) {
                setError("numero", "Ingresa un número de tarjeta válido.");
                valid = false;
            } else {
                setError("numero", "");
            }

            if (!/^(0[1-9]|1[0-2])\/\d{2}$/.test(String(form.vencimiento.value || "").trim())) {
                setError("vencimiento", "Usa el formato MM/AA.");
                valid = false;
            } else {
                setError("vencimiento", "");
            }

            if (!/^\d{3,4}$/.test(String(form.cvv.value || "").trim())) {
                setError("cvv", "Ingresa un CVV de 3 o 4 dígitos.");
                valid = false;
            } else {
                setError("cvv", "");
            }
        }

        const nombre = String(form.nombre?.value || "").trim();
        if (nombre.length < 3) {
            setError("nombre", "Ingresa el nombre de quien recibe.");
            valid = false;
        } else {
            setError("nombre", "");
        }

        const phone = String(form.telefono?.value || "").replace(/\D/g, "");
        if (phone.length < 7) {
            setError("telefono", "Ingresa un teléfono válido.");
            valid = false;
        } else {
            setError("telefono", "");
        }

        const direccion = String(form.direccion?.value || "").trim();
        if (direccion.length < 10) {
            setError("direccion", "Ingresa una dirección completa.");
            valid = false;
        } else {
            setError("direccion", "");
        }

        return valid;
    };

    form.numero?.addEventListener("input", () => {
        const digits = cardDigits().slice(0, 16);
        form.numero.value = digits.replace(/(\d{4})(?=\d)/g, "$1 ").trim();
    });

    form.vencimiento?.addEventListener("input", () => {
        const digits = String(form.vencimiento.value || "").replace(/\D/g, "").slice(0, 4);
        form.vencimiento.value = digits.length > 2 ? `${digits.slice(0, 2)}/${digits.slice(2)}` : digits;
    });

    methodInputs.forEach((input) => input.addEventListener("change", toggleCardFields));

    form.addEventListener("submit", (event) => {
        if (!validate()) {
            event.preventDefault();
            const firstError = form.querySelector(".field-error:not([hidden])");
            firstError?.scrollIntoView({ behavior: "smooth", block: "center" });
        }
    });

    toggleCardFields();
})();
