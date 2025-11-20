-- =============================================================
-- INSERT TEST DATA – 10 rows per table (realistic & consistent)
-- =============================================================

-- 1. Departments (10)
INSERT INTO department (department_id, department_name) VALUES
(1, 'Math'),
(2, 'Physics'),
(3, 'Chemistry'),
(4, 'Biology'),
(5, 'CS'),
(6, 'History'),
(7, 'Literature'),
(8, 'Economics'),
(9, 'Law'),
(10, 'Medicine');

-- 2. Job titles (10)
INSERT INTO job_title (job_title_id, job_title) VALUES
(1, 'Professor'),
(2, 'Associate Prof'),
(3, 'Assistant Prof'),
(4, 'Lecturer'),
(5, 'Senior Lecturer'),
(6, 'Postdoc'),
(7, 'PhD Student'),
(8, 'Adjunct'),
(9, 'Researcher'),
(10, 'Lab Engineer');

-- 3. Persons (10 employees)
INSERT INTO person (person_id, personal_number, first_name, last_name, phone_number, address) VALUES
(1, '197505123456', 'Anna',  'Andersson',   '0701234561', 'Storgatan 1, Lund'),
(2, '198102287890', 'Erik',  'Bergström',   '0702345672', 'Kungsgatan 5, Malmö'),
(3, '196812031122', 'Maria', 'Carlsson',    '0703456783', 'Lilla Torg 12, Lund'),
(4, '199003152233', 'Johan', 'Dahl',        '0704567894', 'Södergatan 8, Malmö'),
(5, '197809223344', 'Lisa',  'Ek',          '0705678905', 'Universitetsgatan 3'),
(6, '198511074455', 'Olof',  'Frisk',       '0706789016', 'Stortorget 10, Lund'),
(7, '199112195566', 'Karin', 'Gunnarsson',  '0707890127', 'Botaniska 4, Lund'),
(8, '196403306677', 'Per',   'Holm',        '0708901238', 'Fysikgatan 22'),
(9, '198707117788', 'Sofia', 'Ivarsson',    '0709012349', 'Kemi vägen 15'),
(10,'197212258899', 'Tomas', 'Jönsson',     '0710123450', 'Biologiska 7');

-- 4. Employees (10) – one manager per department (the first in each dept)
INSERT INTO employee (employment_id, skill_set, supervisor_manager, job_title_id, person_id, department_id, max_allocations) VALUES
(1, 'Calculus, Linear Algebra',        TRUE,  1, 1, 1, 4),  -- Anna = manager of Math
(2, 'Quantum Mechanics',              FALSE, 1, 2, 2, 4),
(3, 'Organic Chemistry',              TRUE,  1, 3, 3, 4),  -- Maria = manager of Chemistry
(4, 'Algorithms, Databases',           FALSE, 2, 4, 5, 5),
(5, 'Ecology, Genetics',               TRUE,  1, 5, 4, 4),  -- Lisa = manager of Biology
(6, 'Ancient History',                 FALSE, 4, 6, 6, 4),
(7, 'Modern Literature',               TRUE,  2, 7, 7, 4),  -- Karin = manager of Literature
(8, 'Thermodynamics',                  FALSE, 1, 8, 2, 4),
(9, 'Civil Law',                       TRUE,  1, 9, 9, 4),  -- Sofia = manager of Law
(10,'Medicine, Surgery',               TRUE,  1, 10,10, 4); -- Tomas = manager of Medicine

-- 5. Teaching activities (10 typical ones)
INSERT INTO teaching_activity (activity_id, activity_name, factor) VALUES
(1, 'Lecture',      1.0),
(2, 'Exercise',     0.8),
(3, 'Lab',          1.2),
(4, 'Seminar',      0.9),
(5, 'Project',      1.5),
(6, 'Exam',         1.0),
(7, 'FieldStudy',  1.3),
(8, 'Tutorial',     0.7),
(9, 'Workshop',     1.1),
(10,'GuestLect',   1.0);

-- 6. Course layouts – 10 different courses (some have 2 versions)
INSERT INTO course_layout (
    course_layout_id, course_code, version_number, course_name, hp, min_students, max_students,
    valid_from, valid_to, is_current, department_id
) VALUES
(1, 'MAT101', 1, 'Calculus I',           7.5, 15, 120, '2023-01-01', NULL, TRUE,  1),
(2, 'MAT201', 1, 'Linear Algebra',       7.5, 20, 100, '2023-01-01', NULL, TRUE,  1),
(3, 'FYS101', 1, 'Mechanics',            7.5, 10,  80, '2023-01-01', NULL, TRUE,  2),
(4, 'KEM101', 1, 'General Chemistry',    15.0, 25, 150, '2023-01-01', '2024-12-31', FALSE, 3),
(5, 'KEM101', 2, 'General Chemistry II',  15.0, 20, 140, '2025-01-01', NULL, TRUE,  3),  -- new version
(6, 'BIO101', 1, 'Cell Biology',         7.5, 15,  90, '2023-01-01', NULL, TRUE,  4),
(7, 'DAT101', 1, 'Programming I',        7.5, 30, 300, '2023-01-01', NULL, TRUE,  5),
(8, 'HIS101', 1, 'World History',       7.5, 10,  60, '2023-01-01', NULL, TRUE,  6),
(9, 'LIT101', 1, 'Swedish Literature',   7.5, 12,  50, '2023-01-01', NULL, TRUE,  7),
(10,'LAW101', 1, 'Introduction to Law',  15.0, 20, 200, '2023-01-01', NULL, TRUE,  9);

-- 7. Course instances – 10 offerings (2024 and 2025)
INSERT INTO course_instance (course_instance_id, num_students, study_year, study_period, course_layout_id) VALUES
(1,  98, 2024, 1, 1),   -- MAT101 spring 2024
(2,  75, 2024, 2, 2),   -- MAT201 fall 2024
(3,  62, 2025, 1, 1),   -- MAT101 spring 2025 (same version)
(4,  45, 2025, 1, 3),   -- FYS101
(5, 132, 2025, 1, 5),   -- KEM101 new version
(6,  88, 2025, 1, 6),   -- BIO101
(7, 280, 2025, 1, 7),   -- DAT101
(8,  42, 2025, 2, 8),   -- HIS101 fall
(9,  38, 2025, 2, 9),   -- LIT101 fall
(10, 165, 2025, 2, 10); -- LAW101 fall

-- 8. Planned activities – 10 different activities across the instances
INSERT INTO planned_activity (planned_activity_id, planned_hours, activity_id, course_instance_id) VALUES
(1,  40, 1, 1),  -- 40h Lectures MAT101 2024
(2,  20, 2, 1),  -- 20h Exercises MAT101 2024
(3,  12, 3, 5),  -- 12h Labs KEM101 2025
(4,  36, 1, 3),  -- Lectures MAT101 2025
(5,  28, 2, 3),  -- Exercises MAT101 2025
(6,  45, 1, 7),  -- Lectures DAT101 2025
(7,  18, 8, 7),  -- Tutorials DAT101
(8,  30, 1, 10), -- Lectures LAW101
(9,   8, 6, 10), -- Exam LAW101
(10, 15, 4, 9);  -- Seminar Literature

-- 9. Allocations – assign employees to activities (respects max 4 per period!)
-- Period 1 2025: Anna (emp 1) gets 4 activities → max reached
INSERT INTO allocation (planned_activity_id, employment_id) VALUES
(1, 1),   -- Anna lectures old MAT101 (but it’s 2024 → different period)
(4, 1),   -- Anna lectures new MAT101 2025
(5, 1),   -- Anna exercises MAT101 2025
(6, 4),   -- Johan lectures DAT101 (CS dept employee)
(7, 4),   -- Johan tutorials DAT101
(3, 3),   -- Maria labs KEM101 (Chemistry manager)
(8, 9),   -- Sofia lectures LAW101
(9, 9),   -- Sofia exam LAW101
(10, 7),  -- Karin seminar Literature
(2, 2);   -- Erik gets one exercise group in 2024 (Physics employee, period 1 2024)

-- 10. Salary history – 10 rows (some employees have raises)
INSERT INTO employee_salary_history (salary_history_id, employment_id, salary, valid_from, is_current) VALUES
(1, 1, 65000, '2023-01-01', FALSE),
(2, 1, 72000, '2025-01-01', TRUE),   -- Anna raise 2025
(3, 2, 58000, '2023-01-01', TRUE),
(4, 3, 70000, '2023-01-01', TRUE),
(5, 4, 48000, '2024-06-01', TRUE),
(6, 5, 68000, '2023-01-01', FALSE),
(7, 5, 74000, '2025-03-01', TRUE),   -- Lisa raise
(8, 6, 42000, '2023-01-01', TRUE),
(9, 7, 61000, '2023-01-01', TRUE),
(10,9, 69000, '2023-01-01', TRUE),
(11,10,85000, '2023-01-01', TRUE);