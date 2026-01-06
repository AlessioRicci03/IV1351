package jdbc.Controller;


import java.sql.SQLException;

import jdbc.DTO.TeacherDTO.AllocationResult;
import jdbc.DTO.TeacherDTO.Case1Result;
import jdbc.DTO.TeacherDTO.Case2Result;
import jdbc.DTO.TeacherDTO.DeallocationResult;
import jdbc.DTO.TeacherDTO.ExerciseResult;
import jdbc.Model.TeachingModel;


public class TeachingController {

    private final TeachingModel model;

    public TeachingController(TeachingModel model){
        this.model = model;
    }

    public Case1Result computeTeachingCost(String courseCode, int year, int period) {
        return model.computeTeachingCost(courseCode, year, period);
    }



    public Case2Result modifyStudents(String courseCode, int year, int period, int deltaStudents) {
        try {
            Case2Result result = model.modifyStudentsAndComputeCost(courseCode, year, period, deltaStudents);
            return result;
        } catch(SQLException e){
            return new Case2Result(-1, -1, -1.0, -1.0);
        }
    }


    public AllocationResult allocateTeacher(int year, int activity, int teacherID){
        return model.allocateTeacher(year, activity, teacherID);
    }

    public DeallocationResult deallocateTeacher(int activityId, int empId) {
        return model.deallocateTeacher(activityId, empId);
    }


    public ExerciseResult addExercise(
        String courseCode,
        int period,
        int year,
        String activityName,
        double hours,
        int teacherId
    ) {
        return model.addExerciseActivity(courseCode, period, year, activityName, hours, teacherId);
    }

}
