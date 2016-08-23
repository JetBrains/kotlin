class dfs_1_Node {
    var left: dfs_1_Node? = null
    var right: dfs_1_Node? = null
    var value: Int = 0
}

fun dfs_1_search(x: dfs_1_Node, value: Int): dfs_1_Node? {
    if (x.value == value) {
        return x
    }

    if (x.left != null) {
        val result = dfs_1_search(x.left!!, value)
        if (result != null) {
            return result
        }
    }
    if (x.right != null) {
        val result = dfs_1_search(x.right!!, value)
        if (result != null) {
            return result
        }
    }
    return null
}

fun dfs_1(searchObject: Int): Int {
    var i = 0
    val root: dfs_1_Node? = dfs_1_Node()
    root!!.value = 111
    var current = root
    while (i < 10) {
        current!!.left = dfs_1_Node()
        current.right = dfs_1_Node()
        current.value = i
        current = current.left
        i++
    }
    val result = dfs_1_search(root, searchObject)
    if (result == null) {
        return -101
    } else {
        return result!!.value
    }
}