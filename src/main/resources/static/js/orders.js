(() => {
    const modal = document.getElementById("orderStatusModal");
    const backdrop = document.getElementById("orderModalBackdrop");
    const form = document.getElementById("orderStatusForm");
    const title = document.getElementById("modalOrderTitle");
    const meta = document.getElementById("modalOrderMeta");
    const currentBadge = document.getElementById("modalCurrentStatus");
    const select = document.getElementById("modalEstado");
    const confirm = document.getElementById("modalConfirm");
    const submit = document.getElementById("modalSubmit");
    const closeButtons = document.querySelectorAll("[data-close-order-modal]");

    if (!modal || !form) {
        return;
    }

    const syncSubmit = () => {
        const changed = select.value && select.value !== select.dataset.current;
        submit.disabled = !confirm.checked || !changed;
    };

    const open = (button) => {
        const id = button.dataset.orderId;
        const cliente = button.dataset.cliente || "Cliente";
        const guia = button.dataset.guia || "Sin guía";
        const statusKey = button.dataset.status || "";
        const statusLabel = button.dataset.statusLabel || "";

        title.textContent = "Pedido #" + id;
        meta.textContent = cliente + " · " + guia;
        currentBadge.textContent = statusLabel;
        currentBadge.className = "badge " + (button.dataset.statusClass || "");

        select.dataset.current = statusKey;
        select.value = "";
        confirm.checked = false;
        form.action = "/pedidos/" + id + "/estado";
        syncSubmit();

        modal.hidden = false;
        backdrop.hidden = false;
        document.body.classList.add("order-modal-open");
        select.focus();
    };

    const close = () => {
        modal.hidden = true;
        backdrop.hidden = true;
        document.body.classList.remove("order-modal-open");
    };

    document.querySelectorAll(".status-action").forEach((button) => {
        button.addEventListener("click", () => open(button));
    });

    closeButtons.forEach((button) => button.addEventListener("click", close));
    backdrop?.addEventListener("click", close);

    confirm?.addEventListener("change", syncSubmit);
    select?.addEventListener("change", syncSubmit);

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && !modal.hidden) {
            close();
        }
    });
})();
