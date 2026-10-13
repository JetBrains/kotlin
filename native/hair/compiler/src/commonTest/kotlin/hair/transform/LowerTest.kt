package hair.transform

import hair.ir.*
import hair.ir.nodes.*
import hair.test.Cls
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LowerTest : IrTest {

    /**
     * Models `return p0 as Foo`. CheckCast is pinned to the control spine, so the following Return
     * uses it both as its control and as its value; lowering must re-link both.
     */
    @Test
    fun testCheckCastIsRemovedFromControl() = withTestSession {
        lateinit var obj: Node
        lateinit var ret: Return

        buildInitialIR {
            obj = Param(0)
            val cast = CheckCast(Cls("Foo"))(obj)
            ret = Return(cast) as Return
        }
        assertTrue(allNodes<CheckCast>().any(), "CheckCast must survive until lowering")

        lower()

        assertTrue(allNodes<CheckCast>().none(), "CheckCast must be lowered")
        assertEquals(obj, ret.result)
        assertEquals(entry, ret.control)
    }
}
