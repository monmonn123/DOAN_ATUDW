package com.edumoet.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.edumoet.entity.Question;
import com.edumoet.entity.Tag;
import com.edumoet.entity.User;
import com.edumoet.repository.QuestionRepository;
import com.edumoet.repository.TagRepository;
import com.edumoet.repository.UserRepository;

import java.util.HashSet;
import java.util.Set;

/**
 * Data Initializer - Khởi tạo dữ liệu mẫu khi chạy lần đầu
 * Comment @Component nếu không muốn tạo dữ liệu mẫu
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Chỉ khởi tạo dữ liệu mẫu nếu database rỗng
        if (userRepository.count() == 0) {
            initializeData();
        }
    }

    private void initializeData() {
        // Tạo users mẫu
        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@edumoet.vn");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole("ADMIN");
        admin.setReputation(1000);
        admin.setAbout("System Administrator");
        admin.setBio("Managing the EDUMOET platform");
        admin.setLocation("Vietnam");
        admin.setWebsite("https://edumoet.vn");
        admin.setGithubUrl("https://github.com/admin");
        admin.setLinkedinUrl("https://linkedin.com/in/admin");
        userRepository.save(admin);

        User manager = new User();
        manager.setUsername("manager");
        manager.setEmail("manager@edumoet.vn");
        manager.setPassword(passwordEncoder.encode("manager123"));
        manager.setRole("MANAGER");
        manager.setReputation(500);
        manager.setAbout("Content Manager");
        manager.setBio("Managing questions, answers and reports");
        manager.setLocation("Vietnam");
        manager.setWebsite("https://edumoet.vn");
        manager.setGithubUrl("https://github.com/manager");
        manager.setLinkedinUrl("https://linkedin.com/in/manager");
        userRepository.save(manager);

        User john = new User();
        john.setUsername("john_doe");
        john.setEmail("john@example.com");
        john.setPassword(passwordEncoder.encode("password123"));
        john.setRole("USER");
        john.setReputation(150);
        john.setAbout("Passionate Java developer with 5 years of experience");
        john.setBio("Senior Java Developer | Spring Boot Expert");
        john.setLocation("Ho Chi Minh City, Vietnam");
        john.setWebsite("https://johndoe.com");
        john.setGithubUrl("https://github.com/johndoe");
        john.setLinkedinUrl("https://linkedin.com/in/johndoe");
        userRepository.save(john);

        User jane = new User();
        jane.setUsername("jane_smith");
        jane.setEmail("jane@example.com");
        jane.setPassword(passwordEncoder.encode("password123"));
        jane.setRole("USER");
        jane.setReputation(250);
        jane.setAbout("Full-stack developer, love Spring Boot and React");
        jane.setBio("Full-stack developer specializing in Java and JavaScript");
        jane.setLocation("Hanoi, Vietnam");
        jane.setWebsite("https://jane.dev");
        jane.setGithubUrl("https://github.com/janedoe");
        jane.setLinkedinUrl("https://linkedin.com/in/janedoe");
        userRepository.save(jane);

        // Tạo tags mẫu
        Tag javaTag = createTag("java", "Programming language for building enterprise applications");
        Tag springTag = createTag("spring-boot", "Framework for building Java applications");
        Tag pythonTag = createTag("python", "High-level programming language");
        Tag javascriptTag = createTag("javascript", "Programming language for web development");
        Tag reactTag = createTag("react", "JavaScript library for building user interfaces");
        Tag sqlTag = createTag("sql", "Language for managing relational databases");

        // Tạo questions mẫu
        Question q1 = new Question();
        q1.setTitle("How to configure Spring Boot with SQL Server?");
        q1.setBody("I'm trying to connect my Spring Boot application to SQL Server. " +
                   "I've added the dependency but getting connection errors. " +
                   "What are the correct configuration steps?");
        q1.setAuthor(john);
        q1.setViews(125);
        q1.setVotes(5);
        q1.setAnswerCount(0);
        q1.setIsPinned(false);
        q1.setIsLocked(false);
        q1.setIsApproved(true);
        Set<Tag> q1Tags = new HashSet<>();
        q1Tags.add(springTag);
        q1Tags.add(sqlTag);
        q1.setTags(q1Tags);
        questionRepository.save(q1);

        Question q2 = new Question();
        q2.setTitle("What is the difference between @Component and @Service in Spring?");
        q2.setBody("I've seen both @Component and @Service annotations used in Spring applications. " +
                   "What's the difference between them? When should I use each one?");
        q2.setAuthor(jane);
        q2.setViews(250);
        q2.setVotes(12);
        q2.setAnswerCount(0);
        q2.setIsPinned(true);  // Pin this one
        q2.setIsLocked(false);
        q2.setIsApproved(true);
        Set<Tag> q2Tags = new HashSet<>();
        q2Tags.add(springTag);
        q2Tags.add(javaTag);
        q2.setTags(q2Tags);
        questionRepository.save(q2);

        Question q3 = new Question();
        q3.setTitle("How to implement JWT authentication in Spring Boot?");
        q3.setBody("I need to add JWT authentication to my REST API. " +
                   "What's the best approach? Should I use Spring Security? " +
                   "Any good tutorials or examples?");
        q3.setAuthor(john);
        q3.setViews(340);
        q3.setVotes(18);
        q3.setAnswerCount(0);
        q3.setIsPinned(false);
        q3.setIsLocked(false);
        q3.setIsApproved(true);
        Set<Tag> q3Tags = new HashSet<>();
        q3Tags.add(springTag);
        q3Tags.add(javaTag);
        q3.setTags(q3Tags);
        questionRepository.save(q3);

        Question q4 = new Question();
        q4.setTitle("Python vs Java for backend development?");
        q4.setBody("I'm starting a new project and can't decide between Python and Java for the backend. " +
                   "What are the pros and cons of each? Which one would you recommend for a REST API?");
        q4.setAuthor(jane);
        q4.setViews(180);
        q4.setVotes(8);
        q4.setAnswerCount(0);
        q4.setIsPinned(false);
        q4.setIsLocked(false);
        q4.setIsApproved(true);
        Set<Tag> q4Tags = new HashSet<>();
        q4Tags.add(pythonTag);
        q4Tags.add(javaTag);
        q4.setTags(q4Tags);
        questionRepository.save(q4);

        Question q5 = new Question();
        q5.setTitle("How to optimize React component re-renders?");
        q5.setBody("My React application is slow because components are re-rendering too often. " +
                   "What are the best practices to optimize React performance? " +
                   "Should I use React.memo or useMemo?");
        q5.setAuthor(john);
        q5.setViews(290);
        q5.setVotes(15);
        q5.setAnswerCount(0);
        q5.setIsPinned(false);
        q5.setIsLocked(true);  // Lock this one
        q5.setIsApproved(true);
        Set<Tag> q5Tags = new HashSet<>();
        q5Tags.add(reactTag);
        q5Tags.add(javascriptTag);
        q5.setTags(q5Tags);
        questionRepository.save(q5);

        // Update tag question counts
        updateTagCounts();

        System.out.println("✅ Sample data initialized successfully!");
        System.out.println("📝 Login credentials:");
        System.out.println("   👑 Admin:   username=admin,      password=admin123");
        System.out.println("   👔 Manager: username=manager,    password=manager123");
        System.out.println("   👤 User:    username=john_doe,   password=password123");
        System.out.println("   👤 User:    username=jane_smith, password=password123");
    }

    private Tag createTag(String name, String description) {
        Tag tag = new Tag();
        tag.setName(name);
        tag.setDescription(description);
        tag.setQuestionCount(0);
        return tagRepository.save(tag);
    }

    private void updateTagCounts() {
        for (Tag tag : tagRepository.findAll()) {
            long count = questionRepository.count();
            tag.setQuestionCount((int) count);
            tagRepository.save(tag);
        }
    }
}
