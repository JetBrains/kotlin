/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kpm

import org.jetbrains.kotlin.gradle.AbstractTheoreticalMppTestsWithSources
import org.jetbrains.kotlin.project.model.infra.generate.generateKpmTestCases
import org.jetbrains.kotlin.project.model.infra.generate.kpmRunnerWithSources

fun main() {
    generateKpmTestCases {
        kpmRunnerWithSources<AbstractTheoreticalMppTestsWithSources>(
            "libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/java",
            "libraries/tools/kotlin-gradle-plugin-integration-tests/testData/kpm"
        )
    }
}
