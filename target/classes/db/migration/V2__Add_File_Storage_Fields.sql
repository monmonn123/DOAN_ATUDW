-- V2__Add_File_Storage_Fields.sql
-- Description: Thêm các cột để lưu đường dẫn ảnh cho lưu trữ local
-- Created: 2026-04-14

-- Thêm avatar_path cho Users (phục vụ lưu trữ file local)
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Users' AND COLUMN_NAME = 'avatar_path')
BEGIN
    ALTER TABLE Users
    ADD avatar_path NVARCHAR(500) NULL;
    
    CREATE INDEX idx_users_avatar_path ON Users(avatar_path);
    
    PRINT 'Added avatar_path column to Users table';
END

-- Thêm image_paths cho Questions (JSON array các ảnh)
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Questions' AND COLUMN_NAME = 'image_paths')
BEGIN
    ALTER TABLE Questions
    ADD image_paths NVARCHAR(MAX) NULL;  -- JSON format: ["/uploads/questions/q_1_uuid.jpg", ...]
    
    PRINT 'Added image_paths column to Questions table';
END

-- Thêm image_paths cho Answers (JSON array các ảnh)
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Answers' AND COLUMN_NAME = 'image_paths')
BEGIN
    ALTER TABLE Answers
    ADD image_paths NVARCHAR(MAX) NULL;  -- JSON format: ["/uploads/answers/a_1_uuid.jpg", ...]
    
    PRINT 'Added image_paths column to Answers table';
END

-- Thêm attachment_paths cho Comments (nếu cần)
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Comments' AND COLUMN_NAME = 'attachment_paths')
BEGIN
    ALTER TABLE Comments
    ADD attachment_paths NVARCHAR(MAX) NULL;  -- JSON format
    
    PRINT 'Added attachment_paths column to Comments table';
END
