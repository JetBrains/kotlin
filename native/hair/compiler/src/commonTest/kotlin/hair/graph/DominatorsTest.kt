package hair.graph

import kotlin.test.*

class DominatorsTest {

    @Test
    fun testSFDA() { // from the paper
        val g = TestGraph().apply {
            n(0) to n(1) to n(3)
            n(0) to n(2) to n(4)
            n(2) to n(5)
            n(3) to n(4) to n(5)
            n(5) to n(4) to n(3)
        }.rootedAt(0)
        val doms = Dominators.sfda(g)
        for (n in postOrder(g)) {
            assertEquals(0, doms.idom(n).id, "Incorrect dominator for node $n")
        }
    }

    @Test
    fun testDiamond() {
        val g = TestGraph().apply {
            n(0) to n(1) to n(3)
            n(0) to n(2) to n(3)
        }.rootedAt(0)
        val doms = Dominators.sfda(g)
        for (n in postOrder(g)) {
            assertEquals(0, doms.idom(n).id, "Incorrect dominator for node $n")
        }
    }

    @Test
    fun testWiki() {
        //      +---------+
        //      V         |
        // 0 -> 1 -> 2 -> 4
        //      | \       ^
        //      V  > 3 --/
        //      5
        val g = TestGraph().apply {
            n(0) to n(1) to n(2) to n(4)
            n(1) to n(3) to n(4)
            n(4) to n(1)
            n(1) to n(5)
        }
        val doms = Dominators.sfda(g.rootedAt(0))
        assertEquals(0, doms.idom(g.n(1)).id)
        assertEquals(1, doms.idom(g.n(2)).id)
        assertEquals(1, doms.idom(g.n(3)).id)
        assertEquals(1, doms.idom(g.n(4)).id)
        assertEquals(1, doms.idom(g.n(5)).id)
    }
}