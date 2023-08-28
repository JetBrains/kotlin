package org.jetbrains.dokka.analysis.kotlin.symbols.plugin

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.model.SourceSetDependent
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.kotlin.analysis.api.standalone.StandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import java.io.Closeable
import java.io.File

@Suppress("FunctionName", "UNUSED_PARAMETER")
internal fun SamplesKotlinAnalysis(
    sourceSets: List<DokkaConfiguration.DokkaSourceSet>,
    context: DokkaContext,
    projectKotlinAnalysis: KotlinAnalysis
): KotlinAnalysis {
    val environments = sourceSets
        .filter { it.samples.isNotEmpty() }
        .associateWith { sourceSet ->
            createAnalysisContext(
                classpath = sourceSet.classpath,
                sourceRoots = sourceSet.samples,
                sourceSet = sourceSet
            )
        }

    return EnvironmentKotlinAnalysis(environments, projectKotlinAnalysis)
}

internal fun ProjectKotlinAnalysis(
    sourceSets: List<DokkaConfiguration.DokkaSourceSet>,
    context: DokkaContext,
): KotlinAnalysis {
    val environments = sourceSets.associateWith { sourceSet ->
        createAnalysisContext(
            context = context,
            sourceSets = sourceSets,
            sourceSet = sourceSet
        )
    }
    return EnvironmentKotlinAnalysis(environments)
}


@Suppress("UNUSED_PARAMETER")
internal fun createAnalysisContext(
    context: DokkaContext,
    sourceSets: List<DokkaConfiguration.DokkaSourceSet>,
    sourceSet: DokkaConfiguration.DokkaSourceSet
): AnalysisContext {
    val parentSourceSets = sourceSets.filter { it.sourceSetID in sourceSet.dependentSourceSets }
    val classpath = sourceSet.classpath + parentSourceSets.flatMap { it.classpath }
    val sources = sourceSet.sourceRoots + parentSourceSets.flatMap { it.sourceRoots }

    return createAnalysisContext(classpath, sources, sourceSet)
}

internal fun createAnalysisContext(
    classpath: List<File>,
    sourceRoots: Set<File>,
    sourceSet: DokkaConfiguration.DokkaSourceSet
): AnalysisContext {
    val applicationDisposable: Disposable = Disposer.newDisposable("StandaloneAnalysisAPISession.application")
    val projectDisposable: Disposable = Disposer.newDisposable("StandaloneAnalysisAPISession.project")

    val analysis= createAnalysisSession(
        classpath = classpath,
        sourceRoots = sourceRoots,
        analysisPlatform = sourceSet.analysisPlatform,
        languageVersion = sourceSet.languageVersion,
        apiVersion = sourceSet.apiVersion,
        applicationDisposable = applicationDisposable,
        projectDisposable = projectDisposable
    )
    return AnalysisContextImpl(
        mainModule = analysis.second,
        analysisSession = analysis.first,
        applicationDisposable = applicationDisposable,
        projectDisposable = projectDisposable
    )
}


/**
 * First child delegation. It does not close [parent].
 */
internal abstract class KotlinAnalysis(
    private val parent: KotlinAnalysis? = null
) : Closeable {

    operator fun get(key: DokkaConfiguration.DokkaSourceSet): AnalysisContext {
        return get(key.sourceSetID)
    }

    internal operator fun get(key: DokkaSourceSetID): AnalysisContext {
        return find(key)
            ?: parent?.get(key)
            ?: throw IllegalStateException("Missing EnvironmentAndFacade for sourceSet $key")
    }

    internal abstract fun find(sourceSetID: DokkaSourceSetID): AnalysisContext?
}

internal open class EnvironmentKotlinAnalysis(
    private val environments: SourceSetDependent<AnalysisContext>,
    parent: KotlinAnalysis? = null,
) : KotlinAnalysis(parent = parent) {

    override fun find(sourceSetID: DokkaSourceSetID): AnalysisContext? =
        environments.entries.firstOrNull { (sourceSet, _) -> sourceSet.sourceSetID == sourceSetID }?.value

    override fun close() {
        environments.values.forEach(AnalysisContext::close)
    }
}

internal interface AnalysisContext: Closeable {
    val project: Project
    val mainModule: KtSourceModule
}

private class AnalysisContextImpl(
    override val mainModule: KtSourceModule,
    private val analysisSession: StandaloneAnalysisAPISession,
    private val applicationDisposable: Disposable,
    private val projectDisposable: Disposable
) : AnalysisContext {
    override val project: Project
        get() = analysisSession.project

    override fun close() {
        Disposer.dispose(applicationDisposable)
        Disposer.dispose(projectDisposable)
    }
}