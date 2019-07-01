/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurablePublishArtifact
import org.gradle.api.artifacts.Dependency
import org.gradle.api.attributes.Usage.JAVA_API
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope
import org.jetbrains.kotlin.gradle.plugin.sources.sourceSetDependencyConfigurationByScope
import org.jetbrains.kotlin.gradle.targets.metadata.KotlinMetadataTargetConfigurator
import org.jetbrains.kotlin.gradle.targets.metadata.isKotlinGranularMetadataEnabled
import org.jetbrains.kotlin.gradle.utils.isGradleVersionAtLeast

internal const val COMMON_MAIN_ELEMENTS_CONFIGURATION_NAME = "commonMainMetadataElements"

open class KotlinMetadataTarget(project: Project) : KotlinOnlyTarget<KotlinCommonCompilation>(project, KotlinPlatformType.common) {
    override val kotlinComponents: Set<KotlinTargetComponent> by lazy {
        if (!project.isKotlinGranularMetadataEnabled)
            super.kotlinComponents
        else {
            val usageContexts = mutableSetOf<DefaultKotlinUsageContext>()

            // This usage value is only needed for Maven scope mapping. Don't replace it with a custom Kotlin Usage value
            val javaApiUsage = project.usageByName(if (isGradleVersionAtLeast(5, 3)) "java-api-jars" else JAVA_API)

            usageContexts += run {
                val allMetadataJar = project.tasks.getByName(KotlinMetadataTargetConfigurator.ALL_METADATA_JAR_NAME)
                val allMetadataArtifact = project.artifacts.add(Dependency.ARCHIVES_CONFIGURATION, allMetadataJar) { it.classifier = "all" }

                DefaultKotlinUsageContext(
                    compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME),
                    javaApiUsage,
                    apiElementsConfigurationName,
                    overrideConfigurationArtifacts = setOf(allMetadataArtifact)
                )
            }

            // Ensure that consumers who expect Kotlin 1.2.x metadata package can still get one: publish the old metadata artifact as well:
            usageContexts += run {
                project.configurations.create(COMMON_MAIN_ELEMENTS_CONFIGURATION_NAME).apply {
                    isCanBeConsumed = false
                    isCanBeResolved = false
                    usesPlatformOf(this@KotlinMetadataTarget)
                    attributes.attribute(USAGE_ATTRIBUTE, KotlinUsages.producerApiUsage(this@KotlinMetadataTarget)) // 'kotlin-api' usage

                    val commonMainApiConfiguration = project.sourceSetDependencyConfigurationByScope(
                        project.kotlinExtension.sourceSets.getByName(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME),
                        KotlinDependencyScope.API_SCOPE
                    )
                    extendsFrom(commonMainApiConfiguration)

                    project.artifacts.add(name, project.tasks.getByName(artifactsTaskName))
                }

                DefaultKotlinUsageContext(
                    compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME),
                    javaApiUsage,
                    COMMON_MAIN_ELEMENTS_CONFIGURATION_NAME
                )
            }

            val component =
                createKotlinVariant(targetName, compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME), usageContexts)

            val sourcesJarTask =
                sourcesJarTask(project, lazy { project.kotlinExtension.sourceSets.toSet() }, null, targetName.toLowerCase())

            component.sourcesArtifacts = setOf(
                project.artifacts.add(Dependency.ARCHIVES_CONFIGURATION, sourcesJarTask).apply {
                    this as ConfigurablePublishArtifact
                    classifier = "sources"
                }
            )

            setOf(component)
        }
    }
}