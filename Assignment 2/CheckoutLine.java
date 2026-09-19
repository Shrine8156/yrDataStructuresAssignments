import java.util.LinkedList;

public class CheckoutLine {

    // Chosen backing structure: LinkedList
    private LinkedList<Customer> line;

    public CheckoutLine() {
        this.line = new LinkedList<>();
    }

    public void addToBack(Customer c) {
        line.addLast(c);
    }

    public void addToFront(Customer c) {
        line.addFirst(c);
    }

    public Customer removeFromFront() {
        if (line.isEmpty()) {
            return null;
        }
        return line.removeFirst();
    }

    public Customer removeFromBack() {
        if (line.isEmpty()) {
            return null;
        }
        return line.removeLast();
    }

    public int size() {
        return line.size();
    }
}