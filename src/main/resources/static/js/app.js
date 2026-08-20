(() => {
    const sidebar = document.getElementById("sidebar");
    const menuButton = document.getElementById("menuButton");
    const backdrop = document.getElementById("backdrop");

    const close = () => {
        sidebar?.classList.remove("open");
        backdrop?.classList.remove("show");
    };

    menuButton?.addEventListener("click", () => {
        sidebar?.classList.toggle("open");
        backdrop?.classList.toggle("show");
    });

    backdrop?.addEventListener("click", close);
    window.addEventListener("resize", () => {
        if (window.innerWidth > 980) {
            close();
        }
    });

    const bell = document.getElementById("inventoryBell");
    const menu = document.getElementById("inventoryMenu");
    bell?.addEventListener("click", (event) => {
        event.stopPropagation();
        if (!menu) {
            return;
        }
        menu.hidden = !menu.hidden;
        bell.setAttribute("aria-expanded", String(!menu.hidden));
    });
    document.addEventListener("click", () => {
        if (menu && !menu.hidden) {
            menu.hidden = true;
            bell?.setAttribute("aria-expanded", "false");
        }
    });
    menu?.addEventListener("click", (event) => event.stopPropagation());
    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && menu && !menu.hidden) {
            menu.hidden = true;
            bell?.setAttribute("aria-expanded", "false");
        }
    });
})();
