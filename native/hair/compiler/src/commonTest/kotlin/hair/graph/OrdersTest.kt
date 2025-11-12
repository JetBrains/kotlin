package hair.graph

import kotlin.test.*

class OrdersTest {

    @Test
    fun dfs() {
        // 0 -> 1 -> 3
        //   \       ||
        //    > 2 -> 4
        //        \  ||
        //         > 5
        val g = TestGraph().apply {
            n(0) to n(1) to n(3)
            n(0) to n(2) to n(4)
            n(2) to n(5)
            n(3) to n(4) to n(5)
            n(5) to n(4) to n(3)
        }.rootedAt(0)
        val dfs = dfs(g)
        assertContentEquals(arrayOf(0, 1, 3, 4, 5, 2), dfs.map {it.id}.toList().toTypedArray())
    }

    @Test
    fun postOrder() {
        val g = TestGraph().apply {
            n(0) to n(1) to n(3)
            n(0) to n(2) to n(4)
            n(2) to n(5)
            n(3) to n(4) to n(5)
            n(5) to n(4) to n(3)
        }.rootedAt(0)
        val postOrder = postOrder(g)
        assertContentEquals(arrayOf(5, 4, 3, 1, 2, 0), postOrder.map {it.id}.toList().toTypedArray())
    }
}