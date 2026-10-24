CREATE DATABASE IF NOT EXISTS kec_academic
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE kec_academic;

CREATE TABLE IF NOT EXISTS students (
    student_id BIGINT NOT NULL AUTO_INCREMENT,
    roll_number VARCHAR(20) NOT NULL,
    student_name VARCHAR(120) NOT NULL,
    department VARCHAR(40) NOT NULL,
    current_semester TINYINT NOT NULL,
    PRIMARY KEY (student_id),
    UNIQUE KEY uq_students_roll_number (roll_number),
    CONSTRAINT chk_students_semester CHECK (current_semester BETWEEN 1 AND 8)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS courses (
    course_id BIGINT NOT NULL AUTO_INCREMENT,
    course_code VARCHAR(20) NOT NULL,
    course_title VARCHAR(160) NOT NULL,
    credits DECIMAL(4,1) NOT NULL,
    course_type VARCHAR(12) NOT NULL,
    PRIMARY KEY (course_id),
    UNIQUE KEY uq_courses_course_code (course_code),
    CONSTRAINT chk_courses_credits CHECK (credits > 0),
    CONSTRAINT chk_courses_type CHECK (course_type IN ('THEORY', 'PRACTICAL', 'EMBEDDED'))
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS enrollments (
    enrollment_id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    semester TINYINT NOT NULL,
    classes_held INT NOT NULL DEFAULT 0,
    classes_attended INT NOT NULL DEFAULT 0,
    PRIMARY KEY (enrollment_id),
    UNIQUE KEY uq_enrollment_student_course_semester (student_id, course_id, semester),
    KEY ix_enrollments_semester (semester),
    CONSTRAINT fk_enrollments_student FOREIGN KEY (student_id)
        REFERENCES students (student_id),
    CONSTRAINT fk_enrollments_course FOREIGN KEY (course_id)
        REFERENCES courses (course_id),
    CONSTRAINT chk_enrollments_semester CHECK (semester BETWEEN 1 AND 8),
    CONSTRAINT chk_enrollments_classes_held CHECK (classes_held >= 0),
    CONSTRAINT chk_enrollments_classes_attended CHECK (
        classes_attended >= 0 AND classes_attended <= classes_held
    )
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS assessment_marks (
    enrollment_id BIGINT NOT NULL,
    cia_test_1 DECIMAL(5,2) NULL,
    cia_test_2 DECIMAL(5,2) NULL,
    phase_2_score DECIMAL(5,2) NULL,
    ese_mark DECIMAL(5,2) NULL,
    ese_absent BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (enrollment_id),
    CONSTRAINT fk_assessment_marks_enrollment FOREIGN KEY (enrollment_id)
        REFERENCES enrollments (enrollment_id) ON DELETE CASCADE,
    CONSTRAINT chk_marks_cia_1 CHECK (cia_test_1 IS NULL OR cia_test_1 BETWEEN 0 AND 100),
    CONSTRAINT chk_marks_cia_2 CHECK (cia_test_2 IS NULL OR cia_test_2 BETWEEN 0 AND 100),
    CONSTRAINT chk_marks_phase_2 CHECK (phase_2_score IS NULL OR phase_2_score BETWEEN 0 AND 100),
    CONSTRAINT chk_marks_ese CHECK (ese_mark IS NULL OR ese_mark BETWEEN 0 AND 100),
    CONSTRAINT chk_marks_ese_absence CHECK (NOT ese_absent OR ese_mark IS NULL)
) ENGINE = InnoDB;
