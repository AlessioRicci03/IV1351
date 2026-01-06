package jdbc.Integration;

import java.sql.*;
// import java.util.Scanner;

public class TeachingDAO {

    private final Connection conn;

	// private PreparedStatement query01;		//used for testing only
    private PreparedStatement ReadPlannedHours;
    private PreparedStatement CreateActualCost;
    private PreparedStatement ReadCurrentStudents;
    private PreparedStatement ReadMaxStudents;
    private PreparedStatement UpdateStudents;
    private PreparedStatement ReadTeacherCourses;
	private PreparedStatement ReadPeriodFromActivity;
    private PreparedStatement CreateAllocationToActivity;
    private PreparedStatement DeleteAllocationToActivity;
    private PreparedStatement CreateActivity;
    private PreparedStatement CreatePlannedActivity;
    private PreparedStatement ReadExerciseAllocations;

    public TeachingDAO(Connection conn) throws SQLException {
        this.conn = conn;
        conn.setAutoCommit(false);
        prepareStatements(conn);
    }

    private void prepareStatements(Connection conn) throws SQLException {
        
		//test
	    /* query01 = conn.prepareStatement(
	        "SELECT first_name, last_name " +
	        "FROM person " +
	        "ORDER BY last_name, first_name"
	    ); */

	    //task 1
	    // 1. PLANNED hours × average salary (85 KSEK/month)
	    ReadPlannedHours = conn.prepareStatement(
	        "SELECT COALESCE(SUM(pa.planned_hours * ta.factor), 0) AS planned_total_hours " +
	        "FROM course_instance ci " +
	        "JOIN course_layout cl ON cl.course_layout_id = ci.course_layout_id " +
	        "JOIN planned_activity pa ON pa.course_instance_id = ci.course_instance_id " +
	        "JOIN teaching_activity ta ON ta.activity_id = pa.activity_id " +
	        "WHERE cl.course_code = ? " +
	        "  AND ci.study_period = ? " +
	        "  AND ci.study_year = ?"
	    );
	    // 2. ACTUAL allocated cost = sum(allocated hours × teacher's real monthly salary) / 12 months
	    CreateActualCost = conn.prepareStatement(
	        "SELECT COALESCE(SUM(pa.planned_hours * ta.factor * sh.salary / 12.0), 0) AS actual_cost_ksek " +
	        "FROM course_instance ci " +
	        "JOIN course_layout cl ON cl.course_layout_id = ci.course_layout_id " +
	        "JOIN planned_activity pa ON pa.course_instance_id = ci.course_instance_id " +
	        "JOIN teaching_activity ta ON ta.activity_id = pa.activity_id " +
	        "JOIN allocation a ON a.planned_activity_id = pa.planned_activity_id " +
	        "JOIN employee e ON e.employment_id = a.employment_id " +
	        "JOIN LATERAL ( " +
	        "    SELECT salary " +
	        "    FROM employee_salary_history " +
	        "    WHERE employment_id = e.employment_id " +
	        "      AND (valid_to IS NULL OR valid_to > CURRENT_DATE) " +
	        "      AND valid_from <= CURRENT_DATE " +
	        "    ORDER BY valid_from DESC " +
	        "    LIMIT 1 " +
	        ") sh ON true " +
	        "WHERE cl.course_code = ? " +
	        "  AND ci.study_period = ? " +
	        "  AND ci.study_year = ?"
	    );
	    // Get current number of students
	    ReadCurrentStudents = conn.prepareStatement(
	        "SELECT num_students " +
	        "FROM course_instance ci " +
	        "JOIN course_layout cl ON cl.course_layout_id = ci.course_layout_id " +
	        "WHERE cl.course_code = ? " +
	        "  AND ci.study_period = ? " +
	        "  AND ci.study_year = ?"
	    );

	    //task 2
	    // Get max number of students for the course
	    ReadMaxStudents = conn.prepareStatement(
	        "SELECT max_students " +
	        "FROM course_layout cl " +
	        "WHERE course_code = ?"	+
	        "	AND is_current = TRUE"
	    );
	    // Add students
	    UpdateStudents = conn.prepareStatement(
	        "UPDATE course_instance " +
	        "SET num_students = ? " +
	        "FROM course_layout cl " +
	        "WHERE course_instance.course_layout_id = cl.course_layout_id " +
	        "  AND cl.course_code = ? " +
	        "  AND course_instance.study_period = ? " +
	        "  AND course_instance.study_year = ?"
	    );
	    

	    //task 3
	    //Check how many courses a teacher is already teaching in this period
	    ReadTeacherCourses = conn.prepareStatement(
	        "SELECT COUNT(DISTINCT ci.course_instance_id) AS course_count " +
	        "FROM course_instance ci " +
	        "JOIN planned_activity pa ON pa.course_instance_id = ci.course_instance_id " +
	        "JOIN allocation a ON a.planned_activity_id = pa.planned_activity_id " +
	        "WHERE a.employment_id = ? " +
	        "  AND ci.study_period = ? " +
	        "  AND ci.study_year = ?"
	    );
		ReadPeriodFromActivity = conn.prepareStatement(
			"SELECT ci.study_period " +
			"FROM planned_activity pa " +
			"JOIN course_instance ci ON ci.course_instance_id = pa.course_instance_id " +
			"WHERE pa.planned_activity_id = ?"
		);

	    //Allocate teacher to a planned activity
	    CreateAllocationToActivity = conn.prepareStatement(
	        "INSERT INTO allocation (planned_activity_id, employment_id) " +
	        "VALUES (?, ?)"
	    );
	    //Deallocate teacher from a planned activity
	    DeleteAllocationToActivity = conn.prepareStatement(
	        "DELETE FROM allocation " +
	        "WHERE planned_activity_id = ? AND employment_id = ?"
	    );

	    
	    //task 4
	    // Insert new teaching activity "Exercise" (factor = 1.0 by default)
	    CreateActivity = conn.prepareStatement(
	        "INSERT INTO teaching_activity AS ta (activity_id, activity_name, factor) " +
	        "VALUES (?, ?, 1.0) " +
	        "ON CONFLICT (activity_name) DO NOTHING " +
	        "RETURNING ta.activity_id"
	    );
	    // Link Exercise to a specific course instance
	    CreatePlannedActivity = conn.prepareStatement(
	        "INSERT INTO planned_activity (planned_activity_id, course_instance_id, activity_id, planned_hours) " +
	        "VALUES (?, ?, ?, ?) " +
	        "ON CONFLICT DO NOTHING"
	    );
	    // Show all Exercise allocations with course code, period, year, teacher name and hours
	    ReadExerciseAllocations = conn.prepareStatement(
	        "SELECT cl.course_code, " +
	        "       ci.study_period, " +
	        "       ci.study_year, " +
	        "       p.first_name || ' ' || p.last_name AS teacher_name, " +
	        "       pa.planned_hours " +
	        "FROM teaching_activity ta " +
	        "JOIN planned_activity pa ON pa.activity_id = ta.activity_id " +
	        "JOIN course_instance ci ON ci.course_instance_id = pa.course_instance_id " +
	        "JOIN course_layout cl ON cl.course_layout_id = ci.course_layout_id " +
	        "JOIN allocation a ON a.planned_activity_id = pa.planned_activity_id " +
	        "JOIN employee e ON e.employment_id = a.employment_id " +
	        "JOIN person p ON p.person_id = e.person_id " +
	        "WHERE ta.activity_name = 'Exercise' " +
	        "ORDER BY cl.course_code, ci.study_period"
	    );
	    
	}

	public void updateStudentCount(String courseCode, int year, int period, int newCount) throws SQLException {
		UpdateStudents.setInt(1, newCount);
		UpdateStudents.setString(2, courseCode);
		UpdateStudents.setInt(3, period);
		UpdateStudents.setInt(4, year);
		UpdateStudents.executeUpdate();
	}

	// Helper method – Returns only the teacher-salary part (without student overhead)
	public double fetchActualHoursCost(String courseCode, int year, int period) throws SQLException {
	    CreateActualCost.setString(1, courseCode);
		CreateActualCost.setInt(2, period);
		CreateActualCost.setInt(3, year);

		try (ResultSet rs = CreateActualCost.executeQuery()) {
			return rs.next() ? rs.getDouble("actual_cost_ksek") : 0.0;
		}
	}

	// Returns number of students for a given course instance (current year)
	public int fetchNumStudents(String courseCode, int year, int period) throws SQLException {
	    ReadCurrentStudents.setString(1, courseCode);
		ReadCurrentStudents.setInt(2, period);
		ReadCurrentStudents.setInt(3, year);

		try (ResultSet rs = ReadCurrentStudents.executeQuery()) {
			return rs.next() ? rs.getInt("num_students") : 0;
		}
	}

	public double fetchPlannedHours(String courseCode, int year, int period) throws SQLException {
		ReadPlannedHours.setString(1, courseCode);
		ReadPlannedHours.setInt(2, period);
		ReadPlannedHours.setInt(3, year);
		try (ResultSet rs = ReadPlannedHours.executeQuery()) {
			return rs.next() ? rs.getDouble("planned_total_hours") : 0;
		}
	}

	// Returns max number of students for a specific course
	public int fetchMaxStudents(String courseCode) throws SQLException {
        ReadMaxStudents.setString(1, courseCode);

        try (ResultSet rs = ReadMaxStudents.executeQuery()) {
            return rs.next() ? rs.getInt("max_students") : 0;
        }
    }

	
	public int fetchActivityPeriod(int activityId) throws SQLException {
		ReadPeriodFromActivity.setInt(1, activityId);
		try (ResultSet rs = ReadPeriodFromActivity.executeQuery()) {
			return rs.next() ? rs.getInt(1) : -1;
		}
	}

	public int countTeacherCourses(int empId, int year, int period) throws SQLException {
		ReadTeacherCourses.setInt(1, empId);
		ReadTeacherCourses.setInt(2, period);
		ReadTeacherCourses.setInt(3, year);
		try (ResultSet rs = ReadTeacherCourses.executeQuery()) {
			return rs.next() ? rs.getInt("course_count") : 0;
		}
	}

	public int allocateTeacherToActivity(int activityId, int empId) throws SQLException {
		CreateAllocationToActivity.setInt(1, activityId);
		CreateAllocationToActivity.setInt(2, empId);
		return CreateAllocationToActivity.executeUpdate();
	}

	public int deallocateTeacherFromActivity(int activityId, int empId) throws SQLException {
		DeleteAllocationToActivity.setInt(1, activityId);
		DeleteAllocationToActivity.setInt(2, empId);
		return DeleteAllocationToActivity.executeUpdate();
	}

	private int currentMaxActivityId = 0;

	public int getOrCreateActivityId(String activityName) throws SQLException {
		if (activityName == null || activityName.trim().isEmpty()){
			throw new IllegalArgumentException("Activity name cannot be empty");
		}
		activityName = activityName.trim();

		if (currentMaxActivityId == 0){	
			try (PreparedStatement ps = conn.prepareStatement(
				"SELECT COALESCE(MAX(activity_id), 0) FROM teaching_activity"
			)) {
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()){
						currentMaxActivityId = rs.getInt(1);
					}
				}
			}
		}
		currentMaxActivityId++;
		int newId = currentMaxActivityId;

		CreateActivity.setInt(1, newId);
		CreateActivity.setString(2, activityName);
		try (ResultSet rs = CreateActivity.executeQuery()){
			if (rs.next()){
				return rs.getInt(1);
			}
		}

		try (PreparedStatement ps = conn.prepareStatement(
			"SELECT activity_id FROM teaching_activity WHERE activity_name = ?"
		)) {
			ps.setString(1, activityName);
			try (ResultSet rs = ps.executeQuery()){
				if (rs.next()){
					return rs.getInt(1);
				}
			}
		}
		throw new SQLException("Failed to create or retrieve activity ID for: " + activityName);
	}

	private int currentMaxPlannedId = 0;
	public void insertPlannedActivity(int instanceId, int activityId, double hours) throws SQLException {
		if (currentMaxPlannedId == 0){
			try (PreparedStatement ps = conn.prepareStatement(
				"SELECT COALESCE(MAX(planned_activity_id), 0) FROM planned_activity"
			)){
				try (ResultSet rs = ps.executeQuery()){
					if (rs.next()){
						currentMaxPlannedId = rs.getInt(1);
					}
				}
			}
		}
		currentMaxPlannedId++;
		int newId = currentMaxPlannedId;

		CreatePlannedActivity.setInt(1, newId);
		CreatePlannedActivity.setInt(2, instanceId);
		CreatePlannedActivity.setInt(3, activityId);
		CreatePlannedActivity.setDouble(4, hours);
		CreatePlannedActivity.executeUpdate();
	}

	public int findCourseInstanceId(String course, int year, int period) throws SQLException {
		try (PreparedStatement ps = conn.prepareStatement(
			"SELECT ci.course_instance_id " +
			"FROM course_instance ci " +
			"JOIN course_layout cl ON cl.course_layout_id = ci.course_layout_id " +
			"WHERE cl.course_code = ? " +
			"  AND ci.study_period = ? " +
			"  AND ci.study_year = ?"
		)) {
			ps.setString(1, course);
			ps.setInt(2, period);
			ps.setInt(3, year);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? rs.getInt(1) : -1;
			}
		}
	}

	public int getPlannedExerciseActivityId(int instanceId, int exerciseId) throws SQLException {
		try (PreparedStatement ps = conn.prepareStatement(
			"SELECT planned_activity_id " +
			"FROM planned_activity " +
			"WHERE course_instance_id = ? AND activity_id = ?"
		)) {
			ps.setInt(1, instanceId);
			ps.setInt(2, exerciseId);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? rs.getInt(1) : -1;
			}
		}
	}

	public ResultSet getExerciseAllocations() throws SQLException {
		return ReadExerciseAllocations.executeQuery();
	}


    public void commit() throws SQLException {
        conn.commit();
    }

    public void rollback() {
        try { conn.rollback(); } catch (SQLException ignored) {}
    }

    public void close() {
        try { conn.close(); } catch (SQLException ignored) {}
    }
}
