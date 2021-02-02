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
import org.jetbrains.kotlin.project.model.KotlinModuleIdentifier
import org.jetbrains.kotlin.project.model.LocalModuleIdentifier
import javax.inject.Inject

open class KotlinGradleModule(
    internal val project: Project,
    val moduleClassifier: String?
) : KotlinModule, Named, HasKotlinDependencies {

    @Inject
    constructor(project: Project, moduleClassifier: CharSequence)
            : this(project, moduleClassifier.toString().takeIf { it != MAIN_MODULE_NAME })

    override val moduleIdentifier: KotlinModuleIdentifier =
        LocalModuleIdentifier(project.currentBuildId().name, project.path, moduleClassifier)

    override val fragments: ExtensiblePolymorphicDomainObjectContainer<KotlinGradleFragment> =
        project.objects.polymorphicDomainObjectContainer(KotlinGradleFragment::class.java)

    // TODO DSL & build script model: find a way to create a flexible typed view on fragments?
    override val variants: NamedDomainObjectSet<KotlinGradleVariant> by lazy {
        fragments.withType(KotlinGradleVariant::class.java)
    }

    var isPublic: Boolean = false
        private set

    private var setPublicHandlers: MutableList<() -> Unit> = mutableListOf()

    fun ifMadePublic(action: () -> Unit) {
        if (isPublic) action() else setPublicHandlers.add(action)
    }

    fun makePublic() {
        if (isPublic) return
        setPublicHandlers.forEach { it() }
        isPublic = true
    }

    companion object {
        const val MAIN_MODULE_NAME = "main"
        const val TEST_MODULE_NAME = "test"
    }

    override fun getName(): String = when (moduleClassifier) {
        null -> MAIN_MODULE_NAME
        else -> moduleClassifier
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

internal val KotlinGradleModule.isMain
    get() = moduleIdentifier.moduleClassifier == null
