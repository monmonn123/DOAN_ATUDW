package com.edumoet.service;

/**
 * ⚠️ DEPRECATED - Use LocalFileStorageService instead
 * 
 * This file is kept for reference only.
 * To avoid bean name conflicts with com.edumoet.service.common.FileStorageService,
 * use LocalFileStorageService for local file uploads.
 * 
 * Reason: 
 * - FileStorageService in common/ package handles AWS S3 uploads
 * - FileStorageService in service/ package (this) handles local file uploads
 * - Spring found both and complained about bean name conflict
 * 
 * Solution:
 * - Rename this file to LocalFileStorageService ✅ DONE
 * - Use LocalFileStorageService in controllers
 * - Delete this file when convenient
 */
public final class FileStorageService {
    private FileStorageService() {
        throw new UnsupportedOperationException("Use LocalFileStorageService instead");
    }
}
