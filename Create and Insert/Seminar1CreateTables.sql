-- =============================================================
-- DEPARTMENT
-- =============================================================
CREATE TABLE department (
	department_id INT PRIMARY KEY,
	department_name CHAR(10) UNIQUE NOT NULL
);
-- =============================================================
-- PERSON
-- =============================================================
CREATE TABLE person (
	person_id INT PRIMARY KEY,
	personal_number VARCHAR(12) UNIQUE NOT NULL,
	first_name CHAR(30),
	last_name CHAR(30),
	phone_number CHAR(10),
	address CHAR(50)
);
-- =============================================================
-- JOB TITLE
-- =============================================================
CREATE TABLE job_title (
	job_title_id INT PRIMARY KEY,
	job_title CHAR(20) UNIQUE NOT NULL
);
-- =============================================================
-- EMPLOYEE
-- =============================================================
CREATE TABLE employee (
	employment_id INT PRIMARY KEY,
	skill_set CHAR(100),
	salary INT,
	supervisor_manager BOOLEAN,
	job_title_id INT NOT NULL,
	person_id INT NOT NULL,
	department_id INT NOT NULL,
	max_allocations INT DEFAULT 4 NOT NULL,

	FOREIGN KEY (job_title_id) REFERENCES job_title(job_title_id),
	FOREIGN KEY (person_id) REFERENCES person(person_id),
	FOREIGN KEY (department_id) REFERENCES department(department_id)
);
-- =============================================================
-- COURSE LAYOUT
-- =============================================================
CREATE TABLE course_layout (
	course_layout_id INT PRIMARY KEY,
	course_code VARCHAR(6) UNIQUE NOT NULL,
	course_name CHAR(50) NOT NULL,
	min_students INT NOT NULL,
	max_students INT NOT NULL,
	hp INT NOT NULL,
	department_id INT NOT NULL,
	FOREIGN KEY (department_id) REFERENCES department(department_id),
	-- enforce logical student limits
	CHECK (min_students >= 0),
	CHECK (max_students >= 0),
	CHECK (min_students <= max_students)
);
-- =============================================================
-- COURSE INSTANCE
-- =============================================================
CREATE TABLE course_instance (
	course_instance_id INT PRIMARY KEY,
	num_students INT,
	study_year INT NOT NULL,
	study_period INT NOT NULL,
	course_layout_id INT NOT NULL,
	FOREIGN KEY (course_layout_id) REFERENCES course_layout(course_layout_id)
);
-- =============================================================
-- TEACHING ACTIVITY
-- =============================================================
CREATE TABLE teaching_activity (
	activity_id INT PRIMARY KEY,
	activity_name CHAR(10) UNIQUE NOT NULL,
	factor DECIMAL(10,2)
);
-- =============================================================
-- PLANNED ACTIVITY
-- =============================================================
CREATE TABLE planned_activity (
	planned_activity_id INT PRIMARY KEY,
	planned_hours INT,
	activity_id INT NOT NULL,
	course_instance_id INT NOT NULL,
	FOREIGN KEY (activity_id) REFERENCES teaching_activity(activity_id),
	FOREIGN KEY (course_instance_id) REFERENCES course_instance(course_instance_id)
);
-- =============================================================
-- ALLOCATION
-- =============================================================
CREATE TABLE allocation (
	planned_activity_id INT NOT NULL,
	employment_id INT NOT NULL,
	PRIMARY KEY (planned_activity_id, employment_id),
	FOREIGN KEY (planned_activity_id) REFERENCES planned_activity(planned_activity_id),
	FOREIGN KEY (employment_id) REFERENCES employee(employment_id)
);
-- =============================================================
--   TRIGGER FUNCTION: CHECK MAX ALLOCATIONS PER PERIOD
-- =============================================================
CREATE OR REPLACE FUNCTION check_max_allocations()
RETURNS TRIGGER AS $$
DECLARE
	allowed     INT;
	period      INT;
	current_count INT;
BEGIN
	-- 1. Check employee exists and get max_allocations	
	SELECT max_allocations INTO allowed
	FROM employee
	WHERE employment_id = NEW.employment_id;

	IF NOT FOUND THEN
		RAISE EXCEPTION 'Employee with employment_id % does not exist.', NEW.employment_id;
	END IF;

    -- 2. Get study period of the planned activity being allocated
    SELECT ci.study_period INTO period
    FROM planned_activity pa
    JOIN course_instance ci ON pa.course_instance_id = ci.course_instance_id
    WHERE pa.planned_activity_id = NEW.planned_activity_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Planned activity % not found or has no course instance.', NEW.planned_activity_id;
    END IF;

    -- 3. Count allocations this employee already has in this period
    SELECT COUNT(*) INTO current_count
    FROM allocation a
    JOIN planned_activity pa ON a.planned_activity_id = pa.planned_activity_id
    JOIN course_instance ci ON pa.course_instance_id = ci.course_instance_id
    WHERE a.employment_id = NEW.employment_id
      AND ci.study_period = period;

    -- On UPDATE: don't count the row we're currently modifying
    IF TG_OP = 'UPDATE' THEN
        current_count := current_count - 1;
    END IF;

    -- 4. Enforce limit
    IF current_count >= allowed THEN
        RAISE EXCEPTION
            'Employee % already has % allocation(s) in study period % (maximum allowed: %)',
            NEW.employment_id, current_count, period, allowed;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =============================================================
--   TRIGGER ON ALLOCATION TABLE
-- =============================================================
CREATE TRIGGER trg_check_max_allocations
BEFORE INSERT OR UPDATE ON allocation
FOR EACH ROW
EXECUTE FUNCTION check_max_allocations();

-- =============================================================
-- ENFORCE: At most ONE manager per department
-- (This completely replaces the need for another trigger)
-- =============================================================
CREATE UNIQUE INDEX idx_one_manager_per_department
    ON employee (department_id)
    WHERE supervisor_manager = TRUE;
