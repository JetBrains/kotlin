/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Project
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.DefaultMavenPublication
import org.jetbrains.kotlin.gradle.dsl.pm20Extension
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.copyConfigurationForPublishing
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.listProperty
import org.jetbrains.kotlin.gradle.tooling.buildKotlinToolingMetadataTask
import org.jetbrains.kotlin.gradle.utils.lowerCaseDashSeparatedName
import javax.inject.Inject

internal fun setupKpmModulesPublication(project: Project) {
    project.kpmModules.all { module ->
        setupPublicationForModule(module)
    }
}

internal fun setupPublicationForModule(module: KotlinGradleModule) {
    val project = module.project
    val softwareComponentFactory = SoftwareComponentFactoryHolder(project).softwareComponentFactory

    module.ifMadePublic {
        val publicationHolder: SingleMavenPublishedModuleHolder? = module.publicationHolder()

        val metadataElements = project.configurations.getByName(metadataElementsConfigurationName(module))
        val sourceElements = project.configurations.getByName(sourceElementsConfigurationName(module))

        val publishedConfigurationNameSuffix = "-published"
        val metadataElementsForPublishing = copyConfigurationForPublishing(
            project,
            metadataElements.name + publishedConfigurationNameSuffix,
            metadataElements,
            overrideDependencies = {
                addAllLater(project.listProperty {
                    replaceProjectDependenciesWithPublishedMavenDependencies(project, metadataElements.allDependencies)
                })
            },
            overrideCapabilities = { setGradlePublishedModuleCapability(this, module) }
        )

        val sourceElementsForPublishing = copyConfigurationForPublishing(
            project,
            sourceElements.name + publishedConfigurationNameSuffix,
            sourceElements,
            overrideCapabilities = { setGradlePublishedModuleCapability(this, module) }
        )

        fun addVariantsToSoftwareComponent(component: AdhocComponentWithVariants) {
            component.addVariantsFromConfiguration(metadataElementsForPublishing) { }
            component.addVariantsFromConfiguration(sourceElementsForPublishing) { }
        }

        val rootSoftwareComponent = when (module.publicationMode) {
            is Standalone -> {
                val component = softwareComponentFactory.adhoc(rootPublicationComponentName(module))
                project.components.add(component)
                component
            }

            is Embedded -> {
                project.components.withType(AdhocComponentWithVariants::class.java)
                    .getByName(rootPublicationComponentName(project.pm20Extension.main))
            }

            Private -> error("unexpected private module; expected publicationMode: Standalone or Embedded")
        }

        addVariantsToSoftwareComponent(rootSoftwareComponent)

        project.pluginManager.withPlugin("maven-publish") {
            val publishing = project.extensions.getByType(PublishingExtension::class.java)
            when (val publicationMode = module.publicationMode) {
                is Standalone -> {
                    project.pluginManager.withPlugin("maven-publish") {
                        publishing.publications.create(rootSoftwareComponent.name, MavenPublication::class.java) { publication ->
                            if (!module.isMain) {
                                (publication as DefaultMavenPublication).isAlias = true
                            }
                            publication.artifactId = lowerCaseDashSeparatedName(project.name, publicationMode.defaultArtifactIdSuffix)
                            publication.from(rootSoftwareComponent)
                            publication.setupKotlinToolingMetadataIfNeeded(module)
                            publicationHolder?.assignMavenPublication(publication)
                        }
                    }
                }

                Embedded -> {
                    val mainPublication = publishing.publications.withType(MavenPublication::class.java).getByName(rootSoftwareComponent.name)
                    publicationHolder?.assignMavenPublication(mainPublication)
                }
            }
        }
    }
}

private fun MavenPublication.setupKotlinToolingMetadataIfNeeded(module: KotlinGradleModule) {
    val buildKotlinToolingMetadataTask = module.buildKotlinToolingMetadataTask ?: return

    artifact(buildKotlinToolingMetadataTask.map { it.outputFile }) { artifact ->
        artifact.classifier = "kotlin-tooling-metadata"
        artifact.builtBy(buildKotlinToolingMetadataTask)
    }
}

private open class SoftwareComponentFactoryHolder @Inject constructor(
    @Inject val softwareComponentFactory: SoftwareComponentFactory
) {
    companion object {
        operator fun invoke(project: Project): SoftwareComponentFactoryHolder {
            return project.objects.newInstance(SoftwareComponentFactoryHolder::class.java)
        }
    }
}