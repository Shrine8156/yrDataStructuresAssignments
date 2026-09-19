import java.util.ArrayList;

public class CheckoutLineArrayList {

    private ArrayList<Customer> line;

    public CheckoutLineArrayList() {
        this.line = new ArrayList<>();
    }

    public void addToBack(Customer c) {
        line.add(c);
    }

    public void addToFront(Customer c) {
        line.add(0, c); // forces shifting all elements right
    }

    public Customer removeFromFront() {
        if (line.isEmpty()) {
            return null;
        }
        return line.remove(0); // forces shifting all elements left
    }

    public Customer removeFromBack() {
        if (line.isEmpty()) {
            return null;
        }
        return line.remove(line.size() - 1);
    }

    public int size() {
        return line.size();
    }
}