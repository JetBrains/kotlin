/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.tests.integration.utils

import org.jetbrains.kotlin.native.interop.indexer.*
import java.io.File

internal fun compileAndIndex(
    headers: List<File>,
    files: IntegrationTempFiles,
    vararg args: String,
): IndexerResult {

    val includeInfos = headers.map {
        IncludeInfo(it.absolutePath, integrationModuleName)
    }

    val compilationArgs = listOf("-I${files.directory}", "-I${getClangResourceDir()}", *args)

    val nativeLibrary = NativeLibrary(
        includes = includeInfos,
        additionalPreambleLines = emptyList(),
        compilerArgs = compilationArgs,
        headerToIdMapper = HeaderToIdMapper(sysRoot = ""),
        language = Language.OBJECTIVE_C,
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

private class HeaderExclusionPolicyImpl : HeaderExclusionPolicy {
    override fun excludeAll(headerId: HeaderId): Boolean = false
}

internal const val integrationModuleName = "Foo"