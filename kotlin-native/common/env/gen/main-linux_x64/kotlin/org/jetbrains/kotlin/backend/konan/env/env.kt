@file:JvmName("env")
@file:Suppress("UNUSED_VARIABLE", "UNUSED_EXPRESSION", "DEPRECATION")
@file:OptIn(ExperimentalForeignApi::class)
package org.jetbrains.kotlin.backend.konan.env

import kotlinx.cinterop.*

// NOTE THIS FILE IS AUTO-GENERATED

@ExperimentalForeignApi
fun setEnv(name: String?, value: String?): Unit {
    memScoped {
        return kniBridge0(name?.cstr?.getPointer(memScope).rawValue, value?.cstr?.getPointer(memScope).rawValue)
    }
}
private external fun kniBridge0(p0: NativePtr, p1: NativePtr): Unit
private val loadLibrary = loadKonanLibrary("orgjetbrainskotlinbackendkonanenvstubs")
