package com.edumoet.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.edumoet.entity.ActivityLog;
import com.edumoet.entity.Question;
import com.edumoet.entity.Report;
import com.edumoet.service.common.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin Dashboard Controller - Trang chính và tổng quan
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    @Autowired
    private AdminService adminService;
    
    @Autowired
    private QuestionService questionService;
    
    @Autowired
    private AnswerService answerService;
    
    @Autowired
    private ActivityLogService activityLogService;
    
    @Autowired
    private TagService tagService;
    
    @Autowired
    private ReportService reportService;

    /**
     * Trang dashboard admin với dữ liệu thực từ database
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "Trang Quản Trị - EDUMOET");
        
        // User statistics
        long totalUsers = adminService.getAllUsers().size();
        long activeUsers = adminService.countActiveUsers();
        long bannedUsers = adminService.countBannedUsers();
        long newUsersToday = adminService.countUsersByDateRange(
            LocalDateTime.now().toLocalDate().atStartOfDay(),
            LocalDateTime.now()
        );
        
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("activeUsers", activeUsers);
        model.addAttribute("bannedUsers", bannedUsers);
        model.addAttribute("newUsersToday", newUsersToday);
        
        // Content statistics
        long totalQuestions = questionService.countAllQuestions();
        long pendingQuestions = questionService.countPending();
        long totalAnswers = answerService.countAll();
        long totalTags = tagService.count();
        
        // Count items created today
        LocalDateTime startOfToday = LocalDateTime.now().toLocalDate().atStartOfDay();
        long newQuestionsToday = questionService.countByDateRange(startOfToday, LocalDateTime.now());
        long newAnswersToday = answerService.countByDateRange(startOfToday, LocalDateTime.now());
        
        model.addAttribute("totalQuestions", totalQuestions);
        model.addAttribute("newQuestionsToday", newQuestionsToday);
        model.addAttribute("totalAnswers", totalAnswers);
        model.addAttribute("newAnswersToday", newAnswersToday);
        model.addAttribute("totalComments", totalAnswers); // Using answers as comments
        model.addAttribute("totalTags", totalTags);
        
        // Moderation statistics
        long pendingReports = reportService.countPendingReports();
        model.addAttribute("pendingReports", pendingReports);
        model.addAttribute("pendingQuestions", pendingQuestions);
        model.addAttribute("unreadNotifications", 0L); // Can implement if needed
        
        // Recent activities (last 10)
        Pageable activityPageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        List<ActivityLog> recentActivities = activityLogService.getAllLogs(activityPageable).getContent();
        model.addAttribute("recentActivities", recentActivities);
        
        // Recent reports (last 5 pending)
        Pageable reportPageable = PageRequest.of(0, 5, Sort.by("createdAt").descending());
        List<Report> recentReports = reportService.getPendingReports(reportPageable).getContent();
        model.addAttribute("recentReports", recentReports);
        
        // Pending questions (last 5)
        Pageable questionPageable = PageRequest.of(0, 5, Sort.by("createdAt").descending());
        List<Question> pendingQuestionsList = questionService.getAllPendingQuestions(questionPageable).getContent();
        model.addAttribute("pendingQuestionsList", pendingQuestionsList);
        
        return "admin/dashboard";
    }

    /**
     * Trang tổng hợp tất cả tính năng admin
     */
    @GetMapping("/all-features")
    public String allFeatures(Model model) {
        model.addAttribute("pageTitle", "Tất Cả Tính Năng - Quản Trị");
        
        // Statistics
        model.addAttribute("totalUsers", adminService.getAllUsers().size());
        model.addAttribute("totalQuestions", questionService.countAllQuestions());
        model.addAttribute("totalAnswers", answerService.countAll());
        model.addAttribute("pendingQuestions", questionService.countPending());
        model.addAttribute("pendingComments", 0L); // TODO: implement if needed
        
        // Recent activity logs (last 10)
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        List<ActivityLog> recentLogs = activityLogService.getAllLogs(pageable).getContent();
        model.addAttribute("recentLogs", recentLogs);
        
        return "admin/all-features";
    }
}
