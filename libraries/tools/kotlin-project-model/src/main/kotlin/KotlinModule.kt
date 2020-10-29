/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model

sealed class ModuleSource {
    class LocalBuild(val buildId: String) : ModuleSource()
    class ExternalDependency(val dependencyId: String) : ModuleSource()
}

interface KotlinModule {
    val moduleName: String
    val moduleSource: ModuleSource

    val fragments: Iterable<KotlinModuleFragment>

    val variants: Iterable<KotlinModuleVariant>
        get() = fragments.filterIsInstance<KotlinModuleVariant>()
}

class BasicKotlinModule(
    override val moduleName: String,
    override val moduleSource: ModuleSource
) : KotlinModule {
    override val fragments = mutableListOf<BasicKotlinModuleFragment>()

    override fun toString(): String = "module '$moduleName'"
}