package jdbc.view;

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
    
    public void run() {
        while (true) {
            showMenu();
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                case "1" -> controller.computeTeachingCost();
                case "2" -> controller.modifyStudentsAndShowCostImpact();
                case "3" -> controller.allocateTeacher();
                case "4" -> controller.deallocateTeacher();
                case "5" -> controller.addExerciseActivityAndAllocate();
                    case "0" -> { System.out.println("Goodbye!"); return; }
                    default -> System.out.println("Invalid option – try again");
                }
            } catch (Exception e) {
                System.out.println("Operation failed: " + e.getMessage());
            }
        }
    }

    private void showMenu() {
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

    private String promptCourseCode() {
        System.out.print("Course code (e.g. MAT101): ");
        return scanner.nextLine().trim().toUpperCase();
    }

    private int promptPeriod() {
        System.out.print("Study period (1-4): ");
        return Integer.parseInt(scanner.nextLine().trim());
    }

    private int promptInt(String message) {
        System.out.print(message + ": ");
        return Integer.parseInt(scanner.nextLine().trim());
    }
}