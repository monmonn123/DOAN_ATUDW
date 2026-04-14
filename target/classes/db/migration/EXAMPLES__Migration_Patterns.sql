/*
 * EXAMPLE MIGRATIONS - Các ví dụ thực tế cho việc quản lý thay đổi database
 * 
 * Sau khi V1__Initial_Schema.sql chạy thành công,
 * hãy theo các pattern sau cho những thay đổi tiếp theo.
 */

-- ======================================================
-- V2__Add_user_followers_feature.sql
-- Feature: Cho phép users follow nhau
-- ======================================================
/*
IF OBJECT_ID('User_Followers', 'U') IS NOT NULL
    DROP TABLE User_Followers;

CREATE TABLE User_Followers (
    id BIGINT PRIMARY KEY IDENTITY(1,1),
    follower_id BIGINT NOT NULL,
    following_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (follower_id) REFERENCES Users(id) ON DELETE CASCADE,
    FOREIGN KEY (following_id) REFERENCES Users(id) ON DELETE CASCADE,
    CONSTRAINT uq_followers UNIQUE (follower_id, following_id)
);

CREATE INDEX idx_followers_follower ON User_Followers(follower_id);
CREATE INDEX idx_followers_following ON User_Followers(following_id);
*/

-- ======================================================
-- V3__Add_email_verification_fields.sql
-- Feature: Email verification system
-- ======================================================
/*
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_NAME = 'Users' AND COLUMN_NAME = 'verification_code')
BEGIN
    ALTER TABLE Users ADD verification_code NVARCHAR(255) NULL;
    ALTER TABLE Users ADD verification_sent_at DATETIME NULL;
    ALTER TABLE Users ADD verified_at DATETIME NULL;
END
*/

-- ======================================================
-- V4__Add_question_category_field.sql
-- Feature: Phân loại câu hỏi
-- ======================================================
/*
-- Tạo table Categories
IF OBJECT_ID('Categories', 'U') IS NOT NULL
    DROP TABLE Categories;

CREATE TABLE Categories (
    id BIGINT PRIMARY KEY IDENTITY(1,1),
    name NVARCHAR(100) NOT NULL UNIQUE,
    slug NVARCHAR(100) NOT NULL UNIQUE,
    description NVARCHAR(500),
    created_at DATETIME DEFAULT GETDATE()
);

-- Thêm category_id vào Questions
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_NAME = 'Questions' AND COLUMN_NAME = 'category_id')
BEGIN
    ALTER TABLE Questions ADD category_id BIGINT NULL;
    ALTER TABLE Questions ADD FOREIGN KEY (category_id) REFERENCES Categories(id);
    CREATE INDEX idx_questions_category ON Questions(category_id);
END
*/

-- ======================================================
-- V5__Add_reputation_and_badges_system.sql
-- Feature: Reputation points & Badges
-- ======================================================
/*
-- Tạo table Badges
IF OBJECT_ID('Badges', 'U') IS NOT NULL
    DROP TABLE Badges;

CREATE TABLE Badges (
    id BIGINT PRIMARY KEY IDENTITY(1,1),
    name NVARCHAR(200) NOT NULL UNIQUE,
    description NVARCHAR(500),
    icon_url NVARCHAR(500),
    criteria NVARCHAR(1000),
    created_at DATETIME DEFAULT GETDATE()
);

-- Tạo bảng User_Badges (many-to-many)
IF OBJECT_ID('User_Badges', 'U') IS NOT NULL
    DROP TABLE User_Badges;

CREATE TABLE User_Badges (
    id BIGINT PRIMARY KEY IDENTITY(1,1),
    user_id BIGINT NOT NULL,
    badge_id BIGINT NOT NULL,
    awarded_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE,
    FOREIGN KEY (badge_id) REFERENCES Badges(id),
    CONSTRAINT uq_user_badges UNIQUE (user_id, badge_id)
);

CREATE INDEX idx_user_badges_user ON User_Badges(user_id);
CREATE INDEX idx_user_badges_badge ON User_Badges(badge_id);

-- Update Users table để lưu reputation
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_NAME = 'Users' AND COLUMN_NAME = 'reputation_points')
BEGIN
    ALTER TABLE Users ADD reputation_points INT DEFAULT 0;
END
*/

-- ======================================================
-- V6__Add_search_optimization_indexes.sql
-- Optimization: Performance improvement
-- ======================================================
/*
-- Index cho full-text search trên Questions
CREATE FULLTEXT CATALOG QuestionCatalog AS DEFAULT;
CREATE FULLTEXT INDEX ON Questions(title, description) 
KEY INDEX PK__Questions__id;

-- Index cho popular questions
CREATE INDEX idx_questions_views_created 
ON Questions(views DESC, created_at DESC);

-- Index cho answer searching
CREATE INDEX idx_answers_votes_created 
ON Answers(votes DESC, created_at DESC);

-- Index cho tag queries
CREATE INDEX idx_question_tags_count 
ON Question_Tags(tag_id);
*/

-- ======================================================
-- V7__Add_audit_logging_trigger.sql
-- Security: Audit trail tự động
-- ======================================================
/*
-- Trigger để log mọi thay đổi Users table
CREATE TRIGGER trg_users_audit
ON Users
AFTER UPDATE
AS
BEGIN
    INSERT INTO Activity_Logs (user_id, action_type, entity_type, entity_id, description, created_at)
    SELECT 
        UPDATED.id,
        'UPDATE',
        'USER',
        UPDATED.id,
        'User profile updated',
        GETDATE()
    FROM inserted UPDATED;
END;
*/

-- ======================================================
-- V8__Add_soft_delete_cascade.sql
-- Data Integrity: Soft delete support
-- ======================================================
/*
-- Constraint để enforce soft delete
ALTER TABLE Questions 
ADD CONSTRAINT chk_questions_deleted 
CHECK (deleted_at IS NULL OR deleted_at > created_at);

-- Stored procedure để soft delete question
CREATE PROCEDURE sp_soft_delete_question
    @question_id BIGINT
AS
BEGIN
    BEGIN TRANSACTION;
    
    BEGIN TRY
        UPDATE Questions SET deleted_at = GETDATE() WHERE id = @question_id;
        UPDATE Answers SET deleted_at = GETDATE() WHERE question_id = @question_id;
        UPDATE Comments SET deleted_at = GETDATE() WHERE question_id = @question_id;
        
        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
*/

-- ======================================================
-- V9__Archive_old_questions.sql
-- Maintenance: Archive old data
-- ======================================================
/*
-- Tạo table để archive old questions
IF OBJECT_ID('Questions_Archive', 'U') IS NOT NULL
    DROP TABLE Questions_Archive;

CREATE TABLE Questions_Archive (
    id BIGINT PRIMARY KEY,
    title NVARCHAR(500) NOT NULL,
    description NVARCHAR(MAX) NOT NULL,
    user_id BIGINT NOT NULL,
    views INT DEFAULT 0,
    has_accepted_answer BIT DEFAULT 0,
    created_at DATETIME,
    updated_at DATETIME,
    deleted_at DATETIME,
    archived_at DATETIME DEFAULT GETDATE()
);

-- Move old questions to archive (tùy business logic)
INSERT INTO Questions_Archive
SELECT * FROM Questions 
WHERE created_at < DATEADD(YEAR, -1, GETDATE()) 
  AND deleted_at IS NOT NULL;
*/

-- ======================================================
-- V10__Migration_status_table.sql
-- Monitoring: Track migration status
-- ======================================================
/*
IF OBJECT_ID('Migration_Status', 'U') IS NOT NULL
    DROP TABLE Migration_Status;

CREATE TABLE Migration_Status (
    id BIGINT PRIMARY KEY IDENTITY(1,1),
    migration_version NVARCHAR(100) NOT NULL UNIQUE,
    description NVARCHAR(500),
    status NVARCHAR(50) DEFAULT 'PENDING',  -- PENDING, IN_PROGRESS, SUCCESS, FAILED
    started_at DATETIME,
    completed_at DATETIME,
    error_message NVARCHAR(MAX),
    created_at DATETIME DEFAULT GETDATE()
);
*/

-- ======================================================
-- HOW TO USE THESE EXAMPLES:
-- ======================================================
/*
1. Uncomment phần code của migration muốn dùng
2. Tạo file mới: V##__Description.sql
3. Copy code vào file
4. Test locally
5. Commit & deploy

HOẶC nếu migrate song song:
- Hãy đợi V1__Initial_Schema.sql chạy thành công trước
- Rồi bắt đầu tạo V2, V3, V4... nếu có change request
*/
