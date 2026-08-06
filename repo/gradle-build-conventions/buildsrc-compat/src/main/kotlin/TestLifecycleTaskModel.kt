/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.kotlin.dsl.withType
import org.gradle.tooling.provider.model.ToolingModelBuilder
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.testFederation.DelicateTestFederationApi
import org.jetbrains.kotlin.testFederation.testFederationDomains
import org.jetbrains.kotlin.testFederation.toArgumentString
import org.jetbrains.kotlin.tooling.core.withClosure
import java.io.Serializable

interface TestLifecycleTasksModel : Serializable {
    interface TestTask : Serializable {
        /**
         * The identityPath of the task
         */
        val path: String
    }

    interface TestLifecycleTask : Serializable {
        /**
         * The identityPath of the task
         */
        val path: String

        /**
         * All *test* tasks which are resolved (transitively) as dependencies of this lifecycle task.
         * The test tasks are stored using their identityPath
         */
        val allDependencies: Set<String>
    }

    /**
     * The [org.jetbrains.kotlin.testFederation.toArgumentString] representation of the set of domains
     * this project belongs to
     */
    val domains: String

    /**
     * The project's identityPath
     */
    val projectPath: String

    /**
     * All regular test tasks within this project
     */
    val testTasks: List<TestTask>

    /**
     * All testLifecycleTasks within this project (expected only in the root project)
     */
    val testLifecycleTasks: List<TestLifecycleTask>
}

internal data class TestLifecycleTaskModelImpl(
    override val domains: String,
    override val projectPath: String,
    override val testTasks: List<TestLifecycleTasksModel.TestTask>,
    override val testLifecycleTasks: List<TestLifecycleTasksModel.TestLifecycleTask>,
) : TestLifecycleTasksModel

internal data class TestLifecycleTaskImpl(
    override val path: String,
    override val allDependencies: Set<String>,
) : TestLifecycleTasksModel.TestLifecycleTask

internal data class TestTaskImpl(
    override val path: String,
) : TestLifecycleTasksModel.TestTask

internal fun Project.configureTestLifecycleTasksModelBuilder() {
    serviceOf<ToolingModelBuilderRegistry>().register(TestLifecycleTasksModelBuilder())
}

class TestLifecycleTasksModelBuilder : ToolingModelBuilder {
    override fun canBuild(modelName: String): Boolean {
        return modelName == TestLifecycleTasksModel::class.java.name
    }

    override fun buildAll(modelName: String, project: Project): Any {
        val testTasks = project.tasks.withType<AbstractTestTask>().toList()
            .filter { testTask -> testTask.isRelevant() }
            .map { testTask -> TestTaskImpl(testTask.identityPath.asString()) }

        val testLifecycleTasks = project.tasks.withType<TestLifecycleTask>().toList()
            .map { testLifecycleTask -> buildLifecycleTask(testLifecycleTask) }

        @OptIn(DelicateTestFederationApi::class)
        return TestLifecycleTaskModelImpl(
            domains = project.testFederationDomains.get().toArgumentString(),
            projectPath = project.buildTreePath,
            testTasks = testTasks,
            testLifecycleTasks = testLifecycleTasks,
        )
    }

    private fun buildLifecycleTask(task: DefaultTask): TestLifecycleTaskImpl {
        val build = task.project.gradle
        return TestLifecycleTaskImpl(
            path = task.identityPath.asString(),
            allDependencies = task.withClosure<Task> { current ->
                if (current.project.gradle == build) {
                    current.taskDependencies.getDependencies(current).toList()
                } else emptyList()
            }.filterIsInstance<AbstractTestTask>()
                .filter { it.isRelevant() }
                .map { it.identityPath.asString() }.toSet()
        )
    }

    @Suppress("RedundantIf")
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    private fun AbstractTestTask.isRelevant(): Boolean {
        /*
        Tests might be created _by default_ without any test sources being present.
        Such tests can be ignored until a single test source is added.

        Tests, manually created, are always considered to be relevant.
        */
        val kotlinSourceSet = project.extensions.findByType<KotlinProjectExtension>()?.sourceSets?.findByName(name)
        if (kotlinSourceSet != null) {
            return kotlinSourceSet.kotlin.srcDirs.any { it.exists() && it.listFiles().orEmpty().isNotEmpty() } ||
                    kotlinSourceSet.generatedKotlin.any()
        }

        val javaSourceSet = project.sourceSets.findByName(name)
        if (javaSourceSet != null) {
            return javaSourceSet.java.srcDirs.any { it.exists() && it.listFiles().orEmpty().isNotEmpty() }
        }

        return true
    }
}
