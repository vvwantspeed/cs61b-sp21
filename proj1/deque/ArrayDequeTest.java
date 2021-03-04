package deque;

public class ArrayDequeTest {

    /* Utility method for printing out empty checks. */
    public static boolean checkEmpty(boolean expected, boolean actual) {
        if (expected != actual) {
            System.out.println("isEmpty() returned " + actual + ", but expected: " + expected);
            return false;
        }
        return true;
    }

    /* Utility method for printing out empty checks. */
    public static boolean checkSize(int expected, int actual) {
        if (expected != actual) {
            System.out.println("size() returned " + actual + ", but expected: " + expected);
            return false;
        }
        return true;
    }

    /* Prints a nice message based on whether a test passed.
     * The \n means newline. */
    public static void printTestStatus(boolean passed) {
        if (passed) {
            System.out.println("Test passed!\n");
        } else {
            System.out.println("Test failed!\n");
        }
    }

    /** Adds a few things to the list, checking isEmpty() and size() are correct,
     * finally printing the results.
     *
     * && is the "and" operation. */
    public static void addIsEmptySizeTest() {
        System.out.println("Running ArrayDeque add/isEmpty/Size test.");

        ArrayDeque<String> ad1 = new ArrayDeque<String>();

        boolean passed = checkEmpty(true, ad1.isEmpty());

        ad1.addFirst("front");

        // The && operator is the same as "and" in Python.
        // It's a binary operator that returns true if both arguments true, and false otherwise.
        passed = checkSize(1, ad1.size()) && passed;
        passed = checkEmpty(false, ad1.isEmpty()) && passed;

        ad1.addLast("middle");
        passed = checkSize(2, ad1.size()) && passed;

        ad1.addLast("back");
        passed = checkSize(3, ad1.size()) && passed;

        System.out.println("Printing out deque: ");
        ad1.printDeque();

        printTestStatus(passed);

    }

    /** Adds an item, then removes an item, and ensures that dll is empty afterwards. */
    public static void addRemoveTest() {

        System.out.println("Running ArrayDeque add/remove test.");


        ArrayDeque<Integer> ad1 = new ArrayDeque<Integer>();
        // should be empty
        boolean passed = checkEmpty(true, ad1.isEmpty());

        ad1.addFirst(10);
        // should not be empty
        passed = checkEmpty(false, ad1.isEmpty()) && passed;

        ad1.removeFirst();
        // should be empty
        passed = checkEmpty(true, ad1.isEmpty()) && passed;

        printTestStatus(passed);

    }


    /** Tests ArrayDeque.resize. */
    public static void testResizeAdd() {
        System.out.println("3. Running resized add tests.\n");
//        int[] expected = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        ArrayDeque<Integer> ad1 = new ArrayDeque<>();

        for (int i = 0; i < 9; i += 1) {
            ad1.addLast(i);
        }
        ad1.printDeque();
    }

    public static void testGet() {
        System.out.print("4. Running Get tests.\n");
        ArrayDeque<Integer> ad = new ArrayDeque<>();
        ad.addFirst(0);
        ad.addFirst(1);
        ad.addLast(2);
        boolean passed = (ad.removeFirst() == 1);

        ad.addLast(4);
        passed = (ad.get(2) == 4) && passed;

        ad.addFirst(6);
        ad.addLast(7);
        ad.addFirst(8);
        ad.addLast(9);
        ad.addFirst(10);
        passed = (ad.get(1) == 8) && passed;

        passed = (ad.removeFirst() == 10) && passed;
        passed = (ad.removeFirst() == 8) && passed;
        passed = (ad.removeFirst() == 6) && passed;

        ad.addFirst(15);
        passed = (ad.removeLast() == 9) && passed;
        ad.addLast(17);
        passed = (ad.get(4) == 7) && passed;
        passed = (ad.removeFirst() == 15) && passed;
        passed = (ad.removeLast() == 17) && passed;
        passed = (ad.removeLast() == 7) && passed;

        printTestStatus(passed);
    }
    public static void main(String[] args) {
        System.out.println("Running tests.\n");
        addIsEmptySizeTest();
        addRemoveTest();
        testResizeAdd();
        testGet();
    }
}
