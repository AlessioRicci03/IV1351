-- =============================================================
--  UTF-8 SAFE INSERT SCRIPT
--  10+ realistic entries in every table
--  Compatible with your exact schema (triggers, constraints, unique index)
-- =============================================================

-- 1. DEPARTMENT
INSERT INTO department (department_id, department_name) VALUES
(1, 'Math'),
(2, 'Physics'),
(3, 'Chemistry'),
(4, 'Biology'),
(5, 'CPU Sci'),
(6, 'History'),
(7, 'Literature'),
(8, 'Economics'),
(9, 'Psychology'),
(10,'Philosophy');

-- 2. PERSON
INSERT INTO person (person_id, personal_number, first_name, last_name, phone_number, address) VALUES
(1, '197512151234', 'Anna',     'Andersson',  '0701234567', 'Storgatan 12, Stockholm'),
(2, '198303035678', 'Erik',     'Bergstrom',  '0729876543', 'Kungsgatan 5, Goteborg'),
(3, '196809219876', 'Maria',    'Lindgren',   '0731112233', 'Drottninggatan 8, Malmo'),
(4, '199011111111', 'Lars',     'Nilsson',    '0765554433', 'Vasagatan 22, Uppsala'),
(5, '197809302222', 'Karin',    'Olsson',     '0709998877', 'Ostermalmstorg 1, Stockholm'),
(6, '198512253333', 'Olof',     'Pettersson', '0732223344', 'Sodermalm, Stockholm'),
(7, '199203144444', 'Sofia',    'Jonsson',    '0798877665', 'Gamla Stan 3, Stockholm'),
(8, '197707075555', 'Johan',    'Karlsson',   '0721112233', 'Lundagatan 45, Lund'),
(9, '198811236666', 'Emma',     'Svensson',   '0705556677', 'Linkopingsvagen 10'),
(10,'199112017777','Peter',    'Wikstrom',   '0739988776', 'Bergsgatan 7, Umea');

-- 3. JOB TITLE
INSERT INTO job_title (job_title_id, job_title) VALUES
(1, 'Professor'),
(2, 'Associate Professor'),
(3, 'Senior Lecturer'),
(4, 'Lecturer'),
(5, 'Postdoc'),
(6, 'PhD Student'),
(7, 'Research Engineer'),
(8, 'Teaching Assistant'),
(9, 'Adjunct Professor'),
(10,'Lab Technician');

-- 4. EMPLOYEE (one manager per department)
INSERT INTO employee (employment_id, skill_set, salary, supervisor_manager,
                      job_title_id, person_id, department_id, max_allocations) VALUES
(1,  'Calculus, Linear Algebra',        95000, true,  1, 1,  1, 4), -- Anna  (Math manager)
(2,  'Statistics, R, Python',           68000, false, 3, 2,  1, 6),

(3,  'Quantum Mechanics',              98000, true,  1, 3,  2, 4), -- Maria (Physics manager)
(4,  'Thermodynamics, Optics',          62000, false, 4, 4,  2, 5),

(5,  'Organic Chemistry',              92000, true,  2, 5,  3, 4), -- Karin (Chemistry manager)
(6,  'Inorganic Chemistry',            59000, false, 5, 6,  3, 5),

(7,  'Genetics, Bioinformatics',       90000, true,  1, 7,  4, 4), -- Sofia (Biology manager)
(8,  'Microbiology, Ecology',           64000, false, 4, 8,  4, 6),

(9,  'Algorithms, AI',                 99000, true,  1, 9,  5, 4), -- Emma  (CS manager)
(10, 'Databases, Web Development',     71000, false, 3, 10, 5, 7);

-- 5. COURSE LAYOUT
INSERT INTO course_layout (course_layout_id, course_code, course_name,
                           min_students, max_students, hp, department_id) VALUES
(1, 'MAT101', 'Calculus I',              15, 120, 7.5, 1),
(2, 'PHY201', 'Classical Mechanics',    10,  80, 7.5, 2),
(3, 'CHE301', 'Organic Chemistry I',    12,  90, 7.5, 3),
(4, 'BIO401', 'Genetics',               10,  70, 7.5, 4),
(5, 'CS150',  'Programming Fundamentals',30, 200, 7.5, 5),
(6, 'HIS101', 'Modern History',         10, 100, 7.5, 6),
(7, 'LIT202', 'Literature Analysis',    15,  80, 7.5, 7),
(8, 'ECO101', 'Microeconomics',         20, 150, 7.5, 8),
(9, 'PSY201', 'Cognitive Psychology',   12,  90, 7.5, 9),
(10,'PHI301','Philosophy of Science',    8,  60, 7.5, 10);

-- 6. COURSE INSTANCE (all in 2025 period 2)
INSERT INTO course_instance (course_instance_id, num_students, study_year, study_period, course_layout_id) VALUES
(1, 78, 2025, 2, 1),
(2, 52, 2025, 2, 2),
(3, 68, 2025, 2, 3),
(4, 59, 2025, 2, 4),
(5,165, 2025, 2, 5),
(6, 87, 2025, 2, 6),
(7, 71, 2025, 2, 7),
(8,112, 2025, 2, 8),
(9, 63, 2025, 2, 9),
(10,41,2025, 2,10);

-- 7. TEACHING ACTIVITY
INSERT INTO teaching_activity (activity_id, activity_name, factor) VALUES
(1, 'Lecture',      1.00),
(2, 'Exercise',     0.75),
(3, 'Laboratory',   1.50),
(4, 'Seminar',      1.20),
(5, 'Project',      2.00),
(6, 'Exam',         1.00),
(7, 'Tutorial',     0.90),
(8, 'Workshop',     1.40),
(9, 'FieldStudy',   2.50),
(10,'GuestLect',    1.10);

-- 8. PLANNED ACTIVITY (one main activity per course instance)
INSERT INTO planned_activity (planned_activity_id, planned_hours, activity_id, course_instance_id) VALUES
(1, 28, 1, 1),	
(2, 20, 1, 2),
(3, 24, 3, 3),
(4, 30, 1, 4),
(5, 40, 1, 5),
(6, 18, 4, 6),
(7, 22, 1, 7),
(8, 26, 1, 8),
(9, 20, 4, 9),
(10,16, 1,10);

-- 9. ALLOCATION (respects max_allocations and same study period)
INSERT INTO allocation (planned_activity_id, employment_id) VALUES
(1, 1), 
(1, 2),           
(2, 3), 
(2, 4),           
(3, 5), 
(3, 6),
(4, 7), 
(4, 8),
(5, 9),
(5,10);