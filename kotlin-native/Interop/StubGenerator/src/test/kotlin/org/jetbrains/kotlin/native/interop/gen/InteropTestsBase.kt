/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.gen

import kotlinx.cinterop.JvmCInteropCallbacks
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.util.DefFile
import org.jetbrains.kotlin.utils.NativeMemoryAllocator
import org.jetbrains.kotlin.native.interop.gen.jvm.KotlinPlatform
import org.jetbrains.kotlin.native.interop.gen.jvm.buildNativeLibrary
import org.jetbrains.kotlin.native.interop.gen.jvm.prepareTool
import org.jetbrains.kotlin.native.interop.indexer.HeaderId
import org.jetbrains.kotlin.native.interop.indexer.IndexerResult
import org.jetbrains.kotlin.native.interop.indexer.Location
import org.jetbrains.kotlin.native.interop.indexer.NativeLibrary
import org.jetbrains.kotlin.native.interop.tool.CInteropArguments
import kotlin.test.*
import java.io.File
import java.nio.file.Paths

abstract class InteropTestsBase {
    init {
        System.load(System.getProperty("kotlin.native.llvm.libclang"))
    }

    @BeforeTest
    fun init() {
        NativeMemoryAllocator.init()
        JvmCInteropCallbacks.init()
    }

    @AfterTest
    fun dispose() {
        JvmCInteropCallbacks.dispose()
        NativeMemoryAllocator.dispose()
    }

    class TempFiles(name: String) {
        private val tempRootDir = System.getProperty("kotlin.native.interop.stubgenerator.temp") ?: System.getProperty("java.io.tmpdir") ?: "."

        val directory: File = File(tempRootDir, name).canonicalFile.also {
            it.mkdirs()
        }

        fun file(relativePath: String, contents: String): File = File(directory, relativePath).canonicalFile.apply {
            parentFile.mkdirs()
            writeText(contents)
        }
    }

    protected fun buildNativeLibraryFrom(defFile: File, headersDirectory: File, imports: Imports = ImportsMock()): NativeLibrary {
        val tool = prepareTool(HostManager.hostName, KotlinPlatform.NATIVE, runFromDaemon = true)
        val cinteropArguments = CInteropArguments()
        cinteropArguments.argParser.parse(arrayOf(
                "-compiler-option", "-I${headersDirectory.absolutePath}"
        ))
        return buildNativeLibrary(
                tool,
                DefFile(defFile, tool.substitutions),
                cinteropArguments,
                imports
        )
    }

    protected fun getTestResources(directoryName: String): File {
        val resource = this::class.java.getResource("/$directoryName")
        return Paths.get(resource.toURI()).toFile()
    }

    protected fun buildNativeLibraryFrom(directoryName: String, defFileName: String, imports: Imports = ImportsMock()): NativeLibrary {
        val directory = getTestResources(directoryName)
        val defFile = File(directory, defFileName)
        return buildNativeLibraryFrom(defFile, directory, imports)
    }

    protected fun buildNativeIndex(directoryName: String, defFileName: String, imports: Imports = ImportsMock()): IndexerResult {
        val library = buildNativeLibraryFrom(directoryName, defFileName, imports)
        return org.jetbrains.kotlin.native.interop.indexer.buildNativeIndex(library, verbose = false)
    }

    protected fun mockImports(dependencyIndex: IndexerResult, packageName: String = "dependencyLibrary"): Imports {
        return ImportsMock(dependencyIndex.index.includedHeaders.associateWith { packageName })
    }

    /**
     * Trivial implementation of [Imports] interface, untied from KonanLibrary.
     */
    protected class ImportsMock(private val headerToPackage: Map<HeaderId, String> = emptyMap()) : Imports {
        override fun getPackage(location: Location): String? {
            return headerToPackage[location.headerId]
        }

        override fun isImported(headerId: HeaderId): Boolean {
            return headerId in headerToPackage
        }
    }
}