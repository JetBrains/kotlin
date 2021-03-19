/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.attributes.Usage
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.DefaultMavenPublication
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.pm20Extension
import org.jetbrains.kotlin.gradle.plugin.KotlinNativeTargetConfigurator
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.CalculatedCapability
import org.jetbrains.kotlin.gradle.plugin.mpp.publishedConfigurationName
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.targets.metadata.filesWithUnpackedArchives
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.dashSeparatedName
import org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION
import org.jetbrains.kotlin.project.model.refinesClosure
import org.jetbrains.kotlin.project.model.utils.variantsContainingFragment
import java.util.concurrent.Callable
import kotlin.reflect.KClass

open class KotlinNativeVariantFactory<T : KotlinNativeVariantInternal>(
    module: KotlinGradleModule,
    val variantClass: KClass<out T>
) :
    AbstractKotlinGradleVariantFactory<T>(module) {

    override fun instantiateFragment(name: String): T =
        module.project.objects.newInstance(variantClass.java, module, name)

    override fun setPlatformAttributesInConfiguration(fragment: T, configuration: Configuration) {
        super.setPlatformAttributesInConfiguration(fragment, configuration)
        configuration.attributes.attribute(KotlinNativeTarget.konanTargetAttribute, fragment.konanTarget.name)
    }

    override fun create(name: String): T =
        super.create(name).also { result ->
            configureVariantPublishing(result)
        }

    override fun configureKotlinCompilation(fragment: T) {
        val compilationData = fragment.compilationData

        //FIXME: deduplicate with KotlinJvmVariantFactory
        val classesTaskName = compilationData.compileAllTaskName
        project.tasks.register(classesTaskName) { classesTask ->
            classesTask.dependsOn(fragment.compilationOutputs.allOutputs)
        }

        //FIXME: needs refactoring so as not to create the configurator
        val compileTask = with(KotlinNativeTargetConfigurator<Nothing>(project.getKotlinPluginVersion()!!)) {
            project.createKlibCompilationTask(compilationData)
        }

        // FIXME: duplication with KotlinJvmVariantFactory
        val allSources = { project.files(Callable { fragment.refinesClosure.map { it.kotlinSourceRoots } }) }
        val commonSources = {
            project.files(Callable {
                fragment.refinesClosure.filter { module.variantsContainingFragment(it).count() > 1 }.map { it.kotlinSourceRoots }
            })
        }

        compileTask.configure {
            it.source(allSources)
            it.commonSources.from(commonSources)
        }
    }

    open fun configureVariantPublishing(variant: T) {
        val rootSoftwareComponent =
            project.components
                .withType(AdhocComponentWithVariants::class.java)
                .getByName(rootPublicationComponentName(module))

        val platformModuleDependencyProvider = project.provider {
            val coordinates = variant.publishedMavenModuleCoordinates
            (project.dependencies.create("${coordinates.group}:${coordinates.name}:${coordinates.version}") as ModuleDependency).apply {
                if (module.moduleClassifier != null) {
                    capabilities { it.requireCapability(CalculatedCapability.fromModule(module)) }
                }
            }
        }

        val apiElements = project.configurations.getByName(variant.apiElementsConfigurationName)
        val hostSpecificMetadataJar = project.registerTask<Jar>(variant.disambiguateName("hostSpecificMetadataJar")) { jar ->
            jar.archiveClassifier.set("metadata")
            jar.archiveAppendix.set(variant.disambiguateName(""))
            project.pm20Extension.metadataCompilationRegistryByModuleId.getValue(variant.containingModule.moduleIdentifier)
                .withAll { metadataCompilation ->
                    val fragment = metadataCompilation.fragment
                    if (metadataCompilation is KotlinNativeFragmentMetadataCompilationData) {
                        jar.from(project.files(Callable {
                            if (fragment in variant.refinesClosure && fragment.isNativeHostSpecific())
                                project.filesWithUnpackedArchives(metadataCompilation.output.allOutputs, setOf(KLIB_FILE_EXTENSION))
                            else emptyList<Any>()
                        })) { spec -> spec.into(fragment.name) }
                    }
                }
        }
        val hostSpecificMetadataElements = project.configurations.create(variant.hostSpecificMetadataConfigurationName).apply {
            isCanBeResolved = false
            isCanBeConsumed = false
            setPlatformAttributesAndMetadataUsage(variant)
            project.artifacts.add(name, hostSpecificMetadataJar)
            dependencies.addAllLater(project.objects.listProperty(Dependency::class.java).apply {
                set(project.provider { apiElements.allDependencies })
            })
        }

        // FIXME inject vs internal API
        val platformComponentName = platformComponentName(variant)
        val platformComponent = (project as ProjectInternal).services
            .get(SoftwareComponentFactory::class.java)
            .adhoc(platformComponentName)
        project.components.add(platformComponent)
        platformComponent.addVariantsFromConfiguration(apiElements) {
            it.mapToMavenScope("compile")
        }
        platformComponent.addVariantsFromConfiguration(hostSpecificMetadataElements) { }

        module.ifMadePublic {
            project.pluginManager.withPlugin("maven-publish") {
                project.extensions.getByType(PublishingExtension::class.java).apply {
                    publications.create(platformComponentName, MavenPublication::class.java).apply {
                        (this as DefaultMavenPublication).isAlias = true
                        from(platformComponent)
                        variant.assignMavenPublication(this)
                        artifactId = dashSeparatedName(project.name, variant.defaultPublishedModuleSuffix)
                    }
                }
            }
        }

        val publishedApiConfiguration =
            project.configurations.create(publishedConfigurationName(variant.apiElementsConfigurationName)).apply {
                isCanBeConsumed = false
                isCanBeResolved = false
                configureApiElementsConfiguration(variant, this)
                setModuleCapability(this, module)
                dependencies.addLater(platformModuleDependencyProvider)
            }
        val publishedMetadataConfiguration =
            project.configurations.create(publishedConfigurationName(variant.hostSpecificMetadataConfigurationName)).apply {
                isCanBeConsumed = false
                isCanBeResolved = false
                setPlatformAttributesAndMetadataUsage(variant)
                setModuleCapability(this, module)
                dependencies.addLater(platformModuleDependencyProvider)
            }
        rootSoftwareComponent.addVariantsFromConfiguration(publishedApiConfiguration) { }
        rootSoftwareComponent.addVariantsFromConfiguration(publishedMetadataConfiguration) { }
    }

    private fun Configuration.setPlatformAttributesAndMetadataUsage(variant: T) {
        configureApiElementsConfiguration(variant, this) // then override the Usage attribute
        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_METADATA))
    }

    open fun platformComponentName(variant: T) = variant.disambiguateName("")
}