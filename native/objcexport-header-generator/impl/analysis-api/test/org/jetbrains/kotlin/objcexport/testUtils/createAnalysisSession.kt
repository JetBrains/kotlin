/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.testUtils

import org.jetbrains.kotlin.analysis.api.standalone.StandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSourceModule
import org.jetbrains.kotlin.backend.konan.testUtils.kotlinNativeStdlibPath
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.nameWithoutExtension

const val defaultKotlinSourceModuleName = "testModule"

/**
 * Creates a standalone analysis session from Kotlin source code passed as [kotlinSources]
 */
fun createStandaloneAnalysisApiSession(
    tempDir: File,
    kotlinSourceModuleName: String = defaultKotlinSourceModuleName,
    kotlinSources: Map</* File Name */ String, /* Source Code */ String>,
    dependencyKlibs: List<Path> = emptyList(),
): StandaloneAnalysisAPISession {
    val testModuleRoot = tempDir.resolve("testModule")
    testModuleRoot.mkdirs()

    kotlinSources.forEach { (fileName, sourceCode) ->
        testModuleRoot.resolve(fileName).apply {
            writeText(sourceCode)
        }
    }
    return createStandaloneAnalysisApiSession(kotlinSourceModuleName, listOf(testModuleRoot), dependencyKlibs)
}

/**
 * Creates a standalone analysis session from [kotlinFiles] on disk.
 * The Kotlin/Native stdlib will be provided as dependency
 */
fun createStandaloneAnalysisApiSession(
    kotlinSourceModuleName: String = defaultKotlinSourceModuleName,
    kotlinFiles: List<File>,
    dependencyKlibs: List<Path> = emptyList(),
): StandaloneAnalysisAPISession {
    val currentArchitectureTarget = HostManager.host
    val nativePlatform = NativePlatforms.nativePlatformByTargets(listOf(currentArchitectureTarget))
    return buildStandaloneAnalysisAPISession {
        buildKtModuleProvider {
            platform = nativePlatform
            val stdlibModule = addModule(
                buildKtLibraryModule {
                    addBinaryRoot(Path(kotlinNativeStdlibPath))
                    platform = nativePlatform
                    libraryName = "stdlib"
                }
            )

            val dependencyKlibModules = dependencyKlibs.map { klib ->
                buildKtLibraryModule {
                    addBinaryRoot(klib)
                    platform = nativePlatform
                    libraryName = klib.nameWithoutExtension
                    addRegularDependency(stdlibModule)
                }
            }

            addModule(
                buildKtSourceModule {
                    addSourceRoots(kotlinFiles.map { it.toPath() })
                    addRegularDependency(stdlibModule)
                    dependencyKlibModules.forEach { dependencyKlibModule ->
                        addRegularDependency(dependencyKlibModule)
                    }
                    platform = nativePlatform
                    moduleName = kotlinSourceModuleName
                }
            )
        }
    }
}
