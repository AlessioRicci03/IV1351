package jdbc.Model;

import java.sql.Connection;
import java.sql.SQLException;

import jdbc.DTO.TeacherDTO.AllocationResult;
import jdbc.DTO.TeacherDTO.Case1Result;
import jdbc.DTO.TeacherDTO.Case2Result;
import jdbc.DTO.TeacherDTO.DeallocationResult;
import jdbc.DTO.TeacherDTO.ExerciseResult;
import jdbc.Integration.TeachingDAO;

public class TeachingModel {

    private final Connection conn;
    private final TeachingDAO dao;

    public TeachingModel(Connection conn) throws SQLException {
        this.conn = conn;
        this.dao = new TeachingDAO(conn);
    }
	
    // task 1: compute teaching cost
    public Case1Result computeTeachingCost(String courseCode, int year, int period) {
        try {

            double plannedHoursValue = dao.fetchPlannedHours(courseCode, year, period);
            int students = dao.fetchNumStudents(courseCode, year, period);
            double studentOverhead = computeOverhead(students);

            final double AVERAGE_SALARY_KSEK_PER_MONTH = 45000.0;
            double plannedCost = plannedHoursValue * AVERAGE_SALARY_KSEK_PER_MONTH / 12.0 + studentOverhead;

            double actualCost = dao.fetchActualHoursCost(courseCode, year, period) + studentOverhead;

            return new Case1Result(plannedCost, actualCost);

        } catch (SQLException e) {
            throw new RuntimeException("Error computing teaching cost", e);
        }
    }

    private double computeOverhead(int students) {
        final double COST_PER_STUDENT_KSEK = 0.5; // or 500 SEK per student
        return students * COST_PER_STUDENT_KSEK;
    }


    //task 2: Add 100 students and show real cost impact (including student overhead)
	public Case2Result modifyStudentsAndComputeCost(String courseCode, int year, int period, int delta) throws SQLException {

        conn.setAutoCommit(false);

        int before = dao.fetchNumStudents(courseCode, year, period);
        double costBefore = dao.fetchActualHoursCost(courseCode, year, period) + computeOverhead(before);

        int after = before + delta;

        int max = dao.fetchMaxStudents(courseCode);
        if (after > max) {
            after = max;
        }

        dao.updateStudentCount(courseCode, year, period, after);

        double costAfter = dao.fetchActualHoursCost(courseCode, year, period) + computeOverhead(after);

        conn.commit();

        return new Case2Result(before, after, costBefore, costAfter);
    }

    // Task 3: Allocate teacher (with 4-course limit check)
    public AllocationResult allocateTeacher(int year, int activityId, int empId) {
        try {
            conn.setAutoCommit(false);

            int period = dao.fetchActivityPeriod(activityId);
            if (period == -1) {
                conn.rollback();
                return new AllocationResult(false, "Invalid activity: not found");
            }

            int currentLoad = dao.countTeacherCourses(empId, year, period);

            if (currentLoad >= 4) {
                conn.rollback();
                return new AllocationResult(
                    false,
                    "Teacher already teaches in " + currentLoad + 
                    " courses (limit = 4)"
                );
            }

            int rows = dao.allocateTeacherToActivity(activityId, empId);

            if (rows == 0) {
                conn.rollback();
                return new AllocationResult(false, "Allocation failed");
            }

            conn.commit();
            return new AllocationResult(true, "Teacher allocated successfully");

        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException("Task 3 failed", e);
        }
    }

    // Task 3: Deallocate teacher
    public DeallocationResult deallocateTeacher(int activityId, int empId) {
        try {
            conn.setAutoCommit(false);

            int rows = dao.deallocateTeacherFromActivity(activityId, empId);

            if (rows == 0) {
                conn.rollback();
                return new DeallocationResult(false, "No such allocation found.");
            }

            conn.commit();
            return new DeallocationResult(true, "Teacher deallocated successfully!");

        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException("Deallocation failed", e);
        }
    }


    //task 4
    public ExerciseResult addExerciseActivity(
        String courseCode,
        int period,
        int year,
        String activityName,
        double plannedHours,
        int teacherId
    ) {
        if (activityName == null || activityName.trim().isEmpty()){
            return new ExerciseResult(false, "Activity name cannot be empty.");
        }
        activityName = activityName.trim();

        try {
            conn.setAutoCommit(false);

            int exId;
            try {
                exId = dao.getOrCreateActivityId(activityName);
            } catch (SQLException e){
                conn.rollback();
                return new ExerciseResult(false, "Could not create or retrieve activity ID.");
            }

            int instanceId = dao.findCourseInstanceId(courseCode, year, period);
            if (instanceId == -1) {
                conn.rollback();
                return new ExerciseResult(false, "Course instance not found.");
            }

            dao.insertPlannedActivity(instanceId, exId, plannedHours);

            int plannedId = dao.getPlannedExerciseActivityId(instanceId, exId);
            if (plannedId == -1) {
                conn.rollback();
                return new ExerciseResult(false, "Planned activity could not be found.");
            }

            int rows = dao.allocateTeacherToActivity(plannedId, teacherId);

            if (rows == 0) {
                conn.rollback();
                return new ExerciseResult(false, "Teacher allocation failed (Already allocated?).");
            }

            conn.commit();
            return new ExerciseResult(true, "Activity '"+ activityName +"' successfully added with "+ plannedHours +" hours and teacher allocated.");

        } catch (Exception e) {
            try { conn.rollback(); } catch (SQLException ignored) {}
            return new ExerciseResult(false, "Unexpected error occurred: " + e.getMessage());
        }
    }
}
