package com.edumoet.service.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.edumoet.entity.Tag;
import com.edumoet.entity.User;
import com.edumoet.repository.*;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Statistics Service - Thống kê hệ thống
 */
@Service
@Transactional(readOnly = true)
public class StatisticsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private TagRepository tagRepository;

    /**
     * Thống kê tổng quan
     */
    public Map<String, Object> getOverviewStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalUsers", userRepository.count());
        stats.put("totalQuestions", questionRepository.count());
        stats.put("totalAnswers", answerRepository.count());
        stats.put("totalComments", 0L); // Comments removed
        stats.put("totalTags", tagRepository.count());
        
        stats.put("activeUsers", userRepository.countByIsActive(true));
        stats.put("bannedUsers", userRepository.countByIsBanned(true));
        
        stats.put("adminCount", userRepository.countByRole("ADMIN"));
        stats.put("managerCount", userRepository.countByRole("MANAGER"));
        stats.put("userCount", userRepository.countByRole("USER"));
        
        stats.put("approvedQuestions", questionRepository.countByIsApproved(true));
        stats.put("pendingQuestions", questionRepository.countByIsApproved(false));
        
        return stats;
    }

    /**
     * Thống kê theo thời gian
     */
    public Map<String, Object> getTimeBasedStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime today = now.toLocalDate().atStartOfDay();
        LocalDateTime thisWeek = now.minusDays(7);
        LocalDateTime thisMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
        
        // Count by created_at date
        long questionsToday = questionRepository.findAll().stream()
                .filter(q -> q.getCreatedAt().isAfter(today))
                .count();
        long questionsThisWeek = questionRepository.findAll().stream()
                .filter(q -> q.getCreatedAt().isAfter(thisWeek))
                .count();
        long questionsThisMonth = questionRepository.findAll().stream()
                .filter(q -> q.getCreatedAt().isAfter(thisMonth))
                .count();
        
        stats.put("questionsToday", questionsToday);
        stats.put("questionsThisWeek", questionsThisWeek);
        stats.put("questionsThisMonth", questionsThisMonth);
        stats.put("newQuestionsThisMonth", questionsThisMonth);
        
        long usersToday = userRepository.findAll().stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(today))
                .count();
        long usersThisWeek = userRepository.findAll().stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(thisWeek))
                .count();
        long usersThisMonth = userRepository.findAll().stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(thisMonth))
                .count();
        
        stats.put("usersToday", usersToday);
        stats.put("usersThisWeek", usersThisWeek);
        stats.put("usersThisMonth", usersThisMonth);
        stats.put("newUsersThisMonth", usersThisMonth);
        
        long answersThisMonth = answerRepository.findAll().stream()
                .filter(a -> a.getCreatedAt().isAfter(thisMonth))
                .count();
        stats.put("newAnswersThisMonth", answersThisMonth);
        stats.put("newCommentsThisMonth", 0L);
        
        return stats;
    }

    /**
     * Thống kê theo tháng (6 tháng gần nhất)
     */
    public Map<String, Object> getMonthlyStatistics() {
        Map<String, Object> result = new HashMap<>();
        
        List<String> months = new ArrayList<>();
        List<Long> questionCounts = new ArrayList<>();
        List<Long> answerCounts = new ArrayList<>();
        List<Long> userCounts = new ArrayList<>();
        
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 5; i >= 0; i--) {
            YearMonth yearMonth = YearMonth.from(now.minusMonths(i));
            months.add("T" + yearMonth.getMonthValue());
            
            LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay();
            LocalDateTime endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59);
            
            long questions = questionRepository.findAll().stream()
                    .filter(q -> q.getCreatedAt().isAfter(startOfMonth) && q.getCreatedAt().isBefore(endOfMonth))
                    .count();
            questionCounts.add(questions);
            
            long answers = answerRepository.findAll().stream()
                    .filter(a -> a.getCreatedAt().isAfter(startOfMonth) && a.getCreatedAt().isBefore(endOfMonth))
                    .count();
            answerCounts.add(answers);
            
            long users = userRepository.findAll().stream()
                    .filter(u -> u.getCreatedAt() != null && 
                                 u.getCreatedAt().isAfter(startOfMonth) && 
                                 u.getCreatedAt().isBefore(endOfMonth))
                    .count();
            userCounts.add(users);
        }
        
        result.put("months", months);
        result.put("questionCounts", questionCounts);
        result.put("answerCounts", answerCounts);
        result.put("userCounts", userCounts);
        
        return result;
    }

    /**
     * Top users by reputation
     */
    public List<User> getTopUsers(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        
        // Fetch top users by reputation
        List<User> users = userRepository.findTopUsersByReputation(pageable);
        
        // Initialize lazy collections within transaction to avoid LazyInitializationException
        // This ensures collections are loaded before transaction ends
        users.forEach(user -> {
            if (user.getQuestions() != null) {
                user.getQuestions().size(); // Initialize questions
            }
            if (user.getAnswers() != null) {
                user.getAnswers().size(); // Initialize answers
            }
        });
        
        return users;
    }

    /**
     * Top tags by question count
     */
    public List<Map<String, Object>> getTopTags(int limit) {
        List<Tag> allTags = tagRepository.findAll();
        
        return allTags.stream()
                .sorted((t1, t2) -> Integer.compare(
                        t2.getQuestions() != null ? t2.getQuestions().size() : 0,
                        t1.getQuestions() != null ? t1.getQuestions().size() : 0))
                .limit(limit)
                .map(tag -> {
                    Map<String, Object> tagMap = new HashMap<>();
                    tagMap.put("name", tag.getName());
                    tagMap.put("count", tag.getQuestions() != null ? tag.getQuestions().size() : 0);
                    tagMap.put("description", tag.getDescription());
                    return tagMap;
                })
                .collect(Collectors.toList());
    }
}

