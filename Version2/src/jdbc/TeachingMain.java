package jdbc;

import jdbc.Controller.TeachingController;
import jdbc.Model.TeachingModel;
import jdbc.view.ConsoleView;

import java.sql.*;

public class TeachingMain {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/IV1351_proj", "postgres", "edmatrix");
            TeachingModel model = new TeachingModel(conn);
            TeachingController controller = new TeachingController(model);
            ConsoleView view = new ConsoleView(controller);
            
            view.run();

        } catch (Exception e) {
            System.err.println("Could not start application:");
            e.printStackTrace();
        }
    }
}
