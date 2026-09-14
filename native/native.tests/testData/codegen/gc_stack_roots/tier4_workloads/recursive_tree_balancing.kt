// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier4_workloads

import kotlin.native.runtime.GC
import kotlin.math.max

class AVLNode(val key: Int) {
    var height: Int = 1
    var left: AVLNode? = null
    var right: AVLNode? = null
}

fun height(n: AVLNode?): Int = n?.height ?: 0

fun balanceFactor(n: AVLNode?): Int = if (n == null) 0 else height(n.left) - height(n.right)

fun rotateRight(y: AVLNode): AVLNode {
    val x = y.left!!
    val t2 = x.right
    x.right = y
    y.left = t2
    y.height = max(height(y.left), height(y.right)) + 1
    x.height = max(height(x.left), height(x.right)) + 1
    return x
}

fun rotateLeft(x: AVLNode): AVLNode {
    val y = x.right!!
    val t2 = y.left
    y.left = x
    x.right = t2
    x.height = max(height(x.left), height(x.right)) + 1
    y.height = max(height(y.left), height(y.right)) + 1
    return y
}

fun insert(node: AVLNode?, key: Int): AVLNode {
    if (node == null) return AVLNode(key)

    if (key < node.key) {
        node.left = insert(node.left, key)
    } else if (key > node.key) {
        node.right = insert(node.right, key)
    } else {
        return node
    }

    node.height = 1 + max(height(node.left), height(node.right))
    val balance = balanceFactor(node)

    if (balance > 1 && key < node.left!!.key) return rotateRight(node)

    if (balance < -1 && key > node.right!!.key) return rotateLeft(node)

    if (balance > 1 && key > node.left!!.key) {
        node.left = rotateLeft(node.left!!)
        return rotateRight(node)
    }

    if (balance < -1 && key < node.right!!.key) {
        node.right = rotateRight(node.right!!)
        return rotateLeft(node)
    }

    return node
}

fun countNodes(node: AVLNode?): Int {
    if (node == null) return 0
    return 1 + countNodes(node.left) + countNodes(node.right)
}

fun box(): String {
    var root: AVLNode? = null

    val values = intArrayOf(
        40, 20, 60, 10, 30, 50, 70, 5, 15, 25, 35, 45, 55, 65, 75,
        2, 7, 12, 17, 22, 27, 32, 37, 42, 47, 52, 57, 62, 67, 72, 77
    )

    for (v in values) {
        root = insert(root, v)
        if (v % 15 == 0) {
            GC.collect()
        }
    }

    GC.collect()

    val total = countNodes(root)
    if (total != values.size) {
        return "FAIL AVL count: expected ${values.size}, got $total"
    }

    val h = height(root)
    if (h < 4 || h > 7) {
        return "FAIL AVL unbalanced height: $h"
    }

    return "OK"
}
