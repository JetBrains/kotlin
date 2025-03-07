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
import org.jetbrains.kotlin.native.interop.indexer.*
import org.jetbrains.kotlin.native.interop.tool.CInteropArguments
import org.junit.Rule
import kotlin.test.*
import java.io.File
import java.nio.file.Paths

abstract class InteropTestsBase {
    init {
        System.load(System.getProperty("kotlin.native.llvm.libclang"))
    }

    private val propertyOverrides = buildMap {
        System.getProperty("kotlin.native.propertyOverrides").splitToSequence(";").forEach {
            val kvp = it.split("=", limit = 2)
            check(kvp.size == 2)
            put(kvp[0], kvp[1])
        }
    }

    @Rule
    @JvmField
    val testFilesFactory = TestFilesFactory()

    fun testFiles() = testFilesFactory.tempFiles()

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

    protected fun buildNativeLibraryFrom(defFile: File, headersDirectory: File, imports: Imports = ImportsMock()): NativeLibrary {
        val tool = prepareTool(HostManager.hostName, KotlinPlatform.NATIVE, runFromDaemon = true, propertyOverrides = propertyOverrides)
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

    protected fun buildNativeIndex(defFile: File, headersDirectory: File, imports: Imports = ImportsMock()): IndexerResult =
            buildNativeIndex(buildNativeLibraryFrom(defFile, headersDirectory, imports), verbose = false)

    protected fun mockImports(dependencyIndex: IndexerResult, packageName: String = "dependencyLibrary"): Imports {
        return mockImports(dependencyIndex to packageName)
    }

    protected fun mockImports(vararg dependencies: Pair<IndexerResult, String>): Imports {
        val headerToPackage = buildMap<HeaderId, String> {
            dependencies.forEach { (dependency, packageName) ->
                dependency.index.includedHeaders.forEach { headerId ->
                    put(headerId, packageName)
                }
            }
        }

        return ImportsMock(headerToPackage)
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