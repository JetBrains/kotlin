/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.external

import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.namedDomainObjectSet
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import org.jetbrains.kotlin.gradle.utils.filesProvider

class KotlinExternalTargetHandle internal constructor(
    internal val target: KotlinExternalTarget
) {
    val project = target.project

    val platformType = target.platformType

    private val compilations = project.objects.namedDomainObjectSet(KotlinExternalTargetCompilationHandle::class)

    fun getKotlinCompilation(name: String): KotlinExternalTargetCompilationHandle {
        return compilations.getByName(name)
    }

    fun createKotlinCompilation(
        name: String,
        classesOutputDirectory: Provider<Directory>,
        defaultSourceSetNameOption: DefaultSourceSetNameOption = DefaultSourceSetNameOption.KotlinConvention
    ): KotlinExternalTargetCompilationHandle {
        val compilation = target.compilationsFactory.create(name, defaultSourceSetNameOption)
        compilation.output.classesDirs.from(project.filesProvider { classesOutputDirectory.get().asFile })

        val compilationTask = KotlinTasksProvider().registerKotlinJVMTask(
            target.project, compilation.compileKotlinTaskName, compilation
        ) { compileTask ->
            compileTask.classpath = target.project.filesProvider { compilation.compileDependencyFiles }
            compilation.allKotlinSourceSets.forEach { sourceSet -> compileTask.source(sourceSet.kotlin) }
            compileTask.destinationDirectory.set(classesOutputDirectory)
        }
        target.compilations.add(compilation)

        val compilationHandle = KotlinExternalTargetCompilationHandle(this, compilation, compilationTask)
        compilations.add(compilationHandle)

        return compilationHandle
    }
}
