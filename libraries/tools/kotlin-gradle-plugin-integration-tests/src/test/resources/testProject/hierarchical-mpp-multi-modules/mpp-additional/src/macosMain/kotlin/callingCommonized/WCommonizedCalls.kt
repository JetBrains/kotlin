// CHECK_HIGHLIGHTING
package callingCommonized

import kotlinx.cinterop.CEnum
import platform.Accelerate.AtlasConj
import platform.Accelerate.CBLAS_TRANSPOSE
import platform.Accelerate.__CLPK_real
import platform.CoreFoundation.CFAllocatorGetTypeID
import platform.CoreFoundation.CFTypeID
import platform.darwin.NSObject
import platform.posix.ns_r_notauth

actual class WCommonizedCalls actual constructor(pc: __CLPK_real) {

    val eFunCall: CFTypeID = CFAllocatorGetTypeID()
    actual val eClass: NSObject
        get() = TODO("Not yet implemented")
    actual val enumInteroped: CEnum
        get() = TODO("Not yet implemented")

    val eVal: CBLAS_TRANSPOSE = AtlasConj

    val theCall = ns_r_notauth


}
