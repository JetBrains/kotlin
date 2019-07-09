/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources

import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.api.internal.file.FileResolver
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.targets.metadata.isKotlinGranularMetadataEnabled
import java.io.File

internal abstract class KotlinSourceSetFactory<T : KotlinSourceSet> internal constructor(
    protected val fileResolver: FileResolver,
    protected val project: Project
) : NamedDomainObjectFactory<KotlinSourceSet> {

    abstract val itemClass: Class<T>

    override fun create(name: String): T {
        val result = doCreateSourceSet(name)
        setUpSourceSetDefaults(result)
        return result
    }

    protected open fun defaultSourceLocation(sourceSetName: String): File =
        project.file("src/$sourceSetName")

    protected open fun setUpSourceSetDefaults(sourceSet: T) {
        sourceSet.kotlin.srcDir(File(defaultSourceLocation(sourceSet.name), "kotlin"))
        defineSourceSetConfigurations(project, sourceSet)
    }

    private fun defineSourceSetConfigurations(project: Project, sourceSet: KotlinSourceSet) = with(project.configurations) {
        sourceSet.relatedConfigurationNames.forEach { configurationName ->
            maybeCreate(configurationName).apply {
                if (!configurationName.endsWith(METADATA_CONFIGURATION_NAME_SUFFIX)) {
                    isCanBeResolved = false
                }
                isCanBeConsumed = false
            }
        }
    }

    protected abstract fun doCreateSourceSet(name: String): T
}

internal class DefaultKotlinSourceSetFactory(
    project: Project,
    fileResolver: FileResolver
) : KotlinSourceSetFactory<DefaultKotlinSourceSet>(fileResolver, project) {

    override val itemClass: Class<DefaultKotlinSourceSet>
        get() = DefaultKotlinSourceSet::class.java

    override fun setUpSourceSetDefaults(sourceSet: DefaultKotlinSourceSet) {
        super.setUpSourceSetDefaults(sourceSet)
        sourceSet.resources.srcDir(File(defaultSourceLocation(sourceSet.name), "resources"))

        val dependencyConfigurationWithMetadata = with(sourceSet) {
            listOf(
                apiConfigurationName to apiMetadataConfigurationName,
                implementationConfigurationName to implementationMetadataConfigurationName,
                compileOnlyConfigurationName to compileOnlyMetadataConfigurationName,
                runtimeOnlyConfigurationName to runtimeOnlyMetadataConfigurationName
            )
        }

        dependencyConfigurationWithMetadata.forEach { (configurationName, metadataName) ->
            project.configurations.maybeCreate(metadataName).apply {
                attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.common)
                attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_API))
                isVisible = false
                isCanBeConsumed = false
                extendsFrom(project.configurations.maybeCreate(configurationName))

                if (project.isKotlinGranularMetadataEnabled) {
                    attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_METADATA))
                }
            }
        }
    }

    override fun doCreateSourceSet(name: String): DefaultKotlinSourceSet {
        return DefaultKotlinSourceSet(project, name, fileResolver)
    }
}