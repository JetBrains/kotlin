/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaCompilation
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import org.jetbrains.kotlin.gradle.tasks.configuration.AbstractKotlinCompileConfig
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.Future
import org.jetbrains.kotlin.gradle.utils.future
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.util.concurrent.Callable

internal abstract class KotlinSourceSetProcessor<T : AbstractKotlinCompile<*>>(
    val tasksProvider: KotlinTasksProvider,
    val taskDescription: String,
    kotlinCompilation: KotlinCompilationInfo,
) : KotlinCompilationProcessor<T>(kotlinCompilation) {
    protected abstract fun doTargetSpecificProcessing()
    protected val logger = Logging.getLogger(this.javaClass)!!

    protected val sourceSetName: String = kotlinCompilation.compilationName

    override val kotlinTask: TaskProvider<out T> = prepareKotlinCompileTask()

    protected val javaSourceSet: Future<SourceSet?>
        get() = when (val compilation = compilationInfo.safeAs<KotlinCompilationInfo.TCS>()?.origin) {
            is KotlinWithJavaCompilation<*, *> -> project.future { compilation.javaSourceSet }
            is KotlinJvmCompilation -> compilation.javaSourceSet
            else -> project.future { null }
        }

    private fun prepareKotlinCompileTask(): TaskProvider<out T> =
        doRegisterTask(project, compilationInfo.compileKotlinTaskName).also { task ->
            // Workaround for case when 'compilationInfo.classesDirs' is actually Java 'SourceSet.output.classesDir'
            // Gradle from 7.3 started to realize 'FileCollection's registered for 'BuildOutputCleanupRegistry'
            // at the end of the configuration phase. One of this 'FileCollection' is 'SourceSetOutput'
            // which causes 'KotlinCompile' tasks to be eagerly realized.
            // Related commit into Gradle: https://github.com/gradle/gradle/commit/758adc5b0c5a5d27e2797a9fa8fd2690530c5de9
            // Proper workaround is peeked from here:
            // https://github.com/gradle/gradle/blob/16cbc3a678771b08418d086eb67e9934c9282dfb/subprojects/plugins/src/main/java/org/gradle/api/plugins/internal/JvmPluginsHelper.java#L142-L148
            if (compilationInfo.tcsOrNull?.compilation is KotlinWithJavaCompilation<*, *>) {
                val kotlinSourceDirectorySet = compilationInfo.tcs.compilation.defaultSourceSet.kotlin
                kotlinSourceDirectorySet.destinationDirectory.value(defaultKotlinDestinationDir)
                task.configure {
                    if (it is Kotlin2JsCompile) {
                        it.defaultDestinationDirectory.convention(kotlinSourceDirectorySet.destinationDirectory)
                    } else {
                        it.destinationDirectory.convention(kotlinSourceDirectorySet.destinationDirectory)
                    }
                }
                compilationInfo.classesDirs.from(kotlinSourceDirectorySet.destinationDirectory).builtBy(task)
                if (compilationInfo.tcs.compilation.platformType == KotlinPlatformType.js) {
                    @Suppress("UNCHECKED_CAST")
                    kotlinSourceDirectorySet.compiledBy(
                        task as TaskProvider<Kotlin2JsCompile>,
                        Kotlin2JsCompile::defaultDestinationDirectory
                    )
                    (kotlinSourceDirectorySet.classesDirectory as Property<Directory>).set(task.flatMap { it.destinationDirectory })
                } else {
                    kotlinSourceDirectorySet.compiledBy(task, AbstractKotlinCompile<*>::destinationDirectory)
                }
                if (compilationInfo.isMain) {
                    project.tasks.named(
                        compilationInfo.tcs.compilation.target.artifactsTaskName,
                        Jar::class.java
                    ) {
                        it.from(kotlinSourceDirectorySet.classesDirectory)
                    }
                }
            } else {
                compilationInfo.classesDirs.from(
                    task.flatMap { it.destinationDirectory }
                )
            }
        }

    override fun run() {
        doTargetSpecificProcessing()

        if (compilationInfo.tcsOrNull?.compilation is KotlinWithJavaCompilation<*, *>) {
            project.launch { addKotlinDirectoriesToJavaSourceSet() }
            createAdditionalClassesTaskForIdeRunner()
        }
    }

    private suspend fun addKotlinDirectoriesToJavaSourceSet() {
        val java = javaSourceSet.await() ?: return

        // Try to avoid duplicate Java sources in allSource; run lazily to allow changing the directory set:
        val kotlinSrcDirsToAdd = Callable {
            compilationInfo.sources.map { it.sourceDirectories.minus(java.java.sourceDirectories) }
        }

        java.allJava.srcDirs(kotlinSrcDirsToAdd)
        java.allSource.srcDirs(kotlinSrcDirsToAdd)
    }


    private fun createAdditionalClassesTaskForIdeRunner() {
        val kotlinCompilation = compilationInfo.tcsOrNull?.compilation ?: return

        @DisableCachingByDefault(because = "Marker task for IDE sync")
        open class IDEClassesTask : DefaultTask()
        // Workaround: as per KT-26641, when there's a Kotlin compilation with a Java source set, we create another task
        // that has a name composed as '<IDE module name>Classes`, where the IDE module name is the default source set name:
        val expectedClassesTaskName = "${kotlinCompilation.defaultSourceSetName}Classes"
        project.tasks.run {
            val shouldCreateTask = expectedClassesTaskName !in names
            if (shouldCreateTask) {
                project.registerTask(expectedClassesTaskName, IDEClassesTask::class.java) {
                    it.dependsOn(getByName(kotlinCompilation.compileAllTaskName))
                }
            }
        }
    }

    protected fun applyStandardTaskConfiguration(taskConfiguration: AbstractKotlinCompileConfig<*>) {
        taskConfiguration.configureTask {
            it.description = taskDescription
            if (it is Kotlin2JsCompile) {
                it.defaultDestinationDirectory.convention(defaultKotlinDestinationDir)
            } else {
                it.destinationDirectory.convention(defaultKotlinDestinationDir)
            }
            it.libraries.from({ compilationInfo.compileDependencyFiles })
        }
    }

    protected abstract fun doRegisterTask(project: Project, taskName: String): TaskProvider<out T>
}
