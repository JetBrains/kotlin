/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.builder.buildModule
import org.jetbrains.kotlin.sir.providers.SirModuleProvider

/**
 * A module provider implementation that generates a [SirModule] for each given [KtModule]
 */
public class SirOneToOneModuleProvider(
    private val mainModuleName: String
) : SirModuleProvider {

    private val moduleCache = mutableMapOf<KtModule, SirModule>()

    public val modules: Map<KtModule, SirModule>
        get() = moduleCache.toMap()

    override fun KtModule.sirModule(): SirModule = moduleCache.getOrPut(this) {
        buildModule {
            name = getModuleName(this@sirModule)
        }
    }

    private fun getModuleName(ktModule: KtModule): String {
        // For now, we use the same name as we put all modules in the same file.
        // Later we should use proper module names.
        return mainModuleName
    }
}