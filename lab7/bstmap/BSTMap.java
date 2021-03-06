package bstmap;

import java.util.Iterator;
import java.util.Set;

public class BSTMap<K extends Comparable<K>, V> implements Map61B<K, V> {
    private class BSTNode {
        private K key;
        private V val;
        private int size;
        private BSTNode less, greater;

        public BSTNode(K key, V val, int size) {
            this.key = key;
            this.val = val;
            this.size = size;
        }
    }

    private BSTNode root;

    /**
     * Removes all of the mappings from this map.
     */
    @Override
    public void clear() {
        root = null;
    }

    @Override
    public boolean containsKey(K key) {
        return getNode(root, key) != null;
    }

    /**
     * return value of the key.
     */
    @Override
    public V get(K key) {
        BSTNode node = getNode(root, key);
        return node == null ? null : node.val;
    }

    // 获取key所在的node；不存在则返回null
    private BSTNode getNode(BSTNode node, K key) {
        if (node == null) {
            return null;
        }

        int cmp = key.compareTo(node.key);
        if (cmp < 0) {
            return getNode(node.less, key);
        } else if (cmp > 0) {
            return getNode(node.greater, key);
        } else {
            return node;
        }
    }

    @Override
    public int size() {
        return root == null ? 0 : root.size;
    }

    @Override
    public void put(K key, V value) {
        root = put(root, key, value);
    }

    private BSTNode put(BSTNode node, K key, V val) {
        if (node == null) {
            return new BSTNode(key, val, 1);
        }

        int cmp = key.compareTo(node.key);
        if (cmp < 0) {
            node.less = put(node.less, key, val);
            node.size += 1;
        } else if (cmp > 0) {
            node.greater = put(node.greater, key, val);
            node.size += 1;
        } else {
            node.val = val;
        }
        return node;
    }

    @Override
    public Set<K> keySet() throws UnsupportedOperationException {
        return null;
    }

    @Override
    public V remove(K key) throws UnsupportedOperationException {
        return null;
    }

    @Override
    public V remove(K key, V value) throws UnsupportedOperationException {
        return null;
    }

    /**
     * Returns an iterator over elements of type {@code T}.
     *
     * @return an Iterator.
     */
    @Override
    public Iterator<K> iterator() throws UnsupportedOperationException {
        return null;
    }

    /**
     * prints out your BSTMap in order of increasing Key
     */
    public void printInOrder() {

    }
}
