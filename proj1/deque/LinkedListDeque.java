package deque;

import java.util.Iterator;

public class LinkedListDeque<T> implements Deque<T>, Iterable<T> {
    private static class Node<T> {
        private Node prev;
        private T item;
        private Node next;

        Node() {
            item = null;
            prev = next = null;
        }

        Node(Node p, T i, Node n) {
            this.prev = p;
            this.item = i;
            this.next = n;
        }
    }

    private int size;
    private Node sentinel;

    /**
     * Creates an empty linked list deque
     */
    public LinkedListDeque() {
        size = 0;
        sentinel = new Node();
        sentinel.prev = sentinel.next = sentinel;
    }

    /**
     * Adds an item of type T to the front of the deque.
     * You can assume that item is never null.
     */
    @Override
    public void addFirst(T item) {
        Node tmp = new Node(sentinel, item, sentinel.next);
        sentinel.next.prev = tmp;
        sentinel.next = tmp;

        size += 1;
    }

    /**
     * Adds an item of type T to the back of the deque.
     * You can assume that item is never null.
     */
    @Override
    public void addLast(T item) {
        Node tmp = new Node(sentinel.prev, item, sentinel);
        sentinel.prev.next = tmp;
        sentinel.prev = tmp;

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
        Node p = sentinel.next;
        while (p != sentinel) {
            System.out.print(p.item + " ");
            p = p.next;
        }
        System.out.println();
    }

    /**
     * Removes and returns the item at the front of the deque.
     * If no such item exists, returns null.
     */
    @Override
    public T removeFirst() {
        Node tmp = sentinel.next;
        sentinel.next = sentinel.next.next;
        sentinel.next.prev = sentinel;

        size = (size == 0) ? size : size - 1;
        return (T) tmp.item;
    }

    /**
     * Removes and returns the item at the back of the deque.
     * If no such item exists, returns null.
     */
    @Override
    public T removeLast() {
        Node tmp = sentinel.prev;
        sentinel.prev = sentinel.prev.prev;
        sentinel.prev.next = sentinel;

        size = (size == 0) ? size : size - 1;
        return (T) tmp.item;
    }

    /**
     * Gets the item at the given index, where 0 is the front, 1 is the next item, and so forth.
     * If no such item exists, returns null.
     * Must not alter the deque!
     */
    @Override
    public T get(int index) {
        Node p = sentinel.next;
        while (p != sentinel && index >= 0) {
            p = p.next;
            index--;
        }
        return (index == 0) ? (T) p.item : null;
    }

    /**
     * Same as get, but uses recursion.
     */
    public T getRecursive(int index) {
        return getRecursiveHelper(sentinel.next, index);
    }

    private T getRecursiveHelper(Node p, int index) {
        if (p == sentinel) {
            return null;
        }
        if (index == 0) {
            return (T) p.item;
        }
        return getRecursiveHelper(p.next, index - 1);
    }

    /**
     * The Deque objects we’ll make are iterable (i.e. Iterable<T>)
     * so we must provide this method to return an iterator.
     */
    public Iterator<T> iterator() {
        return new LinkedListDequeIterator();
    }

    private class LinkedListDequeIterator implements Iterator<T> {
        private Node ptr;
        LinkedListDequeIterator() {
            ptr = sentinel.next;
        }
        public boolean hasNext() {
            return (ptr != sentinel);
        }
        public T next() {
            T item = (T) ptr.item;
            ptr = ptr.next;
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
        if (!(o instanceof LinkedListDeque)) {
            return false;
        }
        LinkedListDeque other = (LinkedListDeque) o;
        if (size != other.size()) {
            return false;
        }

        Node p = sentinel.next;
        for (Object item : other) {
            if (!((T) p.item).equals(item)) {
                return false;
            }
            p = p.next;
        }
        return true;
    }
}
