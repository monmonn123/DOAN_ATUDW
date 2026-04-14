package com.edumoet.service;

/**
 * DEPRECATED - Use com.edumoet.service.common.LocalFileStorageService instead
 * This file is kept for reference only and is no longer active
 */
@Deprecated
public class LocalFileStorageService_DEPRECATED {
    // This class has been moved to com.edumoet.service.common.LocalFileStorageService
    // Please use that one instead
}
    
    /**
     * Core upload logic
     */
    private String uploadFile(MultipartFile file, String directory, String namePrefix) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("File is empty");
        }
        
        String contentType = file.getContentType();
        if (contentType != null && !contentType.startsWith("image/")) {
            throw new IOException("Only image files are allowed");
        }
        
        File dir = new File(directory);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IOException("Failed to create directory: " + directory);
            }
        }
        
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        } else {
            ext = ".jpg";
        }
        
        String newFilename = namePrefix + "_" + UUID.randomUUID() + ext;
        Path filePath = Paths.get(directory, newFilename);
        Files.write(filePath, file.getBytes());
        
        String folderName = directory.substring(directory.lastIndexOf(File.separator) + 1);
        return "/uploads/" + folderName + "/" + newFilename;
    }
    
    /**
     * Xóa file
     */
    public boolean deleteFile(String relativePath) {
        try {
            if (relativePath == null || relativePath.isEmpty()) {
                return false;
            }
            
            String pathWithoutPrefix = relativePath.replace("/uploads/", "")
                                                   .replace("/", "\\");
            String fullPath = fileStorageConfig.getBasePath() + "\\" + pathWithoutPrefix;
            
            File file = new File(fullPath);
            if (file.exists()) {
                return file.delete();
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Kiểm tra file có tồn tại
     */
    public boolean fileExists(String relativePath) {
        try {
            String pathWithoutPrefix = relativePath.replace("/uploads/", "")
                                                   .replace("/", "\\");
            String fullPath = fileStorageConfig.getBasePath() + "\\" + pathWithoutPrefix;
            return new File(fullPath).exists();
        } catch (Exception e) {
            return false;
        }
    }
}
