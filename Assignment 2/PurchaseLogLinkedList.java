import java.util.LinkedList;
import java.util.HashMap;
import java.util.Map;

public class PurchaseLogLinkedList {

    private LinkedList<PurchaseItem> items;

    public PurchaseLogLinkedList() {
        this.items = new LinkedList<>();
    }

    public void addItem(PurchaseItem item) {
        items.add(item);
    }

    public PurchaseItem findItemByName(String name) {
        for (PurchaseItem item : items) {
            if (item.getName().equalsIgnoreCase(name)) {
                return item;
            }
        }
        return null;
    }

    public void updatePrice(String name, double newPrice) {
        PurchaseItem item = findItemByName(name);
        if (item != null) {
            item.setPrice(newPrice);
        }
    }

    public void printDailyReport() {
        int totalCount = items.size();
        double totalRevenue = 0.0;
        Map<String, Integer> counts = new HashMap<>();

        for (PurchaseItem item : items) {
            totalRevenue += item.getPrice();
            counts.put(item.getName(), counts.getOrDefault(item.getName(), 0) + 1);
        }

        String bestSeller = "None";
        int maxSold = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > maxSold) {
                maxSold = entry.getValue();
                bestSeller = entry.getKey();
            }
        }

        System.out.printf("Total items sold: %d, Revenue: $%.2f, Best seller: %s%n",
                totalCount, totalRevenue, bestSeller);
    }

    public int itemCount() {
        return items.size();
    }
}