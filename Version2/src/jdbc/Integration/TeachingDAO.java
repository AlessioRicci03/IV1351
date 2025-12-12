//integration layer
package jdbc.Integration;

import java.sql.*;
import java.util.Scanner;

public class TeachingDAO {

    private final Connection conn;

	private PreparedStatement query01;		//used for testing only
    private PreparedStatement plannedHours;
    private PreparedStatement actualCost;
    private PreparedStatement getCurrentStudents;
    private PreparedStatement getMaxStudents;
    private PreparedStatement updateStudents;
    private PreparedStatement checkTeacherCourses;
	private PreparedStatement getPeriodFromActivity;
    private PreparedStatement allocateActivity;
    private PreparedStatement deallocateActivity;
    private PreparedStatement insertExerciseActivity;
    private PreparedStatement insertPlannedExercise;
    private PreparedStatement showExerciseAllocations;

    public TeachingDAO(Connection conn) throws SQLException {
        this.conn = conn;
        conn.setAutoCommit(false);
        prepareStatements(conn);
    }

    private void prepareStatements(Connection conn) throws SQLException {
        
		//test
	    query01 = conn.prepareStatement(
	        "SELECT first_name, last_name " +
	        "FROM person " +
	        "ORDER BY last_name, first_name"
	    );

	    //task 1
	    // 1. PLANNED hours × average salary (85 KSEK/month)
	    plannedHours = conn.prepareStatement(
	        "SELECT COALESCE(SUM(pa.planned_hours * ta.factor), 0) AS planned_total_hours " +
	        "FROM course_instance ci " +
	        "JOIN course_layout cl ON cl.course_layout_id = ci.course_layout_id " +
	        "JOIN planned_activity pa ON pa.course_instance_id = ci.course_instance_id " +
	        "JOIN teaching_activity ta ON ta.activity_id = pa.activity_id " +
	        "WHERE cl.course_code = ? " +
	        "  AND ci.study_period = ? " +
	        "  AND ci.study_year = EXTRACT(YEAR FROM CURRENT_DATE)"
	    );
	    // 2. ACTUAL allocated cost = sum(allocated hours × teacher's real monthly salary) / 12 months
	    actualCost = conn.prepareStatement(
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
	        "  AND ci.study_year = EXTRACT(YEAR FROM CURRENT_DATE)"
	    );
	    // Get current number of students
	    getCurrentStudents = conn.prepareStatement(
	        "SELECT num_students " +
	        "FROM course_instance ci " +
	        "JOIN course_layout cl ON cl.course_layout_id = ci.course_layout_id " +
	        "WHERE cl.course_code = ? " +
	        "  AND ci.study_period = ? " +
	        "  AND ci.study_year = EXTRACT(YEAR FROM CURRENT_DATE)"
	    );

	    //task 2
	    // Get max number of students for the course
	    getMaxStudents = conn.prepareStatement(
	        "SELECT max_students " +
	        "FROM course_layout cl " +
	        "WHERE course_code = ?"	+
	        "	AND is_current = TRUE"
	    );
	    // Add students
	    updateStudents = conn.prepareStatement(
	        "UPDATE course_instance " +
	        "SET num_students = ? " +
	        "FROM course_layout cl " +
	        "WHERE course_instance.course_layout_id = cl.course_layout_id " +
	        "  AND cl.course_code = ? " +
	        "  AND course_instance.study_period = ? " +
	        "  AND course_instance.study_year = EXTRACT(YEAR FROM CURRENT_DATE)"
	    );
	    

	    //task 3
	    //Check how many courses a teacher is already teaching in this period
	    checkTeacherCourses = conn.prepareStatement(
	        "SELECT COUNT(DISTINCT ci.course_instance_id) AS course_count " +
	        "FROM course_instance ci " +
	        "JOIN planned_activity pa ON pa.course_instance_id = ci.course_instance_id " +
	        "JOIN allocation a ON a.planned_activity_id = pa.planned_activity_id " +
	        "WHERE a.employment_id = ? " +
	        "  AND ci.study_period = ? " +
	        "  AND ci.study_year = EXTRACT(YEAR FROM CURRENT_DATE)"
	    );
		getPeriodFromActivity = conn.prepareStatement(
			"SELECT ci.study_period " +
			"FROM planned_activity pa " +
			"JOIN course_instance ci ON ci.course_instance_id = pa.course_instance_id " +
			"WHERE pa.planned_activity_id = ?"
		);

	    //Allocate teacher to a planned activity
	    allocateActivity = conn.prepareStatement(
	        "INSERT INTO allocation (planned_activity_id, employment_id) " +
	        "VALUES (?, ?)"
	    );
	    //Deallocate teacher from a planned activity
	    deallocateActivity = conn.prepareStatement(
	        "DELETE FROM allocation " +
	        "WHERE planned_activity_id = ? AND employment_id = ?"
	    );

	    
	    //task 4
	    // Insert new teaching activity "Exercise" (factor = 1.0 by default)
	    insertExerciseActivity = conn.prepareStatement(
	        "INSERT INTO teaching_activity AS ta (activity_name, factor) " +
	        "VALUES ('Exercise', 1.0) " +
	        "ON CONFLICT (activity_name) DO NOTHING " +
	        "RETURNING ta.activity_id"
	    );
	    // Link Exercise to a specific course instance
	    insertPlannedExercise = conn.prepareStatement(
	        "INSERT INTO planned_activity (course_instance_id, activity_id, planned_hours) " +
	        "VALUES (?, ?, ?) " +
	        "ON CONFLICT DO NOTHING"
	    );
	    // Show all Exercise allocations with course code, period, year, teacher name and hours
	    showExerciseAllocations = conn.prepareStatement(
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

	public void updateStudentCount(String courseCode, int period, int newCount) throws SQLException {
		updateStudents.setInt(1, newCount);
		updateStudents.setString(2, courseCode);
		updateStudents.setInt(3, period);
		updateStudents.executeUpdate();
	}
   
    public double getActualCost(String courseCode, int period) throws SQLException {
        actualCost.setString(1, courseCode);
        actualCost.setInt(2, period);
        try (ResultSet rs = actualCost.executeQuery()) {
            return rs.next() ? rs.getDouble(1) : 0.0;
        }
    }

	// Helper method – Returns only the teacher-salary part (without student overhead)
	public double getActualHoursCost(String courseCode, int period) throws SQLException {
	    actualCost.setString(1, courseCode);
		actualCost.setInt(2, period);

		try (ResultSet rs = actualCost.executeQuery()) {
			return rs.next() ? rs.getDouble("actual_cost_ksek") : 0.0;
		}
	}

	// Returns number of students for a given course instance (current year)
	public int getNumStudents(String courseCode, int period) throws SQLException {
	    getCurrentStudents.setString(1, courseCode);
		getCurrentStudents.setInt(2, period);

		try (ResultSet rs = getCurrentStudents.executeQuery()) {
			return rs.next() ? rs.getInt("num_students") : 0;
		}
	}

	public double fetchPlannedHours(String courseCode, int period) throws SQLException {
		plannedHours.setString(1, courseCode);
		plannedHours.setInt(2, period);
		try (ResultSet rs = plannedHours.executeQuery()) {
			return rs.next() ? rs.getDouble("planned_total_hours") : 0;
		}
	}

	// Returns max number of students for a specific course
	public int fetchMaxStudents(String courseCode) throws SQLException {
        getMaxStudents.setString(1, courseCode);

        try (ResultSet rs = getMaxStudents.executeQuery()) {
            return rs.next() ? rs.getInt("max_students") : 0;
        }
    }

	
	public int fetchActivityPeriod(int activityId) throws SQLException {
		getPeriodFromActivity.setInt(1, activityId);
		try (ResultSet rs = getPeriodFromActivity.executeQuery()) {
			return rs.next() ? rs.getInt(1) : -1;
		}
	}

	public int countTeacherCourses(int empId, int period) throws SQLException {
		checkTeacherCourses.setInt(1, empId);
		checkTeacherCourses.setInt(2, period);
		try (ResultSet rs = checkTeacherCourses.executeQuery()) {
			return rs.next() ? rs.getInt("course_count") : 0;
		}
	}

	public int allocateTeacherToActivity(int activityId, int empId) throws SQLException {
		allocateActivity.setInt(1, activityId);
		allocateActivity.setInt(2, empId);
		return allocateActivity.executeUpdate();
	}

	public int deallocateTeacherFromActivity(int activityId, int empId) throws SQLException {
		deallocateActivity.setInt(1, activityId);
		deallocateActivity.setInt(2, empId);
		return deallocateActivity.executeUpdate();
	}

	public int insertExercise() throws SQLException {
		try (ResultSet rs = insertExerciseActivity.executeQuery()) {
			if (rs.next()) {
				return rs.getInt(1);
			}
			return -1;
		}
	}

	public int getExerciseActivityId() throws SQLException {
		try (PreparedStatement ps = conn.prepareStatement(
			"SELECT activity_id FROM teaching_activity WHERE activity_name = 'Exercise'"
		)) {
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? rs.getInt(1) : -1;
			}
		}
	}

	public void insertPlannedExercise(int instanceId, int activityId, double hours) throws SQLException {
		insertPlannedExercise.setInt(1, instanceId);
		insertPlannedExercise.setInt(2, activityId);
		insertPlannedExercise.setDouble(3, hours);
		insertPlannedExercise.executeUpdate();
	}

	public int findCourseInstanceId(String course, int period) throws SQLException {
		try (PreparedStatement ps = conn.prepareStatement(
			"SELECT ci.course_instance_id " +
			"FROM course_instance ci " +
			"JOIN course_layout cl ON cl.course_layout_id = ci.course_layout_id " +
			"WHERE cl.course_code = ? " +
			"  AND ci.study_period = ? " +
			"  AND ci.study_year = EXTRACT(YEAR FROM CURRENT_DATE)"
		)) {
			ps.setString(1, course);
			ps.setInt(2, period);
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
		return showExerciseAllocations.executeQuery();
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
