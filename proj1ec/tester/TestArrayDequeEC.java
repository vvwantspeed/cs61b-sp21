package tester;

import static org.junit.Assert.*;

import edu.princeton.cs.algs4.StdRandom;
import org.junit.Test;
import student.StudentArrayDeque;

public class TestArrayDequeEC {

    @Test
    public void testDeque() {
        StudentArrayDeque<Integer> st = new StudentArrayDeque<>();
        ArrayDequeSolution<Integer> ad = new ArrayDequeSolution<>();

        int n = 5000;
        String str = "\n";
        for (int i = 0; i < n; i++) {
            int opt = StdRandom.uniform(0, 4);
            if (opt == 0) {
                // addFirst
                int num = StdRandom.uniform(0, 100);
                st.addFirst(num);
                ad.addFirst(num);
                str += "addFirst(" + num + ")\n";
            } else if (opt == 1) {
                // addLast
                int num = StdRandom.uniform(0, 100);
                st.addLast(num);
                ad.addLast(num);
                str += "addLast(" + num + ")\n";
            }

            if (st.isEmpty()) {
                continue;
            }

            if (opt == 2) {
                // removeFirst
                str += "removeFirst()\n";
                assertEquals(str, ad.removeFirst(), st.removeFirst());
            } else if (opt == 3){
                // removeLast
                str += "removeLast()\n";
                assertEquals(str, ad.removeLast(), st.removeLast());
            }
        }
    }
}
