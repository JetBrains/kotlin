/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This benchmark is a port of the V8 JavaScript benchmark suite
// splay benchmark:
//   https://chromium.googlesource.com/external/v8/+/ba56077937e154aa0adbabd8abb9c24e53aae85d/benchmarks/splay.js

// Copyright 2009 the V8 project authors. All rights reserved.
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//     * Redistributions of source code must retain the above copyright
//       notice, this list of conditions and the following disclaimer.
//     * Redistributions in binary form must reproduce the above
//       copyright notice, this list of conditions and the following
//       disclaimer in the documentation and/or other materials provided
//       with the distribution.
//     * Neither the name of Google Inc. nor the names of its
//       contributors may be used to endorse or promote products derived
//       from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// This benchmark is based on a JavaScript log processing module used
// by the V8 profiler to generate execution time profiles for runs of
// JavaScript applications, and it effectively measures how fast the
// JavaScript engine is at allocating nodes and reclaiming the memory
// used for old nodes. Because of the way splay trees work, the engine
// also has to deal with a lot of changes to the large tree object
// graph.

import kotlin.native.concurrent.*
import kotlin.concurrent.*
import kotlin.random.Random

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

class SplayBenchmark {
    // Seed random number generator for deterministic "random" number generation.
    val random = Random(20)
    val splayTreeSize = 8000;
    // Different from the original as the surrounding runner is different. We want
    // enough modifications that GCs will take place and will matter.
    val splayTreeModifications = 8000;
    val splayTreePayloadDepth = 5;
    val splayTree = splaySetup()

    fun generateKey(): Int = random.nextInt()

    fun generatePayloadTree(depth: Int, tag: String): Pair<Any, Any> {
        return if (depth == 0) {
            Pair(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), "String for key $tag in leaf node")
        } else {
            Pair(generatePayloadTree(depth - 1, tag), generatePayloadTree(depth - 1, tag))
        }
    }

    fun insertNewNode(tree: SplayTree<Int, Pair<Any, Any>>, payloadDepth: Int): Int {
        var key = generateKey()
        while (tree.find(key) != null) {
            key = generateKey()
        }
        var payload = generatePayloadTree(payloadDepth, "$key")
        tree.insert(key, payload)
        return key
    }

    fun splaySetup(): SplayTree<Int, Pair<Any, Any>> {
        val result = SplayTree<Int, Pair<Any, Any>>()
        for (i in 0 until splayTreeSize) insertNewNode(result, splayTreePayloadDepth)
        return result
    }

    fun splayTearDown() {
        val keys = splayTree.exportKeys()
        val length = keys.size
        if (length != splayTreeSize) {
            throw Exception("Splay tree has wrong size")
        }
        for (i in 0 until length - 1) {
            if (keys[i] >= keys[i + 1]) {
                throw Exception("Splay tree not sorted")
            }
        }
    }

    fun runSplay() {
        for (i in 0 until splayTreeModifications) {
            val key = insertNewNode(splayTree, splayTreePayloadDepth)
            val greatest = splayTree.findGreatestLessThan(key)
            if (greatest == null) {
                splayTree.remove(key)
            } else {
                splayTree.remove(greatest.key)
            }
        }
    }
}

class SplayBenchmarkUsingWorkers {
    val numberOfWorkers = 5;
    val workers = Array(numberOfWorkers, { _ -> Worker.start() })
    val splayTrees = Array(numberOfWorkers, { _ -> SplayBenchmark() });

    fun runSplayWorkers() {
        val futures = Array(numberOfWorkers) {i -> workers[i].execute(TransferMode.SAFE, { splayTrees[i] }, {it.runSplay()})};
        futures.forEach{it.consume {}};
    }

    fun splayTearDownWorkers() {
        val futures = Array(numberOfWorkers) {i -> workers[i].execute(TransferMode.SAFE, { splayTrees[i] }, {it.splayTearDown()})};
        futures.forEach{it.consume {}};
        workers.forEach { it.requestTermination().result }
    }
}

class SplayBenchmarkWithMarkHelpers {
    val numberOfMarkHelpers = 5;
    val markHelpers = Array(numberOfMarkHelpers, { _ -> Worker.start() })

    @Volatile
    var done = false
    val markHelperJobs = markHelpers.map {
        it.execute(TransferMode.SAFE, { this }) {
            // run some thread-local work in a loop without allocations or external calls
            fun fib(n: Int): Int {
                if (n == 0) return 0
                var prev = 0
                var cur = 1
                for (i in 2..n) {
                    val next = cur + prev
                    prev = cur
                    cur = next
                }
                return cur
            }

            var sum = 0
            while (!it.done) {
                sum += fib(100)
            }
            return@execute sum
        }
    }

    val splay = SplayBenchmark()

    fun runSplayWithMarkHelpers() {
        splay.runSplay()
    }

    fun splayTearDownMarkHelpers() {
        done = true
        splay.splayTearDown()
        markHelperJobs.forEach { it.result }
        markHelpers.forEach { it.requestTermination().result }
    }
}
