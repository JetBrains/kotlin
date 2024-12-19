@file:JvmName("files")
@file:Suppress("UNUSED_VARIABLE", "UNUSED_EXPRESSION", "DEPRECATION")
@file:OptIn(ExperimentalForeignApi::class)
package org.jetbrains.kotlin.backend.konan.files

import kotlinx.cinterop.*

// NOTE THIS FILE IS AUTO-GENERATED

@ExperimentalForeignApi
fun renameAtomic(from: String?, to: String?, replaceExisting: Boolean): Boolean {
    memScoped {
        return kniBridge0(from?.cstr?.getPointer(memScope).rawValue, to?.cstr?.getPointer(memScope).rawValue, replaceExisting.toByte()).toBoolean()
    }
}

@ExperimentalForeignApi
val __bool_true_false_are_defined: Int get() = 1

@ExperimentalForeignApi
val `true`: Int get() = 1

@ExperimentalForeignApi
val `false`: Int get() = 0
private external fun kniBridge0(p0: NativePtr, p1: NativePtr, p2: Byte): Byte
private val loadLibrary = loadKonanLibrary("orgjetbrainskotlinbackendkonanfilesstubs")
