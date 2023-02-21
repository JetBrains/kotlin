/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.resolve

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.storage.StorageManager

object WasmPlatformAnalyzerServices : PlatformDependentAnalyzerServices() {
    override fun computePlatformSpecificDefaultImports(storageManager: StorageManager, result: MutableList<ImportPath>) {
        result.add(ImportPath.fromString("kotlin.js.*"))
    }

    override val platformConfigurator: PlatformConfigurator = WasmPlatformConfigurator

    val builtIns: KotlinBuiltIns
        get() = DefaultBuiltIns.Instance

    override val excludedImports: List<FqName> =
        listOf("Promise", "Date", "Console", "Math", "RegExp", "RegExpMatch", "Json", "json").map { FqName("kotlin.js.$it") }
}
