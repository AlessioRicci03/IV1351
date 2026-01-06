-- Specify allocation amount in query

CREATE MATERIALIZED VIEW Mv_teacher_allocation_counts AS
SELECT 
    e.employment_id,
    p.first_name,
    p.last_name,
    ci.study_year,
    ci.study_period,
    COUNT(DISTINCT ci.course_instance_id) AS num_course_instances
FROM allocation a
JOIN employee e             ON a.employment_id = e.employment_id
JOIN person p               ON e.person_id = p.person_id
JOIN planned_activity pa    ON a.planned_activity_id = pa.planned_activity_id
JOIN course_instance ci     ON pa.course_instance_id = ci.course_instance_id
GROUP BY e.employment_id, p.first_name, p.last_name, ci.study_year, ci.study_period;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_teacher_alloc_unique 
ON Mv_teacher_allocation_counts (employment_id, study_year, study_period);

CREATE INDEX IF NOT EXISTS idx_mv_teacher_alloc_year_period 
ON Mv_teacher_allocation_counts (study_year, study_period)
INCLUDE (employment_id, num_course_instances);

CREATE INDEX IF NOT EXISTS idx_mv_teacher_alloc_num_courses 
ON Mv_teacher_allocation_counts (num_course_instances DESC, employment_id);
