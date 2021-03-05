package deque;

import java.util.Comparator;
import java.util.Iterator;

public class MaxArrayDeque<T> implements Deque<T>, Iterable<T> {
    private Comparator cmp;
    private T[] items;
    private int size;
    private int nextFirst;
    private int nextLast;

    /**
     * Creates an empty linked list deque
     */
    public MaxArrayDeque() {
        size = 0;
        items = (T[]) new Object[8];
        nextFirst = 0;
        nextLast = 1;
    }

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
    private int minusOne(int index) {
        return (index + items.length - 1) % items.length;
    }

    /**
     * Adds an item of type T to the front of the deque.
     * You can assume that item is never null.
     */
    @Override
    public void addFirst(T item) {
        items[nextFirst] = item;
        nextFirst = minusOne(nextFirst);
        size += 1;
    }

    /**
     * Adds an item of type T to the back of the deque.
     * You can assume that item is never null.
     */
    @Override
    public void addLast(T item) {
        items[nextLast] = item;
        nextLast = addOne(nextLast);
        size += 1;
    }

    /**
     * Returns the number of items in the deque.
     * @return
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * Prints the items in the deque from first to last, separated by a space.
     * Once all the items have been printed, print out a new line.
     */
    @Override
    public void printDeque() {
        int index = addOne(nextFirst);
        for (int i = 0; i < size; i++) {
            System.out.print(items[index] + " ");
            index = addOne(index);
        }
        System.out.println();
    }

    /**
     * Removes and returns the item at the front of the deque.
     * If no such item exists, returns null.
     */
    @Override
    public T removeFirst() {
        if (size == 0) {
            return null;
        }

        nextFirst = addOne(nextFirst);
        T item = items[nextFirst];
        items[nextFirst] = null;
        size -= 1;
        return item;
    }

    /**
     * Removes and returns the item at the back of the deque.
     * If no such item exists, returns null.
     */
    @Override
    public T removeLast() {
        if (size == 0) {
            return null;
        }

        nextLast = minusOne(nextLast);
        T item = items[nextLast];
        items[nextLast] = null;
        size -= 1;
        return item;
    }

    /**
     * Gets the item at the given index, where 0 is the front, 1 is the next item, and so forth.
     * If no such item exists, returns null.
     * Must not alter the deque!
     */
    @Override
    public T get(int index) {
//        if (size <= index || index < 0) {
//            return null;
//        }
        return items[(nextFirst + 1 + index) % items.length];
    }

    /**
     * The Deque objects we’ll make are iterable (i.e. Iterable<T>)
     * so we must provide this method to return an iterator.
     */
    @Override
    public Iterator<T> iterator() {
        return new MaxArrayDequeIterator();
    }

    private class MaxArrayDequeIterator implements Iterator<T> {
        private int ptr;

        MaxArrayDequeIterator() {
            ptr = addOne(nextFirst);
        }
        public boolean hasNext() {
            return ptr != nextLast;
        }
        public T next() {
            T item =  items[ptr];
            ptr = addOne(ptr);
            return item;
        }
    }

    /**
     * Returns whether or not the parameter o is equal to the Deque.
     * o is considered equal if it is a Deque and if it contains the same contents
     * (as goverened by the generic T’s equals method) in the same order.
     * (ADDED 2/12: You’ll need to use the instance of keywords for this.)
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MaxArrayDeque)) {
            return false;
        }

        MaxArrayDeque other = (MaxArrayDeque) o;
        if (size != other.size() || !cmp.equals(other.cmp)) {
            return false;
        }

        int index = addOne(nextFirst);
        for (Object item : other) {
            if (!(items[index].equals(item))) {
                return false;
            }
        }
        return true;
    }
}

