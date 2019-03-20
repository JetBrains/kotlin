package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.testing.base.plugins.TestingBasePlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationToRunnableFiles
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsSetupTask
import org.jetbrains.kotlin.gradle.targets.js.tasks.*
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.utils.addIfNotNull
import java.io.File

internal class KotlinJsCompilationTestsConfigurator(
    val compilation: KotlinCompilationToRunnableFiles<*>
) {
    private val target get() = compilation.target
    private val disambiguationClassifier get() = target.disambiguationClassifier
    private val project get() = target.project
    private val compileTestKotlin2Js get() = compilation.compileKotlinTask as Kotlin2JsCompile

    private fun disambiguate(name: String, includeCompilation: Boolean = false): MutableList<String> {
        val components = mutableListOf<String>()

        components.addIfNotNull(disambiguationClassifier)
        if (includeCompilation) components.add(compilation.name)
        components.add(name)
        return components
    }

    private fun disambiguateCamelCased(name: String, includeCompilation: Boolean): String {
        val components = disambiguate(name, includeCompilation)

        return components.first() + components.drop(1).joinToString("") { it.capitalize() }
    }

    @Suppress("SameParameterValue")
    private fun disambiguateUnderscored(name: String, includeCompilation: Boolean) =
        disambiguate(name, includeCompilation).joinToString("_")

    private val testTaskName: String
        get() = disambiguateCamelCased("test", false)

    private val nodeModulesDir
        get() = project.buildDir.resolve(disambiguateUnderscored("node_modules", true))

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
            disambiguateCamelCased("nodeModules", true),
            KotlinJsNodeModulesTask::class.java
        ) {
            it.dependsOn(compileTestKotlin2Js)

            it.onlyIf {
                compileTestKotlin2Js.outputFile.exists()
            }

            it.nodeModulesDir = nodeModulesDir
            it.classpath = compileTask.jsRuntimeClasspath
        }

        val projectWithNodeJsPlugin = NodeJsPlugin.ensureAppliedInHierarchy(target.project)
        val kotlinJsExtractTestRunnerTask = project.kotlinNodeJsTestRuntimeExtractTask

        val testTask = registerTask(project, testTaskName, KotlinNodeJsTestTask::class.java) { testJs ->
            testJs.group = "verification"

            testJs.dependsOn(
                nodeModulesTask.getTaskOrProvider(),
                kotlinJsExtractTestRunnerTask.getTaskOrProvider()
            )

            testJs.onlyIf {
                // run only if there is org.jetbrains.kotlin:kotlin-test-js in classpath
                // (kotlin-test-nodejs-runner.js requires kotlin-test.js)
                nodeModulesDir.resolve("kotlin-test.js").exists()
                compileTestKotlin2Js.outputFile.exists()
            }

            if (disambiguationClassifier != null) {
                testJs.targetName = disambiguationClassifier
            }

            testJs.nodeJsProcessOptions.workingDir = project.projectDir

            testJs.nodeModulesDir = nodeModulesDir
            testJs.testRuntimeNodeModule = project.extractedKotlinTestNodeJsRunner
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