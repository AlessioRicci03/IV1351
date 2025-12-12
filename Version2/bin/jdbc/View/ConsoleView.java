package jdbc.view;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

import jdbc.Controller.TeachingController;

public class ConsoleView {
    private static final Scanner scanner = new Scanner(System.in);
    private final TeachingController controller;

    public ConsoleView(TeachingController controller) {
        this.controller = controller;
    }

    //what we are doing here is creating an interactive menu for the user to interface
    //in a clearer way to the operations that we have developed
    
    public void run(){
        while (true) {
            showMenu();
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                case "1" -> handleComputeCost();
                case "2" -> handleModifyStudents();
                case "3" -> handleAllocateTeacher();
                case "4" -> handleDeallocateTeacher();
                case "5" -> handleAddExerciseAndAllocate();
                    case "0" -> { System.out.println("Goodbye!"); return; }
                    default -> System.out.println("Invalid option – try again");
                }
            } catch (Exception e) {
                System.out.println("Operation failed: " + e.getMessage());
            }
        }
    }

    private void showMenu(){
        System.out.println("\n" + "=".repeat(60));
        System.out.println("       TEACHING ALLOCATION SYSTEM – IV1351");
        System.out.println("=".repeat(60));
        System.out.println("1. Compute teaching cost");
        System.out.println("2. Add 100 students + see cost impact");
        System.out.println("3. Allocate teacher to activity");
        System.out.println("4. Deallocate teacher");
        System.out.println("5. Add 'Exercise' activity + allocate teacher");
        System.out.println("0. Exit");
        System.out.print("Choose option: ");
    }

    private String promptCourseCode(){
        System.out.print("Course code (e.g. MAT101): ");
        return scanner.nextLine().trim().toUpperCase();
    }

    private int promptPeriod(){
        System.out.print("Study period (1-4): ");
        return Integer.parseInt(scanner.nextLine().trim());
    }

    private int promptInt(String message){
        System.out.print(message + ": ");
        return Integer.parseInt(scanner.nextLine().trim());
    }

    private promptDouble(){
        System.out.print(message + ": ");
        String input = scanner.nextLine().trim();
        return Double.parseDouble(input);
    }

    private void handleComputeCost(){
        String course = promptCourseCode();
        int period = promptPeriod();

        CostDTO dto = controller.computeTeachingCost(course, period);

        System.out.println("Planned cost: " + dto.plannedCost());
        System.out.println("Actual cost: " + dto.actualCost());
    }

    private void handleModifyStudents(){
        String course = promptCourseCode();
        int period = promptPeriod();
        int delta = promptInt("Change in student count (e.g. +100 or -50)");

        ModifyResultDTO dto = controller.modifyStudents(course, period, delta);

        System.out.println("Before: " + dto.before());
        System.out.println("After:  " + dto.after());
        System.out.println("Cost before: " + dto.costBefore());
        System.out.println("Cost after:  " + dto.costAfter());
    }

    private void handleAllocateTeacher(){
        int activity = promptInt("Activity ID");
        int teacher = promptInt("Teacher ID");

        boolean ok = controller.allocateTeacher(course, period, activity, teacher);

        if (ok)
            System.out.println("Teacher successfully allocated.");
        else
            System.out.println("Allocation failed.");
    }

    private void handleDeallocateTeacher(){
        int activity = promptInt("Activity ID");
        int teacher = promptInt("Teacher ID");

        boolean ok = controller.deallocateTeacher(activity, teacher);

        if (ok)
            System.out.println("Teacher successfully deallocated.");
        else
            System.out.println("Deallocation failed.");
    }

    private void handleAddExercise() {

        String course = prompt("Course code");
        int period = promptInt("Study period (1–4)");
        double hours = promptDouble("Planned hours");
        int teacher = promptInt("Teacher employment_id");

        ExerciseResult result = controller.addExercise(course, period, hours, teacher);

        System.out.println(result.message());
    }
}