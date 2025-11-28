CREATE OR REPLACE VIEW v_teacher_hours AS
SELECT
    e.employment_id,
    p.first_name,
    p.last_name,
    ci.course_instance_id,
    cl.course_code,
    cl.course_name,
    ci.study_year,
    ci.study_period,

    -- Activity-specific columns
    SUM(CASE WHEN ta.activity_name = 'Lecture'  THEN pa.planned_hours * ta.factor ELSE 0 END) AS lecture_hours,
    SUM(CASE WHEN ta.activity_name = 'Tutorial' THEN pa.planned_hours * ta.factor ELSE 0 END) AS tutorial_hours,
    SUM(CASE WHEN ta.activity_name = 'Lab'      THEN pa.planned_hours * ta.factor ELSE 0 END) AS lab_hours,
    SUM(CASE WHEN ta.activity_name = 'Seminar'  THEN pa.planned_hours * ta.factor ELSE 0 END) AS seminar_hours,
    SUM(CASE WHEN ta.activity_name = 'Exam'     THEN pa.planned_hours * ta.factor ELSE 0 END) AS exam_hours,
    SUM(CASE WHEN ta.activity_name = 'Admin'    THEN pa.planned_hours * ta.factor ELSE 0 END) AS admin_hours,
    SUM(CASE WHEN ta.activity_name NOT IN ('Lecture','Tutorial','Lab','Seminar','Exam','Admin')
             THEN pa.planned_hours * ta.factor ELSE 0 END) AS other_hours,

    SUM(pa.planned_hours * ta.factor) AS total_hours

FROM allocation a
JOIN planned_activity pa  ON a.planned_activity_id = pa.planned_activity_id
JOIN teaching_activity ta ON pa.activity_id = ta.activity_id
JOIN course_instance ci   ON pa.course_instance_id = ci.course_instance_id
JOIN course_layout cl     ON ci.course_layout_id = cl.course_layout_id
JOIN employee e           ON a.employment_id = e.employment_id
JOIN person p             ON e.person_id = p.person_id

GROUP BY
    e.employment_id,
    p.first_name,
    p.last_name,
    ci.course_instance_id,
    cl.course_code,
    cl.course_name,
    ci.study_year,
    ci.study_period;

-- 2. Actual allocated hours per teacher per course instance (12×/day)
SELECT course_code, course_instance_id, hp, teacher_name, designation, lecture_hours, tutorial_hours, lab_hours, seminar_hours, other_hours, admin_hours, exam_hours, total_hours FROM olap WHERE teacher_name IS NOT NULL AND study_year = EXTRACT(YEAR FROM CURRENT_DATE) ORDER BY total_hours DESC, teacher_name;
