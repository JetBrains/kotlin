package org.jetbrains.ring

import kotlin.native.internal.GC

actual fun cleanup() { GC.collect() }