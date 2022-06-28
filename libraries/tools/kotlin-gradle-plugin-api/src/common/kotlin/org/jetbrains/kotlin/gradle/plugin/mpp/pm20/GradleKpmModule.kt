/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.*
import org.jetbrains.kotlin.gradle.plugin.HasKotlinDependencies
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.project.model.KpmModule
import org.jetbrains.kotlin.project.model.KpmModuleIdentifier
import org.jetbrains.kotlin.project.model.KpmCompilerPlugin

interface GradleKpmModule : KpmModule, Named, HasKotlinDependencies {
    val project: Project
    val moduleClassifier: String?

    override val fragments: ExtensiblePolymorphicDomainObjectContainer<GradleKpmFragment>

    // TODO DSL & build script model: find a way to create a flexible typed view on fragments?
    override val variants: NamedDomainObjectSet<GradleKpmVariant>

    override val plugins: Set<KpmCompilerPlugin>

    val isPublic: Boolean

    fun ifMadePublic(action: () -> Unit)

    fun makePublic()

    companion object {
        val KpmModuleIdentifier.moduleName get() = moduleClassifier ?: MAIN_MODULE_NAME

        const val MAIN_MODULE_NAME = "main"
        const val TEST_MODULE_NAME = "test"
    }

    override fun getName(): String = when (val classifier = moduleClassifier) {
        null -> MAIN_MODULE_NAME
        else -> classifier
    }

    // DSL

    val common: GradleKpmFragment
        get() = fragments.getByName(GradleKpmFragment.COMMON_FRAGMENT_NAME)

    fun common(configure: GradleKpmFragment.() -> Unit) =
        common.configure()

    override fun dependencies(configure: KotlinDependencyHandler.() -> Unit) =
        common.dependencies(configure)

    override fun dependencies(configure: Action<KotlinDependencyHandler>) =
        dependencies { configure.execute(this) }

    override val apiConfigurationName: String
        get() = common.apiConfigurationName

    override val implementationConfigurationName: String
        get() = common.implementationConfigurationName

    override val compileOnlyConfigurationName: String
        get() = common.compileOnlyConfigurationName

    override val runtimeOnlyConfigurationName: String
        get() = common.runtimeOnlyConfigurationName
}
