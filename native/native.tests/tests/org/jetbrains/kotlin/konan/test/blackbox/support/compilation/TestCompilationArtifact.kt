/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.compilation

import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.io.File
import java.nio.file.*

sealed interface TestCompilationArtifact {
    val logFile: File

    data class KLIB(val klibFile: File) : TestCompilationArtifact {
        val path: String get() = klibFile.path

        val headerKlib: File get() = klibFile.resolveSibling(klibFile.name.replaceAfterLast(".", "header.klib"))
        override val logFile: File get() = klibFile.resolveSibling("${klibFile.name}.log")
    }

    interface KLIBStaticCache : TestCompilationArtifact {
        val cacheDir: File
        val klib: KLIB
        val fileCheckStage: String?
        override val logFile: File get() = cacheDir.resolve("${klib.klibFile.nameWithoutExtension}-cache.log")
        val fileCheckDump: File?
            get() = fileCheckStage?.let {
                cacheDir.resolveSibling("out.$it.ll")
            }
    }

    data class KLIBStaticCacheImpl(override val cacheDir: File, override val klib: KLIB, override val fileCheckStage: String? = null) :
        KLIBStaticCache

    data class KLIBStaticCacheHeader(override val cacheDir: File, override val klib: KLIB, override val fileCheckStage: String? = null) :
        KLIBStaticCache

    data class Executable(
        val executableFile: File,
        val fileCheckStage: String? = null,
        val hasSyntheticAccessorsDump: Boolean = false,
    ) : TestCompilationArtifact {
        val path: String get() = executableFile.path
        override val logFile: File get() = executableFile.resolveSibling("${executableFile.name}.log")
        val testDumpFile: File get() = executableFile.resolveSibling("${executableFile.name}.dump")
        val fileCheckDump: File?
            get() = fileCheckStage?.let {
                executableFile.resolveSibling("out.$it.ll")
            }
        val syntheticAccessorsDumpDir: File?
            get() = runIf(hasSyntheticAccessorsDump) { executableFile.resolveSibling("${executableFile.name}.accessors") }
    }

    data class ObjCFramework(private val buildDir: File, val frameworkName: String) : TestCompilationArtifact {
        val frameworkDir: File get() = buildDir.resolve("$frameworkName.framework")
        override val logFile: File get() = frameworkDir.resolveSibling("${frameworkDir.name}.log")
        val headersDir: File get() = frameworkDir.resolve("Headers")
        val mainHeader: File get() = headersDir.resolve("$frameworkName.h")
        val imagePath: File get() = frameworkDir.resolve(frameworkName)
    }

    data class BinaryLibrary(val libraryFile: File) : TestCompilationArtifact {

        override val logFile: File get() = libraryFile.resolveSibling("${libraryFile.name}.log")

        /**
         * Might not exist if the library was compiled without a header.
         */
        val headerFile: File?
            get() {
                val expectedFile = libraryFile.resolveSibling("${libraryFile.nameWithoutExtension}_api.h")
                return expectedFile.takeIf { it.exists() }
            }
    }

    sealed interface Swift : TestCompilationArtifact {
        class Module(val rootDir: File, val moduleName: String) : Swift {
            override val logFile: File get() = rootDir.resolve("$moduleName.log")
            val binaryLibrary: File get() = rootDir.resolve("lib$moduleName.a")
        }
    }

    data class XCTestBundle(val bundleDir: File, val fileCheckStage: String? = null) : TestCompilationArtifact {
        override val logFile: File get() = bundleDir.resolveSibling("${bundleDir.name}.log")
        val testDumpFile: File get() = bundleDir.resolveSibling("${bundleDir.name}.dump")
        val fileCheckDump: File?
            get() = fileCheckStage?.let {
                bundleDir.resolveSibling("out.$it.ll")
            }
    }
}
