/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.Directory
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.ide.idea.model.IdeaModel

fun Project.generatedDiagnosticContainersAndCheckerComponents(): TaskProvider<JavaExec> {
    return generatedSourcesTask(
        taskName = "generateCheckersComponents",
        generatorProject = ":compiler:fir:checkers:checkers-component-generator",
        generatorRoot = "compiler/fir/checkers/checkers-component-generator/src/",
        generatorMainClass = "org.jetbrains.kotlin.fir.checkers.generator.MainKt",
        argsProvider = { generationRoot -> listOf(project.name, generationRoot.toString()) },
    )
}

/**
 * This function creates `generateConfigurationKeys`, which generates
 *   compiler configuration keys passed as arguments (like `CommonConfigurationKeys`)
 */
fun Project.generatedConfigurationKeys(containerName: String, vararg containerNames: String): TaskProvider<JavaExec> {
    return generatedSourcesTask(
        taskName = "generateConfigurationKeys",
        generatorProject = ":compiler:config:configuration-keys-generator",
        generatorRoot = "compiler/config/configuration-keys-generator/src/",
        generatorMainClass = "org.jetbrains.kotlin.config.keys.generator.MainKt",
        argsProvider = { generationRoot -> listOf(generationRoot.toString(), containerName, *containerNames) },
        dependOnTaskOutput = false
    )
}

/**
 * This utility function creates [taskName] task, which invokes specified code generator which produces some new
 *   sources for the current module in the directory ./gen
 *
 * @param [taskName] name for the created task
 * @param [generatorProject] module of the code generator
 * @param [generatorRoot] path to the `src` directory of the code generator
 * @param [generatorMainClass] FQN of the generator main class
 * @param [argsProvider] used for specifying the CLI arguments to the generator.
 *   By default, it passes the pass to the generated sources (`./gen`)
 * @param [dependOnTaskOutput] set to false disable the gradle dependency between the generation task and the compilation of the current
 *   module. This is needed for cases when the module with generator depends on the module for which it generates new sources.
 *   Use it with caution
 */
fun Project.generatedSourcesTask(
    taskName: String,
    generatorProject: String,
    generatorRoot: String,
    generatorMainClass: String,
    argsProvider: JavaExec.(generationRoot: Directory) -> List<String> = { listOf(it.toString()) },
    dependOnTaskOutput: Boolean = true
): TaskProvider<JavaExec> {
    val generatorClasspath: Configuration by configurations.creating

    dependencies {
        generatorClasspath(project(generatorProject))
    }

    val generationRoot = layout.projectDirectory.dir("gen")
    val task = tasks.register<JavaExec>(taskName) {
        workingDir = rootDir
        classpath = generatorClasspath
        mainClass.set(generatorMainClass)
        systemProperty("line.separator", "\n")
        args(argsProvider(generationRoot))

        @Suppress("NAME_SHADOWING")
        val generatorRoot = "$rootDir/$generatorRoot"
        val generatorConfigurationFiles = fileTree(generatorRoot) {
            include("**/*.kt")
        }

        inputs.files(generatorConfigurationFiles)
        outputs.dir(generationRoot)
    }

    sourceSets.named("main") {
        val dependency: Any = when (dependOnTaskOutput) {
            true -> task
            false -> generationRoot
        }
        java.srcDirs(dependency)
    }

    if (kotlinBuildProperties.isInIdeaSync) {
        apply(plugin = "idea")
        (this as org.gradle.api.plugins.ExtensionAware).extensions.configure<IdeaModel>("idea") {
            this.module.generatedSourceDirs.add(generationRoot.asFile)
        }
    }
    return task
}

private val Project.sourceSets: SourceSetContainer
    get() = extensions.getByType<JavaPluginExtension>().sourceSets
