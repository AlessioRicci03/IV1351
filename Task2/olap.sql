CREATE TABLE olap (
	-- Grain: one row per teacher per course instance (most useful)
	olap_id			SERIAL PRIMARY KEY,
	course_instance_id	INT		NOT NULL,
	course_code		VARCHAR(6)	NOT NULL,
	study_year		INT		NOT NULL,
	study_period		INT		NOT NULL,

	-- Teacher dimension (NULL when we want course-level totals only)
	employment_id		INT,			-- NULL = course-level summary row
	teacher_name		TEXT,			-- NULL for course total
	designation		TEXT,			-- NULL for course total

	hp			DECIMAL(10,2)	NOT NULL,
	num_students		INT			NOT NULL,

	-- Activity hours
	lecture_hours		NUMERIC DEFAULT 0,
	tutorial_hours		NUMERIC DEFAULT 0,
	lab_hours		NUMERIC DEFAULT 0,
	seminar_hours		NUMERIC DEFAULT 0,
	other_hours		NUMERIC DEFAULT 0,

	-- Fixed overheads (per teacher if allocated, otherwise full amount at course level)
	admin_hours		NUMERIC DEFAULT 0,
	exam_hours		NUMERIC DEFAULT 0,

	-- Final measure
	total_hours          NUMERIC GENERATED ALWAYS AS (
	COALESCE(lecture_hours,0)	+
	COALESCE(tutorial_hours,0)	+
	COALESCE(lab_hours,0)		+
	COALESCE(seminar_hours,0)	+
	COALESCE(other_hours,0)		+
	COALESCE(admin_hours,0)		+
	COALESCE(exam_hours,0)
	) STORED,
);

-- These two indexes prevent duplicates
CREATE UNIQUE INDEX uq_olap_teacher ON olap(course_instance_id, employment_id) WHERE employment_id IS NOT NULL;

CREATE UNIQUE INDEX uq_olap_course  ON olap(course_instance_id) WHERE employment_id IS NULL;

CREATE INDEX idx_olap_year_period ON olap(study_year, study_period);

CREATE INDEX idx_olap_teacher ON olap(employment_id);

-- =============================================================
-- Refresh function – optimised to run as fast as possible
-- QUERY: SELECT refresh_olap();
-- =============================================================

CREATE OR REPLACE FUNCTION refresh_olap()
RETURNS void AS $$
BEGIN
	TRUNCATE olap;

	-- Insert teacher-level rows + course-level summary rows
	INSERT INTO olap (
		course_instance_id, 
		course_code, 
		study_year, 
		study_period,
		employment_id, 
		teacher_name, 
		designation,
		hp, 
		num_students,
		lecture_hours, 
		tutorial_hours, 
		lab_hours, 
		seminar_hours, 
		other_hours,
		admin_hours, 
		exam_hours
		)
	WITH base AS (
		SELECT
			ci.course_instance_id,
			cl.course_code,
			ci.study_year,
			ci.study_period,
			cl.hp,
			COALESCE(ci.num_students, 0) AS num_students,

			a.employment_id,
			TRIM(p.first_name || ' ' || p.last_name) AS teacher_name,
			jt.job_title AS designation,

			pa.planned_hours,
			ta.activity_name,
			ta.factor,

		-- Count teachers per course
			COUNT(DISTINCT a.employment_id) OVER (PARTITION BY ci.course_instance_id) AS teacher_count
		FROM course_instance ci
		JOIN course_layout cl		ON cl.course_layout_id = ci.course_layout_id
		LEFT JOIN planned_activity pa	ON pa.course_instance_id = ci.course_instance_id
		LEFT JOIN allocation a		ON a.planned_activity_id = pa.planned_activity_id
		LEFT JOIN teaching_activity ta	ON ta.activity_id = pa.activity_id
		LEFT JOIN employee e		ON e.employment_id = a.employment_id
		LEFT JOIN person p		ON p.person_id = e.person_id
		LEFT JOIN job_title jt		ON jt.job_title_id = e.job_title_id
	)
	
	-- Teacher-level rows
	SELECT
		course_instance_id,
		course_code,
		study_year,
		study_period,
		employment_id,
		teacher_name,
		designation,
		hp,
		num_students,

		SUM(CASE WHEN activity_name = 'Lecture'						THEN planned_hours * factor ELSE 0 END),
		SUM(CASE WHEN activity_name = 'Tutorial'					THEN planned_hours * factor ELSE 0 END),
		SUM(CASE WHEN activity_name = 'Lab'						THEN planned_hours * factor ELSE 0 END),
		SUM(CASE WHEN activity_name = 'Seminar'						THEN planned_hours * factor ELSE 0 END),
		SUM(CASE WHEN activity_name NOT IN ('Lecture','Tutorial','Lab','Seminar')	THEN planned_hours * factor ELSE 0 END),

		-- Shared admin/exam
		(2.0 * hp + 28.0 + 0.2 * num_students) / GREATEST(teacher_count, 1),
		(32.0 + 0.725 * num_students) / GREATEST(teacher_count, 1)

	FROM base
	WHERE employment_id IS NOT NULL
	GROUP BY course_instance_id, course_code, study_year, study_period, employment_id, teacher_name, designation, hp, num_students, teacher_count

	UNION ALL

	-- Course-level summary rows (employment_id = NULL)
	SELECT
		course_instance_id,
		course_code,
		study_year,
		study_period,
		NULL::int,
		NULL::text,
		NULL::text,
		hp,
		num_students,

		SUM(CASE WHEN activity_name = 'Lecture'						THEN planned_hours * factor ELSE 0 END),
		SUM(CASE WHEN activity_name = 'Tutorial'					THEN planned_hours * factor ELSE 0 END),
		SUM(CASE WHEN activity_name = 'Lab'						THEN planned_hours * factor ELSE 0 END),
		SUM(CASE WHEN activity_name = 'Seminar'						THEN planned_hours * factor ELSE 0 END),
		SUM(CASE WHEN activity_name NOT IN ('Lecture','Tutorial','Lab','Seminar')	THEN planned_hours * factor ELSE 0 END),

		(2.0 * hp + 28.0 + 0.2 * num_students),  -- full amount
		(32.0 + 0.725 * num_students)

	FROM base
	GROUP BY course_instance_id, course_code, study_year, study_period, hp, num_students;
END;
$$ LANGUAGE plpgsql;

-- Queries for testing:

--SELECT course_code AS "Course Code", course_instance_id AS "Course Instance ID", hp AS "HP", 'P'||study_period AS "Period", num_students AS "Students", lecture_hours AS "Lecture Hours", tutorial_hours AS "Tutorial Hours", lab_hours AS "Lab Hours", seminar_hours AS "Seminar Hours", other_hours AS "Other Overhead Hours", admin_hours AS "Admin", exam_hours AS "Exam", total_hours AS "Total Hours" FROM olap WHERE teacher_name IS NULL AND study_year = EXTRACT(YEAR FROM CURRENT_DATE) ORDER BY study_period, course_code;

--SELECT course_code AS "Course Code", course_instance_id AS "Course Instance ID", hp AS "HP", teacher_name AS "Teacher's Name", designation AS "Designation", lecture_hours AS "Lecture Hours", tutorial_hours AS "Tutorial Hours", lab_hours AS "Lab Hours", seminar_hours AS "Seminar Hours", other_hours AS "Other Overhead Hours", admin_hours AS "Admin", exam_hours AS "Exam", total_hours AS "Total Hours" FROM olap WHERE teacher_name IS NOT NULL AND study_year = EXTRACT(YEAR FROM CURRENT_DATE) ORDER BY total_hours DESC, teacher_name;

--SELECT course_code AS "Course Code", course_instance_id AS "Course Instance ID", hp AS "HP", 'P' || study_period AS "Period", teacher_name AS "Teacher's Name", lecture_hours AS "Lecture Hours", tutorial_hours AS "Tutorial Hours", lab_hours AS "Lab Hours", seminar_hours AS "Seminar Hours", other_hours AS "Other Overhead Hours", admin_hours AS "Admin", exam_hours AS "Exam", total_hours AS "Total Hours" FROM olap WHERE teacher_name = 'Lorem Ipsum' AND study_year = EXTRACT(YEAR FROM CURRENT_DATE) ORDER BY study_period, course_code;

--WITH teacher_load AS (SELECT o.employment_id, TRIM(p.first_name || ' ' || p.last_name) AS teacher_name, o.study_period, COUNT(DISTINCT o.course_instance_id) AS course_count FROM olap o JOIN employee e ON e.employment_id = o.employment_id JOIN person p ON p.person_id = e.person_id WHERE o.teacher_name IS NOT NULL AND o.study_year = EXTRACT(YEAR FROM CURRENT_DATE) GROUP BY o.employment_id, teacher_name, o.study_period) SELECT employment_id AS "Employment ID", teacher_name AS "Teacher's Name", 'P' || study_period AS "Period", course_count AS "No. of courses"FROM teacher_load WHERE course_count > N ORDER BY course_count DESC, teacher_name;


-- 1. Planned hours per course instance (8×/day) – course-summary rows
CREATE INDEX IF NOT EXISTS idx_olap_course_summary ON olap(study_year, study_period, course_code) WHERE employment_id IS NULL;

-- 2. Actual allocated hours per teacher per course instance (12×/day)
CREATE UNIQUE INDEX IF NOT EXISTS idx_olap_teacher_detail ON olap(course_instance_id, employment_id) WHERE employment_id IS NOT NULL;

-- 3. Teacher load per period – total hours per teacher per period (5×/day)
CREATE INDEX IF NOT EXISTS idx_olap_teacher_period_load ON olap(study_year, study_period, employment_id, total_hours) WHERE employment_id IS NOT NULL;

-- 4. Course instances with variance >15% (3×/day) – needs generated column first
ALTER TABLE olap ADD COLUMN IF NOT EXISTS variance_pct NUMERIC GENERATED ALWAYS AS (CASE WHEN employment_id IS NULL THEN ABS(100.0 * (total_hours - (SELECT SUM(total_hours) FROM olap o2 WHERE o2.course_instance_id = olap.course_instance_id AND o2.employment_id IS NOT NULL)) / NULLIF(total_hours, 0)) ELSE NULL END) STORED;
CREATE INDEX IF NOT EXISTS idx_olap_high_variance ON olap(course_instance_id) WHERE employment_id IS NULL AND variance_pct > 15;

-- 5. Teachers with >N courses in current period (20×/day) – MOST FREQUENT!
CREATE INDEX IF NOT EXISTS idx_olap_current_period_overload ON olap(study_year, study_period, employment_id, course_instance_id) WHERE employment_id IS NOT NULL AND study_year = EXTRACT(YEAR FROM CURRENT_DATE);