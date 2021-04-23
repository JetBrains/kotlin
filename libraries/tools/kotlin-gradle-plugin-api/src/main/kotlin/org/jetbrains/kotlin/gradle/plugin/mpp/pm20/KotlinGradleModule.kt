/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import groovy.lang.Closure
import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectSet
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.HasKotlinDependencies
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.project.model.KotlinModule
import org.jetbrains.kotlin.project.model.KpmCompilerPlugin

interface KotlinGradleModule : KotlinModule, Named, HasKotlinDependencies {
    val project: Project
    val moduleClassifier: String?

    override val fragments: ExtensiblePolymorphicDomainObjectContainer<KotlinGradleFragment>

    // TODO DSL & build script model: find a way to create a flexible typed view on fragments?
    override val variants: NamedDomainObjectSet<KotlinGradleVariant>

    override val plugins: Set<KpmCompilerPlugin>

    val isPublic: Boolean

    fun ifMadePublic(action: () -> Unit)

    fun makePublic()

    companion object {
        const val MAIN_MODULE_NAME = "main"
        const val TEST_MODULE_NAME = "test"
    }

    override fun getName(): String = when (val classifier = moduleClassifier) {
        null -> MAIN_MODULE_NAME
        else -> classifier
    }

    // DSL

    val common: KotlinGradleFragment
        get() = fragments.getByName(KotlinGradleFragment.COMMON_FRAGMENT_NAME)

    fun common(configure: KotlinGradleFragment.() -> Unit) =
        common.configure()

    override fun dependencies(configure: KotlinDependencyHandler.() -> Unit) =
        common.dependencies(configure)

    override fun dependencies(configureClosure: Closure<Any?>) =
        common.dependencies(configureClosure)

    override val apiConfigurationName: String
        get() = common.apiConfigurationName

    override val implementationConfigurationName: String
        get() = common.implementationConfigurationName

    override val compileOnlyConfigurationName: String
        get() = common.compileOnlyConfigurationName

    override val runtimeOnlyConfigurationName: String
        get() = common.runtimeOnlyConfigurationName
}
