//integration layer
package jdbc.Integration;

import java.sql.*;
import java.util.Scanner;

public class TeachingDAO {
	
	private static final Scanner scanner = new Scanner(System.in);
	
	private PreparedStatement query01;		//used for testing only
    private PreparedStatement plannedHours;
    private PreparedStatement actualCost;
    private PreparedStatement getCurrentStudents;
    private PreparedStatement getMaxStudents;
    private PreparedStatement updateStudents;
    private PreparedStatement checkTeacherCourses;
    private PreparedStatement allocateActivity;
    private PreparedStatement deallocateActivity;
    private PreparedStatement insertExerciseActivity;
    private PreparedStatement insertPlannedExercise;
    private PreparedStatement showExerciseAllocations;

    private final Connection conn;

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

//task 1: compute teaching cost
	public void computeTeachingCost() {
	    System.out.print("Enter course code (e.g. IV1351): ");
	    String courseCode = scanner.nextLine().trim().toUpperCase();

	    System.out.print("Enter study period (1-4): ");
	    int period = Integer.parseInt(scanner.nextLine().trim());

	    final double AVERAGE_SALARY_KSEK_PER_MONTH = 45000.0;	// standard assumption
	    
	    try{
	        prepareStatements(conn);

	        // === 1. Planned cost (using average salary) ===
	        plannedHours.setString(1, courseCode);
	        plannedHours.setInt(2, period);

	        double Hours = 0.0;
	        try (ResultSet rs = plannedHours.executeQuery()) {
	            if (rs.next()) {
	                Hours = rs.getDouble("planned_total_hours");
	            }
	        }
	        
	        int students = getNumStudents(courseCode, period);
	        double studentOverhead = getOverhead(students);

	        double plannedCostKsek = Hours * AVERAGE_SALARY_KSEK_PER_MONTH / 12.0 + studentOverhead;

	        // === 2. Actual allocated cost (using real teacher salaries) ===
	        double actualCostKsek  = getActualHoursCost(courseCode, period) + studentOverhead;

	        conn.commit();

	        System.out.println("\n=== Teaching cost for " + courseCode +
	                           " – Period " + period + " " + java.time.Year.now() + " ===");
	        System.out.printf("Planned cost           : %8.1f KSEK  (avg salary 85 KSEK/month)%n", plannedCostKsek);
	        System.out.printf("Actually allocated cost: %8.1f KSEK  (real teacher salaries)%n", actualCostKsek);
	        System.out.printf("Difference             : %8.1f KSEK%n", actualCostKsek - plannedCostKsek);

	        if (actualCostKsek > plannedCostKsek + 0.01) {
	            System.out.println("Warning: Over budget!");
	        } else if (plannedCostKsek > actualCostKsek + 0.01) {
	            System.out.println("Under budget – good!");
	        } else {
	            System.out.println("Exactly on budget.");
	        }

	    } catch (SQLException | NumberFormatException e) {
	        System.err.println("Error – transaction rolled back");
	        e.printStackTrace();
	    }
	}
	
	// Returns number of students for a given course instance (current year)
	private int getNumStudents(String courseCode, int period) throws SQLException {
	    getCurrentStudents.setString(1, courseCode);
	    getCurrentStudents.setInt(2, period);
	    try (ResultSet rs = getCurrentStudents.executeQuery()) {
	        return rs.next() ? rs.getInt("num_students") : 0;
	    }
	}
	
	// Returns max number of students for a specific course
	private int MaxStudents(String courseCode) throws SQLException {
		getMaxStudents.setString(1, courseCode);
		try (ResultSet rs = getMaxStudents.executeQuery()) {
	        return rs.next() ? rs.getInt("max_students") : 0;
	    }
	}
	
	private double getOverhead(int students){
		final double COST_PER_STUDENT_KSEK = 500;  				// 500 SEK per student – realistic overhead (for geminiAI when searching on google)
		double overhead = students * COST_PER_STUDENT_KSEK;
		return overhead;
	}
	
	// Helper method – Returns only the teacher-salary part (without student overhead)
	private double getActualHoursCost(String courseCode, int period) throws SQLException {
	    actualCost.setString(1, courseCode);
	    actualCost.setInt(2, period);
	    try (ResultSet rs = actualCost.executeQuery()) {
	        return rs.next() ? rs.getDouble("actual_cost_ksek") : 0.0;
	    }
	}
	
	//task 2: Add 100 students and show real cost impact (including student overhead)
	public void modifyStudentsAndShowCostImpact() {
	    System.out.print("Enter course code (e.g. IV1351): ");
	    String courseCode = scanner.nextLine().trim().toUpperCase();

	    System.out.print("Enter study period (1-4): ");
	    int period = Integer.parseInt(scanner.nextLine().trim());

	    try {
	        prepareStatements(conn);

	        // BEFORE
	        int studentsBefore = getNumStudents(courseCode, period);
	        double CostBefore = getActualHoursCost(courseCode, period) + getOverhead(studentsBefore);

	        System.out.println("\nBefore adding students:");
	        System.out.println(" Students registered : " + studentsBefore);
	        System.out.printf (" Teaching cost       : %.1f KSEK%n", CostBefore);

	        // AFTER
	        int studentsAfter = studentsBefore + 100;
	        int maxStudents = MaxStudents(courseCode);
	        if(maxStudents < studentsAfter) {
	        	System.out.println("The number of students is more than the course can currently handle. Setting it to the maximum number allowed");
	        	studentsAfter = maxStudents;
	        }
	        double CostAfter = getActualHoursCost(courseCode, period) + getOverhead(studentsAfter);
	        
	        // UPDATE – add students
	        updateStudents.setInt(1, studentsAfter);
	        updateStudents.setString(2, courseCode);
	        updateStudents.setInt(3, period);
	        int updated = updateStudents.executeUpdate();

	        if (updated == 0) {
	            System.out.println("No course instance found to update.");
	            conn.rollback();
	            return;
	        }

	        conn.commit();

	        System.out.println("\nAfter adding 100 students:");
	        System.out.println(" Students registered : " + studentsAfter);
	        System.out.printf (" Teaching cost       : %.1f KSEK%n", CostAfter);
	        System.out.printf (" Cost increase       : %.1f KSEK %n",
	                          CostAfter - CostBefore);

	    } catch (Exception e) {
	        System.err.println("Error – transaction rolled back");
	        e.printStackTrace();
	        try { conn.rollback(); } catch (SQLException ignored) {}
	    }
	}
	
	// Task 3: Allocate teacher (with 4-course limit check)
  public void allocateTeacher() {
      System.out.print("Enter planned_activity_id: ");
      int activityId = Integer.parseInt(scanner.nextLine());
      System.out.print("Enter employment_id: ");
      int empId = Integer.parseInt(scanner.nextLine());

      try {
          prepareStatements(conn);

          // Get period from the activity
          PreparedStatement getPeriod = conn.prepareStatement("SELECT ci.study_period FROM planned_activity pa JOIN course_instance ci ON ci.course_instance_id = pa.course_instance_id WHERE pa.planned_activity_id = ?");
          getPeriod.setInt(1, activityId);
          int period = 1;
          try (ResultSet rs = getPeriod.executeQuery()) {
              if (rs.next()) period = rs.getInt(1);
          }

          // Check limit
          checkTeacherCourses.setInt(1, empId);
          checkTeacherCourses.setInt(2, period);
          int currentCount = 0;
          try (ResultSet rs = checkTeacherCourses.executeQuery()) {
              if (rs.next()) currentCount = rs.getInt("course_count");
          }

          if (currentCount >= 4) {
              System.out.println("ERROR: Teacher " + empId + " already teaches in " + currentCount + " courses this period. Limit is 4.");
              conn.rollback();
              return;
          }

          allocateActivity.setInt(1, activityId);
          allocateActivity.setInt(2, empId);
          int rows = allocateActivity.executeUpdate();

          conn.commit();
          System.out.println(rows > 0 ? "Teacher allocated successfully!" : "Allocation failed (check IDs)");

      } catch (Exception e) {
          System.err.println("Error – transaction rolled back");
          e.printStackTrace();
      }
  }

  // Task 3: Deallocate teacher
  public void deallocateTeacher() {
      System.out.print("Enter planned_activity_id: ");
      int activityId = Integer.parseInt(scanner.nextLine());
      System.out.print("Enter employment_id: ");
      int empId = Integer.parseInt(scanner.nextLine());

      try{
          prepareStatements(conn);

          deallocateActivity.setInt(1, activityId);
          deallocateActivity.setInt(2, empId);
          int rows = deallocateActivity.executeUpdate();

          conn.commit();
          System.out.println(rows > 0 ? "Teacher deallocated successfully!" : "No such allocation found");

      } catch (Exception e) {
          System.err.println("Error – transaction rolled back");
          e.printStackTrace();
      }
  }
	
  //task 4
  public void addExerciseActivityAndAllocate() {
      System.out.println("\n=== Adding new teaching activity: Exercise ===");

      try{
          prepareStatements(conn);

          // 1. Insert "Exercise" activity (safe if already exists)
          int exerciseActivityId = -1;
          try (ResultSet rs = insertExerciseActivity.executeQuery()) {
              if (rs.next()) {
                  exerciseActivityId = rs.getInt(1);
                  System.out.println("New activity 'Exercise' created with ID: " + exerciseActivityId);
              } else {
                  // Already exists → get its ID
                  try (PreparedStatement getId = conn.prepareStatement("SELECT activity_id FROM teaching_activity WHERE activity_name = 'Exercise'")) {
                      try (ResultSet rs2 = getId.executeQuery()) {
                          if (rs2.next()) {
                              exerciseActivityId = rs2.getInt(1);
                              System.out.println("'Exercise' already exists → using ID: " + exerciseActivityId);
                          }
                      }
                  }
              }
          }

          if (exerciseActivityId == -1) {
              System.out.println("Failed to get Exercise activity ID");
              conn.rollback();
              return;
          }

          // 2. Choose a course instance
          System.out.print("Enter course code to add an esercise session to: ");
          String courseCode = scanner.nextLine().trim().toUpperCase();

          System.out.print("Enter study period (1-4): ");
          int period = Integer.parseInt(scanner.nextLine().trim());

          // Find the correct course_instance_id automatically
          int courseInstanceId = -1;
          try (PreparedStatement findInstance = conn.prepareStatement("SELECT ci.course_instance_id " + "FROM course_instance ci " + "JOIN course_layout cl ON cl.course_layout_id = ci.course_layout_id " + "WHERE cl.course_code = ? " + "  AND ci.study_period = ? " + "  AND ci.study_year = EXTRACT(YEAR FROM CURRENT_DATE)")) {

              findInstance.setString(1, courseCode);
              findInstance.setInt(2, period);

              try (ResultSet rs = findInstance.executeQuery()) {
                  if (rs.next()) {
                      courseInstanceId = rs.getInt(1);
                      System.out.println("Found course_instance_id = " + courseInstanceId);
                  } else {
                      System.out.println("No active instance found for " + courseCode + " in period " + period);
                      conn.rollback();
                      return;
                  }
              }
          }

          // Enter planned hours
          System.out.print("Enter planned hours for Exercise: ");
          double plannedHours = Double.parseDouble(scanner.nextLine().trim());

          insertPlannedExercise.setInt(1, courseInstanceId);
          insertPlannedExercise.setInt(2, exerciseActivityId);
          insertPlannedExercise.setDouble(3, plannedHours);
          insertPlannedExercise.executeUpdate();

          // 3. Allocate a teacher
          System.out.print("Enter employment_id of teacher to allocate: ");
          int teacherId = Integer.parseInt(scanner.nextLine().trim());

          // Find the planned_activity_id we just created
          try (PreparedStatement getPlannedId = conn.prepareStatement(
                  "SELECT planned_activity_id " +
                  "FROM planned_activity " +
                  "WHERE course_instance_id = ? AND activity_id = ?")) {

              getPlannedId.setInt(1, courseInstanceId);
              getPlannedId.setInt(2, exerciseActivityId);

              try (ResultSet rs = getPlannedId.executeQuery()) {
                  if (rs.next()) {
                      int plannedActivityId = rs.getInt(1);
                      allocateActivity.setInt(1, plannedActivityId);
                      allocateActivity.setInt(2, teacherId);
                      int rows = allocateActivity.executeUpdate();

                      if (rows > 0) {
                          System.out.println("Teacher " + teacherId + " successfully allocated to Exercise!");
                      } else {
                          System.out.println("Allocation failed (maybe teacher already assigned?)");
                      }
                  } else {
                      System.out.println("Could not find the planned Exercise activity – something went wrong.");
                  }
              }
          }

          conn.commit();

          // Show result
          System.out.println("\n=== Current Exercise allocations ===");
          try (ResultSet rs = showExerciseAllocations.executeQuery()) {
              boolean found = false;
              while (rs.next()) {
                  found = true;
                  System.out.printf("Course: %s | Period: %d | Year: %d | Teacher: %-25s | Hours: %.1f%n",
                      rs.getString("course_code"),
                      rs.getInt("study_period"),
                      rs.getInt("study_year"),
                      rs.getString("teacher_name"),
                      rs.getDouble("planned_hours")
                  );
              }
              if (!found) System.out.println("No Exercise activities allocated yet.");
          }

      } catch (Exception e) {
          System.err.println("Error – transaction rolled back");
          e.printStackTrace();
          }
      }
  
	//test to understand how accessing the database works on JDBC
	private void query(){
		try{
          prepareStatements(conn);							// Queries to be used
          
  		try (ResultSet employees = query01.executeQuery()) {
  			while (employees.next()) {
                  System.out.printf("%-20s %-20s%n",employees.getString(1),  employees.getString(2));
              }
  		}
  		
  		conn.commit();                                     // explicit commit
          System.out.println("Query completed successfully.");
          
		} catch (SQLException e) {
          System.err.println("Database error – everything was rolled back automatically");
          e.printStackTrace();
      }
	}
   
    public double getActualCost(String courseCode, int period) throws SQLException {
        actualCost.setString(1, courseCode);
        actualCost.setInt(2, period);
        try (ResultSet rs = actualCost.executeQuery()) {
            return rs.next() ? rs.getDouble(1) : 0.0;
        }
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
