// CHECK_HIGHLIGHTING
package callingCommonized

import kotlinx.cinterop.CEnum
import platform.Accelerate.AtlasConj
import platform.Accelerate.CBLAS_TRANSPOSE
import platform.Accelerate.FFTDirection
import platform.Accelerate.__CLPK_real
import platform.Accounts.ACAccount
import platform.CoreFoundation.CFAllocatorGetTypeID
import platform.CoreFoundation.CFTypeID
import platform.CoreFoundation.__CFByteOrder
import platform.Foundation.NSLog
import platform.darwin.ABDAY_1
import platform.darwin.NSObject
import platform.darwin.PLATFORM_IOS
import platform.posix.DBL_MIN

actual class WCommonizedCalls actual constructor(pc: __CLPK_real) {

    val eFunCall: CFTypeID = CFAllocatorGetTypeID() // create actual doesn't work because of this
    actual val eClass: NSObject = ACAccount()
    actual val enumInteroped: CEnum = __CFByteOrder.CFByteOrderLittleEndian

    val somel = NSLog("")

    val dfg = DBL_MIN

    val device get() = platform.UIKit.UIDevice.currentDevice

    val eVal: CBLAS_TRANSPOSE = AtlasConj

}
