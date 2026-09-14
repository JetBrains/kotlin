// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier2_boundaries.recursion

import kotlin.native.runtime.GC

class TreeNode(val value: Int, val left: TreeNode? = null, val right: TreeNode? = null)

fun sumTree(node: TreeNode?): Int {
    if (node == null) return 0
    val localRoot = node.value
    val leftSum = sumTree(node.left)
    val rightSum = sumTree(node.right)
    if (localRoot % 5 == 0) {
        GC.collect()
    }
    return localRoot + leftSum + rightSum
}

fun box(): String {
    val tree = TreeNode(1,
        TreeNode(2,
            TreeNode(4, TreeNode(8), TreeNode(9)),
            TreeNode(5, TreeNode(10), TreeNode(11))
        ),
        TreeNode(3,
            TreeNode(6, TreeNode(12), TreeNode(13)),
            TreeNode(7, TreeNode(14), TreeNode(15))
        )
    )

    val total = sumTree(tree)
    if (total != 120) return "FAIL sumTree: $total"
    return "OK"
}
