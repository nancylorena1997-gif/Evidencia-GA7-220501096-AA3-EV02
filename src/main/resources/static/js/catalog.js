(() => {
    const panel = document.getElementById("filtersPanel");
    const openBtn = document.getElementById("openFilters");
    const closeBtn = document.getElementById("closeFilters");
    const backdrop = document.getElementById("filtersBackdrop");

    const lockBody = () => {
        const locked = document.querySelector(".filters.open, .cart-drawer.open");
        document.body.style.overflow = locked ? "hidden" : "";
    };

    const closeCart = () => {
        document.getElementById("cartDrawer")?.classList.remove("open");
        const cartBackdrop = document.getElementById("cartBackdrop");
        if (cartBackdrop) {
            cartBackdrop.hidden = true;
        }
        lockBody();
    };

    const open = () => {
        closeCart();
        panel?.classList.add("open");
        if (backdrop) {
            backdrop.hidden = false;
        }
        lockBody();
    };

    const close = () => {
        panel?.classList.remove("open");
        if (backdrop) {
            backdrop.hidden = true;
        }
        lockBody();
    };

    openBtn?.addEventListener("click", open);
    closeBtn?.addEventListener("click", close);
    backdrop?.addEventListener("click", close);

    window.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            close();
        }
    });

    window.addEventListener("resize", () => {
        if (window.innerWidth > 900) {
            close();
        }
    });

    const filterForm = document.getElementById("filterForm");
    filterForm?.querySelectorAll("input[name='categoria'], input[name='disponibles'], select[name='sort']")
        .forEach((control) => {
            control.addEventListener("change", () => filterForm.requestSubmit());
        });
})();
