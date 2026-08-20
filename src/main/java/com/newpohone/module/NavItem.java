package com.newpohone.module;

public record NavItem(boolean section, String label, String key, String icon) {

    public static NavItem section(String name) {
        return new NavItem(true, name, null, null);
    }

    public static NavItem link(ModuleDefinition module) {
        return new NavItem(false, module.getTitle(), module.getKey(), module.getIcon());
    }
}
