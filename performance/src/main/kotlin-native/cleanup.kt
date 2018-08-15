package org.jetbrains.ring

import kotlin.native.internal.GC

fun cleanup() { GC.collect() }