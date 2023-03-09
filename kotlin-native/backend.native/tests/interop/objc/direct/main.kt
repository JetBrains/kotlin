import direct.*
import kotlinx.cinterop.*
import kotlin.test.*

class CallingConventionsNativeHeir() : CallingConventions() {
    // nothing
}

typealias CC = CallingConventions
typealias CCH = CallingConventionsHeir
typealias CCN = CallingConventionsNativeHeir

// KT-54610
fun main(args: Array<String>) {
    autoreleasepool {
        val cc = CC()
        val cch = CCH()
        val ccn = CCN()

        assertEquals(42UL, CC.regular(42u))
        assertEquals(42UL, cc.regular(42u))
        assertEquals(42UL, CC.regularExt(42u))
        assertEquals(42UL, cc.regularExt(42u))

        assertEquals(42UL, CCH.regular(42u))
        assertEquals(42UL, cch.regular(42u))
        assertEquals(42UL, CCH.regularExt(42u))
        assertEquals(42UL, cch.regularExt(42u))

        assertEquals(42UL, ccn.regular(42UL))
        assertEquals(42UL, ccn.regularExt(42UL))

        assertEquals(42UL, CC.direct(42u))
        assertEquals(42UL, cc.direct(42u))
        assertEquals(42UL, CC.directExt(42u))
        assertEquals(42UL, cc.directExt(42u))

        assertEquals(42UL, CCH.direct(42u))
        assertEquals(42UL, cch .direct(42u))
        assertEquals(42UL, CCH.directExt(42u))
        assertEquals(42UL, cch .directExt(42u))

        assertEquals(42UL, ccn .direct(42UL))
        assertEquals(42UL, ccn .directExt(42UL))
    }
}
