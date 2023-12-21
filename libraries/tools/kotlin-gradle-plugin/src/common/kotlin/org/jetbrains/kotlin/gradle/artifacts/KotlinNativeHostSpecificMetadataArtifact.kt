/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.artifacts

import org.gradle.api.artifacts.Dependency
import org.gradle.api.attributes.Usage
import org.gradle.api.plugins.BasePlugin
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.AfterFinaliseDsl
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.launchInStage
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.getHostSpecificSourceSets
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.targets.metadata.findMetadataCompilation
import org.jetbrains.kotlin.gradle.targets.metadata.isKotlinGranularMetadataEnabled
import org.jetbrains.kotlin.gradle.targets.native.internal.includeCommonizedCInteropMetadata
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.copyAttributesTo
import org.jetbrains.kotlin.gradle.utils.createConsumable
import org.jetbrains.kotlin.gradle.utils.setAttribute
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

internal val KotlinNativeHostSpecificMetadataArtifact = KotlinTargetArtifact { target, apiElements, _ ->
    if (target !is KotlinNativeTarget) return@KotlinTargetArtifact
    if (!target.project.isKotlinGranularMetadataEnabled) return@KotlinTargetArtifact
    val project = target.project

    target.project.configurations.createConsumable(target.hostSpecificMetadataElementsConfigurationName).also { configuration ->
        configuration.extendsFrom(*apiElements.extendsFrom.toTypedArray())

        target.project.launchInStage(AfterFinaliseDsl) {
            apiElements.copyAttributesTo(
                target.project,
                dest = configuration.attributes
            )
            configuration.setAttribute(
                Usage.USAGE_ATTRIBUTE,
                target.project.usageByName(KotlinUsages.KOTLIN_METADATA)
            )
        }
    }

    val hostSpecificSourceSets = getHostSpecificSourceSets(target.project)
    if (hostSpecificSourceSets.isEmpty()) return@KotlinTargetArtifact

    val hostSpecificMetadataJar = project.locateOrRegisterTask<Jar>(target.hostSpecificMetadataElementsConfigurationName) { metadataJar ->
        metadataJar.archiveAppendix.set(project.provider { target.disambiguationClassifier.orEmpty().toLowerCaseAsciiOnly() })
        metadataJar.archiveClassifier.set("metadata")
        metadataJar.group = BasePlugin.BUILD_GROUP
        metadataJar.description = "Assembles Kotlin metadata of target '${target.name}'."

        val publishable = target.publishable
        metadataJar.onlyIf { publishable }

        project.launch {
            val metadataCompilations = hostSpecificSourceSets.mapNotNull {
                project.findMetadataCompilation(it)
            }

            metadataCompilations.forEach { compilation ->
                metadataJar.from(compilation.output.allOutputs) { spec ->
                    spec.into(compilation.name)
                }
                metadataJar.dependsOn(compilation.output.classesDirs)

                if (compilation is KotlinSharedNativeCompilation) {
                    project.includeCommonizedCInteropMetadata(metadataJar, compilation)
                }
            }
        }
    }
    project.artifacts.add(Dependency.ARCHIVES_CONFIGURATION, hostSpecificMetadataJar)
    project.artifacts.add(target.hostSpecificMetadataElementsConfigurationName, hostSpecificMetadataJar) { artifact ->
        artifact.classifier = "metadata"
    }
}
