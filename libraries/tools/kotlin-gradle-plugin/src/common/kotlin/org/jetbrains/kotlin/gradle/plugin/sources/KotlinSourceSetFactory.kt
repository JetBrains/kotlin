/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources

import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Project
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.targets.metadata.isKotlinGranularMetadataEnabled
import org.jetbrains.kotlin.gradle.utils.maybeCreateDependencyScope
import org.jetbrains.kotlin.gradle.utils.maybeCreateResolvable
import java.io.File

internal abstract class KotlinSourceSetFactory<T : KotlinSourceSet> internal constructor(
    protected val project: Project
) : NamedDomainObjectFactory<KotlinSourceSet> {

    abstract val itemClass: Class<T>

    override fun create(name: String): T {
        val result = doCreateSourceSet(name)
        setUpSourceSetDefaults(result)
        return result
    }

    protected open fun setUpSourceSetDefaults(sourceSet: T) {
        sourceSet.kotlin.srcDir(defaultSourceFolder(project, sourceSet.name, "kotlin"))
        defineSourceSetConfigurations(project, sourceSet)
    }

    private fun defineSourceSetConfigurations(project: Project, sourceSet: KotlinSourceSet) = with(project.configurations) {
        val configurationNames = sourceSet.run {
            listOfNotNull(
                apiConfigurationName,
                implementationConfigurationName,
                compileOnlyConfigurationName,
                runtimeOnlyConfigurationName,
            )
        }
        configurationNames.forEach { configurationName ->
            if (!configurationName.endsWith(METADATA_CONFIGURATION_NAME_SUFFIX)) {
                maybeCreateDependencyScope(configurationName)
            } else {
                maybeCreateResolvable(configurationName)
            }
        }
    }

    protected abstract fun doCreateSourceSet(name: String): T

    companion object {
        /**
         * @return default location of source folders for a kotlin source set
         * e.g. src/jvmMain/kotlin  (sourceSetName="jvmMain", type="kotlin")
         */
        fun defaultSourceFolder(project: Project, sourceSetName: String, type: String): File {
            return project.file("src/$sourceSetName/$type")
        }
    }
}


internal class DefaultKotlinSourceSetFactory(
    project: Project
) : KotlinSourceSetFactory<DefaultKotlinSourceSet>(project) {

    override val itemClass: Class<DefaultKotlinSourceSet>
        get() = DefaultKotlinSourceSet::class.java

    override fun setUpSourceSetDefaults(sourceSet: DefaultKotlinSourceSet) {
        super.setUpSourceSetDefaults(sourceSet)
        sourceSet.resources.srcDir(defaultSourceFolder(project, sourceSet.name, "resources"))

        val dependencyConfigurationWithMetadata = with(sourceSet) {
            @Suppress("DEPRECATION")
            listOf(
                apiConfigurationName to apiMetadataConfigurationName,
                implementationConfigurationName to implementationMetadataConfigurationName,
                compileOnlyConfigurationName to compileOnlyMetadataConfigurationName,
                null to intransitiveMetadataConfigurationName
            )
        }

        dependencyConfigurationWithMetadata.forEach { (configurationName, metadataName) ->
            project.configurations.maybeCreateResolvable(metadataName).apply {
                attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.common)
                attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_API))
                attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
                isVisible = false

                if (configurationName != null) {
                    extendsFrom(project.configurations.maybeCreateDependencyScope(configurationName))
                }

                if (project.isKotlinGranularMetadataEnabled) {
                    attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_METADATA))
                }
            }
        }
    }

    override fun doCreateSourceSet(name: String): DefaultKotlinSourceSet =
        project.objects.newInstance(DefaultKotlinSourceSet::class.java, project, name)
}
