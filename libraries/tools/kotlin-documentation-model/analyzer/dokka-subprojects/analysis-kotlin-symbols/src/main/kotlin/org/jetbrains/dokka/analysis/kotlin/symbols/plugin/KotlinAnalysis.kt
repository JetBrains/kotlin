/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.plugin

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeTokenProvider
import org.jetbrains.kotlin.analysis.api.standalone.KtAlwaysAccessibleLifetimeTokenProvider
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.analysis.project.structure.builder.KtModuleBuilder
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSdkModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSourceModule
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import java.io.File

internal fun Platform.toTargetPlatform() = when (this) {
    Platform.js, Platform.wasm -> JsPlatforms.defaultJsPlatform
    Platform.common -> CommonPlatforms.defaultCommonPlatform
    Platform.native -> NativePlatforms.unspecifiedNativePlatform
    Platform.jvm -> JvmPlatforms.defaultJvmPlatform
}

private fun getJdkHomeFromSystemProperty(logger: DokkaLogger): File? {
    val javaHome = File(System.getProperty("java.home"))
    if (!javaHome.exists()) {
        logger.error("Set existed java.home to use JDK")
        return null
    }
    return javaHome
}

internal fun getLanguageVersionSettings(
    languageVersionString: String?,
    apiVersionString: String?
): LanguageVersionSettingsImpl {
    val languageVersion = LanguageVersion.fromVersionString(languageVersionString) ?: LanguageVersion.LATEST_STABLE
    val apiVersion =
        apiVersionString?.let { ApiVersion.parse(it) } ?: ApiVersion.createByLanguageVersion(languageVersion)
    return LanguageVersionSettingsImpl(
        languageVersion = languageVersion,
        apiVersion = apiVersion, analysisFlags = hashMapOf(
            // special flag for Dokka
            // force to resolve light classes (lazily by default)
            AnalysisFlags.eagerResolveOfLightClasses to true
        )
    )
}

@OptIn(KtAnalysisApiInternals::class)
internal fun createAnalysisSession(
    sourceSets: List<DokkaConfiguration.DokkaSourceSet>,
    logger: DokkaLogger,
    projectDisposable: Disposable = Disposer.newDisposable("StandaloneAnalysisAPISession.project"),
    isSampleProject: Boolean = false
): KotlinAnalysis {
    val sourcesModule = mutableMapOf<DokkaConfiguration.DokkaSourceSet, KtSourceModule>()

    val analysisSession = buildStandaloneAnalysisAPISession(
        projectDisposable = projectDisposable,
        withPsiDeclarationFromBinaryModuleProvider = false
    ) {
        registerProjectService(KtLifetimeTokenProvider::class.java, KtAlwaysAccessibleLifetimeTokenProvider())

        val sortedSourceSets = topologicalSortByDependantSourceSets(sourceSets, logger)

        val sourcesModuleBySourceSetId = mutableMapOf<DokkaSourceSetID, KtSourceModule>()

        buildKtModuleProvider {
            val jdkModule = getJdkHomeFromSystemProperty(logger)?.let { jdkHome ->
                buildKtSdkModule {
                    this.platform = Platform.jvm.toTargetPlatform()
                    addBinaryRootsFromJdkHome(jdkHome.toPath(), isJre = true)
                    sdkName = "JDK"
                }
            }

            fun KtModuleBuilder.addModuleDependencies(sourceSet: DokkaConfiguration.DokkaSourceSet) {
                val targetPlatform = sourceSet.analysisPlatform.toTargetPlatform()
                addRegularDependency(
                    buildKtLibraryModule {
                        this.platform = targetPlatform
                        addBinaryRoots(sourceSet.classpath.map { it.toPath() })
                        libraryName = "Library for ${sourceSet.displayName}"
                    }
                )
                if (sourceSet.analysisPlatform == Platform.jvm) {
                    jdkModule?.let { addRegularDependency(it) }
                }
                sourceSet.dependentSourceSets.forEach {
                    /**
                     * @see org.jetbrains.kotlin.analysis.project.structure.KtModule.directDependsOnDependencies
                     */
                    addDependsOnDependency(
                        sourcesModuleBySourceSetId[it]
                            ?: error("There is no source module for $it")
                    )
                }
            }

            for (sourceSet in sortedSourceSets) {
                val targetPlatform = sourceSet.analysisPlatform.toTargetPlatform()
                val sourceModule = buildKtSourceModule {
                    languageVersionSettings =
                        getLanguageVersionSettings(sourceSet.languageVersion, sourceSet.apiVersion)
                    platform = targetPlatform
                    moduleName = "<module ${sourceSet.displayName}>"
                    if (isSampleProject)
                        addSourceRoots(sourceSet.samples.map { it.toPath() })
                    else
                        addSourceRoots(sourceSet.sourceRoots.map { it.toPath() })
                    addModuleDependencies(
                        sourceSet,
                    )
                }
                sourcesModule[sourceSet] = sourceModule
                sourcesModuleBySourceSetId[sourceSet.sourceSetID] = sourceModule
                addModule(sourceModule)
            }
            platform = sourceSets.map { it.analysisPlatform }.distinct().singleOrNull()?.toTargetPlatform()
                ?: Platform.common.toTargetPlatform()
        }
    }
    return KotlinAnalysis(sourcesModule, analysisSession, projectDisposable)
}

private enum class State {
    UNVISITED,
    VISITING,
    VISITED;
}

internal fun topologicalSortByDependantSourceSets(
    sourceSets: List<DokkaConfiguration.DokkaSourceSet>,
    logger: DokkaLogger
): List<DokkaConfiguration.DokkaSourceSet> {
    val result = mutableListOf<DokkaConfiguration.DokkaSourceSet>()

    val verticesAssociatedWithState = sourceSets.associateWithTo(mutableMapOf()) { State.UNVISITED }
    fun dfs(souceSet: DokkaConfiguration.DokkaSourceSet) {
        when (verticesAssociatedWithState[souceSet]) {
            State.VISITED -> return
            State.VISITING -> {
                logger.error("Detected cycle in source set graph")
                return
            }

            else -> {
                val dependentSourceSets =
                    souceSet.dependentSourceSets.mapNotNull { dependentSourceSetId ->
                        sourceSets.find { it.sourceSetID == dependentSourceSetId }
                        // just skip
                            ?: null.also { logger.error("Unknown source set Id $dependentSourceSetId in dependencies of ${souceSet.sourceSetID}") }
                    }
                verticesAssociatedWithState[souceSet] = State.VISITING
                dependentSourceSets.forEach(::dfs)
                verticesAssociatedWithState[souceSet] = State.VISITED
                result += souceSet
            }
        }
    }
    sourceSets.forEach(::dfs)
    return result
}
