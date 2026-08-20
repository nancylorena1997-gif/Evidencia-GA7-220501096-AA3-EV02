(() => {
    const widget = document.getElementById("waWidget");
    const fab = document.getElementById("waFab");
    const panel = document.getElementById("waPanel");
    const fallbackBtn = document.getElementById("waFallback");
    const closeBtn = document.getElementById("waClose");
    const copyBtn = document.getElementById("waCopy");
    const copyOk = document.getElementById("waCopyOk");
    const drawer = document.getElementById("cartDrawer");

    if (!widget || !fab) {
        return;
    }

    const openPanel = () => {
        if (panel) {
            panel.hidden = false;
        }
    };

    const closePanel = () => {
        if (panel) {
            panel.hidden = true;
        }
        if (copyOk) {
            copyOk.hidden = true;
        }
    };

    const syncDrawerShift = () => {
        widget.classList.toggle("shift", Boolean(drawer?.classList.contains("open")));
    };

    fallbackBtn?.addEventListener("click", () => {
        if (panel?.hidden) {
            openPanel();
        } else {
            closePanel();
        }
    });
    closeBtn?.addEventListener("click", closePanel);

    copyBtn?.addEventListener("click", async () => {
        const phone = copyBtn.dataset.phone || "";
        try {
            await navigator.clipboard.writeText(phone);
        } catch (error) {
            window.prompt("Copia el número de WhatsApp comercial:", phone);
        }
        if (copyOk) {
            copyOk.hidden = false;
        }
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            closePanel();
        }
    });

    if (drawer) {
        new MutationObserver(syncDrawerShift).observe(drawer, {
            attributes: true,
            attributeFilter: ["class"]
        });
        syncDrawerShift();
    }
})();
