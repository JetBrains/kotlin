/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle.plugin.konan

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Named
import org.gradle.api.Task
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.tasks.KonanBuildingTask
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import java.util.*
import kotlin.NoSuchElementException

/** Base class for all Kotlin/Native artifacts. */
abstract class KonanBuildingConfig<T : KonanBuildingTask>(
    private val name_: String,
    val type: Class<T>,
    val project: ProjectInternal,
    val targets: Iterable<String>
) : KonanBuildingSpec, Named {

    internal val mainVariant = KonanSoftwareComponent(project)
    override fun getName() = name_

    protected val targetToTask = mutableMapOf<KonanTarget, TaskProvider<T>>()

    fun tasks() = targetToTask.values

    private val aggregateBuildTask: TaskProvider<Task>

    internal var pomActions = mutableListOf<Action<MavenPom>>()

    private val konanTargets: Iterable<KonanTarget>
        get() = project.hostManager.toKonanTargets(targets).distinct()

    init {
        for (targetName in targets.distinct()) {
            val konanTarget = project.hostManager.targetByName(targetName)

            if (!project.hostManager.isEnabled(konanTarget)) {
                project.logger.info("The target is not enabled on the current host: $targetName")
                continue
            }
            if (!targetIsSupported(konanTarget)) {
                project.logger.info("The target $targetName is not supported by the artifact $name")
                continue
            }
            if (this[konanTarget] == null) {
                val task = createTask(konanTarget)
                targetToTask[konanTarget] = task
                // Allow accessing targets just by their names in Groovy DSL.
                (this as? ExtensionAware)?.extensions?.add(konanTarget.visibleName, task)
            }

            if (targetName != konanTarget.visibleName) {
                createTargetAliasTaskIfDeclared(targetName)
            }
        }
        aggregateBuildTask = createAggregateTask()
    }

    protected open fun generateTaskName(target: KonanTarget) =
        "compileKonan${name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}${
            target.visibleName.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(
                    Locale.getDefault()
                ) else it.toString()
            }
        }"

    protected open fun generateAggregateTaskName() =
        "compileKonan${name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}"

    protected open fun generateTargetAliasTaskName(targetName: String) =
        "compileKonan${name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}${
            targetName.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(
                    Locale.getDefault()
                ) else it.toString()
            }
        }"

    protected abstract fun generateTaskDescription(task: T): String
    protected abstract fun generateAggregateTaskDescription(task: Task): String
    protected abstract fun generateTargetAliasTaskDescription(task: Task, targetName: String): String

    protected abstract val defaultBaseDir: File

    protected open fun targetIsSupported(target: KonanTarget): Boolean = true

    data class OutputPlacement(val destinationDir: File, val artifactName: String)

    // There are two options for output placement.
    //      1. Gradle's build directory. We use it by default, e.g. if user runs Gradle from command line.
    //         In this case all produced files has the same name but are placed in different directories
    //         depending on their targets (e.g. linux/foo.kexe and macbook/foo.kexe).
    //      2. Custom path provided by IDE. In this case CONFIGURATION_BUILD_DIR environment variable should
    //         contain a path to a destination directory. All produced files are placed in this directory so IDE
    //         should take care about setting different CONFIGURATION_BUILD_DIR for different targets.
    protected fun determineOutputPlacement(target: KonanTarget): OutputPlacement {
        val configurationBuildDir = project.environmentVariables.configurationBuildDir
        return if (configurationBuildDir != null) {
            OutputPlacement(configurationBuildDir, name)
        } else {
            OutputPlacement(defaultBaseDir.targetSubdir(target), name)
        }
    }

    private fun createTask(target: KonanTarget): TaskProvider<T> =
        project.tasks.register(generateTaskName(target), type) {
            val outputDescription = determineOutputPlacement(target)
            init(this@KonanBuildingConfig, outputDescription.destinationDir, outputDescription.artifactName, target)
            group = BasePlugin.BUILD_GROUP
            description = generateTaskDescription(this)
        }

    private fun createAggregateTask(): TaskProvider<Task> =
        project.tasks.register(generateAggregateTaskName()) {
            group = BasePlugin.BUILD_GROUP
            description = generateAggregateTaskDescription(this)

            targetToTask.filter {
                project.targetIsRequested(it.key)
            }.forEach {
                dependsOn(it.value)
            }
        }.also {
            project.compileAllTask.dependsOn(it)
        }

    protected fun createTargetAliasTaskIfDeclared(targetName: String): TaskProvider<Task>? {
        val canonicalTarget = project.hostManager.targetByName(targetName)

        return this[canonicalTarget]?.let { canonicalBuild ->
            project.tasks.register(generateTargetAliasTaskName(targetName)) {
                group = BasePlugin.BUILD_GROUP
                description = generateTargetAliasTaskDescription(this, targetName)
                dependsOn(canonicalBuild)
            }
        }
    }

    internal operator fun get(target: KonanTarget) = targetToTask[target]

    fun getByTarget(target: String) =
        findByTarget(target) ?: throw NoSuchElementException("No such target for artifact $name: $target")

    fun findByTarget(target: String) = this[project.hostManager.targetByName(target)]

    fun getArtifactByTarget(target: String) = getByTarget(target).get().artifact
    fun findArtifactByTarget(target: String) = findByTarget(target)?.get()?.artifact

    // Common building DSL.

    override fun artifactName(name: String) = tasks().forEach { it.configure { artifactName(name) } }

    fun baseDir(dir: Any) =
        tasks().forEach {
            it.configure {
                destinationDir(
                    project.file(dir).targetSubdir(konanTarget)
                )
            }
        }

    override fun libraries(closure: Closure<Unit>) =
        tasks().forEach { it.configure { libraries(closure) } }

    override fun libraries(action: Action<KonanLibrariesSpec>) =
        tasks().forEach { it.configure { libraries(action) } }

    override fun libraries(configure: KonanLibrariesSpec.() -> Unit) =
        tasks().forEach { it.configure { libraries(configure) } }

    override fun noDefaultLibs(flag: Boolean) =
        tasks().forEach { it.configure { noDefaultLibs(flag) } }

    override fun noEndorsedLibs(flag: Boolean) =
        tasks().forEach { it.configure { noEndorsedLibs(flag) } }

    override fun dumpParameters(flag: Boolean) =
        tasks().forEach { it.configure { dumpParameters(flag) } }

    override fun extraOpts(vararg values: Any) =
        tasks().forEach { it.configure { extraOpts(*values) } }

    override fun extraOpts(values: List<Any>) =
        tasks().forEach { it.configure { extraOpts(values) } }

    fun dependsOn(vararg dependencies: Any?) =
        tasks().forEach { it.configure { dependsOn(*dependencies) } }

    fun target(targetString: String, configureAction: T.() -> Unit) {
        val target = project.hostManager.targetByName(targetString)

        if (!project.hostManager.isEnabled(target)) {
            project.logger.info("Target '$targetString' of artifact '$name' is not supported on the current host")
            return
        }

        val task = this[target]
            ?: throw InvalidUserDataException("Target '$targetString' is not declared. Please add it into project.konanTasks list")
        task.configure(configureAction)
    }

    fun target(targetString: String, configureAction: Action<T>) =
        target(targetString) { configureAction.execute(this) }

    fun target(targetString: String, configureAction: Closure<Unit>) =
        target(targetString) { project.configure(this, configureAction) }

    fun pom(action: Action<MavenPom>) = pomActions + action
}
