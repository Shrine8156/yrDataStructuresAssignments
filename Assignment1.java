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

        System.out.println("Student: " + student.name);

        scanner.close();
    }
}
