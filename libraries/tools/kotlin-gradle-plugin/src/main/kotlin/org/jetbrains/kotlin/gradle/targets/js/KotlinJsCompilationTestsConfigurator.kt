package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.api.Project
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.testing.base.plugins.TestingBasePlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationToRunnableFiles
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaTarget
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsSetupTask
import org.jetbrains.kotlin.gradle.targets.js.tasks.KotlinJsNodeModulesTask
import org.jetbrains.kotlin.gradle.targets.js.tasks.KotlinNodeJsTestRuntimeToNodeModulesTask
import org.jetbrains.kotlin.gradle.targets.js.tasks.KotlinNodeJsTestTask
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.registerTask
import java.io.File

internal class KotlinJsCompilationTestsConfigurator(
        val compilation: KotlinCompilationToRunnableFiles<*>
) {
    private val target get() = compilation.target
    private val project get() = target.project
    private val compileTestKotlin2Js get() = compilation.compileKotlinTask as Kotlin2JsCompile
    private val isSinglePlatformProject get() = target is KotlinWithJavaTarget<*>
    private val testTaskName: String
        get() = if (isSinglePlatformProject) "testJs" else camelCaseTargetName("test")

    private fun camelCaseTargetName(prefix: String): String {
        return if (isSinglePlatformProject) prefix
        else target.name + prefix.capitalize()
    }

    @Suppress("SameParameterValue")
    private fun underscoredCompilationName(prefix: String): String {
        return if (isSinglePlatformProject) prefix
        else "${target.name}_${compilation.name}_$prefix"
    }

    private val nodeModulesDir
        get() = project.buildDir.resolve(underscoredCompilationName("node_modules"))

    @Suppress("UnstableApiUsage")
    private val Project.testResults: File
        get() = project.buildDir.resolve(TestingBasePlugin.TEST_RESULTS_DIR_NAME)

    @Suppress("UnstableApiUsage")
    private val Project.testReports: File
        get() = project.buildDir.resolve(TestingBasePlugin.TESTS_DIR_NAME)

    private val compileTask: Kotlin2JsCompile
        get() = project.tasks.findByName(compileTestKotlin2Js.name) as Kotlin2JsCompile

    private val Kotlin2JsCompile.jsRuntimeClasspath: FileCollection
        get() = classpath.plus(project.files(destinationDir))

    fun configure() {
        val nodeModulesTask = registerTask(
                project,
                camelCaseTargetName("kotlinJsNodeModules"),
                KotlinJsNodeModulesTask::class.java
        ) {
            it.dependsOn(compileTestKotlin2Js)

            it.nodeModulesDir = nodeModulesDir
            it.classpath = compileTask.jsRuntimeClasspath
        }

        val nodeModulesTestRuntimeTask = registerTask(
                project,
                camelCaseTargetName("kotlinJsNodeModulesTestRuntime"),
                KotlinNodeJsTestRuntimeToNodeModulesTask::class.java
        ) {
            it.nodeModulesDir = nodeModulesDir
        }

        val projectWithNodeJsPlugin = NodeJsPlugin.ensureAppliedInHierarchy(target.project)

        val testTask = registerTask(project, testTaskName, KotlinNodeJsTestTask::class.java) { testJs ->
            testJs.group = "verification"

            testJs.dependsOn(
                    nodeModulesTask.getTaskOrProvider(),
                    nodeModulesTestRuntimeTask.getTaskOrProvider()
            )

            if (!isSinglePlatformProject) {
                testJs.targetName = target.name
            }

            testJs.nodeJsProcessOptions.workingDir = project.projectDir

            testJs.nodeModulesDir = nodeModulesDir
            testJs.nodeModulesToLoad = setOf(compileTestKotlin2Js.outputFile.name)

            val htmlReport = DslObject(testJs.reports.html)
            val xmlReport = DslObject(testJs.reports.junitXml)

            xmlReport.conventionMapping.map("destination") { project.testResults.resolve(testJs.name) }
            htmlReport.conventionMapping.map("destination") { project.testReports.resolve(testJs.name) }
            testJs.conventionMapping.map("binResultsDir") { project.testResults.resolve(testJs.name + "/binary") }
        }

        project.afterEvaluate {
            project.tasks.findByName(JavaBasePlugin.CHECK_TASK_NAME)?.dependsOn(testTask.getTaskOrProvider())

            // defer nodeJs executable setup, as nodejs project settings may change during configuration
            testTask.configure {
                val nodeJsSetupTask = projectWithNodeJsPlugin.tasks.findByName(NodeJsSetupTask.NAME)
                it.dependsOn(nodeJsSetupTask)

                if (it.nodeJsProcessOptions.executable == null) {
                    it.nodeJsProcessOptions.executable = NodeJsExtension[projectWithNodeJsPlugin].buildEnv().nodeExec
                }
            }
        }
    }
}