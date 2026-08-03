/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.testUtils

/**
 * Marks a test as expected to fail when run with the K1 ObjCExport implementation.
 *
 * @param expectCrash If `false` (default), failed test assertions ([org.opentest4j.AssertionFailedError]) are ignored,
 * but other exceptions are reported.
 * If `true`, it is the opposite: failed test assertions are reported, other exceptions are ignored.
 *
 * @see TodoAnalysisApi
 */
annotation class IgnoreK1(val expectCrash: Boolean = false)
