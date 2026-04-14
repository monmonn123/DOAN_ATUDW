package com.edumoet.controller.manager;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.edumoet.service.common.*;

@Controller("managerHomeController")
@RequestMapping("/manager")
public class HomeController {

    private final QuestionService questionService;
    private final ReportService reportService;
    private final NotificationService notificationService;
    private final ActivityLogService logService;
    private final UserService userService;

    public HomeController(QuestionService questionService,
                          ReportService reportService,
                          NotificationService notificationService,
                          ActivityLogService logService,
                          UserService userService) {
        this.questionService = questionService;
        this.reportService = reportService;
        this.notificationService = notificationService;
        this.logService = logService;
        this.userService = userService;
    }

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("pendingPosts", questionService.countPending());
        model.addAttribute("openReports", reportService.countOpenReports());
        model.addAttribute("unreadNotifications", notificationService.countUnread()); // Total unread
        model.addAttribute("activeMembers", userService.countActiveMembers());
        model.addAttribute("recentActivities", logService.findRecent());
        return "manager/dashboard";
    }
}
