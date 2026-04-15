package com.edumoet.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Custom UTF-8 Filter để đảm bảo Content-Type headers đúng
     * Note: CharacterEncodingFilter đã được Spring Boot tự động cấu hình từ application.properties
     */
    @Bean
    public FilterRegistrationBean<Filter> utf8ContentTypeFilter() {
        FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                HttpServletRequest httpRequest = (HttpServletRequest) request;
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                
                // Set Content-Type với UTF-8 cho text/html, application/json, etc.
                String contentType = httpResponse.getContentType();
                if (contentType != null) {
                    if (contentType.contains("text/html") && !contentType.contains("charset")) {
                        httpResponse.setContentType(contentType + "; charset=UTF-8");
                    } else if (contentType.contains("application/json") && !contentType.contains("charset")) {
                        httpResponse.setContentType(contentType + "; charset=UTF-8");
                    }
                }
                
                // Set default charset if not set
                if (httpResponse.getCharacterEncoding() == null || 
                    httpResponse.getCharacterEncoding().isEmpty()) {
                    httpResponse.setCharacterEncoding("UTF-8");
                }
                
                chain.doFilter(request, response);
            }
        });
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(2);
        return registrationBean;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/");
        
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/");
        
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
        
        // Handle uploaded files (images, avatars, logos, attachments)
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
        
        registry.addResourceHandler("/attachments/**")
                .addResourceLocations("file:uploads/attachments/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login").setViewName("auth/login");
        registry.addViewController("/register").setViewName("auth/register");
    }
}

