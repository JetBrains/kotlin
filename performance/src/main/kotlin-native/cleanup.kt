package org.jetbrains.ring

import konan.internal.GC

fun cleanup() { GC.collect() }