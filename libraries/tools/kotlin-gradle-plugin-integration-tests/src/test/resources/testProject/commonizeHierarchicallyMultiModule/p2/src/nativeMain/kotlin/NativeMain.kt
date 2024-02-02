@file:Suppress("unused")

import kotlinx.cinterop.pointed
import platform.posix.stat
import withPosixOther.getMyStructPointer
import withPosixOther.getStructFromPosix
import withPosixOther.getStructPointerFromPosix

object NativeMain {
    val structFromPosix = getStructFromPosix()
    val structPointerFromPosix = getStructPointerFromPosix()

    object MyStruct {
        val struct = getMyStructPointer()?.pointed ?: error("Missing my struct")
        val posixProperty: stat = struct.posixProperty
        val longProperty: Long = struct.longProperty
        val doubleProperty: Double = struct.doubleProperty
    }
}
