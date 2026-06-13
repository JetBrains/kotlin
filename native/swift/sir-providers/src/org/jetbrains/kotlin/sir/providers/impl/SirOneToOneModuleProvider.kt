/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.projectStructure.KaBuiltinsModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.builder.buildModule
import org.jetbrains.kotlin.sir.providers.SirModuleProvider
import org.jetbrains.kotlin.sir.util.SirCinteropModule
import org.jetbrains.kotlin.sir.util.SirPlatformModule

/**
 * A module provider implementation that generates a [SirModule] for each given [KaModule].
 *
 * [platformLibs] are Kotlin/Native distribution platform libraries, represented as [SirPlatformModule].
 * [cinteropReexportLibs] are user-provided cinterop klibs to be re-exported through an existing ObjC module,
 * represented as [SirCinteropModule]. For both, no Swift code is generated; references become `import <name>`.
 */
public class SirOneToOneModuleProvider(
    platformLibs: Collection<KaLibraryModule>,
    cinteropReexportLibs: Collection<KaLibraryModule> = emptyList(),
) : SirModuleProvider {

    private val moduleCache: MutableMap<KaModule, SirModule> = buildMap<KaModule, SirModule> {
        platformLibs.forEach { put(it, SirPlatformModule(it.moduleName)) }
        cinteropReexportLibs.forEach { put(it, SirCinteropModule(it.moduleName)) }
    }.toMutableMap()

    public val modules: Map<KaModule, SirModule>
        get() = moduleCache.toMap()

    override fun KaModule.sirModule(): SirModule = moduleCache.getOrPut(this) {
        buildModule {
            name = getModuleName(this@sirModule)
        }
    }

    private fun getModuleName(ktModule: KaModule): String {
        return ktModule.moduleName
    }
}

@OptIn(KaExperimentalApi::class, KaPlatformInterface::class)
private val KaModule.moduleName: String
    get() = when(this) {
        is KaLibraryModule -> libraryName
        is KaSourceModule -> name
        is KaBuiltinsModule -> "builtin"
        else -> error("Tried to calculate KaModule.moduleName for unknown module type: $this, described as: ${this.moduleDescription}")
    }
