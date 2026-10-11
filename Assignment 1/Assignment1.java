import java.util.Scanner;

class Student {
    String name;

    Student(String name) {
        this.name = name;
    }
}

public class Assignment1 {

public static void main(String[] args) {

    Scanner scanner = new Scanner(System.in);

    System.out.print("Enter student name: ");
    String name = scanner.nextLine();

    Student student = new Student(name);

    System.out.print("How many grades do you want to enter? ");
    int numberOfGrades = scanner.nextInt();

    double[] grades = new double[numberOfGrades];

    for (int i = 0; i < grades.length; i++) {
        System.out.print("Enter grade " + (i + 1) + ": ");
        grades[i] = scanner.nextDouble();
    }

    displayGrades(grades);

double average = calculateAverage(grades);
System.out.println("Average: " + average);

if (average >= 70) {
    System.out.println(student.name + " is passing.");
} else {
    System.out.println(student.name + " is not passing.");
}

testRuntime();

scanner.close();

}
public static double calculateAverage(double[] grades) {

    double total = 0;

    for (int i = 0; i < grades.length; i++) {
        total += grades[i];
    }

    return total / grades.length;
}

public static void displayGrades(double[] grades) {

    System.out.println("Grades:");

    for (int i = 0; i < grades.length; i++) {
        System.out.println(grades[i]);
    }
}

public static void testRuntime() {

    double[] smallArray = new double[1000];
    double[] largeArray = new double[100000];

    for (int i = 0; i < smallArray.length; i++) {
        smallArray[i] = 80;
    }

    for (int i = 0; i < largeArray.length; i++) {
        largeArray[i] = 80;
    }

    long startSmall = System.nanoTime();
    calculateAverage(smallArray);
    long endSmall = System.nanoTime();

    long startLarge = System.nanoTime();
    calculateAverage(largeArray);
    long endLarge = System.nanoTime();

    System.out.println();
    System.out.println("Runtime Test");
    System.out.println("Small array: " + (endSmall - startSmall) + " nanoseconds");
    System.out.println("Large array: " + (endLarge - startLarge) + " nanoseconds");
}

}