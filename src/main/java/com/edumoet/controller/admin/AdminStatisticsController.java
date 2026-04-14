package com.edumoet.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.edumoet.service.common.StatisticsService;

/**
 * Admin Statistics Controller - Thống kê báo cáo
 */
@Controller
@RequestMapping("/admin/statistics")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    /**
     * Dashboard thống kê
     */
    @GetMapping
    public String statisticsDashboard(Model model) {
        
        // Overview statistics
        var overview = statisticsService.getOverviewStatistics();
        model.addAllAttributes(overview);
        
        // Time-based statistics
        var timeStats = statisticsService.getTimeBasedStatistics();
        model.addAllAttributes(timeStats);
        
        // Monthly statistics for charts
        var monthlyStats = statisticsService.getMonthlyStatistics();
        model.addAllAttributes(monthlyStats);
        
        // Top users
        var topUsers = statisticsService.getTopUsers(10);
        model.addAttribute("topUsers", topUsers);
        
        // Top tags
        var topTags = statisticsService.getTopTags(10);
        model.addAttribute("topTags", topTags);
        
        model.addAttribute("pageTitle", "Statistics & Reports - Admin");
        
        return "admin/statistics/dashboard";
    }
}

