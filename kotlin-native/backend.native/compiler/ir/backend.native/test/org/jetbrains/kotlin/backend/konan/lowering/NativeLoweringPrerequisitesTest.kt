/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lowering

import org.jetbrains.kotlin.backend.common.LoweringPrerequisitesTest
import org.jetbrains.kotlin.backend.konan.driver.phases.getNativeLoweringPhaseListsForTests
import kotlin.test.Test

class NativeLoweringPrerequisitesTest : LoweringPrerequisitesTest() {
    @Test
    fun checkPrerequisites() {
        for (generateTestDumper in listOf(false, true)) {
            for (optimizationsEnabled in listOf(false, true)) {
                for (genericSafeCasts in listOf(false, true)) {
                    for (isCache in listOf(false, true)) {
                        checkPrerequisites(
                                getNativeLoweringPhaseListsForTests(
                                        generateTestDumper = generateTestDumper,
                                        optimizationsEnabled = optimizationsEnabled,
                                        genericSafeCasts = genericSafeCasts,
                                        isCache = isCache,
                                ).flatten()
                        )
                    }
                }
            }
        }
    }
}
