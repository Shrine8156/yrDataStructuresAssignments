public class Main {
    public static void main(String[] args) {

        // ---- Sample data to load into the Purchase Log ----
        PurchaseItem[] sampleItems = {
            new PurchaseItem("Bread", 3.49),
            new PurchaseItem("Milk", 2.99),
            new PurchaseItem("Eggs", 4.29),
            new PurchaseItem("Coffee", 8.99),
            new PurchaseItem("Bananas", 1.29),
            new PurchaseItem("Cereal", 4.79),
            new PurchaseItem("Chicken Breast", 9.99),
            new PurchaseItem("Paper Towels", 6.49)
        };

        PurchaseLog log = new PurchaseLog();

        // Load every item into log
        for (PurchaseItem item : sampleItems) {
            log.addItem(item);
        }

        System.out.println("Purchase Log loaded with " + log.itemCount() + " items.");

        // Example test call - findItemByName
        PurchaseItem found = log.findItemByName("Coffee");
        System.out.println("Looked up 'Coffee', found: " +
            (found != null ? found.getName() + " $" + found.getPrice() : "NOT FOUND"));

        // Test updatePrice()
        System.out.println("\nTesting updatePrice()...");
        log.updatePrice("Coffee", 7.49);
        PurchaseItem updated = log.findItemByName("Coffee");
        System.out.println("Looked up 'Coffee' again: " +
            (updated != null ? updated.getName() + " $" + updated.getPrice() : "NOT FOUND"));

        // Test printDailyReport()
        System.out.println();
        log.printDailyReport();

        // ---- Sample data to load into the Checkout Line ----
        Customer[] sampleCustomers = {
            new Customer("Alvarez", 12),
            new Customer("Chen", 3),
            new Customer("Patel", 27),
            new Customer("O'Brien", 1)
        };

        CheckoutLine line = new CheckoutLine();

        // Load every customer into line
        for (Customer c : sampleCustomers) {
            line.addToBack(c);
        }

        System.out.println("Checkout Line loaded, size = " + line.size());

        // Example test call - addToFront (express override)
        Customer express = new Customer("Nguyen", 1);
        System.out.println("Waving " + express.getName() + " to the front...");
        line.addToFront(express);

        // Test removeFromFront()
        Customer served = line.removeFromFront();
        System.out.println("Served from front: " + (served != null ? served.getName() : "None") + " (Expected: Nguyen)");

        // Test removeFromBack()
        Customer leftLine = line.removeFromBack();
        System.out.println("Removed from back: " + (leftLine != null ? leftLine.getName() : "None") + " (Expected: O'Brien)");

        // Confirm line size after removals
        System.out.println("Checkout Line size after removals: " + line.size() + " (Expected: 3)");

        // ============================================================
        // Part 3: Performance Comparison Benchmark
        // ============================================================
        System.out.println("\n==============================================");
        System.out.println("        PART 3: BENCHMARK EXECUTION           ");
        System.out.println("==============================================");

        // PurchaseLog Benchmark (1,000 lookups over 5,000 items)
        PurchaseLog logAL = new PurchaseLog();
        PurchaseLogLinkedList logLL = new PurchaseLogLinkedList();

        for (int i = 0; i < 5000; i++) {
            PurchaseItem item = new PurchaseItem("Item" + i, 1.99);
            logAL.addItem(item);
            logLL.addItem(item);
        }

        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            logAL.findItemByName("Item2500");
        }
        long alSearchTime = System.nanoTime() - start;

        start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            logLL.findItemByName("Item2500");
        }
        long llSearchTime = System.nanoTime() - start;

        // CheckoutLine Benchmark (10,000 addToFront calls)
        CheckoutLine lineLL = new CheckoutLine();
        CheckoutLineArrayList lineAL = new CheckoutLineArrayList();

        start = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            lineLL.addToFront(new Customer("Cust" + i, 1));
        }
        long llInsertTime = System.nanoTime() - start;

        start = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            lineAL.addToFront(new Customer("Cust" + i, 1));
        }
        long alInsertTime = System.nanoTime() - start;

        System.out.printf("%-35s | %-20s | %-20s%n", "Workload", "ArrayList Time (ns)", "LinkedList Time (ns)");
        System.out.println("----------------------------------------------------------------------------------");
        System.out.printf("%-35s | %-20d | %-20d%n", "PurchaseLog (1,000 lookups)", alSearchTime, llSearchTime);
        System.out.printf("%-35s | %-20d | %-20d%n", "CheckoutLine (10,000 addToFront)", alInsertTime, llInsertTime);
    }
}