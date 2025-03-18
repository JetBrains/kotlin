/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication

import org.gradle.api.Project
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.awaitMetadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.isUklib
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.isUklibTrue
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.utils.createConsumable

internal const val UKLIB_API_ELEMENTS_NAME = "uklibApiElements"
internal const val UKLIB_RUNTIME_ELEMENTS_NAME = "uklibRuntimeElements"

internal const val UKLIB_JAVA_API_ELEMENTS_STUB_NAME = "javaApiElements"
internal const val UKLIB_JAVA_RUNTIME_ELEMENTS_STUB_NAME = "javaRuntimeElements"

internal suspend fun Project.createUklibPublication(): List<DefaultKotlinUsageContext> {
    val taskName = "archiveUklib"
    val archiveUklib = tasks.register(taskName, ArchiveUklibTask::class.java)

    val kgpFragments = multiplatformExtension.validateKgpModelIsUklibCompliantAndCreateKgpFragments()

    kgpFragments.forEach { fragment ->
        archiveUklib.configure {
            // outputFile might be a directory or a file
            it.inputs.files(fragment.outputFile)
        }
    }

    archiveUklib.configure { task ->
        kgpFragments.forEach { fragment ->
            task.fragments.add(
                ArchiveUklibTask.ArchiveUklibTaskFragment(
                    fragment.fragment,
                    fragment.refineesTransitiveClosure,
                )
            )
        }
    }

    return setupOutgoingUklibConfigurations(
        archiveUklib,
        kgpFragments
    )
}

internal suspend fun Project.setupOutgoingUklibConfigurations(
    archiveTask: TaskProvider<ArchiveUklibTask>,
    publishedCompilations: List<KGPUklibFragment>,
): List<DefaultKotlinUsageContext> {
    configurations.createConsumable(UKLIB_API_ELEMENTS_NAME) {
        attributes.apply {
            attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_UKLIB_API))
            attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
            attribute(isUklib, isUklibTrue)
        }
        publishedCompilations.forEach {
            extendsFrom(
                it.compilation.internal.configurations.apiConfiguration
            )
            if (it.compilation is KotlinNativeCompilation) {
                extendsFrom(it.compilation.internal.configurations.implementationConfiguration)
            }
        }
    }
    configurations.createConsumable(UKLIB_RUNTIME_ELEMENTS_NAME) {
        attributes.apply {
            attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_UKLIB_RUNTIME))
            attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
            attribute(isUklib, isUklibTrue)
        }
        publishedCompilations.forEach {
            it.compilation.internal.configurations.runtimeDependencyConfiguration?.let {
                extendsFrom(it)
            }
        }
    }

    project.artifacts.add(UKLIB_API_ELEMENTS_NAME, archiveTask) {
        it.extension = Uklib.UKLIB_EXTENSION
    }
    project.artifacts.add(UKLIB_RUNTIME_ELEMENTS_NAME, archiveTask) {
        it.extension = Uklib.UKLIB_EXTENSION
    }

    KotlinPluginLifecycle.Stage.AfterFinaliseCompilations.await()
    val metadataTarget = multiplatformExtension.awaitMetadataTarget()
    val variants = mutableListOf(
        DefaultKotlinUsageContext(
            // Whatever, this compilation doesn't matter
            compilation = metadataTarget.compilations.getByName("main"),
            dependencyConfigurationName = UKLIB_RUNTIME_ELEMENTS_NAME,
            includeIntoProjectStructureMetadata = false,
        ),
        DefaultKotlinUsageContext(
            compilation = metadataTarget.compilations.getByName("main"),
            dependencyConfigurationName = UKLIB_API_ELEMENTS_NAME,
            includeIntoProjectStructureMetadata = false,
        )
    )

    /**
     * FIXME: Rewrite this. KotlinJvmTarget creates it's usages (variants) in AbstractKotlinTarget.kotlinComponents and we want to get these
     * usages here or create them using a shared execution path without the copypasted code below when jvm target is not present
     */
    val jvmTarget = project.multiplatformExtension.awaitTargets().singleOrNull { it is KotlinJvmTarget }
    if (jvmTarget != null) {
        val kotlinTargetComponent = jvmTarget.components.single() as KotlinTargetSoftwareComponentImpl
        @Suppress("UNCHECKED_CAST")
        variants += (kotlinTargetComponent.kotlinComponent as InternalKotlinTargetComponent).usages as Set<DefaultKotlinUsageContext>
    } else {
        // FIXME: Test that stubJvmVariant inherits dependencies from relevant configurations
        val jar = stubJvmJarTask()
        configurations.createConsumable(UKLIB_JAVA_API_ELEMENTS_STUB_NAME) {
            attributes.apply {
                attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(Usage.JAVA_API))
                attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
            }
            publishedCompilations.forEach {
                extendsFrom(
                    it.compilation.internal.configurations.apiConfiguration
                )
                if (it.compilation is KotlinNativeCompilation) {
                    extendsFrom(it.compilation.internal.configurations.implementationConfiguration)
                }
            }
        }
        configurations.createConsumable(UKLIB_JAVA_RUNTIME_ELEMENTS_STUB_NAME) {
            attributes.apply {
                attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(Usage.JAVA_RUNTIME))
                attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
            }
            publishedCompilations.forEach {
                it.compilation.internal.configurations.runtimeDependencyConfiguration?.let {
                    extendsFrom(it)
                }
            }
        }
        project.artifacts.add(UKLIB_JAVA_API_ELEMENTS_STUB_NAME, jar) {
            it.extension = "jar"
        }
        project.artifacts.add(UKLIB_JAVA_RUNTIME_ELEMENTS_STUB_NAME, jar) {
            it.extension = "jar"
        }

        val comp = metadataTarget.compilations.getByName("main")
        variants.addAll(
            listOf(
                DefaultKotlinUsageContext(
                    compilation = comp,
                    dependencyConfigurationName = UKLIB_JAVA_API_ELEMENTS_STUB_NAME,
                    includeIntoProjectStructureMetadata = false,
                ),
                DefaultKotlinUsageContext(
                    compilation = comp,
                    dependencyConfigurationName = UKLIB_JAVA_RUNTIME_ELEMENTS_STUB_NAME,
                    includeIntoProjectStructureMetadata = false,
                ),
            )
        )
    }

    return variants
}

private fun Project.stubJvmJarTask(): TaskProvider<Jar> {
    val stubTaskName = "stubJvmJar"
    return tasks.locateTask<Jar>(stubTaskName) ?: project.tasks.register(stubTaskName, Jar::class.java)
}