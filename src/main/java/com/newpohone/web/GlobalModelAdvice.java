package com.newpohone.web;

import com.newpohone.module.ModuleDefinition;
import com.newpohone.module.ModuleRegistry;
import com.newpohone.module.NavItem;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {

    private final ModuleRegistry moduleRegistry;

    public GlobalModelAdvice(ModuleRegistry moduleRegistry) {
        this.moduleRegistry = moduleRegistry;
    }

    @ModelAttribute("navItems")
    public List<NavItem> navItems() {
        Map<String, List<ModuleDefinition>> grouped = moduleRegistry.all().stream()
                .collect(Collectors.groupingBy(ModuleDefinition::getGroup, LinkedHashMap::new, Collectors.toList()));
        List<NavItem> items = new ArrayList<>();
        grouped.forEach((name, modules) -> {
            items.add(NavItem.section(name));
            modules.forEach(module -> items.add(NavItem.link(module)));
        });
        return items;
    }

    @ModelAttribute("allModules")
    public Collection<ModuleDefinition> allModules() {
        return moduleRegistry.all();
    }
}
