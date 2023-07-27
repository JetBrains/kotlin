/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources

import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.targets
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.metadata.isKotlinGranularMetadataEnabled
import org.jetbrains.kotlin.gradle.utils.getOrCreate
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
            maybeCreate(configurationName).apply {
                if (!configurationName.endsWith(METADATA_CONFIGURATION_NAME_SUFFIX)) {
                    isCanBeResolved = false
                }
                isCanBeConsumed = false
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
            project.configurations.getOrCreate(metadataName).apply {
                attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.common)
                attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_API))
                attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
                isVisible = false
                isCanBeConsumed = false

                if (configurationName != null) {
                    extendsFrom(project.configurations.maybeCreate(configurationName))
                }

                if (project.isKotlinGranularMetadataEnabled) {
                    attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_METADATA))
                }

                project.afterEvaluate {
                    setJsCompilerIfNecessary(sourceSet, this)
                }
            }
        }
    }

    // KT-47163
    // It is necessary to set jsCompilerAttribute to configurations which associated with ONLY js source sets
    // Otherwise configuration cannot be resolved because ambiguity between IR and Legacy variants inside one module
    private val notOnlyJsSourceSets = mutableSetOf<KotlinSourceSet>()

    private val jsOnlySourceSetsAttributes = mutableMapOf<KotlinSourceSet, KotlinJsCompilerAttribute>()

    private fun setJsCompilerIfNecessary(sourceSet: KotlinSourceSet, configuration: Configuration) {
        if (sourceSet in notOnlyJsSourceSets) return

        if (sourceSet in jsOnlySourceSetsAttributes) {
            configuration.attributes.attribute(
                KotlinJsCompilerAttribute.jsCompilerAttribute,
                jsOnlySourceSetsAttributes.getValue(sourceSet)
            )
            return
        }

        project.kotlinExtension.targets
            .filter { it !is KotlinJsIrTarget && it !is KotlinJsTarget }
            .forEach { target ->
                target.compilations.forEach { compilation ->
                    notOnlyJsSourceSets.addAll(compilation.allKotlinSourceSets)
                }
            }

        if (sourceSet in notOnlyJsSourceSets) return

        fun chooseCompilerAttribute(target: KotlinTarget): KotlinJsCompilerAttribute {
            if (target is KotlinJsIrTarget) {
                return KotlinJsCompilerAttribute.ir
            }

            target as KotlinJsTarget
            return if (target.irTarget != null) KotlinJsCompilerAttribute.ir else KotlinJsCompilerAttribute.legacy
        }

        project.kotlinExtension.targets
            .filter { it is KotlinJsTarget || (it is KotlinJsIrTarget && it.platformType == KotlinPlatformType.js) }
            .forEach { target ->
                target.compilations
                    .filterIsInstance<KotlinJsCompilation>()
                    .forEach { compilation ->
                        if (sourceSet in compilation.allKotlinSourceSets) {
                            val compilerAttribute = chooseCompilerAttribute(target)
                            jsOnlySourceSetsAttributes[sourceSet] = compilerAttribute
                            configuration.attributes.attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, compilerAttribute)
                            return
                        }
                    }
            }
    }

    override fun doCreateSourceSet(name: String): DefaultKotlinSourceSet =
        project.objects.newInstance(DefaultKotlinSourceSet::class.java, project, name)
}
