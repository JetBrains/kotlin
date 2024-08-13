/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.swiftexport.compilerconfig.CompilerConfig

class SwiftExportCompilerConfig(
        val classNamesMapping: Map<FqName, String>
) {
    companion object {
        fun mergeConfigs(configs: List<CompilerConfig>) = SwiftExportCompilerConfig(buildMap {
            configs.forEach { config ->
                config.classNamingMappings.forEach { mapping ->
                    val kotlinName = FqName(mapping.kotlinClassFqName)
                    putIfAbsent(kotlinName, mapping.objCClassName)?.let {
                        error("Conflicting Obj-C class mapping for Kotlin class $kotlinName: $it and ${mapping.objCClassName}")
                    }
                }
            }
        })
    }
}