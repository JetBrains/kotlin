/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.ReadyForExecution
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.Companion.COMMON_MAIN_SOURCE_SET_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.Companion.COMMON_TEST_SOURCE_SET_NAME
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.MultipleSourceSetRootsInCompilation
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.plugin.mpp.isTest
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.plugin.sources.withDependsOnClosure

internal object MultipleSourceSetRootsInCompilationChecker : KotlinGradleProjectChecker {

    private fun KotlinCompilation<*>.sourceSetRoots() = kotlinSourceSets.withDependsOnClosure.filter { it.dependsOn.isEmpty() }

    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        // Await for the last Stage and perform check to ensure that the final state is correct.
        ReadyForExecution.await()

        val targets = multiplatformExtension?.awaitTargets() ?: return

        val (allDefaultCompilationsWithMultipleRoots, allNonDefaultCompilationsWithMultipleRoots) = targets
            // Exclude metadata target because users don't declare it explicitly, and we don't want to ask them to configure it.
            // If some metadata compilation has multiple source set roots,
            // then underlying platform compilations should report the same.
            .filter { it.platformType != KotlinPlatformType.common }
            .flatMap { it.compilations }
            .filter { it.sourceSetRoots().size > 1 }
            .partition { it.isMain() || it.isTest() }

        collector.reportForDefaultPlatformCompilations(allDefaultCompilationsWithMultipleRoots)
        collector.reportForNonDefaultCompilations(allNonDefaultCompilationsWithMultipleRoots)
    }

    /**
     * Report for 'main' and 'test' compilations.
     * These are special because we know that all source sets should depend on `commonMain` or `commonTest` accordingly.
     */
    private fun KotlinToolingDiagnosticsCollector.reportForDefaultPlatformCompilations(compilations: Collection<KotlinCompilation<*>>) {
        // Some source sets can be included in multiple compilations we don't want to report a diagnostic for each case.
        val alreadyReportedSourceSet = mutableSetOf<KotlinSourceSet>()

        for (compilation in compilations) {
            val expectedSourceSetRoot = when {
                compilation.isMain() -> COMMON_MAIN_SOURCE_SET_NAME
                compilation.isTest() -> COMMON_TEST_SOURCE_SET_NAME
                else -> continue
            }

            val unexpectedSourceSetRoots = compilation.sourceSetRoots().filter { it.name != expectedSourceSetRoot }
            // In most cases, I expect to have only 1 unexpected source set root.
            // So it is ok to report diagnostic for each unexpectedSourceSetRoot.
            unexpectedSourceSetRoots.forEach { unexpectedSourceSetRoot ->
                if (!alreadyReportedSourceSet.add(unexpectedSourceSetRoot)) return@forEach

                val includedIntoCompilations = unexpectedSourceSetRoot.internal
                    .compilations
                    .filter { it.platformType != KotlinPlatformType.common }
                if (includedIntoCompilations.isEmpty()) return@forEach // this case is handled by a different diagnostic
                val singleCompilation = includedIntoCompilations.singleOrNull()

                val diagnostic = if (singleCompilation != null) {
                    MultipleSourceSetRootsInCompilation(
                        singleCompilation,
                        unexpectedSourceSetRoot.name,
                        expectedSourceSetRoot
                    )
                } else {
                    MultipleSourceSetRootsInCompilation(
                        targetNames = includedIntoCompilations.map { it.target.name },
                        unexpectedSourceSetRoot.name,
                        expectedSourceSetRoot
                    )
                }

                report(compilation.project, diagnostic)
            }
        }
    }

    /**
     * For non-default compilations, we don't know which of the multiple source set roots should win so report diagnostic differently
     */
    private fun KotlinToolingDiagnosticsCollector.reportForNonDefaultCompilations(compilations: Collection<KotlinCompilation<*>>) {
        for (compilation in compilations) {
            // Exclude android compilations as they might incorrectly report due to connection to android source sets
            // where no dependsOn relations don't have to be declared.
            if (compilation.target.platformType == KotlinPlatformType.androidJvm) continue
            val diagnostic = MultipleSourceSetRootsInCompilation(compilation, compilation.sourceSetRoots().map { it.name })
            report(compilation.project, diagnostic)
        }
    }
}