public class Customer {
    private String name;
    private int itemCount;

    public Customer(String name, int itemCount) {
        this.name = name;
        this.itemCount = itemCount;
    }

    public String getName() { return name; }
    public int getItemCount() { return itemCount; }
}
