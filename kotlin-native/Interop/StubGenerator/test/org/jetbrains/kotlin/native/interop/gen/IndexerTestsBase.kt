/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.native.interop.indexer.EnumDef
import org.jetbrains.kotlin.native.interop.indexer.FunctionDecl
import org.jetbrains.kotlin.native.interop.indexer.GlobalDecl
import org.jetbrains.kotlin.native.interop.indexer.IndexerResult
import org.jetbrains.kotlin.native.interop.indexer.Language
import org.jetbrains.kotlin.native.interop.indexer.ObjCProtocol
import org.jetbrains.kotlin.native.interop.indexer.StructDecl
import org.jetbrains.kotlin.utils.atMostOne

open class IndexerTestsBase : InteropTestsBase() {
    fun index(headerContents: String, language: Language = Language.C, appendDefFile: String? = null): IndexerResult {
        val files = testFiles()
        files.file("header.h", headerContents)
        val defFileLanguage = when (language) {
            Language.C -> "C"
            Language.CPP -> "C++"
            Language.OBJECTIVE_C -> "Objective-C"
        }
        val defFile = files.file("test.def", """
            headers = header.h
            headerFilter = header.h
            language = $defFileLanguage
            """.trimIndent() + if (appendDefFile != null) "\n$appendDefFile" else ""
        )

        val library = buildNativeLibraryFrom(defFile, files.directory)
        return org.jetbrains.kotlin.native.interop.indexer.buildNativeIndex(library, verbose = false)
    }

    val IndexerResult.global: GlobalDecl
        get() = index.globals.atMostOne()!!

    val IndexerResult.function: FunctionDecl
        get() = index.functions.atMostOne()!!

    fun indexFunctionOrNull(headerContents: String): FunctionDecl? =
            index(headerContents).index.functions.atMostOne()

    fun indexFunction(headerContents: String): FunctionDecl =
            indexFunctionOrNull(headerContents)!!

    fun indexStructs(headerContents: String): Collection<StructDecl> =
            index(headerContents).index.structs

    fun indexStruct(headerContents: String): StructDecl =
            indexStructs(headerContents).single()

    fun indexEnum(headerContents: String): EnumDef =
            index(headerContents).index.enums.single()

    fun indexProtocol(headerContents: String): ObjCProtocol =
            index(headerContents, language = Language.OBJECTIVE_C).index.objCProtocols.single()
}
