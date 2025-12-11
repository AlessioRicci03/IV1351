package jdbc;

import jdbc.Controller.TeachingController;
import jdbc.Integration.TeachingDAO;
import jdbc.view.ConsoleView;

import java.sql.*;

public class TeachingMain {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/course_advanced", "postgres", "2003");
            TeachingDAO dao = new TeachingDAO(conn);
            TeachingController controller = new TeachingController(dao);
            ConsoleView view = new ConsoleView(controller);
            
            view.run();

        } catch (Exception e) {
            System.err.println("Could not start application:");
            e.printStackTrace();
        }
    }
}