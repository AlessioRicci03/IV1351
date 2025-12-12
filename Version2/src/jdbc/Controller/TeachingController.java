package jdbc.Controller;

import java.sql.SQLException;

import jdbc.Integration.TeachingDAO;

import java.sql.*;

public class TeachingController {

    public Case1Result computeTeachingCost(String courseCode, int period) {
        return model.computeTeachingCost(courseCode, period);
    }



    public void modifyStudents(String courseCode, int period, int deltaStudents) {
        try {
            Case2Result result = model.modifyStudentsAndComputeCost(courseCode, period, deltaStudents);
            view.showCase2Result(result);
        } catch (Exception e) {
            view.showError(e);
        }
    }


    public void allocateTeacher(int activity, string teacherID){
        return model.allocateTeacher(activity, teacherID);
    }

    public DeallocationResult deallocateTeacher(int activityId, int empId) {
        return model.deallocateTeacher(activityId, empId);
    }


    public ExerciseResult addExercise(
        String courseCode,
        int period,
        double hours,
        int teacherId
    ) {
        return model.addExerciseActivity(courseCode, period, hours, teacherId);
    }

}
