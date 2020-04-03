/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.jvm

import org.gradle.api.plugins.JavaBasePlugin
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.Kotlin2JvmSourceSetProcessor
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetProcessor
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.testing.internal.kotlinTestRegistry
import org.jetbrains.kotlin.gradle.testing.testTaskName

class KotlinJvmTargetConfigurator(kotlinPluginVersion: String) :
    KotlinOnlyTargetConfigurator<KotlinJvmCompilation, KotlinJvmTarget>(true, true, kotlinPluginVersion),
    KotlinTargetWithTestsConfigurator<KotlinJvmTestRun, KotlinJvmTarget> {

    override fun configurePlatformSpecificModel(target: KotlinJvmTarget) {
        super<KotlinOnlyTargetConfigurator>.configurePlatformSpecificModel(target)
        super<KotlinTargetWithTestsConfigurator>.configurePlatformSpecificModel(target)

        // Create the configuration under the name 'compileClasspath', as Android lint tasks want it, KT-27170
        target.project.whenEvaluated {
            if (configurations.findByName("compileClasspath") == null) {
                configurations.create("compileClasspath").apply {
                    isCanBeResolved = false
                    isCanBeConsumed = false
                    extendsFrom(
                        target.project.configurations.getByName(
                            target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME).compileDependencyConfigurationName
                        )
                    )
                }
            }
        }
    }

    override val testRunClass: Class<KotlinJvmTestRun>
        get() = KotlinJvmTestRun::class.java

    override fun createTestRun(
        name: String,
        target: KotlinJvmTarget
    ): KotlinJvmTestRun = KotlinJvmTestRun(name, target).apply {
        val testTaskOrProvider = target.project.registerTask<KotlinJvmTest>(testTaskName) { testTask ->
            testTask.targetName = target.disambiguationClassifier
            testTask.project.tasks.findByName(JavaBasePlugin.CHECK_TASK_NAME)?.dependsOn(testTask)
        }

        executionTask = testTaskOrProvider

        val testCompilation = target.compilations.getByName(KotlinCompilation.TEST_COMPILATION_NAME)

        setExecutionSourceFrom(testCompilation)

        target.project.whenEvaluated {
            // use afterEvaluate to override the JavaPlugin defaults for Test tasks
            testTaskOrProvider.configure { testTask ->
                testTask.description = "Runs the tests of the $name test run."
                testTask.group = JavaBasePlugin.VERIFICATION_GROUP
            }
        }

        target.project.kotlinTestRegistry.registerTestTask(testTaskOrProvider)
    }

    override fun buildCompilationProcessor(compilation: KotlinJvmCompilation): KotlinSourceSetProcessor<*> {
        val tasksProvider = KotlinTasksProvider(compilation.target.targetName)
        return Kotlin2JvmSourceSetProcessor(tasksProvider, compilation, kotlinPluginVersion)
    }
}