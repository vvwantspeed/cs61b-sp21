package deque;

import java.util.Comparator;
import java.util.Iterator;

public class MaxArrayDeque<T> extends ArrayDeque<T> {
    private Comparator cmp;
    private T[] items;
    private int size;
    private int nextFirst;
    private int nextLast;

    public MaxArrayDeque(Comparator<T> c) {
        size = 0;
        items = (T[]) new Object[8];
        nextFirst = 0;
        nextLast = 1;
        this.cmp = c;
    }

    public T max() {
        if (size == 0) {
            return null;
        }

        int index = addOne(nextFirst);
        T max = items[index];
        for (int i = 0; i < size; i++) {
            if (cmp.compare(items[index], max) > 0) {
                max = items[index];
            }
            index = addOne(index);
        }

        return max;
    }

    public T max(Comparator<T> c) {
        if (size == 0) {
            return null;
        }

        int index = addOne(nextFirst);
        T max = items[index];
        for (int i = 0; i < size; i++) {
            if (c.compare(items[index], max) > 0) {
                max = items[index];
            }
            index = addOne(index);
        }

        return max;
    }

    private int addOne(int index) {
        return (index + 1) % items.length;
    }
}


