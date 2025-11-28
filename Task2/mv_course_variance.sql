CREATE MATERIALIZED VIEW mv_course_instance_variance AS
SELECT
    ci.course_code,
    ci.course_instance_id,
    ci.study_period,
    ci.study_year,

    -- Planned total hours
    SUM(pa.planned_hours * ta.factor) AS planned_hours,

    -- Actual allocated hours (same as view1 but aggregated)
    (
        SELECT SUM(pa2.planned_hours * ta2.factor)
        FROM planned_activity pa2
        JOIN teaching_activity ta2 ON pa2.activity_id = ta2.activity_id
        JOIN allocation a2 ON pa2.planned_activity_id = a2.planned_activity_id
        WHERE pa2.course_instance_id = ci.course_instance_id
    ) AS actual_hours,

    -- Variance %
    CASE
        WHEN SUM(pa.planned_hours * ta.factor) = 0 THEN NULL
        ELSE ROUND(
              (
                (
                  (
                    SELECT SUM(pa2.planned_hours * ta2.factor)
                    FROM planned_activity pa2
                    JOIN teaching_activity ta2 ON pa2.activity_id = ta2.activity_id
                    JOIN allocation a2 ON pa2.planned_activity_id = a2.planned_activity_id
                    WHERE pa2.course_instance_id = ci.course_instance_id
                  ) 
                  - SUM(pa.planned_hours * ta.factor)
                ) / SUM(pa.planned_hours * ta.factor)
              ) * 100, 2
        )
    END AS variance_percent

FROM course_instance ci
JOIN planned_activity pa  ON ci.course_instance_id = pa.course_instance_id
JOIN teaching_activity ta ON pa.activity_id = ta.activity_id
GROUP BY
    ci.course_code, ci.course_instance_id, ci.study_period, ci.study_year;

-- 4. Course instances with planned vs actual variance >15% (3×/day)
WITH course_totals AS (SELECT course_instance_id, MAX(CASE WHEN employment_id IS NULL THEN total_hours END) AS planned, SUM(CASE WHEN employment_id IS NOT NULL THEN total_hours END) AS allocated FROM olap WHERE study_year = EXTRACT(YEAR FROM CURRENT_DATE) GROUP BY course_instance_id) SELECT course_instance_id, course_code, study_year||'-P'||study_period AS period, planned, allocated, ROUND(100.0*ABS(planned-allocated)/NULLIF(planned,0),2) AS variance_pct FROM course_totals JOIN olap USING(course_instance_id) WHERE employment_id IS NULL AND ABS(planned-allocated)/NULLIF(planned,0) > 0.15 ORDER BY variance_pct DESC;
