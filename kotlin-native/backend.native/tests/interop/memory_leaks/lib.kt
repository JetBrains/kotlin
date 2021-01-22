/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlinx.cinterop.*
import kotlin.native.Platform

fun enableMemoryChecker() {
    Platform.isMemoryLeakCheckerActive = true
}

fun leakMemory() {
    StableRef.create(Any())
}
