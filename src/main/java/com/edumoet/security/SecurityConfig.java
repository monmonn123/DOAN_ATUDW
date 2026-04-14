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
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Admin and Manager areas
                .requestMatchers(new AntPathRequestMatcher("/admin/**")).hasRole("ADMIN")
                .requestMatchers(new AntPathRequestMatcher("/manager/**")).hasAnyRole("ADMIN", "MANAGER")

                // Authenticated actions
                .requestMatchers(new AntPathRequestMatcher("/questions/ask")).authenticated()
                .requestMatchers(new AntPathRequestMatcher("/questions/*/edit")).authenticated()
                .requestMatchers(new AntPathRequestMatcher("/questions/*/delete")).authenticated()
                .requestMatchers(new AntPathRequestMatcher("/questions/*/images/*/delete")).authenticated()
                .requestMatchers(new AntPathRequestMatcher("/questions/*/upvote")).authenticated()
                .requestMatchers(new AntPathRequestMatcher("/questions/*/downvote")).authenticated()

                .requestMatchers(new AntPathRequestMatcher("/answers/create")).authenticated()
                .requestMatchers(new AntPathRequestMatcher("/answers/*/edit")).authenticated()
                .requestMatchers(new AntPathRequestMatcher("/answers/*/delete")).authenticated()
                .requestMatchers(new AntPathRequestMatcher("/answers/*/upvote")).authenticated()
                .requestMatchers(new AntPathRequestMatcher("/answers/*/downvote")).authenticated()
                .requestMatchers(new AntPathRequestMatcher("/answers/*/accept")).authenticated()

                .requestMatchers(new AntPathRequestMatcher("/questions/*/comments")).authenticated()
                .requestMatchers(new AntPathRequestMatcher("/answers/*/comments")).authenticated()
                .requestMatchers(new AntPathRequestMatcher("/comments/*/delete")).authenticated()
                .requestMatchers(new AntPathRequestMatcher("/comments/*/edit")).authenticated()

                .requestMatchers(new AntPathRequestMatcher("/profile")).authenticated()
                .requestMatchers(new AntPathRequestMatcher("/profile/edit")).authenticated()
                .requestMatchers(new AntPathRequestMatcher("/profile/update")).authenticated()
                .requestMatchers(new AntPathRequestMatcher("/profile/change-password")).authenticated()

                .requestMatchers(new AntPathRequestMatcher("/messages/**")).authenticated()
                .requestMatchers(new AntPathRequestMatcher("/notifications/**")).authenticated()
                .requestMatchers(new AntPathRequestMatcher("/follow/**")).authenticated()
                .requestMatchers(new AntPathRequestMatcher("/reports/**")).authenticated()

                .requestMatchers(new AntPathRequestMatcher("/users/*/edit")).hasRole("ADMIN")
                .requestMatchers(new AntPathRequestMatcher("/users/*/delete")).hasRole("ADMIN")

                // Public pages
                .requestMatchers(new AntPathRequestMatcher("/")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/home")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/questions")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/questions/*/view")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/questions/{id}")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/tags/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/users")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/users/*")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/profile/{username}")).permitAll()

                .requestMatchers(new AntPathRequestMatcher("/login")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/register")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/auth/**")).permitAll()

                .requestMatchers(new AntPathRequestMatcher("/password/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/tags/**")).permitAll()

                // Static resources
                .requestMatchers(new AntPathRequestMatcher("/css/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/js/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/images/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/static/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/webjars/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/uploads/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/attachments/**")).permitAll()

                // Error & WebSocket
                .requestMatchers(new AntPathRequestMatcher("/error/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/ws/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/topic/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/queue/**")).permitAll()

                // Default
                .anyRequest().authenticated()
            )
            .exceptionHandling(exception -> exception.accessDeniedPage("/error/403"))
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .defaultSuccessUrl("/", true)
                .failureHandler(authenticationFailureHandler)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/login?logout")
                .deleteCookies("JSESSIONID", "jwt")
                .invalidateHttpSession(true)
                .permitAll()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.ALWAYS));

        return http.build();
    }
}
