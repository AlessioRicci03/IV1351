CREATE OR REPLACE VIEW v_planned_hours AS
SELECT
    cl.course_code,
    ci.course_instance_id,
    cl.hp,
    ci.study_period AS period,
    ci.num_students,
    SUM(CASE WHEN ta.activity_name = 'Lecture' THEN pa.planned_hours * ta.factor ELSE 0 END) AS lecture_hours,
    SUM(CASE WHEN ta.activity_name = 'Tutorial' THEN pa.planned_hours * ta.factor ELSE 0 END) AS tutorial_hours,
    SUM(CASE WHEN ta.activity_name = 'Lab' THEN pa.planned_hours * ta.factor ELSE 0 END) AS lab_hours,
    SUM(CASE WHEN ta.activity_name = 'Seminar' THEN pa.planned_hours * ta.factor ELSE 0 END) AS seminar_hours,
    SUM(CASE WHEN ta.activity_name = 'Admin' THEN pa.planned_hours * ta.factor ELSE 0 END) AS admin_hours,
    SUM(CASE WHEN ta.activity_name = 'Exam' THEN pa.planned_hours * ta.factor ELSE 0 END) AS exam_hours,
    SUM(CASE WHEN ta.activity_name NOT IN ('Lecture','Tutorial','Lab','Seminar','Admin','Exam')
             THEN pa.planned_hours * ta.factor ELSE 0 END) AS other_hours,
    SUM(pa.planned_hours * ta.factor) AS total_hours,
    ci.study_year
FROM planned_activity pa
JOIN teaching_activity ta   ON pa.activity_id = ta.activity_id
JOIN course_instance ci     ON pa.course_instance_id = ci.course_instance_id
JOIN course_layout cl       ON ci.course_layout_id = cl.course_layout_id
GROUP BY
    cl.course_code, ci.course_instance_id, cl.hp, ci.study_period, ci.num_students, ci.study_year;

CREATE INDEX IF NOT EXISTS idx_planned_activity_join_cols
ON planned_activity (course_instance_id, activity_id)
INCLUDE (planned_hours);

CREATE INDEX IF NOT EXISTS idx_teaching_activity_name_factor
ON teaching_activity (activity_name)
INCLUDE (factor);

CREATE INDEX IF NOT EXISTS idx_course_layout_join
ON course_layout (course_layout_id)
INCLUDE (course_code, hp);
