/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.awaitMetadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.utils.createConsumable


internal suspend fun Project.createUklibPublication(): List<DefaultKotlinUsageContext> {
    val taskName = "archiveUklib"
    val archiveUklib = tasks.register(taskName, ArchiveUklibTask::class.java)

    val kgpFragments = multiplatformExtension.validateKgpModelIsUklibCompliantAndCreateKgpFragments()

    kgpFragments.forEach { fragment ->
        archiveUklib.configure {
            // outputFile might be a directory or a file
            it.inputs.files(fragment.outputFile)
            // FIXME: some outputFiles are derived from a project.provider, use explicit task dependency as a temporary workaround
            it.dependsOn(fragment.providingTask)
        }
    }

    archiveUklib.configure {
        it.fragmentsWithTransitiveRefinees.set(
            kgpFragments.map {
                it.fragment to it.refineesTransitiveClosure
            }.toMap()
        )
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
    val uklibApiElements = "uklibApiElements"
    val uklibRuntimeElements = "uklibRuntimeElements"

    configurations.createConsumable(uklibApiElements) {
        attributes.apply {
            attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_UKLIB_API))
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
    configurations.createConsumable(uklibRuntimeElements) {
        attributes.apply {
            attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_UKLIB_RUNTIME))
            attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
        }
        publishedCompilations.forEach {
            it.compilation.internal.configurations.runtimeDependencyConfiguration?.let {
                extendsFrom(it)
            }
        }
    }

    project.artifacts.add(uklibApiElements, archiveTask) {
        it.extension = Uklib.UKLIB_EXTENSION
    }
    project.artifacts.add(uklibRuntimeElements, archiveTask) {
        it.extension = Uklib.UKLIB_EXTENSION
    }

    val metadataTarget = multiplatformExtension.awaitMetadataTarget()
    val variants = mutableListOf(
        DefaultKotlinUsageContext(
            // Whatever, this compilation doesn't matter
            compilation = metadataTarget.compilations.getByName("commonMain"),
            dependencyConfigurationName = uklibRuntimeElements,
            includeIntoProjectStructureMetadata = false,
        ),
        DefaultKotlinUsageContext(
            // Whatever, this compilation doesn't matter
            compilation = metadataTarget.compilations.getByName("commonMain"),
            dependencyConfigurationName = uklibApiElements,
            includeIntoProjectStructureMetadata = false,
        )
    )

    val jar = locateOrStubJvmJarTask()
    val jvmApiElements = "uklib-jvmApiElements"
    val jvmRuntimeElements = "uklib-jvmRuntimeElements"
    configurations.createConsumable(jvmApiElements) {
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
    configurations.createConsumable(jvmRuntimeElements) {
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
    project.artifacts.add(jvmApiElements, jar) {
        it.extension = "jar"
    }
    project.artifacts.add(jvmRuntimeElements, jar) {
        it.extension = "jar"
    }

    val jvmComp: KotlinCompilation<*> = publishedCompilations.singleOrNull {
        it.compilation is KotlinJvmCompilation
    }?.compilation ?: metadataTarget.compilations.getByName("commonMain")
    variants.addAll(
        listOf(
            DefaultKotlinUsageContext(
                compilation = jvmComp,
                dependencyConfigurationName = jvmApiElements,
                includeIntoProjectStructureMetadata = false,
            ),
            DefaultKotlinUsageContext(
                compilation = jvmComp,
                dependencyConfigurationName = jvmRuntimeElements,
                includeIntoProjectStructureMetadata = false,
            ),
        )
    )

    return variants
}

internal suspend fun Project.locateOrStubJvmJarTask(): TaskProvider<Jar> {
    val jvmTarget = project.multiplatformExtension.awaitTargets().singleOrNull {
        it is KotlinJvmTarget
    }
    return if (jvmTarget != null) {
        @Suppress("UNCHECKED_CAST")
        project.tasks.named(jvmTarget.artifactsTaskName) as TaskProvider<Jar>
    } else {
        val stubTaskName = "stubJvmJar"
        tasks.locateTask<Jar>(stubTaskName) ?: project.tasks.register(stubTaskName, Jar::class.java)
    }
}