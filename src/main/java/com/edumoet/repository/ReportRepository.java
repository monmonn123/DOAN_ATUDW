package com.edumoet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.edumoet.entity.Report;
import com.edumoet.entity.User;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    
    Page<Report> findByReporter(User reporter, Pageable pageable);
    
    Page<Report> findByStatus(String status, Pageable pageable);
    
    long countByStatus(String status);
    
    // For user deletion
    void deleteByReporter(User reporter);
}
