/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model

import java.io.Serializable

sealed class KotlinModuleIdentifier(open val moduleClassifier: String?): Serializable

// TODO consider id: Any, to allow IDs with custom equality?
data class LocalModuleIdentifier(
    val buildId: String,
    val projectId: String,
    override val moduleClassifier: String?
) : KotlinModuleIdentifier(moduleClassifier) {
    companion object {
        private const val SINGLE_BUILD_ID = ":"
    }

    override fun toString(): String =
        "project '$projectId'" +
                moduleClassifier?.let { " / $it" }.orEmpty() +
                buildId.takeIf { it != SINGLE_BUILD_ID }?.let { "(build '$it')" }.orEmpty()
}

data class MavenModuleIdentifier(
    val group: String,
    val name: String,
    override val moduleClassifier: String?
) : KotlinModuleIdentifier(moduleClassifier) {
    override fun toString(): String = "$group:$name" + moduleClassifier?.let { " / $it" }.orEmpty()
}

// TODO Gradle allows having multiple capabilities in a published module, we need to figure out how we can include them in the module IDs

interface KotlinModule {
    val moduleIdentifier: KotlinModuleIdentifier

    val fragments: Iterable<KotlinModuleFragment>

    val variants: Iterable<KotlinModuleVariant>
        get() = fragments.filterIsInstance<KotlinModuleVariant>()

    val plugins: Iterable<KpmCompilerPlugin>

    // TODO: isSynthetic?
}

open class BasicKotlinModule(
    override val moduleIdentifier: KotlinModuleIdentifier
) : KotlinModule {
    override val fragments = mutableListOf<BasicKotlinModuleFragment>()

    override val plugins = mutableListOf<KpmCompilerPlugin>()

    override fun toString(): String = "module $moduleIdentifier"
}
