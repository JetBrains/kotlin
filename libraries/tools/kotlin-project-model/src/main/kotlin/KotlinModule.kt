/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model

// TODO sealed with an abstract subclass? this will make exhaustive checks work
open class ModuleIdentifier

// TODO consider id: Any, to allow IDs with custom equality?
data class LocalModuleIdentifier(val buildId: String, val projectId: String) : ModuleIdentifier() {
    companion object {
        private const val SINGLE_BUILD_ID = ":"
    }

    override fun toString(): String = "project '$projectId'" + buildId.takeIf { it != SINGLE_BUILD_ID }?.let { "(build '$it')" }.orEmpty()
}

data class MavenModuleIdentifier(val group: String, val name: String) : ModuleIdentifier() {
    override fun toString(): String = "$group:$name"
}

// TODO Gradle allows having multiple capabilities in a published module, we need to figure out how we can include them in the module IDs

interface KotlinModule {
    val moduleIdentifier: ModuleIdentifier

    val fragments: Iterable<KotlinModuleFragment>

    val variants: Iterable<KotlinModuleVariant>
        get() = fragments.filterIsInstance<KotlinModuleVariant>()
}

class BasicKotlinModule(
    override val moduleIdentifier: ModuleIdentifier
) : KotlinModule {
    override val fragments = mutableListOf<BasicKotlinModuleFragment>()

    override fun toString(): String = "module $moduleIdentifier"
}