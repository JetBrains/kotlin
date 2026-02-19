/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.jvm

import org.gradle.api.Task
import org.gradle.api.plugins.JavaBasePlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.launchInStage
import org.jetbrains.kotlin.gradle.targets.KotlinTestRunFactory
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.testing.internal.kotlinTestRegistry
import org.jetbrains.kotlin.gradle.testing.testTaskName

internal class KotlinJvmTestRunFactory(private val target: KotlinJvmTarget) : KotlinTestRunFactory<KotlinJvmTestRun> {
    override fun create(name: String): KotlinJvmTestRun {
        return KotlinJvmTestRun(name, target).apply {
            val testTaskOrProvider = target.project.registerTask<KotlinJvmTest>(testTaskName) { testTask ->
                testTask.targetName = target.disambiguationClassifier
            }
            target.project.locateTask<Task>(JavaBasePlugin.CHECK_TASK_NAME)?.dependsOn(testTaskOrProvider)

            executionTask = testTaskOrProvider

            val testCompilation = target.compilations.getByName(KotlinCompilation.TEST_COMPILATION_NAME)

            setExecutionSourceFrom(testCompilation)

            target.project.launchInStage(KotlinPluginLifecycle.Stage.AfterEvaluateBuildscript) {
                // use afterEvaluate to override the JavaPlugin defaults for Test tasks
                testTaskOrProvider.configure { testTask ->
                    testTask.description = "Runs the tests of the $name test run."
                    testTask.group = JavaBasePlugin.VERIFICATION_GROUP
                }
            }

            target.project.kotlinTestRegistry.registerTestTask(testTaskOrProvider)
        }
    }
}
