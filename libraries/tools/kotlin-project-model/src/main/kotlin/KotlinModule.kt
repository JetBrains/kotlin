/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model

sealed class ModuleOrigin
data class LocalBuild(val buildId: String) : ModuleOrigin() // TODO add project ID?
data class ExternalOrigin(val dependencyIdParts: List<String>) : ModuleOrigin()

interface KotlinModule {
    val moduleName: String
    val moduleOrigin: ModuleOrigin

    val fragments: Iterable<KotlinModuleFragment>

    val variants: Iterable<KotlinModuleVariant>
        get() = fragments.filterIsInstance<KotlinModuleVariant>()
}

class BasicKotlinModule(
    override val moduleName: String,
    override val moduleOrigin: ModuleOrigin
) : KotlinModule {
    override val fragments = mutableListOf<BasicKotlinModuleFragment>()

    override fun toString(): String = "module '$moduleName'"
}