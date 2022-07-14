/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.project.model.infra.KpmCoreCasesTestRunner
import org.jetbrains.kotlin.project.model.infra.KpmTestCase

abstract class AbstractTheoreticalMppTestsWithSources : KGPBaseTest(), KpmCoreCasesTestRunner {
    fun runTest(kpmTestCase: KpmTestCase) {
        println("run test ${kpmTestCase.name}")
    }
}
