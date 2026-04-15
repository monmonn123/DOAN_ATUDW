package com.edumoet.controller.common;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import com.edumoet.entity.User;
import com.edumoet.security.JwtTokenProvider;
import com.edumoet.service.common.UserService;

@Controller
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @GetMapping("/login")
    public String loginForm(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password");
        }
        model.addAttribute("pageTitle", "Log In - EDUMOET");
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("pageTitle", "Sign Up - EDUMOET");
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute User user,
            BindingResult result,
            Model model) {
        
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Sign Up - EDUMOET");
            return "auth/register";
        }
        
        try {
            userService.registerUser(user);
            return "redirect:/login?registered";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("pageTitle", "Sign Up - EDUMOET");
            return "auth/register";
        }
    }

	    // Spring Security sẽ tự động xử lý /login POST
	    // Không cần viết lại controller cho login
	
	    // Spring Security sẽ tự động xử lý /logout
	    // Không cần viết lại controller cho logout
}

