/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.membench

import org.jetbrains.benchmarksLauncher.Random

// TODO replace with Splay
class Tree(scale: Int) : Workload {
    private val size = 1024 * scale
    private val data = {
        val splay = SplayTree<Int, Any?>()
        for (i in 0 until size) {
            splay.insert(i, null)
        }
        splay
    }()

    companion object : WorkloadProvider<Tree> {
        override fun name(): String = "Tree"
        override fun allocate(scale: Int) = Tree(scale)
    }
}

// TODO add proper copyrights and other stuff from SplayBenchmark

// A splay tree is a self-balancing binary search tree with the additional
// property that recently accessed elements are quick to access again.
// It performs basic operations such as insertion, look-up and removal in
// O(log(n)) amortized time.
class SplayTree<K: Comparable<K>, V> {

    // Nodes of the splay tree.
    class Node<K: Comparable<K>, V>(val key: K, val value: V) {
        var left: Node<K, V>? = null
        var right: Node<K, V>? = null

        // Performs an ordered traversal of the subtree starting at this SplayTree.Node.
        fun traverse(f: (Node<K, V>) -> Unit) {
            var current: Node<K, V>? = this
            while (current != null) {
                current.left?.traverse(f)
                f(current)
                current = current.right
            }
        }
    }

    // Root of the splay tree.
    private var root: Node<K, V>? = null

    // Return whether the splay tree is empty.
    fun isEmpty() = root == null

    // Inserts a node into the tree with the specified key and value if
    // the tree does not already contain a node with the specified key. If
    // the value is inserted, it becomes the root of the tree.
    fun insert(key: K, value: V) {
        if (isEmpty()) {
            root = Node(key, value)
            return
        }
        // Splay on the key to move the last node on the search path for
        // the key to the root of the tree.
        splay(key)
        val r = root!!
        if (r.key == key) {
            return
        }
        val node = Node(key, value)
        if (key > r.key) {
            node.left = r
            node.right = r.right
            r.right = null
        } else {
            node.right = r
            node.left = r.left
            r.left = null
        }
        root = node
    }

    // Removes a node with the specified key from the tree if the tree
    // contains a node with this key. The removed node is returned. If the
    // key is not found, an exception is thrown.
    fun remove(key: K): Node<K, V> {
        if (this.isEmpty()) {
            throw Exception("Key not found: $key")
        }
        splay(key)
        val r = root!!
        if (r.key != key) {
            throw Exception("Key not found: $key")
        }
        val removed = r
        if (r.left == null) {
            root = r.right
        } else {
            val right = r.right
            root = r.left
            // Splay to make sure that the new root has an empty right child.
            splay(key)
            // Insert the original right child as the right child of the new root.
            root!!.right = right
        }
        return removed
    }

    // Returns the node having the specified key or null if the tree doesn't contain
    // a node with the specified key.
    fun find(key: K): Node<K, V>? {
        if (isEmpty()) return null
        splay(key)
        return if (root!!.key == key) root else null
    }

    // Returns node having the maximum key value.
    fun findMax(startNode: Node<K, V>? = null): Node<K, V>? {
        if (isEmpty()) return null
        var current = startNode ?: root!!
        while (current.right != null) {
            current = current.right!!
        }
        return current
    }

    // Returns node having the maximum key value that is less than the
    // specified key value.
    fun findGreatestLessThan(key: K): Node<K, V>? {
        if (isEmpty()) return null
        // Splay on the key to move the node with the given key or the last
        // node on the search path to the top of the tree.
        splay(key)
        // Now the result is either the root node or the greatest node in the
        // left subtree.
        val r = root!!
        return if (r.key < key) {
            root
        } else if (r.left != null) {
            findMax(r.left)
        } else {
            null
        }
    }

    // Returns a list containing all the keys in the tree's nodes.
    fun exportKeys(): List<K> {
        val result = mutableListOf<K>()
        if (!isEmpty()) {
            root?.traverse { result.add(it.key) }
        }
        return result
    }

    // Perform the splay operation for the given key. Moves the node with
    // the given key to the top of the tree.  If no node has the given
    // key, the last node on the search path is moved to the top of the
    // tree. This is the simplified top-down splaying algorithm from:
    // "Self-adjusting Binary Search Trees" by Sleator and Tarjan
    private fun splay(key: K) {
        if (isEmpty()) return
        // Create a dummy node.  The use of the dummy node is a bit
        // counter-intuitive: The right child of the dummy node will hold
        // the L tree of the algorithm.  The left child of the dummy node
        // will hold the R tree of the algorithm.  Using a dummy node, left
        // and right will always be nodes and we avoid special cases.
        // The key and value for the dummy node will not be used, so we just
        // use the key and value of the root node for the dummy.
        val dummy = Node(root!!.key, root!!.value)
        var left = dummy
        var right = dummy

        var current = root!!
        while (true) {
            if (key < current.key) {
                if (current.left == null) {
                    break
                }
                if (key < current.left!!.key) {
                    // Rotate right
                    val tmp = current.left!!
                    current.left = tmp.right
                    tmp.right = current
                    current = tmp
                    if (current.left == null) {
                        break
                    }
                }
                // Link right.
                right.left = current
                right = current
                current = current.left!!
            } else if (key > current.key) {
                if (current.right == null) {
                    break
                }
                if (key > current.right!!.key) {
                    // Rotate left.
                    val tmp = current.right!!
                    current.right = tmp.left
                    tmp.left = current
                    current = tmp
                    if (current.right == null) {
                        break
                    }
                }
                // Link left.
                left.right = current
                left = current
                current = current.right!!
            } else {
                break
            }
        }
        // Assemble.
        left.right = current.left
        right.left = current.right
        current.left = dummy.right
        current.right = dummy.left
        root = current
    }
}
