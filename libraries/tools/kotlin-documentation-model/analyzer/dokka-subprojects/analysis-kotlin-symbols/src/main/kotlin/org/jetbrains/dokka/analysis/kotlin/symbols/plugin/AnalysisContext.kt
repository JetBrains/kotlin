/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.plugin

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.model.SourceSetDependent
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.kotlin.analysis.api.standalone.StandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import java.io.Closeable

internal fun SamplesKotlinAnalysis(
    sourceSets: List<DokkaConfiguration.DokkaSourceSet>,
    context: DokkaContext,
): KotlinAnalysis = createAnalysisSession(
    sourceSets = sourceSets,
    logger = context.logger,
    isSampleProject = true
)

internal fun ProjectKotlinAnalysis(
    sourceSets: List<DokkaConfiguration.DokkaSourceSet>,
    context: DokkaContext,
): KotlinAnalysis = createAnalysisSession(
    sourceSets = sourceSets,
    logger = context.logger
)

internal class KotlinAnalysis(
    private val sourceModules: SourceSetDependent<KtSourceModule>,
    private val analysisSession: StandaloneAnalysisAPISession,
    private val projectDisposable: Disposable
) : Closeable {

    fun getModule(sourceSet: DokkaConfiguration.DokkaSourceSet) =
        sourceModules[sourceSet] ?: error("Missing a source module for sourceSet ${sourceSet.displayName} with id ${sourceSet.sourceSetID}")

    fun getModuleOrNull(sourceSet: DokkaConfiguration.DokkaSourceSet) =
        sourceModules[sourceSet]

    val modulesWithFiles
        get() = analysisSession.modulesWithFiles

    override fun close() {
        Disposer.dispose(projectDisposable)
    }
}
