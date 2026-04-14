package com.edumoet.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.edumoet.entity.User;
import com.edumoet.service.common.UserService;

/**
 * Follow Controller - Theo dõi người dùng
 */
@Controller
@RequestMapping("/follow")
public class FollowController {

    @Autowired
    private UserService userService;

    /**
     * Follow user
     */
    @PostMapping("/{username}")
    public String followUser(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        
        try {
            User currentUser = userService.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Current user not found"));
            
            User userToFollow = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (currentUser.equals(userToFollow)) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "You cannot follow yourself!");
                return "redirect:/profile/" + username;
            }
            
            if (!currentUser.getFollowing().contains(userToFollow)) {
                currentUser.getFollowing().add(userToFollow);
                userToFollow.getFollowers().add(currentUser);
                
                userService.updateUser(currentUser);
                userService.updateUser(userToFollow);
                
                redirectAttributes.addFlashAttribute("successMessage", 
                    "✅ You are now following " + username);
            }
            
            return "redirect:/profile/" + username;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "❌ Error: " + e.getMessage());
            return "redirect:/";
        }
    }

    /**
     * Unfollow user
     */
    @PostMapping("/unfollow/{username}")
    public String unfollowUser(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        
        try {
            User currentUser = userService.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Current user not found"));
            
            User userToUnfollow = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            currentUser.getFollowing().remove(userToUnfollow);
            userToUnfollow.getFollowers().remove(currentUser);
            
            userService.updateUser(currentUser);
            userService.updateUser(userToUnfollow);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "You unfollowed " + username);
            
            return "redirect:/profile/" + username;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "❌ Error: " + e.getMessage());
            return "redirect:/";
        }
    }
}

