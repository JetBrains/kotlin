/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.plugin

import com.intellij.openapi.Disposable
import org.jetbrains.dokka.Platform
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeTokenProvider
import org.jetbrains.kotlin.analysis.api.standalone.KtAlwaysAccessibleLifetimeTokenProvider
import org.jetbrains.kotlin.analysis.api.standalone.StandaloneAnalysisAPISession
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

private fun getJdkHomeFromSystemProperty(): File? {
    val javaHome = File(System.getProperty("java.home"))
    if (!javaHome.exists()) {
        // messageCollector.report(CompilerMessageSeverity.WARNING, "Set existed java.home to use JDK")
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

// it should be changed after https://github.com/Kotlin/dokka/issues/3114
@OptIn(KtAnalysisApiInternals::class)
internal fun createAnalysisSession(
    classpath: List<File>,
    sourceRoots: Set<File>,
    analysisPlatform: Platform,
    languageVersion: String?,
    apiVersion: String?,
    applicationDisposable: Disposable,
    projectDisposable: Disposable
): Pair<StandaloneAnalysisAPISession, KtSourceModule> {

    var sourceModule: KtSourceModule? = null
    val analysisSession = buildStandaloneAnalysisAPISession(
        applicationDisposable = applicationDisposable,
        projectDisposable = projectDisposable,
        withPsiDeclarationFromBinaryModuleProvider = false
    ) {
        registerProjectService(KtLifetimeTokenProvider::class.java, KtAlwaysAccessibleLifetimeTokenProvider())
        val targetPlatform = analysisPlatform.toTargetPlatform()

        buildKtModuleProvider {
            val libraryRoots = classpath
            fun KtModuleBuilder.addModuleDependencies(moduleName: String) {
                addRegularDependency(
                    buildKtLibraryModule {
                        this.platform = targetPlatform
                        addBinaryRoots(libraryRoots.map { it.toPath() })
                        libraryName = "Library for $moduleName"
                    }
                )
                getJdkHomeFromSystemProperty()?.let { jdkHome ->
                    addRegularDependency(
                        buildKtSdkModule {
                            this.platform = targetPlatform
                            addBinaryRootsFromJdkHome(jdkHome.toPath(), isJre = true)
                            sdkName = "JDK for $moduleName"
                        }
                    )
                }
            }
            sourceModule = buildKtSourceModule {
                languageVersionSettings = getLanguageVersionSettings(languageVersion, apiVersion)
                platform = targetPlatform
                moduleName = "<module>"
                // TODO: We should handle (virtual) file changes announced via LSP with the VFS
                addSourceRoots(sourceRoots.map { it.toPath() })
                addModuleDependencies(moduleName)
            }
            platform = targetPlatform
            addModule(sourceModule!!)
        }
    }
    return Pair(analysisSession, sourceModule ?: throw IllegalStateException())
}