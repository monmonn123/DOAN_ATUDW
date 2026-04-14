-- V1__Initial_Schema.sql
-- Initial database schema for EDMOET application
-- Created: 2026-04-14

-- Users table
IF OBJECT_ID('Users', 'U') IS NOT NULL
    DROP TABLE Users;

CREATE TABLE Users (
    id BIGINT PRIMARY KEY IDENTITY(1,1),
    username NVARCHAR(255) NOT NULL UNIQUE,
    email NVARCHAR(255) NOT NULL UNIQUE,
    password NVARCHAR(255) NOT NULL,
    full_name NVARCHAR(255),
    avatar_url NVARCHAR(500),
    bio NVARCHAR(1000),
    reputation INT DEFAULT 0,
    verified BIT DEFAULT 0,
    role NVARCHAR(50) DEFAULT 'ROLE_USER',
    is_active BIT DEFAULT 1,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    deleted_at DATETIME NULL
);

-- Questions table
IF OBJECT_ID('Questions', 'U') IS NOT NULL
    DROP TABLE Questions;

CREATE TABLE Questions (
    id BIGINT PRIMARY KEY IDENTITY(1,1),
    title NVARCHAR(500) NOT NULL,
    description NVARCHAR(MAX) NOT NULL,
    user_id BIGINT NOT NULL,
    views INT DEFAULT 0,
    has_accepted_answer BIT DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    deleted_at DATETIME NULL,
    FOREIGN KEY (user_id) REFERENCES Users(id)
);

-- Answers table
IF OBJECT_ID('Answers', 'U') IS NOT NULL
    DROP TABLE Answers;

CREATE TABLE Answers (
    id BIGINT PRIMARY KEY IDENTITY(1,1),
    content NVARCHAR(MAX) NOT NULL,
    question_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    is_accepted BIT DEFAULT 0,
    votes INT DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    deleted_at DATETIME NULL,
    FOREIGN KEY (question_id) REFERENCES Questions(id),
    FOREIGN KEY (user_id) REFERENCES Users(id)
);

-- Tags table
IF OBJECT_ID('Tags', 'U') IS NOT NULL
    DROP TABLE Tags;

CREATE TABLE Tags (
    id BIGINT PRIMARY KEY IDENTITY(1,1),
    name NVARCHAR(100) NOT NULL UNIQUE,
    description NVARCHAR(500),
    color NVARCHAR(50),
    created_at DATETIME DEFAULT GETDATE()
);

-- Question_Tags table (many-to-many)
IF OBJECT_ID('Question_Tags', 'U') IS NOT NULL
    DROP TABLE Question_Tags;

CREATE TABLE Question_Tags (
    question_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (question_id, tag_id),
    FOREIGN KEY (question_id) REFERENCES Questions(id),
    FOREIGN KEY (tag_id) REFERENCES Tags(id)
);

-- Comments table
IF OBJECT_ID('Comments', 'U') IS NOT NULL
    DROP TABLE Comments;

CREATE TABLE Comments (
    id BIGINT PRIMARY KEY IDENTITY(1,1),
    content NVARCHAR(MAX) NOT NULL,
    user_id BIGINT NOT NULL,
    question_id BIGINT,
    answer_id BIGINT,
    votes INT DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(id),
    FOREIGN KEY (question_id) REFERENCES Questions(id),
    FOREIGN KEY (answer_id) REFERENCES Answers(id)
);

-- Messages table (inbox/sent)
IF OBJECT_ID('Messages', 'U') IS NOT NULL
    DROP TABLE Messages;

CREATE TABLE Messages (
    id BIGINT PRIMARY KEY IDENTITY(1,1),
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    subject NVARCHAR(500),
    body NVARCHAR(MAX) NOT NULL,
    is_read BIT DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    deleted_by_sender BIT DEFAULT 0,
    deleted_by_receiver BIT DEFAULT 0,
    FOREIGN KEY (sender_id) REFERENCES Users(id),
    FOREIGN KEY (receiver_id) REFERENCES Users(id)
);

-- Notifications table
IF OBJECT_ID('Notifications', 'U') IS NOT NULL
    DROP TABLE Notifications;

CREATE TABLE Notifications (
    id BIGINT PRIMARY KEY IDENTITY(1,1),
    user_id BIGINT NOT NULL,
    type NVARCHAR(100) NOT NULL,
    title NVARCHAR(500),
    message NVARCHAR(MAX),
    related_user_id BIGINT,
    question_id BIGINT,
    answer_id BIGINT,
    is_read BIT DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(id),
    FOREIGN KEY (related_user_id) REFERENCES Users(id),
    FOREIGN KEY (question_id) REFERENCES Questions(id),
    FOREIGN KEY (answer_id) REFERENCES Answers(id)
);

-- Reports table (spam, abuse reports)
IF OBJECT_ID('Reports', 'U') IS NOT NULL
    DROP TABLE Reports;

CREATE TABLE Reports (
    id BIGINT PRIMARY KEY IDENTITY(1,1),
    reporter_id BIGINT NOT NULL,
    reported_user_id BIGINT,
    question_id BIGINT,
    answer_id BIGINT,
    reason NVARCHAR(500) NOT NULL,
    description NVARCHAR(MAX),
    status NVARCHAR(50) DEFAULT 'PENDING',
    created_at DATETIME DEFAULT GETDATE(),
    resolved_at DATETIME NULL,
    resolved_by_id BIGINT,
    FOREIGN KEY (reporter_id) REFERENCES Users(id),
    FOREIGN KEY (reported_user_id) REFERENCES Users(id),
    FOREIGN KEY (question_id) REFERENCES Questions(id),
    FOREIGN KEY (answer_id) REFERENCES Answers(id),
    FOREIGN KEY (resolved_by_id) REFERENCES Users(id)
);

-- Activity Logs table (audit trail)
IF OBJECT_ID('Activity_Logs', 'U') IS NOT NULL
    DROP TABLE Activity_Logs;

CREATE TABLE Activity_Logs (
    id BIGINT PRIMARY KEY IDENTITY(1,1),
    user_id BIGINT NOT NULL,
    action_type NVARCHAR(100) NOT NULL,
    entity_type NVARCHAR(100),
    entity_id BIGINT,
    description NVARCHAR(MAX),
    ip_address NVARCHAR(50),
    created_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(id)
);

-- Create Indexes for better performance
CREATE INDEX idx_questions_user_id ON Questions(user_id);
CREATE INDEX idx_questions_created_at ON Questions(created_at);
CREATE INDEX idx_answers_question_id ON Answers(question_id);
CREATE INDEX idx_answers_user_id ON Answers(user_id);
CREATE INDEX idx_comments_user_id ON Comments(user_id);
CREATE INDEX idx_messages_sender_id ON Messages(sender_id);
CREATE INDEX idx_messages_receiver_id ON Messages(receiver_id);
CREATE INDEX idx_notifications_user_id ON Notifications(user_id);
CREATE INDEX idx_activity_logs_user_id ON Activity_Logs(user_id);
CREATE INDEX idx_activity_logs_created_at ON Activity_Logs(created_at);
