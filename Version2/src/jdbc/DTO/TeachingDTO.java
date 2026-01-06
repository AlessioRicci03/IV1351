package jdbc.DTO;

public class TeacherDTO {
    public record Case1Result(double plannedCost, double actualCost) {}

    public record Case2Result(int before, int after, double costBefore, double costAfter) {}

    public record AllocationResult(boolean success, String message) {}

    public record DeallocationResult(boolean success, String message) {}

    public record ExerciseResult(boolean success, String message) {}
}

