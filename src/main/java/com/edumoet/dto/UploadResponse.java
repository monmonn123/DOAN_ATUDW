package com.edumoet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho file upload
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponse {
    private boolean success;
    private String message;
    private String filePath;  // /uploads/users/user_1_uuid.jpg
}
