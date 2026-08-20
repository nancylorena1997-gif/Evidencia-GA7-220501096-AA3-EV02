package com.newpohone.web;

import com.newpohone.database.GenericModuleRepository;
import com.newpohone.module.ModuleDefinition;
import com.newpohone.module.ModuleRegistry;
import com.newpohone.security.PasswordHasher;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ModuleController {

    private final ModuleRegistry moduleRegistry;
    private final GenericModuleRepository repository;

    public ModuleController(ModuleRegistry moduleRegistry, GenericModuleRepository repository) {
        this.moduleRegistry = moduleRegistry;
        this.repository = repository;
    }

    @GetMapping("/modules/{key}")
    public String list(@PathVariable String key,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(required = false) String tipo,
            Model model,
            RedirectAttributes redirectAttributes) {
        ModuleDefinition module = moduleRegistry.get(key);
        if (module == null) {
            redirectAttributes.addFlashAttribute("error", "Módulo no encontrado.");
            return "redirect:/dashboard";
        }

        String fromDate = blankToNull(desde);
        String toDate = blankToNull(hasta);
        String reportType = blankToNull(tipo);
        List<Map<String, Object>> records = repository.findAll(module, q, fromDate, toDate, reportType);
        boolean hasFilters = (q != null && !q.isBlank())
                || fromDate != null
                || toDate != null
                || reportType != null;
        model.addAttribute("module", module);
        model.addAttribute("records", records);
        model.addAttribute("query", q == null ? "" : q);
        model.addAttribute("desde", fromDate == null ? "" : fromDate);
        model.addAttribute("hasta", toDate == null ? "" : toDate);
        model.addAttribute("tipo", reportType == null ? "" : reportType);
        model.addAttribute("hasFilters", hasFilters);
        model.addAttribute("pageTitle", module.getTitle());
        model.addAttribute("breadcrumb", module.getTitle());
        model.addAttribute("activeNav", key);
        return "modules/list";
    }

    @GetMapping("/modules/{key}/new")
    public String createForm(@PathVariable String key,
            @RequestParam(required = false) String next,
            Model model,
            RedirectAttributes redirectAttributes) {
        ModuleDefinition module = moduleRegistry.get(key);
        if (module == null) {
            redirectAttributes.addFlashAttribute("error", "Módulo no encontrado.");
            return "redirect:/dashboard";
        }
        model.addAttribute("module", module);
        model.addAttribute("record", new LinkedHashMap<String, Object>());
        model.addAttribute("creating", true);
        model.addAttribute("next", next);
        model.addAttribute("pageTitle", "Nuevo " + module.getSingular());
        model.addAttribute("breadcrumb", module.getTitle());
        model.addAttribute("activeNav", key);
        return "modules/form";
    }

    @GetMapping("/modules/{key}/{id}/edit")
    public String editForm(@PathVariable String key,
            @PathVariable String id,
            @RequestParam(required = false) String next,
            Model model,
            RedirectAttributes redirectAttributes) {
        ModuleDefinition module = moduleRegistry.get(key);
        if (module == null) {
            redirectAttributes.addFlashAttribute("error", "Módulo no encontrado.");
            return "redirect:/dashboard";
        }
        Object parsedId = parseId(id);
        Map<String, Object> record = repository.findById(module, parsedId);
        if (record == null) {
            redirectAttributes.addFlashAttribute("error", "Registro no encontrado.");
            return "redirect:/modules/" + key;
        }
        model.addAttribute("module", module);
        model.addAttribute("record", record);
        model.addAttribute("creating", false);
        model.addAttribute("recordId", id);
        model.addAttribute("next", next);
        model.addAttribute("pageTitle", "Editar " + module.getSingular());
        model.addAttribute("breadcrumb", module.getTitle());
        model.addAttribute("activeNav", key);
        return "modules/form";
    }

    @PostMapping("/modules/{key}")
    public String create(@PathVariable String key,
            @RequestParam Map<String, String> form,
            RedirectAttributes redirectAttributes) {
        ModuleDefinition module = moduleRegistry.get(key);
        if (module == null) {
            redirectAttributes.addFlashAttribute("error", "Módulo no encontrado.");
            return "redirect:/dashboard";
        }

        try {
            Map<String, Object> values = sanitize(module, form, true);
            repository.create(module, values);
            redirectAttributes.addFlashAttribute("success", "Registro creado correctamente.");
            return redirectAfterChange(key, form.get("next"));
        } catch (DataAccessException | IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error",
                    "No fue posible crear el registro. Revisa los datos e intenta de nuevo.");
            return "redirect:/modules/" + key + "/new" + nextQuery(form.get("next"));
        }
    }

    @PostMapping("/modules/{key}/{id}")
    public String update(@PathVariable String key,
            @PathVariable String id,
            @RequestParam Map<String, String> form,
            RedirectAttributes redirectAttributes) {
        ModuleDefinition module = moduleRegistry.get(key);
        if (module == null) {
            redirectAttributes.addFlashAttribute("error", "Módulo no encontrado.");
            return "redirect:/dashboard";
        }

        try {
            Map<String, Object> values = sanitize(module, form, false);
            Map<String, Object> updated = repository.update(module, parseId(id), values);
            if (updated == null) {
                redirectAttributes.addFlashAttribute("error", "Registro no encontrado.");
            } else {
                redirectAttributes.addFlashAttribute("success", "Registro actualizado correctamente.");
            }
            return redirectAfterChange(key, form.get("next"));
        } catch (DataAccessException | IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error",
                    "No fue posible actualizar el registro. Revisa los datos e intenta de nuevo.");
            return "redirect:/modules/" + key + "/" + id + "/edit" + nextQuery(form.get("next"));
        }
    }

    @PostMapping("/modules/{key}/{id}/delete")
    public String delete(@PathVariable String key,
            @PathVariable String id,
            @RequestParam(required = false) String next,
            RedirectAttributes redirectAttributes) {
        ModuleDefinition module = moduleRegistry.get(key);
        if (module == null) {
            redirectAttributes.addFlashAttribute("error", "Módulo no encontrado.");
            return "redirect:/dashboard";
        }
        try {
            boolean deleted = repository.delete(module, parseId(id));
            if (deleted) {
                redirectAttributes.addFlashAttribute("success", "Registro eliminado.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Registro no encontrado.");
            }
        } catch (DataAccessException exception) {
            redirectAttributes.addFlashAttribute("error",
                    "No fue posible eliminar el registro. Puede tener dependencias asociadas.");
        }
        return redirectAfterChange(key, next);
    }

    private String redirectAfterChange(String key, String next) {
        if ("inventario".equals(next) && "productos".equals(key)) {
            return "redirect:/inventario";
        }
        return "redirect:/modules/" + key;
    }

    private String nextQuery(String next) {
        if ("inventario".equals(next)) {
            return "?next=inventario";
        }
        return "";
    }

    private Map<String, Object> sanitize(ModuleDefinition module, Map<String, String> form, boolean creating) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (ModuleDefinition.FieldDefinition field : module.getFields()) {
            if (field.isReadOnly() && creating && module.isAutoId()) {
                continue;
            }
            if (!form.containsKey(field.getName())) {
                continue;
            }
            String raw = form.get(field.getName());
            if (raw == null) {
                continue;
            }
            raw = raw.trim();
            if ("contrasena".equals(field.getName())) {
                if (raw.isEmpty()) {
                    continue;
                }
                values.put(field.getName(), PasswordHasher.hash(raw));
                continue;
            }
            if (raw.isEmpty()) {
                if (field.isRequired()) {
                    throw new IllegalArgumentException("Campo requerido: " + field.getLabel());
                }
                values.put(field.getName(), "");
                continue;
            }
            values.put(field.getName(), cast(field.getType(), raw));
        }
        return values;
    }

    private Object cast(String type, String raw) {
        return switch (type) {
            case "number" -> Long.parseLong(raw.contains(".") ? raw.substring(0, raw.indexOf('.')) : raw);
            case "decimal" -> Double.parseDouble(raw.replace(',', '.'));
            default -> raw;
        };
    }

    private Object parseId(String id) {
        try {
            if (id.contains(".")) {
                return Double.parseDouble(id);
            }
            return Long.parseLong(id);
        } catch (NumberFormatException exception) {
            return id;
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
