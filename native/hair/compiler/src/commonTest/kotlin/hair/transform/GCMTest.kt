package hair.transform

import hair.ir.*
import hair.ir.nodes.*
import hair.sym.HairType
import hair.test.Cls
import hair.test.Fld
import hair.test.Glb
import kotlin.test.Test
import kotlin.test.assertTrue

class GCMTest : IrTest {

    /**
     * Models `b.value = 42; return b.value` — a store immediately followed by a load on the same
     * field.  Both are PinnedMemoryOp (Controlled+Controlling), so they are chained through the
     * control spine and the linearizer must emit the store before the load.
     */
    @Test
    fun testStoreFieldBeforeLoadField() = withTestSession {
        val field = Fld("value", HairType.INT, Cls("Box"))
        lateinit var store: StoreField
        lateinit var load: LoadField

        buildInitialIR {
            val obj = Param(0)
            val value = ConstI(42)
            store = StoreField(field)(obj, value) as StoreField
            load = LoadField(field)(obj) as LoadField
            Return(load)
        }

        val gcm = performGCM(this)
        val order = gcm.linearOrder(entry)
        val storeIdx = order.indexOf(store)
        val loadIdx = order.indexOf(load)
        assertTrue(storeIdx >= 0, "StoreField must appear in the linearized order")
        assertTrue(loadIdx >= 0, "LoadField must appear in the linearized order")
        assertTrue(storeIdx < loadIdx,
            "StoreField (pos $storeIdx) must come before LoadField (pos $loadIdx)")
    }

    /**
     * Models `counter = counter + 1` — a load, an arithmetic op, and a store back to the same
     * global.  The data-dependency chain already imposes the correct order, and the linearizer
     * must preserve it.
     */
    @Test
    fun testLoadGlobalBeforeStoreGlobal() = withTestSession {
        val global = Glb("counter", HairType.INT)
        lateinit var load: LoadGlobal
        lateinit var store: StoreGlobal

        buildInitialIR {
            load = LoadGlobal(global) as LoadGlobal
            val inc = Add(HairType.INT)(load, ConstI(1))
            store = StoreGlobal(global)(inc) as StoreGlobal
            ReturnVoid()
        }

        val gcm = performGCM(this)
        val order = gcm.linearOrder(entry)
        val loadIdx = order.indexOf(load)
        val storeIdx = order.indexOf(store)
        assertTrue(loadIdx >= 0, "LoadGlobal must appear in the linearized order")
        assertTrue(storeIdx >= 0, "StoreGlobal must appear in the linearized order")
        assertTrue(loadIdx < storeIdx,
            "LoadGlobal (pos $loadIdx) must come before StoreGlobal (pos $storeIdx)")
    }
}
