package jdbc.view;

import java.util.Scanner;

import jdbc.Controller.TeachingController;
import jdbc.DTO.TeacherDTO.AllocationResult;
import jdbc.DTO.TeacherDTO.Case1Result;
import jdbc.DTO.TeacherDTO.Case2Result;
import jdbc.DTO.TeacherDTO.DeallocationResult;
import jdbc.DTO.TeacherDTO.ExerciseResult;

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
                case "5" -> handleAddExercise();
                    case "0" -> { System.out.println("Goodbye!"); return; }
                    default -> System.out.println("Invalid option, try again");
                }
            } catch (Exception e) {
                System.out.println("Operation failed: " + e.getMessage());
            }
        }
    }

    private void showMenu(){
        System.out.println("\n" + "=".repeat(60));
        System.out.println("       TEACHING ALLOCATION SYSTEM - IV1351");
        System.out.println("=".repeat(60));
        System.out.println("1. Compute teaching cost");
        System.out.println("2. Add 100 students + see cost impact");
        System.out.println("3. Allocate teacher to activity");
        System.out.println("4. Deallocate teacher");
        System.out.println("5. Add new activity + allocate teacher");
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

    private double promptDouble(String message){
        System.out.print(message + ": ");
        String input = scanner.nextLine().trim();
        return Double.parseDouble(input);
    }

    private String promptString(String message){
        System.out.print(message + ": ");
        return scanner.nextLine().trim();
    }

    private int promptYear(){
        System.out.println("Study year (e.g. 2025): ");
        return Integer.parseInt(scanner.nextLine().trim());
    }

    private void handleComputeCost(){
        String course = promptCourseCode();
        int year = promptYear();
        int period = promptPeriod();

        Case1Result dto = controller.computeTeachingCost(course, year, period);

        System.out.println("Planned cost: " + dto.plannedCost());
        System.out.println("Actual cost: " + dto.actualCost());
    }

    private void handleModifyStudents(){
        String course = promptCourseCode();
        int year = promptYear();
        int period = promptPeriod();
        int delta = promptInt("Change in student count (e.g. +100 or -50)");

        Case2Result dto = controller.modifyStudents(course, year, period, delta);

        System.out.println("Before: " + dto.before());
        System.out.println("After:  " + dto.after());
        System.out.println("Cost before: " + dto.costBefore());
        System.out.println("Cost after:  " + dto.costAfter());
    }

    private void handleAllocateTeacher(){
        int year = promptYear();
        int activity = promptInt("Activity ID");
        int teacher = promptInt("Teacher ID");

        AllocationResult ok = controller.allocateTeacher(year, activity, teacher);

        System.out.println(ok.message());
    }

    private void handleDeallocateTeacher(){
        int activity = promptInt("Activity ID");
        int teacher = promptInt("Teacher ID");

        DeallocationResult ok = controller.deallocateTeacher(activity, teacher);

        System.out.println(ok.message());
    }

    private void handleAddExercise() {

        String course = promptCourseCode();
        int period = promptPeriod();
        int year = promptYear();
        String ActivityName = promptString("Activity name");
        double hours = promptDouble("Planned hours");
        int teacher = promptInt("Teacher ID");

        ExerciseResult result = controller.addExercise(course, period, year, ActivityName, hours, teacher);

        if (result.success()){
            System.out.println(result.message());
        } else {
            System.out.println("Failed: " + result.message());
        }
    }
}
