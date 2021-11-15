/*
* Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
* Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
*/

package org.jetbrains.kotlin.gradle.targets.external

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetComponent
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmExternalCompilationFactory
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmExternalCompilation
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmExternalCompilation.DefaultSourceSetNameOption
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmExternalCompilation.DefaultSourceSetNameOption.KotlinConvention
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import org.jetbrains.kotlin.gradle.utils.filesProvider


data class ExternalKotlinTargetDescriptor(
    val targetName: String,
    val platformType: KotlinPlatformType,
)

fun KotlinMultiplatformExtension.externalTarget(descriptor: ExternalKotlinTargetDescriptor): KotlinExternalTargetHandle {
    val target = KotlinExternalTarget(descriptor, project)
    targets.add(target)
    return KotlinExternalTargetHandle(target)
}

class KotlinExternalTarget(
    private val descriptor: ExternalKotlinTargetDescriptor,
    project: Project
) : AbstractKotlinTarget(project) {
    override val targetName: String = descriptor.targetName

    override val platformType: KotlinPlatformType = descriptor.platformType

    internal val compilationsFactory = KotlinJvmExternalCompilationFactory(project, this)

    override val kotlinComponents: Set<KotlinTargetComponent>
        get() = emptySet()  // TODO NOW

    override val components: Set<SoftwareComponent>
        get() = emptySet()  // TODO NOW

    override val compilations: NamedDomainObjectContainer<KotlinJvmExternalCompilation> =
        project.container(KotlinJvmExternalCompilation::class.java, compilationsFactory)

}

class KotlinExternalTargetHandle(
    val target: KotlinExternalTarget
) {
    fun createCompilation(
        name: String,
        classesOutputDirectory: Provider<Directory>,
        defaultSourceSetNameOption: DefaultSourceSetNameOption = KotlinConvention
    ): KotlinExternalTargetCompilationHandle {
        val compilation = target.compilationsFactory.create(name, defaultSourceSetNameOption)

        val compilationTask = KotlinTasksProvider().registerKotlinJVMTask(
            target.project, compilation.compileKotlinTaskName, compilation
        ) { compileTask ->
            compileTask.classpath = target.project.filesProvider { compilation.compileDependencyFiles }
            compilation.allKotlinSourceSets.forEach { sourceSet -> compileTask.source(sourceSet.kotlin) }
            compileTask.destinationDirectory.set(classesOutputDirectory)
        }

        target.compilations.add(compilation)
        return KotlinExternalTargetCompilationHandle(target, compilation, compilationTask)
    }
}

class KotlinExternalTargetCompilationHandle(
    val target: KotlinExternalTarget,
    val compilation: KotlinJvmExternalCompilation,
    val compilationTask: TaskProvider<out KotlinCompile>
) {
    fun source(sourceSet: KotlinSourceSet) {
        compilation.source(sourceSet)
    }

    fun createSourceSet(name: String): KotlinSourceSet {
        return target.project.multiplatformExtension.sourceSets.create(name).also { sourceSet -> source(sourceSet) }
    }

    fun addCompileDependenciesFiles(files: FileCollection) {
        compilation.compileDependencyFiles = compilation.compileDependencyFiles + files
    }
}


