package com.edumoet.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private CustomAuthenticationFailureHandler authenticationFailureHandler;

    @Autowired
    private CustomAuthenticationSuccessHandler authenticationSuccessHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        
        // Matcher riêng cho H2 Console
        AntPathRequestMatcher h2Matcher = new AntPathRequestMatcher("/h2-console/**");

        // ========== 1. CSRF SECURITY ==========
        http.csrf(csrf -> csrf
            // Sử dụng Cookie để frontend (Angular/React/JS) có thể đọc token
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            // Ngoại trừ H2 console vì nó không tương thích CSRF mặc định
            .ignoringRequestMatchers(h2Matcher)
        );

        // ========== 2. CLICKJACKING FIX (QUAN TRỌNG) ==========
// Chặn hoàn toàn việc nhúng site vào iframe để chống Clickjacking
        http.headers(headers -> headers
            .frameOptions(frame -> frame.deny())
        );

        // ========== 3. AUTHORIZATION ==========
        http.authorizeHttpRequests(auth -> auth
            // H2 Console: Chỉ cho phép ADMIN (Thay vì permitAll như cũ)
            .requestMatchers(h2Matcher).hasRole("ADMIN")

            // Admin / Manager Routes
            .requestMatchers(new AntPathRequestMatcher("/admin/**")).hasRole("ADMIN")
            .requestMatchers(new AntPathRequestMatcher("/manager/**")).hasAnyRole("ADMIN", "MANAGER")
            .requestMatchers(new AntPathRequestMatcher("/users/*/edit")).hasRole("ADMIN")
            .requestMatchers(new AntPathRequestMatcher("/users/*/delete")).hasRole("ADMIN")

            // Authenticated Actions (Cần đăng nhập)
            .requestMatchers(new AntPathRequestMatcher("/questions/ask")).authenticated()
            .requestMatchers(new AntPathRequestMatcher("/questions/*/edit")).authenticated()
            .requestMatchers(new AntPathRequestMatcher("/questions/*/delete")).authenticated()
            .requestMatchers(new AntPathRequestMatcher("/questions/*/upvote")).authenticated()
            .requestMatchers(new AntPathRequestMatcher("/questions/*/downvote")).authenticated()
            .requestMatchers(new AntPathRequestMatcher("/answers/**")).authenticated()
            .requestMatchers(new AntPathRequestMatcher("/comments/**")).authenticated()
            .requestMatchers(new AntPathRequestMatcher("/profile")).authenticated()
            .requestMatchers(new AntPathRequestMatcher("/profile/edit")).authenticated()
            .requestMatchers(new AntPathRequestMatcher("/profile/update")).authenticated()
            .requestMatchers(new AntPathRequestMatcher("/profile/change-password")).authenticated()
            .requestMatchers(new AntPathRequestMatcher("/messages/**")).authenticated()
            .requestMatchers(new AntPathRequestMatcher("/notifications/**")).authenticated()
            .requestMatchers(new AntPathRequestMatcher("/follow/**")).authenticated()
            .requestMatchers(new AntPathRequestMatcher("/reports/**")).authenticated()

            // Public Routes (Ai cũng xem được)
            .requestMatchers(new AntPathRequestMatcher("/")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/home")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/questions/**")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/tags/**")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/users/**")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/login")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/register")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/api/auth/**")).permitAll()
.requestMatchers(new AntPathRequestMatcher("/password/**")).permitAll()

            // Static Resources & WebSockets
            .requestMatchers(new AntPathRequestMatcher("/css/**")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/js/**")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/images/**")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/static/**")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/uploads/**")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/ws/**")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/topic/**")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/queue/**")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/error/**")).permitAll()

            .anyRequest().authenticated()
        );

        // ========== 4. EXCEPTION HANDLING ==========
        http.exceptionHandling(exception -> exception.accessDeniedPage("/error/403"));

        // ========== 5. LOGIN ==========
        http.formLogin(form -> form
            .loginPage("/login")
            .loginProcessingUrl("/login")
            .usernameParameter("username")
            .passwordParameter("password")
            .successHandler(authenticationSuccessHandler)
            .failureHandler(authenticationFailureHandler)
            .permitAll()
        );

        // ========== 6. LOGOUT ==========
        http.logout(logout -> logout
            .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
            .logoutSuccessUrl("/login?logout")
            .deleteCookies("JSESSIONID", "jwt")
            .invalidateHttpSession(true)
            .clearAuthentication(true)
            .permitAll()
        );

        // ========== 7. SESSION SECURITY (FIX HIJACKING) ==========
        http.sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            // Fix Session Fixation: đổi session id sau khi login
            .sessionFixation(fixation -> fixation.changeSessionId())
            // Giới hạn 1 session trên mỗi user để tránh dùng chung tài khoản
            .maximumSessions(1)
            .maxSessionsPreventsLogin(false)
        );

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}