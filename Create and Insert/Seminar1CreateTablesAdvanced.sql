-- =============================================================
--   DEPARTMENT
-- =============================================================
CREATE TABLE department (
    department_id    SERIAL PRIMARY KEY,
    department_name  CHAR(10) UNIQUE NOT NULL
);

-- =============================================================
--   PERSON
-- =============================================================
CREATE TABLE person (
    person_id        SERIAL PRIMARY KEY,
    personal_number  VARCHAR(12) UNIQUE NOT NULL,
    first_name       CHAR(30),
    last_name        CHAR(30),
    phone_number     CHAR(10),
    address          CHAR(50)
);

-- =============================================================
--   JOB TITLE
-- =============================================================
CREATE TABLE job_title (
    job_title_id   SERIAL PRIMARY KEY,
    job_title      CHAR(20) UNIQUE NOT NULL
);

-- =============================================================
--   EMPLOYEE
-- =============================================================
CREATE TABLE employee (
    employment_id     SERIAL PRIMARY KEY,,
    skill_set         CHAR(100),
    supervisor_manager BOOLEAN,
    job_title_id      INT NOT NULL,
    person_id         INT NOT NULL,
    department_id     INT NOT NULL,
    max_allocations   INT DEFAULT 4 NOT NULL,

    FOREIGN KEY (job_title_id) REFERENCES job_title(job_title_id),
    FOREIGN KEY (person_id) REFERENCES person(person_id),
    FOREIGN KEY (department_id) REFERENCES department(department_id)
);

-- =============================================================
-- 2. EMPLOYEE – now with salary history (only salary is versioned)
-- =============================================================
-- We keep one row per employee, but salary gets its own history table
CREATE TABLE employee_salary_history (
    salary_history_id  SERIAL PRIMARY KEY,,
    employment_id      INT NOT NULL,
    
    salary             INT NOT NULL,
    currency           CHAR(3) DEFAULT 'SEK' NOT NULL,
    
    valid_from         DATE NOT NULL DEFAULT CURRENT_DATE,
    valid_to           DATE,
    is_current         BOOLEAN NOT NULL DEFAULT TRUE,
    
    FOREIGN KEY (employment_id) REFERENCES employee(employment_id),
    
    UNIQUE (employment_id, valid_from)
);

-- Index to get current salary instantly
CREATE UNIQUE INDEX idx_current_salary ON employee_salary_history(employment_id) WHERE is_current = TRUE;
-- Example Query: SELECT salary FROM employee_salary_history WHERE employment_id = 1 AND is_current = TRUE;

-- =============================================================
-- NEW: COURSE_LAYOUT with versioning (replaces the old one)
-- =============================================================
DROP TABLE IF EXISTS course_instance;      -- must drop in correct order
DROP TABLE IF EXISTS course_layout;

CREATE TABLE course_layout (
    course_layout_id    SERIAL PRIMARY KEY,,  
    course_code         VARCHAR(6) NOT NULL,           
    version_number      INT NOT NULL DEFAULT 1,        -- 1, 2, 3,...
    course_name         CHAR(50) NOT NULL,
    hp                  DECIMAL(10,2) NOT NULL,         -- now can be 7.5 or 15.0
    min_students        INT NOT NULL,
    max_students        INT NOT NULL,
    valid_from          DATE NOT NULL DEFAULT CURRENT_DATE,
    valid_to            DATE,                          -- NULL = currently valid
    is_current          BOOLEAN NOT NULL DEFAULT TRUE,
    department_id       INT NOT NULL,
    
    FOREIGN KEY (department_id) REFERENCES department(department_id),
    
    -- Natural key + validity (makes sure that we never accidentally create conflicting versions)
    UNIQUE (course_code, valid_from),
    UNIQUE (course_code, version_number),
    
    CHECK (min_students >= 0),
    CHECK (max_students >= min_students),
    CHECK (valid_to IS NULL OR valid_to > valid_from)
);
-- Allows us to get the current version of course X
CREATE INDEX idx_course_layout_current ON course_layout(course_code) WHERE is_current = TRUE;
-- Example Query: SELECT * FROM course_layout WHERE course_code = 'MAT101' AND is_current = TRUE;

-- Allows us to show the full history of course X OR which version of the course was valid in date Y
CREATE INDEX idx_course_layout_valid ON course_layout(course_code, valid_from, valid_to);
-- Example Query: SELECT * FROM course_layout WHERE course_code = 'MAT101' AND '2025-03-01' BETWEEN valid_from AND COALESCE(valid_to, '9999-12-31');

-- =============================================================
--   COURSE INSTANCE
-- =============================================================
CREATE TABLE course_instance (
    course_instance_id   SERIAL PRIMARY KEY,,
    num_students         INT,
    study_year           INT NOT NULL,
    study_period         INT NOT NULL,
    
    -- Now points to the exact version that was valid during this period
    course_layout_id     INT NOT NULL,
    
    FOREIGN KEY (course_layout_id) REFERENCES course_layout(course_layout_id)
);

-- =============================================================
--   TEACHING ACTIVITY
-- =============================================================
CREATE TABLE teaching_activity (
    activity_id     SERIAL PRIMARY KEY,,
    activity_name   CHAR(10) UNIQUE NOT NULL,
    factor          DECIMAL(10,2)
);

-- =============================================================
--   PLANNED ACTIVITY
-- =============================================================
CREATE TABLE planned_activity (
    planned_activity_id  SERIAL PRIMARY KEY,,
    planned_hours        INT,
    activity_id          INT NOT NULL,
    course_instance_id   INT NOT NULL,

    FOREIGN KEY (activity_id)         REFERENCES teaching_activity(activity_id),
    FOREIGN KEY (course_instance_id)  REFERENCES course_instance(course_instance_id)
);

-- =============================================================
--   ALLOCATION
-- =============================================================
CREATE TABLE allocation (
    planned_activity_id  INT NOT NULL,
    employment_id        INT NOT NULL,

    PRIMARY KEY (planned_activity_id, employment_id),

    FOREIGN KEY (planned_activity_id) REFERENCES planned_activity(planned_activity_id),
    FOREIGN KEY (employment_id)       REFERENCES employee(employment_id)
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

-- =============================================================
-- Helper 1: Change a course layout (creates a new version)
-- SELECT change_course_layout('course code', 'new name', new_hp , new min students, new max students, 'change date');
-- =============================================================
CREATE OR REPLACE FUNCTION change_course_layout(
    p_course_code VARCHAR(6),
    p_new_name CHAR(50),
    p_new_hp DECIMAL(10,2),
    p_new_min_students INT,
    p_new_max_students INT,
    p_change_date DATE DEFAULT CURRENT_DATE
)
RETURNS void AS $$
DECLARE
    v_dept_id INT;
    v_new_version INT;
BEGIN
    -- Get current version and department
    SELECT department_id, MAX(version_number)
    INTO v_dept_id, v_new_version
    FROM course_layout
    WHERE course_code = p_course_code
    GROUP BY department_id;

    IF v_dept_id IS NULL THEN
        RAISE EXCEPTION 'No current version found for course code %', p_course_code;
    END IF;

    v_new_version := COALESCE(v_new_version, 0) + 1;

    -- 1. Close current version
    UPDATE course_layout
    SET is_current = FALSE,
        valid_to = p_change_date - INTERVAL '1 day'
    WHERE course_code = p_course_code
      AND is_current = TRUE;

    -- 2. Insert new version
    INSERT INTO course_layout (
        course_code, version_number, course_name, hp,
        min_students, max_students, valid_from, valid_to,
        is_current, department_id
    ) VALUES (
        p_course_code,
        v_new_version,
        p_new_name,
        p_new_hp,
        p_new_min_students,
        p_new_max_students,
        p_change_date,
        NULL,
        TRUE,
        v_dept_id
    );
END;
$$ LANGUAGE plpgsql;


-- =============================================================
-- Helper 2: Change an employee's salary (creates a new history row)
-- SELECT salary_change(employment id, news alary, 'change date');
-- =============================================================
CREATE OR REPLACE FUNCTION salary_change(
    p_employment_id INT,
    p_new_salary    INT,
    p_change_date   DATE DEFAULT CURRENT_DATE
) 
RETURNS void AS $$
BEGIN
    -- Close any current salary record
    UPDATE employee_salary_history
    SET    is_current = FALSE,
           valid_to = p_change_date - INTERVAL '1 day'
    WHERE  employment_id = p_employment_id
      AND  is_current = TRUE;

    -- Insert the new salary record
    INSERT INTO employee_salary_history (
        employment_id, salary, valid_from, valid_to, is_current
    ) VALUES (
        p_employment_id, p_new_salary, p_change_date, NULL, TRUE
    );
END;
$$ LANGUAGE plpgsql;

