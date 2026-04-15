package com.edumoet.controller.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.edumoet.entity.ImageAttachment;
import com.edumoet.entity.User;
import com.edumoet.service.common.ImageService;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    @Autowired
    private ImageService imageService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "questionId", required = false) Long questionId,
            @RequestParam(value = "answerId", required = false) Long answerId,
            @AuthenticationPrincipal User currentUser) {
        try {
            ImageAttachment attachment = imageService.saveImage(file, questionId, answerId, currentUser);
            return ResponseEntity.ok(attachment);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Failed to upload image: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteImage(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        try {
            ImageAttachment attachment = imageService.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found"));
                
            // Check if user has permission to delete
            if (!currentUser.equals(attachment.getUploadedBy())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You don't have permission to delete this image");
            }
            
            imageService.deleteImage(attachment);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to delete image: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getImage(@PathVariable Long id) {
        try {
            ImageAttachment attachment = imageService.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found"));
            
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .body(imageService.getImageData(attachment));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Image not found: " + e.getMessage());
        }
    }
}