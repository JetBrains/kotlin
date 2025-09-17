/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.konan.platform

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DefaultImportProvider
import org.jetbrains.kotlin.resolve.ImportPath

object NativeDefaultImportsProvider : DefaultImportProvider() {
    override val platformSpecificDefaultImports: List<ImportPath> = listOf(ImportPath.fromString("kotlin.native.*"))

    override val excludedImports: List<FqName> = listOf("identityHashCode").map {
        FqName("kotlin.native.$it")
    }
}
