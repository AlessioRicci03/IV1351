import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class teachingModel {
	
    // task 1: compute teaching cost
    public Case1Result computeTeachingCost(String courseCode, int period) {
        try {
            conn.setAutoCommit(false);

            double plannedHoursValue = dao.fetchPlannedHours(courseCode, period);
            int students = dao.fetchNumStudents(courseCode, period);
            double studentOverhead = computeOverhead(students);

            final double AVERAGE_SALARY_KSEK_PER_MONTH = 45000.0;
            double plannedCost = plannedHoursValue * AVERAGE_SALARY_KSEK_PER_MONTH / 12.0 + studentOverhead;

            double actualCost = dao.fetchActualHoursCost(courseCode, period) + studentOverhead;

            conn.commit();

            return new Case1Result(plannedCost, actualCost);

        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException("Error computing teaching cost", e);
        }
    }

    private double computeOverhead(int students) {
        final double COST_PER_STUDENT_KSEK = 0.5; // or 500 SEK per student
        return students * COST_PER_STUDENT_KSEK;
    }


    //task 2: Add 100 students and show real cost impact (including student overhead)
	public Case2Result modifyStudentsAndComputeCost(String courseCode, int period, int delta) throws SQLException {

        conn.setAutoCommit(false);

        int before = dao.fetchNumStudents(courseCode, period);
        double costBefore = dao.fetchActualHoursCost(courseCode, period) + computeOverhead(before);

        int after = before + delta;

        int max = dao.fetchMaxStudents(courseCode);
        if (after > max) {
            after = max;
        }

        dao.updateStudentCount(courseCode, period, after);

        double costAfter = dao.fetchActualHoursCost(courseCode, period) + computeOverhead(after);

        conn.commit();

        return new Case2Result(before, after, costBefore, costAfter);
    }

    // Task 3: Allocate teacher (with 4-course limit check)
    public Case3Result allocateTeacher(int activityId, int empId) {
        try {
            conn.setAutoCommit(false);

            int period = dao.fetchActivityPeriod(activityId);
            if (period == -1) {
                conn.rollback();
                return new AllocationResult(false, "Invalid activity: not found");
            }

            int currentLoad = dao.countTeacherCourses(empId, period);

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
    public Case4Result deallocateTeacher(int activityId, int empId) {
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
        double plannedHours,
        int teacherId
    ) {
        try {
            conn.setAutoCommit(false);

            int exId = dao.insertExercise();
            if (exId == -1)
                exId = dao.getExerciseActivityId();

            if (exId == -1) {
                conn.rollback();
                return new ExerciseResult(false, "Could not create or retrieve Exercise activity ID.");
            }

            int instanceId = dao.findCourseInstanceId(courseCode, period);
            if (instanceId == -1) {
                conn.rollback();
                return new ExerciseResult(false, "Course instance not found.");
            }

            dao.insertPlannedExercise(instanceId, exId, plannedHours);

            int plannedId = dao.getPlannedExerciseActivityId(instanceId, exId);
            if (plannedId == -1) {
                conn.rollback();
                return new ExerciseResult(false, "Exercise planned activity could not be found.");
            }

            int rows = dao.allocateTeacherToActivity(plannedId, teacherId);

            if (rows == 0) {
                conn.rollback();
                return new ExerciseResult(false, "Teacher allocation failed (Already allocated?).");
            }

            conn.commit();
            return new ExerciseResult(true, "Exercise activity successfully added and teacher allocated.");

        } catch (Exception e) {
            try { conn.rollback(); } catch (SQLException ignored) {}
            return new ExerciseResult(false, "Unexpected error occurred: " + e.getMessage());
        }
    }
}
