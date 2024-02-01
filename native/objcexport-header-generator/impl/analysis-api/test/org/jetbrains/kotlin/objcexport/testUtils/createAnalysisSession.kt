/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.testUtils

import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeTokenProvider
import org.jetbrains.kotlin.analysis.api.standalone.KtAlwaysAccessibleLifetimeTokenProvider
import org.jetbrains.kotlin.analysis.api.standalone.StandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSourceModule
import org.jetbrains.kotlin.backend.konan.testUtils.kotlinNativeStdlibPath
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import java.io.File
import kotlin.io.path.Path

/**
 * Creates a standalone analysis session from Kotlin source code passed as [kotlinSources]
 */
fun createStandaloneAnalysisApiSession(
    tempDir: File,
    kotlinSources: Map</* File Name */ String, /* Source Code */ String>,
): StandaloneAnalysisAPISession {
    val testModuleRoot = tempDir.resolve("testModule")
    testModuleRoot.mkdirs()

    kotlinSources.forEach { (fileName, sourceCode) ->
        testModuleRoot.resolve(fileName).apply {
            writeText(sourceCode)
        }
    }
    return createStandaloneAnalysisApiSession(listOf(testModuleRoot))
}

/**
 * Creates a standalone analysis session from [kotlinFiles] on disk.
 * The Kotlin/Native stdlib will be provided as dependency
 */
fun createStandaloneAnalysisApiSession(kotlinFiles: List<File>): StandaloneAnalysisAPISession {
    val currentArchitectureTarget = HostManager.host
    val nativePlatform = NativePlatforms.nativePlatformByTargets(listOf(currentArchitectureTarget))
    return buildStandaloneAnalysisAPISession {
        @OptIn(KtAnalysisApiInternals::class)
        registerProjectService(KtLifetimeTokenProvider::class.java, KtAlwaysAccessibleLifetimeTokenProvider())

        buildKtModuleProvider {
            platform = nativePlatform
            val kLib = addModule(
                buildKtLibraryModule {
                    addBinaryRoot(Path(kotlinNativeStdlibPath))
                    platform = nativePlatform
                    libraryName = "klib"
                }
            )
            addModule(
                buildKtSourceModule {
                    addSourceRoots(kotlinFiles.map { it.toPath() })
                    addRegularDependency(kLib)
                    platform = nativePlatform
                    moduleName = "source"
                }
            )
        }
    }
}
