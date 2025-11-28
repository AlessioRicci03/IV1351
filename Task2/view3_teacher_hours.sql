CREATE OR REPLACE VIEW v_teacher_hours_current_year AS
SELECT
    ci.course_instance_id,
    cl.course_code,
    cl.hp,
    ci.study_period AS period,

    e.employment_id,
    p.first_name,
    p.last_name,

    -- Activity hours pivoted per activity type
    SUM(CASE WHEN ta.activity_name = 'Lecture'
             THEN pa.planned_hours * ta.factor ELSE 0 END) AS lecture_hours,

    SUM(CASE WHEN ta.activity_name = 'Tutorial'
             THEN pa.planned_hours * ta.factor ELSE 0 END) AS tutorial_hours,

    SUM(CASE WHEN ta.activity_name = 'Lab'
             THEN pa.planned_hours * ta.factor ELSE 0 END) AS lab_hours,

    SUM(CASE WHEN ta.activity_name = 'Seminar'
             THEN pa.planned_hours * ta.factor ELSE 0 END) AS seminar_hours,

    SUM(CASE WHEN ta.activity_name = 'Admin'
             THEN pa.planned_hours * ta.factor ELSE 0 END) AS admin_hours,

    SUM(CASE WHEN ta.activity_name = 'Exam'
             THEN pa.planned_hours * ta.factor ELSE 0 END) AS exam_hours,

    SUM(CASE WHEN ta.activity_name NOT IN ('Lecture','Tutorial','Lab','Seminar','Admin','Exam')
             THEN pa.planned_hours * ta.factor ELSE 0 END) AS other_hours,

    SUM(pa.planned_hours * ta.factor) AS total_hours

FROM allocation a
JOIN planned_activity pa    ON a.planned_activity_id = pa.planned_activity_id
JOIN teaching_activity ta   ON pa.activity_id = ta.activity_id
JOIN course_instance ci     ON pa.course_instance_id = ci.course_instance_id
JOIN course_layout cl       ON ci.course_layout_id = cl.course_layout_id
JOIN employee e             ON a.employment_id = e.employment_id
JOIN person p               ON e.person_id = p.person_id

WHERE ci.study_year = EXTRACT(YEAR FROM CURRENT_DATE)

GROUP BY
    ci.course_instance_id,
    cl.course_code,
    cl.hp,
    ci.study_period,
    e.employment_id,
    p.first_name,
    p.last_name;


-- 3. Teacher load per period (5×/day)
SELECT employment_id, teacher_name, designation, study_year, study_period, COUNT(DISTINCT course_instance_id) AS courses, SUM(total_hours) AS total_hours FROM olap WHERE teacher_name IS NOT NULL AND study_year = EXTRACT(YEAR FROM CURRENT_DATE) GROUP BY employment_id, teacher_name, designation, study_year, study_period ORDER BY total_hours DESC;
