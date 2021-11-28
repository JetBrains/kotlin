/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.attributes.Usage
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.pm20Extension
import org.jetbrains.kotlin.gradle.plugin.KotlinNativeTargetConfigurator
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.targets.metadata.filesWithUnpackedArchives
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION
import org.jetbrains.kotlin.project.model.refinesClosure
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
            configureHostSpecificMetadata(result)
            configureVariantPublishing(result)
        }

    override fun configureKotlinCompilation(fragment: T) {
        val compilationData = fragment.compilationData
        LifecycleTasksManager(project).registerClassesTask(compilationData)
        KotlinCompilationTaskConfigurator(project).createKotlinNativeCompilationTask(fragment, compilationData)
    }

    private fun configureHostSpecificMetadata(variant: T) {
        val hostSpecificConfigurationNameIfEnabled = variant.hostSpecificMetadataElementsConfigurationName
            ?: return

        val hostSpecificMetadataJar = project.registerTask<Jar>(variant.disambiguateName("hostSpecificMetadataJar")) { jar ->
            jar.archiveClassifier.set("metadata")
            jar.archiveAppendix.set(variant.disambiguateName(""))
            project.pm20Extension.metadataCompilationRegistryByModuleId.getValue(variant.containingModule.moduleIdentifier)
                .withAll { metadataCompilation ->
                    val fragment = metadataCompilation.fragment
                    if (metadataCompilation is KotlinNativeFragmentMetadataCompilationData) {
                        jar.from(project.files(project.provider {
                            if (fragment in variant.refinesClosure && fragment.isNativeHostSpecific())
                                project.filesWithUnpackedArchives(metadataCompilation.output.allOutputs, setOf(KLIB_FILE_EXTENSION))
                            else emptyList<Any>()
                        })) { spec -> spec.into(fragment.name) }
                    }
                }
        }
        val apiElements = project.configurations.getByName(variant.apiElementsConfigurationName)
        project.configurations.create(hostSpecificConfigurationNameIfEnabled).apply {
            isCanBeResolved = false
            isCanBeConsumed = false
            setPlatformAttributesAndMetadataUsage(variant)
            project.artifacts.add(name, hostSpecificMetadataJar)
            dependencies.addAllLater(project.objects.listProperty(Dependency::class.java).apply {
                set(project.provider { apiElements.allDependencies })
            })
        }
    }

    open fun configureVariantPublishing(variant: T) {
        VariantPublishingConfigurator.get(project).configureNativeVariantPublication(variant)
    }

    private fun Configuration.setPlatformAttributesAndMetadataUsage(variant: T) {
        configureApiElementsConfiguration(variant, this) // then override the Usage attribute
        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_METADATA))
    }
}