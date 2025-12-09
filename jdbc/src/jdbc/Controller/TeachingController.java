package jdbc.Controller;

import java.sql.SQLException;

import jdbc.Integration.TeachingDAO;

import java.sql.*;

public class TeachingController {
    private final TeachingDAO dao;

    public TeachingController(TeachingDAO dao) {
        this.dao = dao;
    }

    public void computeTeachingCost() {
        dao.computeTeachingCost();
    }

    public void modifyStudentsAndShowCostImpact() {
        dao.modifyStudentsAndShowCostImpact();
    }

    public void allocateTeacher() {
        dao.allocateTeacher();
    }

    public void deallocateTeacher() {
        dao.deallocateTeacher();
    }

    public void addExerciseActivityAndAllocate() {
        dao.addExerciseActivityAndAllocate();
    }
}
