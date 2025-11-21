/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.resolve

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DefaultImportsProvider
import org.jetbrains.kotlin.resolve.ImportPath

object WasmJsDefaultImportsProvider : DefaultImportsProvider() {
    override val platformSpecificDefaultImports: List<ImportPath> = listOf(ImportPath.fromString("kotlin.js.*"))

    override val excludedImports: List<FqName> = listOf("Promise", "Date", "Console", "Math", "RegExp", "RegExpMatch", "Json", "json")
        .map { FqName("kotlin.js.$it") }
}

object WasmWasiDefaultImportsProvider : DefaultImportsProvider() {
    override val platformSpecificDefaultImports: List<ImportPath> = listOf(ImportPath.fromString("kotlin.wasm.*"))
}
