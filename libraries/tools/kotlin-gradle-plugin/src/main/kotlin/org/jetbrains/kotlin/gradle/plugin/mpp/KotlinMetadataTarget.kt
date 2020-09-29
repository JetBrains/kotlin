/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurablePublishArtifact
import org.gradle.api.artifacts.Dependency
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.targets.metadata.KotlinMetadataTargetConfigurator
import org.jetbrains.kotlin.gradle.targets.metadata.isCompatibilityMetadataVariantEnabled
import org.jetbrains.kotlin.gradle.targets.metadata.isKotlinGranularMetadataEnabled
import javax.inject.Inject

internal const val COMMON_MAIN_ELEMENTS_CONFIGURATION_NAME = "commonMainMetadataElements"

open class KotlinMetadataTarget @Inject constructor(project: Project) :
    KotlinOnlyTarget<AbstractKotlinCompilation<*>>(project, KotlinPlatformType.common) {

    override val artifactsTaskName: String
        // The IDE import looks at this task name to determine the artifact and register the path to the artifact;
        // in HMPP, since the project resolves to the all-metadata JAR, the IDE import needs to work with that JAR, too
        get() = if (project.isKotlinGranularMetadataEnabled) KotlinMetadataTargetConfigurator.ALL_METADATA_JAR_NAME else super.artifactsTaskName

    internal val legacyArtifactsTaskName: String
        get() = super.artifactsTaskName

    override val kotlinComponents: Set<KotlinTargetComponent> by lazy {
        if (!project.isKotlinGranularMetadataEnabled)
            super.kotlinComponents
        else {
            val usageContexts = mutableSetOf<DefaultKotlinUsageContext>()

            // This usage value is only needed for Maven scope mapping. Don't replace it with a custom Kotlin Usage value
            val javaApiUsage = project.usageByName("java-api-jars")

            usageContexts += run {
                val allMetadataJar = project.tasks.named(KotlinMetadataTargetConfigurator.ALL_METADATA_JAR_NAME)
                val allMetadataArtifact = project.artifacts.add(Dependency.ARCHIVES_CONFIGURATION, allMetadataJar) {
                    it.classifier = if (project.isCompatibilityMetadataVariantEnabled) "all" else ""
                }

                DefaultKotlinUsageContext(
                    compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME),
                    javaApiUsage,
                    apiElementsConfigurationName,
                    overrideConfigurationArtifacts = setOf(allMetadataArtifact)
                )
            }

            if (PropertiesProvider(project).enableCompatibilityMetadataVariant == true) {
                // Ensure that consumers who expect Kotlin 1.2.x metadata package can still get one:
                // publish the old metadata artifact:
                usageContexts += run {
                    DefaultKotlinUsageContext(
                        compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME),
                        javaApiUsage,
                        /** this configuration is created by [KotlinMetadataTargetConfigurator.createCommonMainElementsConfiguration] */
                        COMMON_MAIN_ELEMENTS_CONFIGURATION_NAME
                    )
                }
            }

            val component =
                createKotlinVariant(targetName, compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME), usageContexts).apply {
                    publishable = false // this component is not published on its own, its variants are included in the 'root' module
                }

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