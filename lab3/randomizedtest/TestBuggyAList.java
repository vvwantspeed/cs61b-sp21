package randomizedtest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import edu.princeton.cs.algs4.StdRandom;

/**
 * Created by hug.
 */
public class TestBuggyAList {
  // YOUR TESTS HERE
    @Test
    public void testThreeAddThreeRemove() {
        AListNoResizing<Integer> lista = new AListNoResizing<>();
        BuggyAList<Integer> listb = new BuggyAList<>();

        lista.addLast(4);
        lista.addLast(5);
        lista.addLast(6);

        listb.addLast(4);
        listb.addLast(5);
        listb.addLast(6);

        assertEquals(lista.size(), listb.size());

        assertEquals(lista.removeLast(), listb.removeLast());
        assertEquals(lista.removeLast(), listb.removeLast());
        assertEquals(lista.removeLast(), listb.removeLast());
    }

    @Test
    public void randomizedTest() {
        AListNoResizing<Integer> L = new AListNoResizing<>();
        BuggyAList<Integer> B = new BuggyAList<>();

        int N = 50000;
        for (int i = 0; i < N; i += 1) {
            int operationNumber = StdRandom.uniform(0, 4); // returns a random integer in the range [0, 2)
            if (operationNumber == 0) {
                // addLast
                int randVal = StdRandom.uniform(0, 100);
                L.addLast(randVal);
                B.addLast(randVal);
                System.out.println("addLast(" + randVal + ")");
            } else if (operationNumber == 1) {
                // size
                int size = L.size();
                assertEquals(L.size(), B.size());
                System.out.println("size: " + size);
            }

            if (L.size() == 0)
                continue;

            if (operationNumber == 2) {
                // getLast
                int val = L.getLast();
                assertEquals(L.getLast(), B.getLast());
                System.out.println("getLast: " + val);
            } else if (operationNumber == 3) {
                // removeLast
                int val = L.removeLast();
                assertEquals(val, (int) B.removeLast());
                System.out.println("removeLast: " + val);
            }
        }
    }
}
