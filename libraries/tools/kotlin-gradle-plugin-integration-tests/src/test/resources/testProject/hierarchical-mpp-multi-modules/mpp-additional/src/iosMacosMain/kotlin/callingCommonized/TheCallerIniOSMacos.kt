// CHECK_HIGHLIGHTING
package callingCommonized

import kotlinx.cinterop.NativePtr
import platform.Accelerate.*
import platform.CoreFoundation.CFAllocatorGetTypeID
import platform.CoreFoundation.CFTypeID
import platform.CoreFoundation.__CFByteOrder
import platform.darwin.NSObject

class TheCallerIniOSMacos(p: NativePtr) {
    val eVal: CBLAS_TRANSPOSE = AtlasConj

    val eval2 = BNNSActivationFunctionAbs

    fun someWrapper() {
        CFAllocatorGetTypeID()
    }

    val enumCall: kotlinx.cinterop.CEnum = __CFByteOrder.CFByteOrderLittleEndian
}

 expect class WCommonizedCalls(pc: __CLPK_real) {

    val eClass: NSObject // = ACAccount
    val enumInteroped: kotlinx.cinterop.CEnum
}

class CallMyFields {
}
