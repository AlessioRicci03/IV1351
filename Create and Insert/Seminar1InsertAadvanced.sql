-- =============================================================
-- INSERT TEST DATA – 10 rows per table (realistic & consistent)
-- =============================================================

-- 1. Departments
INSERT INTO department (department_name) VALUES
('Math'), ('Physics'), ('Chemistry'), ('Biology'), ('CS'),
('History'), ('Literature'), ('Economics'), ('Law'), ('Medicine');

-- 2. Job titles
INSERT INTO job_title (job_title) VALUES
('Professor'), ('Associate Prof'), ('Assistant Prof'), ('Lecturer'),
('Senior Lecturer'), ('Postdoc'), ('PhD Student'), ('Adjunct'),
('Researcher'), ('Lab Engineer');

-- 3. Persons
INSERT INTO person (personal_number, first_name, last_name, phone_number, address) VALUES
('197505123456', 'Anna', 'Andersson', '0701234561', 'Storgatan 1, Lund'),
('198102287890', 'Erik', 'Bergström', '0702345672', 'Kungsgatan 5, Malmö'),
('196812031122', 'Maria', 'Carlsson', '0703456783', 'Lilla Torg 12, Lund'),
('199003152233', 'Johan', 'Dahl', '0704567894', 'Södergatan 8, Malmö'),
('197809223344', 'Lisa', 'Ek', '0705678905', 'Universitetsgatan 3'),
('198511074455', 'Olof', 'Frisk', '0706789016', 'Stortorget 10, Lund'),
('199112195566', 'Karin', 'Gunnarsson', '0707890127', 'Botaniska 4, Lund'),
('196403306677', 'Per', 'Holm', '0708901238', 'Fysikgatan 22'),
('198707117788', 'Sofia', 'Ivarsson', '0709012349', 'Kemi vägen 15'),
('197212258899', 'Tomas', 'Jönsson', '0710123450', 'Biologiska 7');

-- 4. Employees (references person_id and department_id generated above)
INSERT INTO employee (skill_set, supervisor_manager, job_title_id, person_id, department_id) VALUES
('Calculus, Linear Algebra', TRUE,  1, 1, 1),  -- Anna → Math manager
('Quantum Mechanics',       FALSE, 1, 2, 2),
('Organic Chemistry',       TRUE,  1, 3, 3),  -- Maria → Chemistry manager
('Algorithms, Databases',   FALSE, 2, 4, 5),
('Ecology, Genetics',       TRUE,  1, 5, 4),  -- Lisa → Biology manager
('Ancient History',         FALSE, 4, 6, 6),
('Modern Literature',       TRUE,  2, 7, 7),  -- Karin → Literature manager
('Thermodynamics',          FALSE, 1, 8, 2),
('Civil Law',               TRUE,  1, 9, 9),  -- Sofia → Law manager
('Medicine, Surgery',       TRUE,  1, 10, 10); -- Tomas → Medicine manager

-- 5. Teaching activities
INSERT INTO teaching_activity (activity_name, factor) VALUES
('Lecture',    1.0),
('Exercise',   0.8),
('Lab',        1.2),
('Seminar',    0.9),
('Project',    1.5),
('Exam',       1.0),
('FieldStudy', 1.3),
('Tutorial',   0.7),
('Workshop',   1.1),
('GuestLect',  1.0);

-- 6. Course layouts
INSERT INTO course_layout (
    course_code, version_number, course_name, hp, min_students, max_students,
    valid_from, is_current, department_id
) VALUES
('MAT101', 1, 'Calculus I',          7.5,  15, 120, '2023-01-01', TRUE,  1),
('MAT201', 1, 'Linear Algebra',     7.5,  20, 100, '2023-01-01', TRUE,  1),
('FYS101', 1, 'Mechanics',          7.5,  10,  80, '2023-01-01', TRUE,  2),
('KEM101', 1, 'General Chemistry', 15.0,  25, 150, '2023-01-01', FALSE, 3),
('KEM101', 2, 'General Chemistry II',15.0, 20, 140, '2025-01-01', TRUE,  3),
('BIO101', 1, 'Cell Biology',        7.5,  15,  90, '2023-01-01', TRUE,  4),
('DAT101', 1, 'Programming I',      7.5,  30, 300, '2023-01-01', TRUE,  5),
('HIS101', 1, 'World History',      7.5,  10,  60, '2023-01-01', TRUE,  6),
('LIT101', 1, 'Swedish Literature', 7.5,  12,  50, '2023-01-01', TRUE,  7),
('LAW101', 1, 'Introduction to Law',15.0,  20, 200, '2023-01-01', TRUE,  9);

-- 7. Course instances
INSERT INTO course_instance (num_students, study_year, study_period, course_layout_id) VALUES
(98,  2024, 1, 1), -- MAT101 2024 P1
(75,  2024, 2, 2), -- MAT201 2024 P2
(62,  2025, 1, 1), -- MAT101 2025 P1 (same version)
(45,  2025, 1, 3), -- FYS101
(132, 2025, 1, 5), -- KEM101 new version
(88,  2025, 1, 6), -- BIO101
(280, 2025, 1, 7), -- DAT101
(42,  2025, 2, 8), -- HIS101
(38,  2025, 2, 9), -- LIT101
(165, 2025, 2, 10);-- LAW101

-- 8. Planned activities
INSERT INTO planned_activity (planned_hours, activity_id, course_instance_id) VALUES
(40, 1, 1), -- Lecture MAT101 2024
(20, 2, 1), -- Exercise MAT101 2024
(12, 3, 5), -- Lab KEM101 2025
(36, 1, 3), -- Lecture MAT101 2025
(28, 2, 3), -- Exercise MAT101 2025
(45, 1, 7), -- Lecture DAT101
(18, 8, 7), -- Tutorial DAT101
(30, 1, 10),-- Lecture LAW101
(8,  6, 10),-- Exam LAW101
(15, 4, 9); -- Seminar LIT101

-- 9. Allocations (respects max 4 per period)
INSERT INTO allocation (planned_activity_id, employment_id) VALUES
(1, 1), -- Anna: old MAT101 2024
(4, 1), -- Anna: MAT101 2025
(5, 1), -- Anna: Exercise MAT101 2025
(6, 4), -- Johan: DAT101 lecture
(7, 4), -- Johan: DAT101 tutorial
(3, 3), -- Maria: KEM101 lab
(8, 9), -- Sofia: LAW101 lecture
(9, 9), -- Sofia: LAW101 exam
(10,7), -- Karin: LIT101 seminar
(2, 2); -- Erik: Exercise MAT101 2024

-- 10. Allocation rule
INSERT INTO allocation_rule (rule_name, max_allocations) VALUES
('allocation_limit', 4);

-- 11. Salary history
INSERT INTO employee_salary_history (employment_id, salary, valid_from, is_current) VALUES
(1, 65000, '2023-01-01', FALSE),
(1, 72000, '2025-01-01', TRUE),
(2, 58000, '2023-01-01', TRUE),
(3,70000, '2023-01-01', TRUE),
(4,48000, '2024-06-01', TRUE),
(5,68000, '2023-01-01', FALSE),
(5,74000, '2025-03-01', TRUE),
(6,42000, '2023-01-01', TRUE),
(7,61000, '2023-01-01', TRUE),
(9,69000, '2023-01-01', TRUE),
(10,85000,'2023-01-01', TRUE);

