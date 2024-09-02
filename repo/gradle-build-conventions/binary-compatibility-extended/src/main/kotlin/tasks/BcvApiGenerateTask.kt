/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.build.bcv.tasks

import org.jetbrains.kotlin.build.bcv.internal.adding
import org.jetbrains.kotlin.build.bcv.internal.domainObjectContainer
import org.jetbrains.kotlin.build.bcv.targets.BcvTarget
import org.jetbrains.kotlin.build.bcv.workers.BcvSignaturesWorker
import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.submit
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class BcvApiGenerateTask
@Inject
constructor(
    private val workers: WorkerExecutor,
    private val fs: FileSystemOperations,
    private val objects: ObjectFactory,
) : DefaultTask() {

    @get:Nested
    val targets: NamedDomainObjectContainer<BcvTarget> =
        extensions.adding("targets") { objects.domainObjectContainer() }

    @get:Classpath
    abstract val runtimeClasspath: ConfigurableFileCollection

    @get:Input
    abstract val projectName: Property<String>

    @get:OutputDirectory
    abstract val outputApiBuildDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val workQueue = prepareWorkQueue()

        val outputApiBuildDir = outputApiBuildDir.get()
        fs.delete { delete(outputApiBuildDir) }
        outputApiBuildDir.asFile.mkdirs()

        val enabledTargets = targets.asMap.values.filter { it.enabled.getOrElse(true) }

        enabledTargets.forEach { target ->
            val outputDir = if (enabledTargets.size == 1) {
                outputApiBuildDir
            } else {
                outputApiBuildDir.dir(target.platformType)
            }

            workQueue.submit(
                target = target,
                outputDir = outputDir.asFile,
            )
        }

        // The worker queue is asynchronous, so any code here won't wait for the workers to finish.
        // Any follow-up work must be done in another task.
    }

    private fun prepareWorkQueue(): WorkQueue {
        fs.delete { delete(temporaryDir) }
        temporaryDir.mkdirs()

        return workers.classLoaderIsolation {
            classpath.from(runtimeClasspath)
        }
    }

    private fun WorkQueue.submit(
        target: BcvTarget,
        outputDir: File,
    ) {
        val task = this@BcvApiGenerateTask

        submit(BcvSignaturesWorker::class) worker@{
            this@worker.projectName.set(task.projectName)

            this@worker.outputApiDir.set(outputDir)
            this@worker.inputClasses.from(target.inputClasses)
            this@worker.inputJar.set(target.inputJar)

            this@worker.publicMarkers.set(target.publicMarkers)
            this@worker.publicPackages.set(target.publicPackages)
            this@worker.publicClasses.set(target.publicClasses)

            this@worker.ignoredPackages.set(target.ignoredPackages)
            this@worker.ignoredMarkers.set(target.ignoredMarkers)
            this@worker.ignoredClasses.set(target.ignoredClasses)
        }
    }
}
