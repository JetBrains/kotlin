/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.tests

import org.jetbrains.kotlin.native.interop.indexer.*
import java.io.File


private val appleSdkPath = "/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX15.4.sdk"
private val appleFrameworkPath = "$appleSdkPath/System/Library/Frameworks"

private val xcodeDir = "/Applications/Xcode.app/Contents/Developer"
private val clangVersion = "17.0.0"
private val clangResourceDir = "$xcodeDir/Toolchains/XcodeDefault.xctoolchain/usr/lib/clang/$clangVersion/include"


internal fun compileAndIndex(
    headers: List<File>,
    files: TempFiles,
    moduleName: String?,
    vararg args: String,
): IndexerResult {

    val headersNames = headers.map {
        "header \"" + it.name + "\"\n"
    }

    files.file(
        "module.modulemap", """
            module Foo {
              ${headersNames.joinToString(separator = "")}
            }
        """.trimIndent()
    )

    val includeInfos = headers.map {
        IncludeInfo(it.absolutePath, moduleName)
    }


    val compilation = compilation(
        includeInfos,
        "-I${files.directory}",
        "-I$clangResourceDir",
        *args
    )

    val nativeLibrary = NativeLibrary(
        includes = compilation.includes,
        additionalPreambleLines = compilation.additionalPreambleLines,
        compilerArgs = compilation.compilerArgs,
        headerToIdMapper = HeaderToIdMapper(sysRoot = ""),
        language = compilation.language,
        excludeSystemLibs = false,
        headerExclusionPolicy = HeaderExclusionPolicyImpl(),
        headerFilter = NativeLibraryHeaderFilter.Predefined(
            files.directory.listFiles()?.filter { it.extension == "h" }?.map { it.path }.orEmpty().toSet(), listOf("*")
        ),
        objCClassesIncludingCategories = emptySet(),
        allowIncludingObjCCategoriesFromDefFile = false
    )

    return buildNativeIndex(nativeLibrary, verbose = true)
}

class HeaderExclusionPolicyImpl : HeaderExclusionPolicy {
    override fun excludeAll(headerId: HeaderId): Boolean = false
}

private fun compilation(includes: List<IncludeInfo>, vararg args: String) = CompilationImpl(
    includes = includes,
    additionalPreambleLines = emptyList(),
    compilerArgs = listOf(*args),
    language = Language.OBJECTIVE_C
)

internal class TempFiles(name: String) {
    private val tempRootDir = System.getProperty("kotlin.native.interop.indexer.temp") ?: System.getProperty("java.io.tmpdir") ?: "."

    val directory: File = File(tempRootDir, name).canonicalFile.also {
        it.mkdirs()
    }

    fun file(relativePath: String, contents: String): File = File(directory, relativePath).canonicalFile.apply {
        parentFile.mkdirs()
        writeText(contents)
    }
}