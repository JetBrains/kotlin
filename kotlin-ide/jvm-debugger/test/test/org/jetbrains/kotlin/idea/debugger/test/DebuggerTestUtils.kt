/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("DebuggerTestUtils")

package org.jetbrains.kotlin.idea.debugger.test

import org.jetbrains.kotlin.test.KotlinTestUtils

const val DEBUGGER_TESTDATA_PATH_RELATIVE = "idea/jvm-debugger/jvm-debugger-test/testData"

@JvmField
val DEBUGGER_TESTDATA_PATH_BASE = KotlinTestUtils.getHomeDirectory() + "/" + DEBUGGER_TESTDATA_PATH_RELATIVE