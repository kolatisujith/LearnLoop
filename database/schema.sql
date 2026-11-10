-- LearnLoop database schema
-- Target: MySQL 8.0+ / MySQL 8.4 LTS
--
-- This script creates the learnloop_db database and all LearnLoop tables. It is
-- intentionally non-destructive: existing tables are left in place. For a clean
-- development reset, drop the database explicitly and run this script again.
--
-- Usage:
--   mysql -u root -p -e "source database/schema.sql"

CREATE DATABASE IF NOT EXISTS learnloop_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE learnloop_db;

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- Accounts and approvals
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS users (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    role VARCHAR(20) NOT NULL,
    approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_at DATETIME(6) NULL,
    approved_by_user_id BIGINT UNSIGNED NULL,
    rejection_reason VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email),
    KEY idx_users_role_status (role, approval_status),
    KEY idx_users_approved_by (approved_by_user_id),
    CONSTRAINT chk_users_role
        CHECK (role IN ('ADMIN', 'TEACHER', 'STUDENT')),
    CONSTRAINT chk_users_approval_status
        CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED')),
    CONSTRAINT chk_users_full_name
        CHECK (CHAR_LENGTH(TRIM(full_name)) > 0),
    CONSTRAINT fk_users_approved_by
        FOREIGN KEY (approved_by_user_id) REFERENCES users (id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Phase 5 migrations
-- ---------------------------------------------------------------------------

-- MySQL does not support ADD COLUMN IF NOT EXISTS, so guard the migration with
-- information_schema. Existing databases created by earlier phases are upgraded
-- in place while fresh databases get the column from the CREATE TABLE above.
SET @rejection_reason_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'users'
      AND column_name = 'rejection_reason'
);
SET @add_rejection_reason_sql = IF(
    @rejection_reason_exists = 0,
    'ALTER TABLE users ADD COLUMN rejection_reason VARCHAR(500) NULL AFTER approved_by_user_id',
    'DO 0'
);
PREPARE add_rejection_reason_statement FROM @add_rejection_reason_sql;
EXECUTE add_rejection_reason_statement;
DEALLOCATE PREPARE add_rejection_reason_statement;

-- ---------------------------------------------------------------------------
-- Subjects and teacher assignments
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS subjects (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description TEXT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_subjects_code (code),
    UNIQUE KEY uq_subjects_name (name),
    KEY idx_subjects_active (is_active),
    CONSTRAINT chk_subjects_code
        CHECK (CHAR_LENGTH(TRIM(code)) > 0),
    CONSTRAINT chk_subjects_name
        CHECK (CHAR_LENGTH(TRIM(name)) > 0)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS teacher_subject_assignments (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    teacher_user_id BIGINT UNSIGNED NOT NULL,
    subject_id BIGINT UNSIGNED NOT NULL,
    assigned_by_user_id BIGINT UNSIGNED NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_teacher_subject_assignments_pair (teacher_user_id, subject_id),
    KEY idx_teacher_subject_assignments_subject (subject_id, is_active),
    KEY idx_teacher_subject_assignments_teacher (teacher_user_id, is_active),
    KEY idx_teacher_subject_assignments_assigned_by (assigned_by_user_id),
    CONSTRAINT fk_teacher_subject_assignments_teacher
        FOREIGN KEY (teacher_user_id) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_teacher_subject_assignments_subject
        FOREIGN KEY (subject_id) REFERENCES subjects (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_teacher_subject_assignments_assigned_by
        FOREIGN KEY (assigned_by_user_id) REFERENCES users (id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Classes and invitations
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS classes (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    subject_id BIGINT UNSIGNED NOT NULL,
    teacher_user_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(160) NOT NULL,
    description TEXT NULL,
    invitation_token_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    invitation_token_created_at DATETIME(6) NULL,
    invitation_token_expires_at DATETIME(6) NULL,
    invitation_token_revoked_at DATETIME(6) NULL,
      status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
      max_students INT UNSIGNED NULL,
      meeting_url VARCHAR(1000) NULL,
      created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_classes_invitation_token_hash (invitation_token_hash),
    KEY idx_classes_teacher_status (teacher_user_id, status),
    KEY idx_classes_subject_status (subject_id, status),
    KEY idx_classes_invitation_expiry (invitation_token_expires_at),
    CONSTRAINT chk_classes_title
        CHECK (CHAR_LENGTH(TRIM(title)) > 0),
    CONSTRAINT chk_classes_status
        CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED', 'CANCELLED')),
    CONSTRAINT chk_classes_max_students
        CHECK (max_students IS NULL OR max_students > 0),
    CONSTRAINT fk_classes_teacher_subject
        FOREIGN KEY (teacher_user_id, subject_id)
        REFERENCES teacher_subject_assignments (teacher_user_id, subject_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Phase 7 migrations
-- ---------------------------------------------------------------------------

-- Track when the current invitation link was issued and when it was revoked.
-- Existing issued links are treated as created when their class was created.
SET @invitation_created_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'classes'
      AND column_name = 'invitation_token_created_at'
);
SET @add_invitation_created_sql = IF(
    @invitation_created_exists = 0,
    'ALTER TABLE classes ADD COLUMN invitation_token_created_at DATETIME(6) NULL AFTER invitation_token_hash',
    'DO 0'
);
PREPARE add_invitation_created_statement FROM @add_invitation_created_sql;
EXECUTE add_invitation_created_statement;
DEALLOCATE PREPARE add_invitation_created_statement;

SET @backfill_invitation_created_sql = IF(
    @invitation_created_exists = 0,
    'UPDATE classes SET invitation_token_created_at = created_at WHERE invitation_token_expires_at IS NOT NULL',
    'DO 0'
);
PREPARE backfill_invitation_created_statement FROM @backfill_invitation_created_sql;
EXECUTE backfill_invitation_created_statement;
DEALLOCATE PREPARE backfill_invitation_created_statement;

SET @invitation_revoked_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'classes'
      AND column_name = 'invitation_token_revoked_at'
);
SET @add_invitation_revoked_sql = IF(
    @invitation_revoked_exists = 0,
    'ALTER TABLE classes ADD COLUMN invitation_token_revoked_at DATETIME(6) NULL AFTER invitation_token_expires_at',
    'DO 0'
);
PREPARE add_invitation_revoked_statement FROM @add_invitation_revoked_sql;
EXECUTE add_invitation_revoked_statement;
DEALLOCATE PREPARE add_invitation_revoked_statement;

-- ---------------------------------------------------------------------------
-- Join requests and enrollments
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS class_join_requests (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    class_id BIGINT UNSIGNED NOT NULL,
    student_user_id BIGINT UNSIGNED NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    message VARCHAR(500) NULL,
    reviewed_by_user_id BIGINT UNSIGNED NULL,
    reviewed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_class_join_requests_class_student (class_id, student_user_id),
    KEY idx_class_join_requests_class_status (class_id, status),
    KEY idx_class_join_requests_student_status (student_user_id, status),
    KEY idx_class_join_requests_reviewer (reviewed_by_user_id),
    CONSTRAINT chk_class_join_requests_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT fk_class_join_requests_class
        FOREIGN KEY (class_id) REFERENCES classes (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_class_join_requests_student
        FOREIGN KEY (student_user_id) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_class_join_requests_reviewer
        FOREIGN KEY (reviewed_by_user_id) REFERENCES users (id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS class_enrollments (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    class_id BIGINT UNSIGNED NOT NULL,
    student_user_id BIGINT UNSIGNED NOT NULL,
    join_request_id BIGINT UNSIGNED NULL,
    enrolled_by_user_id BIGINT UNSIGNED NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    removed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_class_enrollments_class_student (class_id, student_user_id),
    UNIQUE KEY uq_class_enrollments_join_request (join_request_id),
    KEY idx_class_enrollments_class_status (class_id, status),
    KEY idx_class_enrollments_student_status (student_user_id, status),
    KEY idx_class_enrollments_enrolled_by (enrolled_by_user_id),
    CONSTRAINT chk_class_enrollments_status
        CHECK (status IN ('ACTIVE', 'REMOVED', 'COMPLETED')),
    CONSTRAINT fk_class_enrollments_class
        FOREIGN KEY (class_id) REFERENCES classes (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_class_enrollments_student
        FOREIGN KEY (student_user_id) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_class_enrollments_join_request
        FOREIGN KEY (join_request_id) REFERENCES class_join_requests (id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_class_enrollments_enrolled_by
        FOREIGN KEY (enrolled_by_user_id) REFERENCES users (id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Class materials
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS class_materials (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    class_id BIGINT UNSIGNED NOT NULL,
    uploaded_by_user_id BIGINT UNSIGNED NULL,
    title VARCHAR(160) NOT NULL,
    description TEXT NULL,
    material_type VARCHAR(20) NOT NULL,
    resource_url VARCHAR(1000) NULL,
    content_text MEDIUMTEXT NULL,
    file_name VARCHAR(255) NULL,
    stored_name VARCHAR(255) NULL,
    mime_type VARCHAR(120) NULL,
    file_size_bytes BIGINT UNSIGNED NULL,
    position INT UNSIGNED NOT NULL DEFAULT 0,
    is_published BOOLEAN NOT NULL DEFAULT TRUE,
    published_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_class_materials_class_published (class_id, is_published, position),
    KEY idx_class_materials_uploaded_by (uploaded_by_user_id),
    CONSTRAINT chk_class_materials_type
        CHECK (material_type IN ('LINK', 'FILE', 'TEXT')),
    CONSTRAINT chk_class_materials_source
        CHECK (
            (material_type = 'LINK' AND resource_url IS NOT NULL AND CHAR_LENGTH(resource_url) > 0)
            OR (material_type = 'FILE' AND file_name IS NOT NULL AND CHAR_LENGTH(file_name) > 0)
            OR (material_type = 'TEXT' AND content_text IS NOT NULL AND CHAR_LENGTH(content_text) > 0)
        ),
    CONSTRAINT chk_class_materials_title
        CHECK (CHAR_LENGTH(TRIM(title)) > 0),
    CONSTRAINT fk_class_materials_class
        FOREIGN KEY (class_id) REFERENCES classes (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_class_materials_uploaded_by
        FOREIGN KEY (uploaded_by_user_id) REFERENCES users (id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Assessments, questions, options and attempts
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS assessments (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    class_id BIGINT UNSIGNED NOT NULL,
    created_by_user_id BIGINT UNSIGNED NULL,
    title VARCHAR(160) NOT NULL,
    instructions TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    available_from DATETIME(6) NULL,
    due_at DATETIME(6) NULL,
    time_limit_minutes INT UNSIGNED NULL,
    max_attempts INT UNSIGNED NOT NULL DEFAULT 1,
    passing_score_percent DECIMAL(5, 2) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_assessments_class_status (class_id, status),
    KEY idx_assessments_due_at (due_at),
    KEY idx_assessments_created_by (created_by_user_id),
    CONSTRAINT chk_assessments_title
        CHECK (CHAR_LENGTH(TRIM(title)) > 0),
    CONSTRAINT chk_assessments_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'CLOSED', 'ARCHIVED')),
    CONSTRAINT chk_assessments_max_attempts
        CHECK (max_attempts > 0),
    CONSTRAINT chk_assessments_time_limit
        CHECK (time_limit_minutes IS NULL OR time_limit_minutes > 0),
    CONSTRAINT chk_assessments_passing_score
        CHECK (passing_score_percent IS NULL OR (passing_score_percent >= 0 AND passing_score_percent <= 100)),
    CONSTRAINT chk_assessments_window
        CHECK (available_from IS NULL OR due_at IS NULL OR due_at >= available_from),
    CONSTRAINT fk_assessments_class
        FOREIGN KEY (class_id) REFERENCES classes (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_assessments_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES users (id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS questions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    assessment_id BIGINT UNSIGNED NOT NULL,
    question_type VARCHAR(20) NOT NULL,
    prompt TEXT NOT NULL,
    points DECIMAL(6, 2) NOT NULL DEFAULT 1.00,
    position INT UNSIGNED NOT NULL DEFAULT 0,
    is_required BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_questions_assessment_position (assessment_id, position),
    CONSTRAINT chk_questions_type
        CHECK (question_type IN ('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE', 'SHORT_TEXT')),
    CONSTRAINT chk_questions_prompt
        CHECK (CHAR_LENGTH(TRIM(prompt)) > 0),
    CONSTRAINT chk_questions_points
        CHECK (points > 0),
    CONSTRAINT fk_questions_assessment
        FOREIGN KEY (assessment_id) REFERENCES assessments (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS question_options (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    question_id BIGINT UNSIGNED NOT NULL,
    option_text VARCHAR(500) NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    position INT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_question_options_id_question (id, question_id),
    KEY idx_question_options_question_position (question_id, position),
    CONSTRAINT chk_question_options_text
        CHECK (CHAR_LENGTH(TRIM(option_text)) > 0),
    CONSTRAINT fk_question_options_question
        FOREIGN KEY (question_id) REFERENCES questions (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS assessment_attempts (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    assessment_id BIGINT UNSIGNED NOT NULL,
    student_user_id BIGINT UNSIGNED NOT NULL,
    attempt_number INT UNSIGNED NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    started_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    submitted_at DATETIME(6) NULL,
    graded_at DATETIME(6) NULL,
    score DECIMAL(8, 2) NULL,
    graded_by_user_id BIGINT UNSIGNED NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_assessment_attempts_number (assessment_id, student_user_id, attempt_number),
    KEY idx_assessment_attempts_student_status (student_user_id, status),
    KEY idx_assessment_attempts_assessment_status (assessment_id, status),
    KEY idx_assessment_attempts_graded_by (graded_by_user_id),
    CONSTRAINT chk_assessment_attempts_number
        CHECK (attempt_number > 0),
    CONSTRAINT chk_assessment_attempts_status
        CHECK (status IN ('IN_PROGRESS', 'SUBMITTED', 'GRADED', 'EXPIRED', 'ABANDONED')),
    CONSTRAINT chk_assessment_attempts_score
        CHECK (score IS NULL OR score >= 0),
    CONSTRAINT chk_assessment_attempts_submission_time
        CHECK (submitted_at IS NULL OR submitted_at >= started_at),
    CONSTRAINT chk_assessment_attempts_grading_time
        CHECK (graded_at IS NULL OR submitted_at IS NOT NULL),
    CONSTRAINT fk_assessment_attempts_assessment
        FOREIGN KEY (assessment_id) REFERENCES assessments (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_assessment_attempts_student
        FOREIGN KEY (student_user_id) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_assessment_attempts_graded_by
        FOREIGN KEY (graded_by_user_id) REFERENCES users (id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS assessment_answers (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    attempt_id BIGINT UNSIGNED NOT NULL,
    question_id BIGINT UNSIGNED NOT NULL,
    answer_text TEXT NULL,
    is_correct BOOLEAN NULL,
    points_awarded DECIMAL(6, 2) NULL,
    answered_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_assessment_answers_attempt_question (attempt_id, question_id),
    UNIQUE KEY uq_assessment_answers_id_question (id, question_id),
    KEY idx_assessment_answers_question (question_id),
    CONSTRAINT chk_assessment_answers_points
        CHECK (points_awarded IS NULL OR points_awarded >= 0),
    CONSTRAINT fk_assessment_answers_attempt
        FOREIGN KEY (attempt_id) REFERENCES assessment_attempts (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_assessment_answers_question
        FOREIGN KEY (question_id) REFERENCES questions (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Multiple-choice answers are represented by zero or more selected options.
-- The composite foreign keys ensure a selected option belongs to the same
-- question as the answer that references it.
CREATE TABLE IF NOT EXISTS assessment_answer_options (
    answer_id BIGINT UNSIGNED NOT NULL,
    option_id BIGINT UNSIGNED NOT NULL,
    question_id BIGINT UNSIGNED NOT NULL,
    selected_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (answer_id, option_id),
    KEY idx_assessment_answer_options_option (option_id, question_id),
    CONSTRAINT fk_assessment_answer_options_answer
        FOREIGN KEY (answer_id, question_id)
        REFERENCES assessment_answers (id, question_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_assessment_answer_options_option
        FOREIGN KEY (option_id, question_id)
        REFERENCES question_options (id, question_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Notifications and audit logs
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    recipient_user_id BIGINT UNSIGNED NOT NULL,
    actor_user_id BIGINT UNSIGNED NULL,
    type VARCHAR(40) NOT NULL,
    title VARCHAR(160) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    link_url VARCHAR(1000) NULL,
    related_entity_type VARCHAR(40) NULL,
    related_entity_id BIGINT UNSIGNED NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_notifications_recipient_read (recipient_user_id, is_read, created_at),
    KEY idx_notifications_actor (actor_user_id),
    KEY idx_notifications_entity (related_entity_type, related_entity_id),
    CONSTRAINT chk_notifications_read_state
        CHECK (
            (is_read = FALSE AND read_at IS NULL)
            OR (is_read = TRUE AND read_at IS NOT NULL)
        ),
    CONSTRAINT chk_notifications_related_entity
        CHECK (
            (related_entity_type IS NULL AND related_entity_id IS NULL)
            OR (related_entity_type IS NOT NULL AND related_entity_id IS NOT NULL)
        ),
    CONSTRAINT fk_notifications_recipient
        FOREIGN KEY (recipient_user_id) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_notifications_actor
        FOREIGN KEY (actor_user_id) REFERENCES users (id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    actor_user_id BIGINT UNSIGNED NULL,
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT UNSIGNED NULL,
    details JSON NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(512) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_audit_logs_actor_created (actor_user_id, created_at),
    KEY idx_audit_logs_entity (entity_type, entity_id),
    KEY idx_audit_logs_created (created_at),
    CONSTRAINT chk_audit_logs_action
        CHECK (CHAR_LENGTH(TRIM(action)) > 0),
    CONSTRAINT chk_audit_logs_entity_type
        CHECK (CHAR_LENGTH(TRIM(entity_type)) > 0),
    CONSTRAINT fk_audit_logs_actor
        FOREIGN KEY (actor_user_id) REFERENCES users (id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Phase 10 migrations
-- ---------------------------------------------------------------------------

-- External (Google Meet) live-session link for a class.
SET @meeting_url_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'classes'
      AND column_name = 'meeting_url'
);
SET @add_meeting_url_sql = IF(
    @meeting_url_exists = 0,
    'ALTER TABLE classes ADD COLUMN meeting_url VARCHAR(1000) NULL AFTER max_students',
    'DO 0'
);
PREPARE add_meeting_url_statement FROM @add_meeting_url_sql;
EXECUTE add_meeting_url_statement;
DEALLOCATE PREPARE add_meeting_url_statement;

-- Server-generated storage key for uploaded material files. The original file
-- name is kept separately in file_name for display and download headers.
SET @stored_name_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'class_materials'
      AND column_name = 'stored_name'
);
SET @add_stored_name_sql = IF(
    @stored_name_exists = 0,
    'ALTER TABLE class_materials ADD COLUMN stored_name VARCHAR(255) NULL AFTER file_name',
    'DO 0'
);
PREPARE add_stored_name_statement FROM @add_stored_name_sql;
EXECUTE add_stored_name_statement;
DEALLOCATE PREPARE add_stored_name_statement;

-- ---------------------------------------------------------------------------
-- Phase 11 migrations
-- ---------------------------------------------------------------------------

-- Controls whether graded students can see which answers were correct.
SET @reveal_correct_answers_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'assessments'
      AND column_name = 'reveal_correct_answers'
);
SET @add_reveal_correct_answers_sql = IF(
    @reveal_correct_answers_exists = 0,
    'ALTER TABLE assessments ADD COLUMN reveal_correct_answers BOOLEAN NOT NULL DEFAULT TRUE AFTER passing_score_percent',
    'DO 0'
);
PREPARE add_reveal_correct_answers_statement FROM @add_reveal_correct_answers_sql;
EXECUTE add_reveal_correct_answers_statement;
DEALLOCATE PREPARE add_reveal_correct_answers_statement;

-- End of schema.
