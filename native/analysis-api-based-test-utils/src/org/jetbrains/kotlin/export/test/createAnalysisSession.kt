/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.export.test

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.io.URLUtil.JAR_SEPARATOR
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.standalone.StandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSourceModule
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.library.KlibConstants.KLIB_FILE_EXTENSION_WITH_DOT
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

const val defaultKotlinSourceModuleName = "testModule"

data class LibraryModuleInfo(
    val libraryName: String,
    val klibs: List<Path>,
)

/**
 * Creates a standalone analysis session from Kotlin source code passed as [kotlinSources]
 */
fun createStandaloneAnalysisApiSession(
    tempDir: File,
    kotlinSourceModuleName: String = defaultKotlinSourceModuleName,
    kotlinSources: Map</* File Name */ String, /* Source Code */ String>,
    dependencies: List<LibraryModuleInfo> = emptyList(),
): StandaloneAnalysisAPISession {
    val testModuleRoot = tempDir.resolve("testModule")
    testModuleRoot.mkdirs()

    kotlinSources.forEach { (fileName, sourceCode) ->
        testModuleRoot.resolve(fileName).apply {
            writeText(sourceCode)
        }
    }
    return createStandaloneAnalysisApiSession(kotlinSourceModuleName, listOf(testModuleRoot), dependencies)
}

/**
 * Creates a standalone analysis session from [kotlinFiles] on disk.
 * The Kotlin/Native stdlib will be provided as dependency
 */
fun createStandaloneAnalysisApiSession(
    kotlinSourceModuleName: String = defaultKotlinSourceModuleName,
    kotlinFiles: List<File>,
    dependencies: List<LibraryModuleInfo> = emptyList(),
): StandaloneAnalysisAPISession {
    val currentArchitectureTarget = HostManager.host
    val nativePlatform = NativePlatforms.nativePlatformByTargets(listOf(currentArchitectureTarget))
    return buildStandaloneAnalysisAPISession {
        buildKtModuleProvider {
            platform = nativePlatform
            val stdlibModule = addModule(
                buildKtLibraryModule {
                    val klib = Paths.get(kotlinNativeStdlibPath)
                    addBinaryRoot(klib)
                    addBinaryVirtualFile(klib.toVirtualFile())
                    platform = nativePlatform
                    libraryName = "stdlib"
                }
            )

            val dependencyKlibModules = dependencies.map { dep ->
                buildKtLibraryModule {
                    dep.klibs.forEach {
                        addBinaryRoot(it)
                        addBinaryVirtualFile(it.toVirtualFile())
                    }
                    platform = nativePlatform
                    libraryName = dep.libraryName
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

/**
 * Basically mimics what the IDE does when filling [KaLibraryModule.binaryVirtualFiles], e.g.
 * [here](https://github.com/JetBrains/intellij-community/blob/91f75cd1610d2dd77c99c28a90a715d8de85c1a1/platform/external-system-impl/src/com/intellij/openapi/externalSystem/service/project/manage/LibraryDataService.java#L175-L177).
 * Which is important, as it helps to ensure that the code tested in the compiler environment
 * will work in the IDE environment.
 */
private fun Path.toVirtualFile(): VirtualFile {
    val pathString = FileUtil.toSystemIndependentName(this.toAbsolutePath().toString())
    return if (pathString.endsWith(KLIB_FILE_EXTENSION_WITH_DOT)) {
        val fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.JAR_PROTOCOL)
        fileSystem.findFileByPath(pathString + JAR_SEPARATOR)
    } else {
        VirtualFileManager.getInstance().findFileByNioPath(this)
    } ?: error("Virtual file not found for path: $this")
}
