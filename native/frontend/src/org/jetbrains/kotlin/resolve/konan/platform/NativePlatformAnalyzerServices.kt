/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.konan.platform

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.storage.StorageManager

object NativePlatformAnalyzerServices : PlatformDependentAnalyzerServices() {
    override fun computePlatformSpecificDefaultImports(storageManager: StorageManager, result: MutableList<ImportPath>) {
        result.add(ImportPath.fromString("kotlin.native.*"))
    }

    override val platformConfigurator: PlatformConfigurator = NativePlatformConfigurator

    override val excludedImports: List<FqName> =
        listOf("identityHashCode").map {
            FqName("kotlin.native.$it")
        }
}
