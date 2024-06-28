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
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.AndroidSourceSetLayoutV1SourceSetsNotFoundError
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.sources.android.kotlinAndroidSourceSetLayout
import org.jetbrains.kotlin.gradle.plugin.sources.android.multiplatformAndroidSourceSetLayoutV2
import org.jetbrains.kotlin.tooling.core.withClosure

internal object AndroidSourceSetLayoutV1SourceSetsNotFoundChecker : KotlinGradleProjectChecker {

    private val androidSourceSetRegex by lazy { Regex("""(androidTest|androidAndroidTest)\w*""") }

    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        /* Checker will only try to provide additional diagnostics if the project configuration failed */
        val failure = project.configurationResult.await() as? KotlinPluginLifecycle.ProjectConfigurationResult.Failure ?: return

        /* Checker is only relevant if multiplatformAndroidSourceSetLayoutV2 is applied */
        if (project.kotlinAndroidSourceSetLayout != multiplatformAndroidSourceSetLayoutV2) return

        val allReasons = failure.failures.withClosure<Throwable> { listOfNotNull(it.cause) }

        val unknownAndroidSourceSetNames = allReasons.filterIsInstance<UnknownDomainObjectException>()
            .filter { it.message.orEmpty().contains("KotlinSourceSet") }
            .mapNotNull { androidSourceSetRegex.find(it.message.orEmpty()) }
            .map { it.value }

        unknownAndroidSourceSetNames.forEach { unknownAndroidSourceSetName ->
            collector.report(project, AndroidSourceSetLayoutV1SourceSetsNotFoundError(unknownAndroidSourceSetName))
        }
    }
}
