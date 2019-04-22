package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.tasks.createOrRegisterTask
import org.jetbrains.kotlin.gradle.testing.internal.configureConventions
import org.jetbrains.kotlin.gradle.testing.internal.registerTestTask
import org.jetbrains.kotlin.utils.addIfNotNull

internal open class KotlinJsCompilationTestsConfigurator(val compilation: KotlinJsCompilation) {
    private val target get() = compilation.target
    private val disambiguationClassifier get() = target.disambiguationClassifier
    private val project get() = target.project
    private val compileTask get() = compilation.compileKotlinTask

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
        // apply plugin (cannot be done at task instantiation time)
        val nodeJs = NodeJsPlugin.apply(target.project).root

        val testJs = project.createOrRegisterTask<KotlinJsTest>(testTaskName) { testJs ->
            testJs.group = LifecycleBasePlugin.VERIFICATION_GROUP

            testJs.dependsOn(compileTask, nodeJs.nodeJsSetupTask)

            testJs.onlyIf {
                compileTask.outputFile.exists()
            }

            testJs.runtimeDependencyHandler = compilation
            testJs.targetName = disambiguationClassifier
            testJs.nodeModulesToLoad.add(compileTask.outputFile.name)

            testJs.configureConventions()
        }

        registerTestTask(testJs)

        project.afterEvaluate {
            testJs.configure {
                if (it.testFramework == null) {
                    configureDefaultTestFramework(it)
                }
            }
        }
    }

    protected open fun configureDefaultTestFramework(it: KotlinJsTest) {
        it.useNodeJs { }
    }
}