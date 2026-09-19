import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PurchaseLog {

    // Chosen backing structure: ArrayList
    private ArrayList<PurchaseItem> items;

    public PurchaseLog() {
        this.items = new ArrayList<>();
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

        System.out.printf("--- Daily Purchase Report ---%n");
        System.out.printf("Total items sold: %d%n", totalCount);
        System.out.printf("Total revenue:    $%.2f%n", totalRevenue);
        System.out.printf("Best seller:      %s (%d sold)%n", bestSeller, maxSold);
        System.out.printf("-----------------------------%n");
    }

    public int itemCount() {
        return items.size();
    }
}