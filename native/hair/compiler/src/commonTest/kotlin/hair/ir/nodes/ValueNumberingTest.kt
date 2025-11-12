package hair.ir.nodes

import hair.ir.*
import kotlin.test.*

class ValueNumberingTest : IrTest {

    @Test
    fun testConst() = withTestSession {
        buildInitialIR {
            val c1 = ConstI(0)
            val c2 = ConstI(0)
            assertSame(c1, c2)
            BlockEntry()
        }
    }

    @Test
    fun testAfterReplace() = withTestSession{
        buildInitialIR {
            val p0 = Param(0)
            val p1 = Param(1)
            val p2 = Param(2)

            val a1 = AddI(p0, p1) as AddI
            val a2 = AddI(p0, p2) as AddI
            assertNotSame(a1, a2)

            val u1 = Use(a1)
            val u2 = Use(a2)

            modifyIR {
                a2.rhs = p1
            }
            assertSame(u1.value, u2.value)
        }
    }
}