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
}
