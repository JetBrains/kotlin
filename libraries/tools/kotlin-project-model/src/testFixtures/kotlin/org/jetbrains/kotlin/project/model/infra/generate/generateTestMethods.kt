/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model.infra.generate

import org.jetbrains.kotlin.generators.*
import org.jetbrains.kotlin.generators.model.*
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.project.model.infra.KpmCoreCasesTestRunner
import java.io.File

fun generateKpmTestCases(
    dryRun: Boolean = false,
    init: TestGroupSuite.() -> Unit,
) {
    val suite = TestGroupSuite(DefaultTargetBackendComputer).apply {
        init()
    }
    val mainClassName = TestGeneratorUtil.getMainClassName()
    suite.forEachTestClassParallel { testClass ->
        val (changed, testSourceFilePath) = NewTestGeneratorImpl(
            listOf(KpmCoreCaseTestMethodGenerator)
        ).generateAndSave(testClass, dryRun, mainClassName)
        if (changed) {
            InconsistencyChecker.inconsistencyChecker(dryRun).add(testSourceFilePath)
        }
    }
}

// NB: [testRunnersSourceRoot] is a root-folder against which will be resolved a usual java-like folder structure
// based on packages of T
// E.g.: testRunnersSourceRoot = "foo/bar", T == "org.jetbrains.kotlin.AbstractMyClass"
// Then the resulting file will be created at "foo/bar/org/jetbrains/kotlin"
inline fun <reified T : KpmCoreCasesTestRunner> TestGroupSuite.kpmRunnerWithSources(testRunnersSourceRoot: String, testSourcesPath: String) {
    testGroup(testRunnersSourceRoot, testSourcesPath) {
        testClass<T> {
            testModels += KpmCoreCasesTestClassModel(File(testSourcesPath))
        }
    }
}

internal fun generateTestMethodsTemplateForCases(cases: Set<String>, generateTestMethodBody: (String) -> String = { "TODO()" }): String {
    return buildString {
        for (case in cases) {
            append(
                """
                    fun test$case(case: KpmTestCase) {
                        ${generateTestMethodBody(case)}
                    }
                    
                    """.trimIndent()
            )
        }
    }
}
