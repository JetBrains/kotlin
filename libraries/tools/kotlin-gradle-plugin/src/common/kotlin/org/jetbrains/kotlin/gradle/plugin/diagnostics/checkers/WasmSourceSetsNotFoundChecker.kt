/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.gradle.api.UnknownDomainObjectException
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.configurationResult
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.tooling.core.withClosure

internal object WasmSourceSetsNotFoundChecker : KotlinGradleProjectChecker {

    private val wasmSourceSetRegex by lazy { Regex("""(wasmMain|wasmTest)\w*""") }

    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        val failure = project.configurationResult.await() as? KotlinPluginLifecycle.ProjectConfigurationResult.Failure ?: return

        val unknownWasmSourceSetNames = failure
            .failures
            .withClosure<Throwable> { listOfNotNull(it.cause) }
            .filterIsInstance<UnknownDomainObjectException>()
            .filter { it.message.orEmpty().contains("KotlinSourceSet") }
            .mapNotNull { wasmSourceSetRegex.find(it.message.orEmpty()) }
            .map { it.value }

        unknownWasmSourceSetNames.forEach { unknownWasmSourceSetName ->
            collector.report(project, KotlinToolingDiagnostics.WasmSourceSetsNotFoundError(unknownWasmSourceSetName))
        }
    }
}
