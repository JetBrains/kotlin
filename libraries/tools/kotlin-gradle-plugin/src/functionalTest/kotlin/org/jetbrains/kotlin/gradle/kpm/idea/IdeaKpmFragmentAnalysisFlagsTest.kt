/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.kpm.buildIdeaKpmProjectModel
import org.jetbrains.kotlin.gradle.kpm.idea.testFixtures.assertIsNotEmpty
import org.jetbrains.kotlin.tooling.core.extrasKeyOf
import org.junit.Test
import org.junit.jupiter.api.Assertions
import kotlin.test.assertNotNull

class IdeaKpmFragmentAnalysisFlagsTest : AbstractIdeaKpmFragmentsContentTest() {
    @Test
    fun testLanguageFeaturesConfigureFreeArgs() {
        doSetupProject()
        analysisFlagsAsFreeArgs.forEach { (expectedAnalysisFlags, freeArgsValue) ->
            testAnalysisFlagsDirectArgumentsClosure(expectedAnalysisFlags) {
                it.freeCompilerArgs += freeArgsValue
            }
        }
    }

    private fun testAnalysisFlagsDirectArgumentsClosure(
        expectedAnalysisFlags: IdeaKpmFragmentAnalysisFlags,
        configure: (KotlinCommonOptions) -> Unit
    ) = doTestAnalysisFlags(expectedAnalysisFlags) {
        configure(it.kotlinOptions)
    }

    private fun doTestAnalysisFlags(expectedAnalysisFlags: IdeaKpmFragmentAnalysisFlags, configure: (KotlinCompile<*>) -> Unit) {
        project.tasks.withType(KotlinCompile::class.java).forEach(configure)
        project.evaluate()
        kotlin.buildIdeaKpmProjectModel().assertIsNotEmpty().assertAnalysisFlags(expectedAnalysisFlags)
    }

    private fun IdeaKpmProject.assertAnalysisFlags(expectedAnalysisFlags: IdeaKpmFragmentAnalysisFlags) {
        modules.forEach { kpmModule ->
            kpmModule.fragments.forEach { kpmFragment ->
                val extraFlags = kpmFragment.extras[extrasKeyOf<IdeaKpmFragmentAnalysisFlags>()]
                assertNotNull(extraFlags, "Fragment '${kpmFragment.coordinates}' doesn't contain IdeaKpmFragmentAnalysisFlags extra")
                Assertions.assertIterableEquals(expectedAnalysisFlags.analysisFlags, extraFlags.analysisFlags)
            }
        }
    }

    companion object {

        private val analysisFlagsAsFreeArgs: Sequence<Pair<IdeaKpmFragmentAnalysisFlags, String>>
            get() = sequenceOf(
                IdeaKpmFragmentAnalysisFlagsImpl(
                    listOf(
                        IdeaKpmFragmentAnalysisFlagImpl("skipMetadataVersionCheck", "true"),
                        IdeaKpmFragmentAnalysisFlagImpl("skipPrereleaseCheck", "true")
                    )
                ) to "-Xskip-metadata-version-check",
                IdeaKpmFragmentAnalysisFlagsImpl(
                    listOf(
                        IdeaKpmFragmentAnalysisFlagImpl("skipPrereleaseCheck", "true")
                    )
                ) to "-Xskip-prerelease-check",
                IdeaKpmFragmentAnalysisFlagsImpl(
                    listOf(
                        IdeaKpmFragmentAnalysisFlagImpl("multiPlatformDoNotCheckActual", "true")
                    )
                ) to "-Xno-check-actual",

                IdeaKpmFragmentAnalysisFlagsImpl(
                    listOf(
                        IdeaKpmFragmentAnalysisFlagImpl("optIn", "kotlin.RequiresOptIn")
                    )
                ) to "-opt-in=kotlin.RequiresOptIn",

                IdeaKpmFragmentAnalysisFlagsImpl(
                    listOf(
                        IdeaKpmFragmentAnalysisFlagImpl("expectActualLinker", "true")
                    )
                ) to "-Xexpect-actual-linker",

                IdeaKpmFragmentAnalysisFlagsImpl(
                    listOf(
                        IdeaKpmFragmentAnalysisFlagImpl("explicitApiVersion", "true")
                    )
                ) to "-api-version 1.7", //TODO maybe vararg?

                IdeaKpmFragmentAnalysisFlagsImpl(
                    listOf(
                        IdeaKpmFragmentAnalysisFlagImpl("allowResultReturnType", "true")
                    )
                ) to "-Xallow-result-return-type",

                IdeaKpmFragmentAnalysisFlagsImpl(
                    listOf(
                        IdeaKpmFragmentAnalysisFlagImpl("explicitApi", "warning")
                    )
                ) to "-Xexplicit-api=warning",
                IdeaKpmFragmentAnalysisFlagsImpl(
                    listOf(
                        IdeaKpmFragmentAnalysisFlagImpl("extendedCompilerChecks", "true")
                    )
                ) to "-Xextended-compiler-checks",
                IdeaKpmFragmentAnalysisFlagsImpl(
                    listOf(
                        IdeaKpmFragmentAnalysisFlagImpl("allowKotlinPackage", "true")
                    )
                ) to "-Xallow-kotlin-package",
                IdeaKpmFragmentAnalysisFlagsImpl(
                    listOf(
                        IdeaKpmFragmentAnalysisFlagImpl("builtInsFromSources", "true")
                    )
                ) to "-Xbuiltins-from-sources",

                )
    }
}