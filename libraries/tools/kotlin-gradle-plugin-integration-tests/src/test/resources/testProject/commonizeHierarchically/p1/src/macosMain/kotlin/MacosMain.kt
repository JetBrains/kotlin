@file:Suppress("unused")

import kotlinx.cinterop.pointed
import platform.posix.stat
import withPosix.getMyStructPointer
import withPosix.getStructFromPosix
import withPosix.getStructPointerFromPosix

object MacosMain {
    val structFromPosix = getStructFromPosix()
    val structPointerFromPosix = getStructPointerFromPosix()

    object MyStruct {
        val struct = getMyStructPointer()?.pointed ?: error("Missing my struct")
        val posixProperty: stat = struct.posixProperty
        val longProperty: Long = struct.longProperty
        val doubleProperty: Double = struct.doubleProperty
        val int32tProperty: Int = struct.int32tProperty
        val int64TProperty: Long = struct.int64tProperty
        val appleOnlyProperty: Boolean = struct.appleOnlyProperty
    }
}
