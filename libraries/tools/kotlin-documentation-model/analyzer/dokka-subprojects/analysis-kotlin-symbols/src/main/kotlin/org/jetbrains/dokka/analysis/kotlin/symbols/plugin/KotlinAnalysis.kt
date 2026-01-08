/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.plugin

import com.intellij.diagnostic.LoadingState
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.project.structure.builder.KtModuleBuilder
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSdkModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSourceModule
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import java.io.File

internal fun Platform.toTargetPlatform() = when (this) {
    Platform.wasm -> WasmPlatforms.unspecifiedWasmPlatform
    Platform.js -> JsPlatforms.defaultJsPlatform
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
    apiVersionString: String?,
    isMultiplatformProject: Boolean,
): LanguageVersionSettingsImpl {
    val languageVersion = LanguageVersion.fromVersionString(languageVersionString)
        ?: LanguageVersion.LATEST_STABLE
    val apiVersion = apiVersionString?.let { ApiVersion.parse(it) }
        ?: ApiVersion.createByLanguageVersion(languageVersion)
    return LanguageVersionSettingsImpl(
        languageVersion = languageVersion,
        apiVersion = apiVersion,
        analysisFlags = hashMapOf(
            AnalysisFlags.allowKotlinPackage to InternalConfiguration.allowKotlinPackage,
            // special flag for Dokka
            // force to resolve light classes (lazily by default)
            AnalysisFlags.eagerResolveOfLightClasses to true,
        ),
        specificFeatures = listOfNotNull(
            if (isMultiplatformProject) LanguageFeature.MultiPlatformProjects to LanguageFeature.State.ENABLED else null
        ).toMap()
    )
}

internal fun createAnalysisSession(
    sourceSets: List<DokkaConfiguration.DokkaSourceSet>,
    logger: DokkaLogger,
    projectDisposable: Disposable = Disposer.newDisposable("StandaloneAnalysisAPISession.project"),
    isSampleProject: Boolean = false
): KotlinAnalysis {
    if (InternalConfiguration.experimentalKDocResolutionEnabled) {
        enableExperimentalKDocResolution()
    }

    val sourcesModule = mutableMapOf<DokkaConfiguration.DokkaSourceSet, KaSourceModule>()
    val isMultiplatformProject = sourceSets.any { it.analysisPlatform != Platform.jvm }

    val analysisSession = buildStandaloneAnalysisAPISession(
        projectDisposable = projectDisposable,
    ) {
        val sortedSourceSets = topologicalSortByDependantSourceSets(sourceSets, logger)

        val sourcesModuleBySourceSetId = mutableMapOf<DokkaSourceSetID, KaSourceModule>()

        buildKtModuleProvider {
            val jdkModule = getJdkHomeFromSystemProperty(logger)?.let { jdkHome ->
                buildKtSdkModule {
                    this.platform = Platform.jvm.toTargetPlatform()
                    addBinaryRootsFromJdkHome(jdkHome.toPath(), isJre = true)
                    libraryName = "JDK"
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
                     * @see org.jetbrains.kotlin.analysis.api.projectStructure.KaModule.directDependsOnDependencies
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
                    languageVersionSettings = getLanguageVersionSettings(
                        languageVersionString = sourceSet.languageVersion,
                        apiVersionString = sourceSet.apiVersion,
                        isMultiplatformProject = isMultiplatformProject,
                    )
                    platform = targetPlatform
                    moduleName = "<module ${sourceSet.displayName}>"

                    // can be removed after https://youtrack.jetbrains.com/issue/KT-81107 is implemented (see #4266)
                    // here we mimic the logic, which happens inside AA during building KaModule, but we follow symlinks
                    // https://github.com/JetBrains/kotlin/blob/dcd24449718cba21bd86428e5cddb9b25e5612af/analysis/analysis-api-standalone/src/org/jetbrains/kotlin/analysis/project/structure/builder/KaSourceModuleBuilder.kt#L80
                    if (isSampleProject) {
                        addSourceRoots(sourceSet.samples.map { it.toPath() })
                    } else {
                        addSourceRoots(sourceSet.sourceRoots.map { it.toPath() })
                    }
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
    fun dfs(sourceSet: DokkaConfiguration.DokkaSourceSet) {
        when (verticesAssociatedWithState[sourceSet]) {
            State.VISITED -> return
            State.VISITING -> {
                logger.error("Detected cycle in source set graph")
                return
            }

            else -> {
                val dependentSourceSets =
                    sourceSet.dependentSourceSets.mapNotNull { dependentSourceSetId ->
                        sourceSets.find { it.sourceSetID == dependentSourceSetId } ?: run {
                            logger.error("Cannot find source set with id $dependentSourceSetId")
                            null
                        }

                    }
                verticesAssociatedWithState[sourceSet] = State.VISITING
                dependentSourceSets.forEach(::dfs)
                verticesAssociatedWithState[sourceSet] = State.VISITED
                result += sourceSet
            }
        }
    }
    sourceSets.forEach(::dfs)
    return result
}

private fun enableExperimentalKDocResolution() {
    // Enable experimental KDoc resolution in Kotlin Analysis API (K2)
    @Suppress("UnstableApiUsage")
    LoadingState.setCurrentState(LoadingState.COMPONENTS_LOADED)
    System.setProperty("kotlin.analysis.experimentalKDocResolution", "true")
}
