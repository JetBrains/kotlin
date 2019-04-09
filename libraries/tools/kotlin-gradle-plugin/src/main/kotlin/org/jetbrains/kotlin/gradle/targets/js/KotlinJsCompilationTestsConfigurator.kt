package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationToRunnableFiles
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.testing.internal.configureConventions
import org.jetbrains.kotlin.gradle.testing.internal.registerTestTask
import org.jetbrains.kotlin.utils.addIfNotNull

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

    private val testTaskName: String
        get() = disambiguateCamelCased("test", false)

    fun configure() {
        // apply plugin (cannot do it lazy)
        val nodeJs = NodeJsPlugin.apply(target.project)

        registerTask(project, testTaskName, KotlinJsTest::class.java) { testJs ->
            testJs.group = LifecycleBasePlugin.VERIFICATION_GROUP

            testJs.dependsOn(compileTestKotlin2Js, nodeJs.nodeJsSetupTask)

            testJs.onlyIf {
                compileTestKotlin2Js.outputFile.exists()
            }

            testJs.runtimeDependencyHandler = compilation
            testJs.targetName = disambiguationClassifier
            testJs.nodeModulesToLoad.add(compileTestKotlin2Js.outputFile.name)

            testJs.configureConventions()
            registerTestTask(testJs)
        }
    }
}