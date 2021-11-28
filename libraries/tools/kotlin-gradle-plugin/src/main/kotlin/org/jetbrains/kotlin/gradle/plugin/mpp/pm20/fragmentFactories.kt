/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.sources.METADATA_CONFIGURATION_NAME_SUFFIX
import org.jetbrains.kotlin.gradle.utils.addExtendsFromRelation
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.project.model.KotlinModuleFragment
import java.io.File

abstract class AbstractKotlinGradleFragmentFactory<T : KotlinGradleFragment>(
    protected val module: KotlinGradleModule
) : NamedDomainObjectFactory<T> {

    protected val project: Project
        get() = module.project

    abstract fun instantiateFragment(name: String): T

    override fun create(name: String): T =
        instantiateFragment(name).apply {
            createDependencyConfigurations(this)
            addDefaultSourceDirectories(this)
        }

    protected open fun createDependencyConfigurations(fragment: T) {
        with(project.configurations) {
            fragment.relatedConfigurationNames.forEach { configurationName ->
                maybeCreate(configurationName).apply {
                    // FIXME add metadata configurations or API for IDE import?
                    if (!configurationName.endsWith(METADATA_CONFIGURATION_NAME_SUFFIX)) {
                        isCanBeResolved = false
                    }
                    isCanBeConsumed = false
                }
            }
            listOf(
                fragment.apiConfigurationName to fragment.transitiveApiConfigurationName,
                fragment.implementationConfigurationName to fragment.transitiveImplementationConfigurationName
            ).forEach { (configuration, transitiveConfiguration) ->
                project.addExtendsFromRelation(transitiveConfiguration, configuration)
            }
        }
    }

    protected open fun addDefaultSourceDirectories(fragment: KotlinGradleFragment) {
        fragment.kotlinSourceRoots.srcDir(defaultSourceFolder(project, module.name, fragment.fragmentName, "kotlin"))
        // TODO handle resources
    }

    companion object {
        fun defaultSourceFolder(project: Project, moduleName: String, fragmentName: String, type: String): File {
            return project.file("src/${lowerCamelCaseName(fragmentName, moduleName)}/$type")
        }
    }
}

class CommonGradleFragmentFactory(module: KotlinGradleModule) : AbstractKotlinGradleFragmentFactory<KotlinGradleFragmentInternal>(module) {
    override fun instantiateFragment(name: String): KotlinGradleFragmentInternal =
        project.objects.newInstance(KotlinGradleFragmentInternal::class.java, module, name)
}