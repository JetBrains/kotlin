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
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.extension

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
                        sourceSet.samples.forEach { root ->
                            addSourceRoots(collectSourceFilePaths(root.toPath()))
                        }
                    } else {
                        sourceSet.sourceRoots.forEach { root ->
                            addSourceRoots(collectSourceFilePaths(root.toPath()))
                        }
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

// copied from AA: https://github.com/JetBrains/kotlin/blob/dcd24449718cba21bd86428e5cddb9b25e5612af/analysis/analysis-api-standalone/src/org/jetbrains/kotlin/analysis/project/structure/impl/KaModuleUtils.kt#L60-L110
// with a fix for following symlinks

private fun collectSourceFilePaths(root: Path): List<Path> {
    // NB: [Files#walk] throws an exception if there is an issue during IO.
    // With [Files#walkFileTree] with a custom visitor, we can take control of exception handling.
    val result = mutableListOf<Path>()
    Files.walkFileTree(
        /* start = */ root,
        /* options = */ setOf(FileVisitOption.FOLLOW_LINKS), // <-- THIS IS THE FIX
        /* maxDepth = */ Int.MAX_VALUE,
        /* visitor = */ object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                return if (Files.isReadable(dir))
                    FileVisitResult.CONTINUE
                else
                    FileVisitResult.SKIP_SUBTREE
            }

            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                if (!Files.isRegularFile(file) || !Files.isReadable(file))
                    return FileVisitResult.CONTINUE
                if (file.hasSuitableExtensionToAnalyse()) {
                    result.add(file)
                }
                return FileVisitResult.CONTINUE
            }

            override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
                // TODO: report or log [IOException]?
                // NB: this intentionally swallows the exception, hence fail-safe.
                // Skipping subtree doesn't make any sense, since this is not a directory.
                // Skipping sibling may drop valid file paths afterward, so we just continue.
                return FileVisitResult.CONTINUE
            }
        }
    )
    return result
}

private fun Path.hasSuitableExtensionToAnalyse(): Boolean {
    val extension = extension
    return extension == "kt" || extension == "kts" || extension == "java"
}
