package org.jetbrains.ring

import kotlin.native.runtime.GC

@OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
actual fun cleanup() { GC.collect() }