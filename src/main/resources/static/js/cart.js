(() => {
    const cartDrawer = document.getElementById("cartDrawer");
    const openCartBtn = document.getElementById("openCart");
    const closeCartBtn = document.getElementById("closeCart");
    const cartBackdrop = document.getElementById("cartBackdrop");
    const cartItems = document.getElementById("cartItems");
    const cartPageList = document.getElementById("cartPageList");
    const cartPageEmpty = document.getElementById("cartPageEmpty");
    const cartPageContent = document.getElementById("cartPageContent");
    const cartBadge = document.getElementById("cartBadge");
    const cartTotal = document.getElementById("cartTotal");
    const cartSubtotal = document.getElementById("cartSubtotal");
    const cartCountLabel = document.getElementById("cartCountLabel");
    const cartLinesLabel = document.getElementById("cartLinesLabel");
    const cartStatus = document.getElementById("cartStatus");
    const editCartLink = document.getElementById("editCartLink");
    const clearCartBtn = document.getElementById("clearCart");

    const lockBody = () => {
        const locked = document.querySelector(".filters.open, .cart-drawer.open");
        document.body.style.overflow = locked ? "hidden" : "";
    };

    const closeFilters = () => {
        document.getElementById("filtersPanel")?.classList.remove("open");
        const backdrop = document.getElementById("filtersBackdrop");
        if (backdrop) {
            backdrop.hidden = true;
        }
        lockBody();
    };

    const openCart = () => {
        if (!cartDrawer) {
            return;
        }
        closeFilters();
        cartDrawer.classList.add("open");
        if (cartBackdrop) {
            cartBackdrop.hidden = false;
        }
        lockBody();
    };

    const closeCart = () => {
        cartDrawer?.classList.remove("open");
        if (cartBackdrop) {
            cartBackdrop.hidden = true;
        }
        lockBody();
    };

    openCartBtn?.addEventListener("click", openCart);
    closeCartBtn?.addEventListener("click", closeCart);
    cartBackdrop?.addEventListener("click", closeCart);
    window.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            closeCart();
        }
    });

    const escapeHtml = (value) => String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");

    const qtyControls = (item) => `
        <div class="qty">
            <button type="button" data-action="dec" aria-label="Quitar una unidad">−</button>
            <input type="number" min="1" max="${item.stock}" value="${item.cantidad}"
                   aria-label="Cantidad de ${escapeHtml(item.nombre)}">
            <button type="button" data-action="inc" aria-label="Agregar una unidad"
                    ${item.cantidad >= item.stock ? "disabled" : ""}>+</button>
        </div>`;

    const showStatus = (cart) => {
        if (!cartStatus) {
            return;
        }
        if (!cart?.message) {
            cartStatus.hidden = true;
            cartStatus.textContent = "";
            return;
        }
        cartStatus.hidden = false;
        cartStatus.textContent = cart.message;
        cartStatus.classList.toggle("error", Boolean(cart.error));
    };

    const renderDrawer = (cart) => {
        if (!cartItems) {
            return;
        }
        if (editCartLink) {
            editCartLink.hidden = Boolean(cart.empty);
        }
        if (cart.empty || !cart.items?.length) {
            cartItems.innerHTML = `
                <div class="cart-empty">
                    <i class="bi bi-bag"></i>
                    <h3>Tu carrito está vacío</h3>
                    <p>Elige un producto del catálogo para agregarlo aquí.</p>
                </div>`;
            return;
        }
        cartItems.innerHTML = cart.items.map((item) => `
            <article class="cart-line" data-id="${item.id}">
                <img src="${escapeHtml(item.image)}" alt="${escapeHtml(item.nombre)}">
                <div class="cart-line-body">
                    <div>
                        <h3>${escapeHtml(item.nombre)}</h3>
                        <div class="unit">${escapeHtml(item.precioLabel)} c/u</div>
                    </div>
                    <div class="cart-line-actions">
                        ${qtyControls(item)}
                        <strong class="cart-line-total">${escapeHtml(item.subtotalLabel)}</strong>
                    </div>
                    <button class="remove-item" type="button" data-action="remove">Quitar</button>
                </div>
            </article>`).join("");
    };

    const renderPage = (cart) => {
        if (!cartPageList) {
            return;
        }
        if (cartPageEmpty) {
            cartPageEmpty.hidden = !cart.empty;
        }
        if (cartPageContent) {
            cartPageContent.hidden = Boolean(cart.empty);
        }
        if (cartLinesLabel) {
            cartLinesLabel.textContent = String(cart.lines || 0);
        }
        if (cartSubtotal) {
            cartSubtotal.textContent = cart.totalLabel || "$0";
        }
        if (cart.empty || !cart.items?.length) {
            cartPageList.innerHTML = "";
            return;
        }
        cartPageList.innerHTML = cart.items.map((item) => `
            <article class="cart-page-line cart-line" data-id="${item.id}">
                <div class="cart-page-info">
                    <img src="${escapeHtml(item.image)}" alt="${escapeHtml(item.nombre)}">
                    <span class="category">${escapeHtml(item.categoria)}</span>
                    <h3>${escapeHtml(item.nombre)}</h3>
                    <small>Hasta ${item.stock} unidades disponibles</small>
                    <button class="remove-item" type="button" data-action="remove">
                        <i class="bi bi-trash3"></i> Quitar
                    </button>
                </div>
                <div class="cart-page-price">${escapeHtml(item.precioLabel)}</div>
                ${qtyControls(item)}
                <strong class="cart-line-total">${escapeHtml(item.subtotalLabel)}</strong>
            </article>`).join("");
    };

    const renderCart = (cart) => {
        if (!cart) {
            return;
        }
        const count = Number(cart.count || 0);
        if (cartBadge) {
            cartBadge.textContent = String(count);
            cartBadge.hidden = count === 0;
        }
        if (cartCountLabel) {
            cartCountLabel.textContent = String(count);
        }
        if (cartTotal) {
            cartTotal.textContent = cart.totalLabel || "$0";
        }
        showStatus(cart);
        renderDrawer(cart);
        renderPage(cart);
    };

    const request = async (url, body) => {
        const options = { method: body ? "POST" : "GET" };
        if (body) {
            options.headers = { "Content-Type": "application/x-www-form-urlencoded" };
            options.body = new URLSearchParams(body);
        }
        const response = await fetch(url, options);
        if (!response.ok) {
            throw new Error("No se pudo actualizar el carrito.");
        }
        return response.json();
    };

    const refresh = async () => {
        try {
            renderCart(await request("/catalog/cart"));
        } catch (error) {
            showStatus({ message: error.message, error: true });
        }
    };

    const updateQuantity = async (productId, cantidad) => {
        renderCart(await request("/catalog/cart/update", { productId, cantidad }));
    };

    const handleLineEvent = async (event) => {
        const action = event.target.closest("[data-action]");
        const line = event.target.closest(".cart-line");
        if (!action || !line) {
            return;
        }
        const input = line.querySelector("input");
        const productId = line.dataset.id;
        const current = Number(input?.value || 1);
        try {
            if (action.dataset.action === "inc") {
                await updateQuantity(productId, current + 1);
            } else if (action.dataset.action === "dec") {
                await updateQuantity(productId, current - 1);
            } else if (action.dataset.action === "remove") {
                renderCart(await request("/catalog/cart/remove", { productId }));
            }
        } catch (error) {
            showStatus({ message: error.message, error: true });
        }
    };

    const handleQuantityChange = async (event) => {
        const input = event.target.closest("input[type='number']");
        const line = event.target.closest(".cart-line");
        if (!input || !line) {
            return;
        }
        try {
            await updateQuantity(line.dataset.id, Number(input.value || 0));
        } catch (error) {
            showStatus({ message: error.message, error: true });
        }
    };

    document.querySelectorAll(".add-cart").forEach((button) => {
        button.addEventListener("click", async () => {
            try {
                renderCart(await request("/catalog/cart/add", { productId: button.dataset.id }));
                openCart();
            } catch (error) {
                showStatus({ message: error.message, error: true });
                openCart();
            }
        });
    });

    cartItems?.addEventListener("click", handleLineEvent);
    cartItems?.addEventListener("change", handleQuantityChange);
    cartPageList?.addEventListener("click", handleLineEvent);
    cartPageList?.addEventListener("change", handleQuantityChange);

    clearCartBtn?.addEventListener("click", async () => {
        if (!window.confirm("¿Quieres vaciar todo el carrito?")) {
            return;
        }
        try {
            renderCart(await request("/catalog/cart/clear", {}));
        } catch (error) {
            showStatus({ message: error.message, error: true });
        }
    });

    refresh();
})();
