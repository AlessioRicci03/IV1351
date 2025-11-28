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

-- 5. Teachers allocated to more than N courses in current period (20×/day) — change >3 as needed
WITH current_period AS (SELECT MAX(study_period) AS p FROM olap WHERE study_year = EXTRACT(YEAR FROM CURRENT_DATE)) SELECT employment_id, teacher_name, 'P'||study_period AS period, COUNT(DISTINCT course_instance_id) AS courses FROM olap, current_period WHERE employment_id IS NOT NULL AND study_year = EXTRACT(YEAR FROM CURRENT_DATE) AND study_period = current_period.p GROUP BY employment_id, teacher_name, study_period HAVING COUNT(DISTINCT course_instance_id) > 3 ORDER BY courses DESC, teacher_name;
