-- LearnLoop development seed data
-- Target: MySQL 8.0+ / MySQL 8.4 LTS
--
-- DEVELOPMENT ONLY. The seeded teacher and student accounts use the password:
--   LearnLoopSeed#2026
--
-- The password hashes are BCrypt hashes for that development password. Do not
-- reuse these accounts or hashes in a production environment.
--
-- No administrator account is seeded. Create the first administrator with the
-- one-time bootstrap command documented in README.md. This keeps default
-- administrator credentials out of the repository and deployed databases.
--
-- This script is idempotent. It inserts missing seed rows and does not reset
-- statuses or overwrite data that a developer has changed. Run it only against
-- a development database.
--
-- Development invitation tokens (only their SHA-256 hashes are stored):
--   Algebra Foundations: learnloop-dev-invite-math-class-2026
--   Intro to Programming: learnloop-dev-invite-cs-class-2026
--
-- Usage:
--   mysql -u root -p -e "source database/seed.sql"

USE learnloop_test_db;

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- Users
-- ---------------------------------------------------------------------------

-- Seed rows reference an administrator when one is present. A missing admin is
-- represented by NULL so a developer can bootstrap the first admin and rerun
-- this script without restoring a known default password.
SET @admin_id = (
    SELECT id FROM users
    WHERE role = 'ADMIN' AND approval_status = 'APPROVED'
    ORDER BY id
    LIMIT 1
);

INSERT INTO users (email, password_hash, full_name, role, approval_status, approved_at, approved_by_user_id)
VALUES (
    'teacher@learnloop.local',
    '$2a$12$4w94uD6YecafJkItwNd9ZeVrbunSAEDIFtedkp3O43dYy6Li6zxg2',
    'Maya Teacher',
    'TEACHER',
    'APPROVED',
    CURRENT_TIMESTAMP(6),
    @admin_id
)
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @teacher_id = (SELECT id FROM users WHERE email = 'teacher@learnloop.local');

INSERT INTO users (email, password_hash, full_name, role, approval_status)
VALUES (
    'pending.teacher@learnloop.local',
    '$2a$12$4w94uD6YecafJkItwNd9ZeVrbunSAEDIFtedkp3O43dYy6Li6zxg2',
    'Pending Teacher',
    'TEACHER',
    'PENDING'
)
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @pending_teacher_id = (SELECT id FROM users WHERE email = 'pending.teacher@learnloop.local');

INSERT INTO users (email, password_hash, full_name, role, approval_status, approved_at, approved_by_user_id)
VALUES (
    'alice.student@learnloop.local',
    '$2a$12$4w94uD6YecafJkItwNd9ZeVrbunSAEDIFtedkp3O43dYy6Li6zxg2',
    'Alice Student',
    'STUDENT',
    'APPROVED',
    CURRENT_TIMESTAMP(6),
    @admin_id
)
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @alice_id = (SELECT id FROM users WHERE email = 'alice.student@learnloop.local');

INSERT INTO users (email, password_hash, full_name, role, approval_status, approved_at, approved_by_user_id)
VALUES (
    'bob.student@learnloop.local',
    '$2a$12$4w94uD6YecafJkItwNd9ZeVrbunSAEDIFtedkp3O43dYy6Li6zxg2',
    'Bob Student',
    'STUDENT',
    'APPROVED',
    CURRENT_TIMESTAMP(6),
    @admin_id
)
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @bob_id = (SELECT id FROM users WHERE email = 'bob.student@learnloop.local');

-- ---------------------------------------------------------------------------
-- Subjects
-- ---------------------------------------------------------------------------

INSERT INTO subjects (code, name, description, is_active)
VALUES ('MATH', 'Mathematics', 'Algebra, geometry, data and mathematical reasoning.', TRUE)
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @math_subject_id = (SELECT id FROM subjects WHERE code = 'MATH');

INSERT INTO subjects (code, name, description, is_active)
VALUES ('PHYS', 'Physics', 'Mechanics, energy, waves and scientific reasoning.', TRUE)
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @physics_subject_id = (SELECT id FROM subjects WHERE code = 'PHYS');

INSERT INTO subjects (code, name, description, is_active)
VALUES ('CS', 'Computer Science', 'Programming fundamentals, algorithms and software design.', TRUE)
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @cs_subject_id = (SELECT id FROM subjects WHERE code = 'CS');

-- ---------------------------------------------------------------------------
-- Teacher-subject assignments
-- ---------------------------------------------------------------------------

INSERT INTO teacher_subject_assignments (teacher_user_id, subject_id, assigned_by_user_id, is_active)
VALUES (@teacher_id, @math_subject_id, @admin_id, TRUE)
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @teacher_math_assignment_id = (
    SELECT id FROM teacher_subject_assignments
    WHERE teacher_user_id = @teacher_id AND subject_id = @math_subject_id
);

INSERT INTO teacher_subject_assignments (teacher_user_id, subject_id, assigned_by_user_id, is_active)
VALUES (@teacher_id, @cs_subject_id, @admin_id, TRUE)
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @teacher_cs_assignment_id = (
    SELECT id FROM teacher_subject_assignments
    WHERE teacher_user_id = @teacher_id AND subject_id = @cs_subject_id
);

-- The pending teacher is assigned to Physics on purpose. Class creation must
-- still be rejected until the account is approved.
INSERT INTO teacher_subject_assignments (teacher_user_id, subject_id, assigned_by_user_id, is_active)
VALUES (@pending_teacher_id, @physics_subject_id, @admin_id, TRUE)
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);

-- ---------------------------------------------------------------------------
-- Classes and invitation hashes
-- ---------------------------------------------------------------------------

INSERT INTO classes (
    subject_id,
    teacher_user_id,
    title,
    description,
    invitation_token_hash,
    invitation_token_created_at,
    invitation_token_expires_at,
    status,
    max_students
)
VALUES (
    @math_subject_id,
    @teacher_id,
    'Algebra Foundations',
    'A development class covering expressions, equations and introductory functions.',
    '1fa9ceed494009cc3e3fada6dff630266f68f057bc938768a4af23c6a3a60d9e',
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6) + INTERVAL 365 DAY,
    'ACTIVE',
    25
)
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @math_class_id = (
    SELECT id FROM classes
    WHERE invitation_token_hash = '1fa9ceed494009cc3e3fada6dff630266f68f057bc938768a4af23c6a3a60d9e'
);

INSERT INTO classes (
    subject_id,
    teacher_user_id,
    title,
    description,
    invitation_token_hash,
    invitation_token_created_at,
    invitation_token_expires_at,
    status,
    max_students
)
VALUES (
    @cs_subject_id,
    @teacher_id,
    'Intro to Programming',
    'A development class introducing variables, control flow and small programs.',
    '4559c018c5dbe0d90694e8b106882dd773637d46b8e87a9211b4809855be2659',
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6) + INTERVAL 365 DAY,
    'ACTIVE',
    30
)
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @cs_class_id = (
    SELECT id FROM classes
    WHERE invitation_token_hash = '4559c018c5dbe0d90694e8b106882dd773637d46b8e87a9211b4809855be2659'
);

-- Example external live-session link for local development (the placeholder is
-- not a real Google Meet room). Teachers can replace it in the class workspace.
UPDATE classes
SET meeting_url = 'https://meet.google.com/learnloop-dev-session'
WHERE id = @math_class_id AND meeting_url IS NULL;

-- ---------------------------------------------------------------------------
-- Join requests and enrollments
-- ---------------------------------------------------------------------------

INSERT INTO class_join_requests (
    class_id,
    student_user_id,
    status,
    message,
    reviewed_by_user_id,
    reviewed_at
)
VALUES (
    @math_class_id,
    @alice_id,
    'APPROVED',
    'I would like to strengthen my algebra foundation.',
    @teacher_id,
    CURRENT_TIMESTAMP(6)
)
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @alice_math_request_id = (
    SELECT id FROM class_join_requests
    WHERE class_id = @math_class_id AND student_user_id = @alice_id
);

INSERT INTO class_join_requests (class_id, student_user_id, status, message)
VALUES (
    @math_class_id,
    @bob_id,
    'PENDING',
    'Please add me to the next algebra group.'
)
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @bob_math_request_id = (
    SELECT id FROM class_join_requests
    WHERE class_id = @math_class_id AND student_user_id = @bob_id
);

INSERT INTO class_enrollments (
    class_id,
    student_user_id,
    join_request_id,
    enrolled_by_user_id,
    status
)
VALUES (
    @math_class_id,
    @alice_id,
    @alice_math_request_id,
    @teacher_id,
    'ACTIVE'
)
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @alice_math_enrollment_id = (
    SELECT id FROM class_enrollments
    WHERE class_id = @math_class_id AND student_user_id = @alice_id
);

INSERT INTO class_enrollments (
    class_id,
    student_user_id,
    join_request_id,
    enrolled_by_user_id,
    status
)
VALUES (
    @cs_class_id,
    @bob_id,
    NULL,
    @teacher_id,
    'ACTIVE'
)
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @bob_cs_enrollment_id = (
    SELECT id FROM class_enrollments
    WHERE class_id = @cs_class_id AND student_user_id = @bob_id
);

-- ---------------------------------------------------------------------------
-- Class materials
-- ---------------------------------------------------------------------------

INSERT INTO class_materials (
    class_id,
    uploaded_by_user_id,
    title,
    description,
    material_type,
    resource_url,
    position,
    is_published,
    published_at
)
SELECT
    @math_class_id,
    @teacher_id,
    'Course syllabus',
    'Topics, expectations and the assessment schedule for this development class.',
    'LINK',
    'https://example.com/learnloop/algebra-syllabus',
    1,
    TRUE,
    CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (
    SELECT 1 FROM class_materials
    WHERE class_id = @math_class_id AND title = 'Course syllabus'
);

INSERT INTO class_materials (
    class_id,
    uploaded_by_user_id,
    title,
    description,
    material_type,
    content_text,
    position,
    is_published,
    published_at
)
SELECT
    @math_class_id,
    @teacher_id,
    'Formula sheet',
    'A short reference sheet used during practice.',
    'TEXT',
    'Distributive property: a(b + c) = ab + ac. Combine like terms before solving linear equations.',
    2,
    TRUE,
    CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (
    SELECT 1 FROM class_materials
    WHERE class_id = @math_class_id AND title = 'Formula sheet'
);

INSERT INTO class_materials (
    class_id,
    uploaded_by_user_id,
    title,
    description,
    material_type,
    resource_url,
    position,
    is_published,
    published_at
)
SELECT
    @cs_class_id,
    @teacher_id,
    'Starter repository',
    'Example programs and exercises for the first programming sessions.',
    'LINK',
    'https://example.com/learnloop/programming-starter',
    1,
    TRUE,
    CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (
    SELECT 1 FROM class_materials
    WHERE class_id = @cs_class_id AND title = 'Starter repository'
);

-- ---------------------------------------------------------------------------
-- Assessments and questions
-- ---------------------------------------------------------------------------

INSERT INTO assessments (
    class_id,
    created_by_user_id,
    title,
    instructions,
    status,
    available_from,
    due_at,
    time_limit_minutes,
    max_attempts,
    passing_score_percent
)
SELECT
    @math_class_id,
    @teacher_id,
    'Algebra Checkpoint 1',
    'Answer every question. Show your reasoning for the written response.',
    'PUBLISHED',
    CURRENT_TIMESTAMP(6) - INTERVAL 1 DAY,
    CURRENT_TIMESTAMP(6) + INTERVAL 14 DAY,
    30,
    2,
    60.00
WHERE NOT EXISTS (
    SELECT 1 FROM assessments
    WHERE class_id = @math_class_id AND title = 'Algebra Checkpoint 1'
);
SET @math_assessment_id = (
    SELECT id FROM assessments
    WHERE class_id = @math_class_id AND title = 'Algebra Checkpoint 1'
    LIMIT 1
);

INSERT INTO assessments (
    class_id,
    created_by_user_id,
    title,
    instructions,
    status,
    time_limit_minutes,
    max_attempts,
    passing_score_percent
)
SELECT
    @cs_class_id,
    @teacher_id,
    'Programming Basics Checkpoint',
    'This draft assessment is included to demonstrate the assessment workflow.',
    'DRAFT',
    20,
    1,
    70.00
WHERE NOT EXISTS (
    SELECT 1 FROM assessments
    WHERE class_id = @cs_class_id AND title = 'Programming Basics Checkpoint'
);
SET @cs_assessment_id = (
    SELECT id FROM assessments
    WHERE class_id = @cs_class_id AND title = 'Programming Basics Checkpoint'
    LIMIT 1
);

INSERT INTO questions (assessment_id, question_type, prompt, points, position, is_required)
SELECT
    @math_assessment_id,
    'SINGLE_CHOICE',
    'What is the value of 2x + 3 when x = 4?',
    5.00,
    1,
    TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM questions
    WHERE assessment_id = @math_assessment_id AND position = 1
);
SET @math_q1_id = (
    SELECT id FROM questions
    WHERE assessment_id = @math_assessment_id AND position = 1
    LIMIT 1
);

INSERT INTO questions (assessment_id, question_type, prompt, points, position, is_required)
SELECT
    @math_assessment_id,
    'MULTIPLE_CHOICE',
    'Which of these numbers are prime? Select all that apply.',
    5.00,
    2,
    TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM questions
    WHERE assessment_id = @math_assessment_id AND position = 2
);
SET @math_q2_id = (
    SELECT id FROM questions
    WHERE assessment_id = @math_assessment_id AND position = 2
    LIMIT 1
);

INSERT INTO questions (assessment_id, question_type, prompt, points, position, is_required)
SELECT
    @math_assessment_id,
    'SHORT_TEXT',
    'Explain the distributive property in one sentence.',
    10.00,
    3,
    TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM questions
    WHERE assessment_id = @math_assessment_id AND position = 3
);
SET @math_q3_id = (
    SELECT id FROM questions
    WHERE assessment_id = @math_assessment_id AND position = 3
    LIMIT 1
);

INSERT INTO questions (assessment_id, question_type, prompt, points, position, is_required)
SELECT
    @cs_assessment_id,
    'SINGLE_CHOICE',
    'Which Java keyword declares a constant?',
    5.00,
    1,
    TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM questions
    WHERE assessment_id = @cs_assessment_id AND position = 1
);
SET @cs_q1_id = (
    SELECT id FROM questions
    WHERE assessment_id = @cs_assessment_id AND position = 1
    LIMIT 1
);

-- ---------------------------------------------------------------------------
-- Question options
-- ---------------------------------------------------------------------------

INSERT INTO question_options (question_id, option_text, is_correct, position)
SELECT @math_q1_id, '9', FALSE, 1
WHERE NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = @math_q1_id AND option_text = '9');

INSERT INTO question_options (question_id, option_text, is_correct, position)
SELECT @math_q1_id, '10', FALSE, 2
WHERE NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = @math_q1_id AND option_text = '10');

INSERT INTO question_options (question_id, option_text, is_correct, position)
SELECT @math_q1_id, '11', TRUE, 3
WHERE NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = @math_q1_id AND option_text = '11');

INSERT INTO question_options (question_id, option_text, is_correct, position)
SELECT @math_q1_id, '12', FALSE, 4
WHERE NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = @math_q1_id AND option_text = '12');

INSERT INTO question_options (question_id, option_text, is_correct, position)
SELECT @math_q2_id, '2', TRUE, 1
WHERE NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = @math_q2_id AND option_text = '2');

INSERT INTO question_options (question_id, option_text, is_correct, position)
SELECT @math_q2_id, '3', TRUE, 2
WHERE NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = @math_q2_id AND option_text = '3');

INSERT INTO question_options (question_id, option_text, is_correct, position)
SELECT @math_q2_id, '4', FALSE, 3
WHERE NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = @math_q2_id AND option_text = '4');

INSERT INTO question_options (question_id, option_text, is_correct, position)
SELECT @math_q2_id, '5', TRUE, 4
WHERE NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = @math_q2_id AND option_text = '5');

INSERT INTO question_options (question_id, option_text, is_correct, position)
SELECT @cs_q1_id, 'final', TRUE, 1
WHERE NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = @cs_q1_id AND option_text = 'final');

INSERT INTO question_options (question_id, option_text, is_correct, position)
SELECT @cs_q1_id, 'const', FALSE, 2
WHERE NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = @cs_q1_id AND option_text = 'const');

INSERT INTO question_options (question_id, option_text, is_correct, position)
SELECT @cs_q1_id, 'static', FALSE, 3
WHERE NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = @cs_q1_id AND option_text = 'static');

INSERT INTO question_options (question_id, option_text, is_correct, position)
SELECT @cs_q1_id, 'let', FALSE, 4
WHERE NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = @cs_q1_id AND option_text = 'let');

SET @math_q1_option_11_id = (
    SELECT id FROM question_options
    WHERE question_id = @math_q1_id AND option_text = '11'
    LIMIT 1
);
SET @math_q2_option_2_id = (
    SELECT id FROM question_options
    WHERE question_id = @math_q2_id AND option_text = '2'
    LIMIT 1
);
SET @math_q2_option_3_id = (
    SELECT id FROM question_options
    WHERE question_id = @math_q2_id AND option_text = '3'
    LIMIT 1
);
SET @math_q2_option_5_id = (
    SELECT id FROM question_options
    WHERE question_id = @math_q2_id AND option_text = '5'
    LIMIT 1
);

-- ---------------------------------------------------------------------------
-- Graded assessment attempt and answers
-- ---------------------------------------------------------------------------

INSERT INTO assessment_attempts (
    assessment_id,
    student_user_id,
    attempt_number,
    status,
    started_at,
    submitted_at,
    graded_at,
    score,
    graded_by_user_id
)
VALUES (
    @math_assessment_id,
    @alice_id,
    1,
    'GRADED',
    CURRENT_TIMESTAMP(6) - INTERVAL 3 DAY,
    CURRENT_TIMESTAMP(6) - INTERVAL 2 DAY,
    CURRENT_TIMESTAMP(6) - INTERVAL 1 DAY,
    20.00,
    @teacher_id
)
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @alice_math_attempt_id = (
    SELECT id FROM assessment_attempts
    WHERE assessment_id = @math_assessment_id
      AND student_user_id = @alice_id
      AND attempt_number = 1
);

INSERT INTO assessment_answers (attempt_id, question_id, is_correct, points_awarded)
VALUES (@alice_math_attempt_id, @math_q1_id, TRUE, 5.00)
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @alice_math_q1_answer_id = (
    SELECT id FROM assessment_answers
    WHERE attempt_id = @alice_math_attempt_id AND question_id = @math_q1_id
);

INSERT INTO assessment_answers (attempt_id, question_id, is_correct, points_awarded)
VALUES (@alice_math_attempt_id, @math_q2_id, TRUE, 5.00)
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @alice_math_q2_answer_id = (
    SELECT id FROM assessment_answers
    WHERE attempt_id = @alice_math_attempt_id AND question_id = @math_q2_id
);

INSERT INTO assessment_answers (attempt_id, question_id, answer_text, is_correct, points_awarded)
VALUES (
    @alice_math_attempt_id,
    @math_q3_id,
    'Multiplying a sum by a number is the same as multiplying each addend and adding the products.',
    TRUE,
    10.00
)
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);

INSERT IGNORE INTO assessment_answer_options (answer_id, option_id, question_id)
VALUES (@alice_math_q1_answer_id, @math_q1_option_11_id, @math_q1_id);

INSERT IGNORE INTO assessment_answer_options (answer_id, option_id, question_id)
VALUES (@alice_math_q2_answer_id, @math_q2_option_2_id, @math_q2_id);

INSERT IGNORE INTO assessment_answer_options (answer_id, option_id, question_id)
VALUES (@alice_math_q2_answer_id, @math_q2_option_3_id, @math_q2_id);

INSERT IGNORE INTO assessment_answer_options (answer_id, option_id, question_id)
VALUES (@alice_math_q2_answer_id, @math_q2_option_5_id, @math_q2_id);

-- ---------------------------------------------------------------------------
-- Notifications
-- ---------------------------------------------------------------------------

INSERT INTO notifications (
    recipient_user_id,
    actor_user_id,
    type,
    title,
    message,
    link_url,
    related_entity_type,
    related_entity_id,
    is_read
)
SELECT
    @teacher_id,
    @bob_id,
    'JOIN_REQUEST',
    'New class join request',
    'Bob Student requested to join Algebra Foundations.',
    '/teacher/requests',
    'CLASS',
    @math_class_id,
    FALSE
WHERE NOT EXISTS (
    SELECT 1 FROM notifications
    WHERE recipient_user_id = @teacher_id
      AND type = 'JOIN_REQUEST'
      AND related_entity_type = 'CLASS'
      AND related_entity_id = @math_class_id
);

INSERT INTO notifications (
    recipient_user_id,
    actor_user_id,
    type,
    title,
    message,
    link_url,
    related_entity_type,
    related_entity_id,
    is_read,
    read_at
)
SELECT
    @alice_id,
    @teacher_id,
    'ASSESSMENT_GRADED',
    'Your assessment has been graded',
    'Algebra Checkpoint 1 was graded. Review your answers and feedback.',
    '/student/assessments',
    'ASSESSMENT',
    @math_assessment_id,
    TRUE,
    CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (
    SELECT 1 FROM notifications
    WHERE recipient_user_id = @alice_id
      AND type = 'ASSESSMENT_GRADED'
      AND related_entity_type = 'ASSESSMENT'
      AND related_entity_id = @math_assessment_id
);

-- ---------------------------------------------------------------------------
-- Audit logs
-- ---------------------------------------------------------------------------

INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, details, ip_address, user_agent)
SELECT
    @admin_id,
    'USER_APPROVED',
    'USER',
    @teacher_id,
    JSON_OBJECT('role', 'TEACHER', 'approvalStatus', 'APPROVED'),
    '127.0.0.1',
    'learnloop-seed'
WHERE NOT EXISTS (
    SELECT 1 FROM audit_logs
    WHERE action = 'USER_APPROVED' AND entity_type = 'USER' AND entity_id = @teacher_id
);

INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, details, ip_address, user_agent)
SELECT
    @teacher_id,
    'CLASS_CREATED',
    'CLASS',
    @math_class_id,
    JSON_OBJECT('subjectId', @math_subject_id, 'seed', TRUE),
    '127.0.0.1',
    'learnloop-seed'
WHERE NOT EXISTS (
    SELECT 1 FROM audit_logs
    WHERE action = 'CLASS_CREATED' AND entity_type = 'CLASS' AND entity_id = @math_class_id
);

INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, details, ip_address, user_agent)
SELECT
    @teacher_id,
    'CLASS_CREATED',
    'CLASS',
    @cs_class_id,
    JSON_OBJECT('subjectId', @cs_subject_id, 'seed', TRUE),
    '127.0.0.1',
    'learnloop-seed'
WHERE NOT EXISTS (
    SELECT 1 FROM audit_logs
    WHERE action = 'CLASS_CREATED' AND entity_type = 'CLASS' AND entity_id = @cs_class_id
);

-- End of seed data.
