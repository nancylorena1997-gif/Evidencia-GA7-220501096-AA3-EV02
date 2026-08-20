package com.newpohone.web;

import com.newpohone.database.DashboardRepository;
import com.newpohone.database.DatabaseBootstrap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DashboardRepository dashboardRepository;

    public DashboardController(DashboardRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Map<String, Object> summary = dashboardRepository.getSummary();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> salesByCategory =
                (List<Map<String, Object>>) summary.get("salesByCategory");
        double maxSales = salesByCategory.stream()
                .mapToDouble(row -> {
                    Object total = row.get("total");
                    return total == null ? 0 : ((Number) total).doubleValue();
                })
                .max()
                .orElse(1);
        if (maxSales <= 0) {
            maxSales = 1;
        }
        for (Map<String, Object> row : salesByCategory) {
            Object total = row.get("total");
            double value = total == null ? 0 : ((Number) total).doubleValue();
            row.put("percent", Math.max(4, Math.round((value / maxSales) * 100)));
        }

        model.addAttribute("pageTitle", "Centro de operaciones");
        model.addAttribute("breadcrumb", "Resumen");
        model.addAttribute("activeNav", "dashboard");
        model.addAttribute("summary", summary);
        model.addAttribute("databasePath", DatabaseBootstrap.getLocation());
        return "dashboard/index";
    }
}
