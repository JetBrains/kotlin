/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("JUnitTestCaseWithNoTests")

package org.jetbrains.kotlin.abi.tools.tests

import org.jetbrains.kotlin.abi.tools.tests.utils.BinariesType
import org.jetbrains.kotlin.abi.tools.tests.utils.CasesTestBase

class CasesJvmTest : CasesTestBase("cases", BinariesType.JVM)

class CasesKlibTest : CasesTestBase("cases-klib", BinariesType.KLIB)
