/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvableConfiguration
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

fun Project.generatedDiagnosticContainersAndCheckerComponents(): TaskProvider<JavaExec> {
    return generatedSourcesTask(
        taskName = "generateCheckersComponents",
        generatorProject = ":compiler:fir:checkers:checkers-component-generator",
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
 * @param [generatorMainClass] FQN of the generator main class
 * @param [argsProvider] used for specifying the CLI arguments to the generator.
 *   By default, it passes the pass to the generated sources (`./gen`)
 * @param [dependOnTaskOutput] set to false disable the gradle dependency between the generation task and the compilation of the current
 *   module. This is needed for cases when the module with generator depends on the module for which it generates new sources.
 *   Use it with caution
 * @param [additionalInputsToTrack] add files used by generator as generator task input. On change in these files - generator task will re-run
 * instead of being in 'UP-TO-DATE' state
 */
fun Project.generatedSourcesTask(
    taskName: String,
    generatorProject: String,
    generatorMainClass: String,
    argsProvider: JavaExec.(generationRoot: Directory) -> List<String> = { listOf(it.toString()) },
    dependOnTaskOutput: Boolean = true,
    additionalInputsToTrack: (ConfigurableFileCollection) -> Unit = {},
): TaskProvider<JavaExec> {
    val generatorDependencies = configurations.dependencyScope("${taskName}GeneratorDependencies")
    val generatorClasspath = configurations.resolvable("${taskName}GeneratorClasspath") {
        extendsFrom(generatorDependencies.get())
    }

    dependencies.add(generatorDependencies.name, dependencies.project(generatorProject))

    return generatedSourcesTask(
        taskName,
        generatorClasspath,
        generatorMainClass,
        argsProvider,
        dependOnTaskOutput = dependOnTaskOutput,
        additionalInputsToTrack = additionalInputsToTrack,
    )
}

/**
 * The utility can be used for sources generation by third-party tools.
 * For instance, it's used for Kotlin and KDoc lexer generations by JFlex.
 */
fun Project.generatedSourcesTask(
    taskName: String,
    generatorClasspath: NamedDomainObjectProvider<ResolvableConfiguration>,
    generatorMainClass: String,
    argsProvider: JavaExec.(generationRoot: Directory) -> List<String> = { listOf(it.toString()) },
    dependOnTaskOutput: Boolean = true,
    commonSourceSet: Boolean = false,
    additionalInputsToTrack: (ConfigurableFileCollection) -> Unit = {},
): TaskProvider<JavaExec> {
    val genPath = if (commonSourceSet) {
        "common/src/gen"
    } else {
        "gen"
    }
    val generationRoot = layout.projectDirectory.dir(genPath)
    val task = tasks.register<JavaExec>(taskName) {
        workingDir = rootDir
        classpath(generatorClasspath)
        mainClass.set(generatorMainClass)
        systemProperties["line.separator"] = "\n"
        args(argsProvider(generationRoot))

        val additionalInputFiles = objects.fileCollection()
        inputs.files(additionalInputFiles)
            .ignoreEmptyDirectories()
            .normalizeLineEndings()
            .optional()
            .withPathSensitivity(PathSensitivity.RELATIVE)
            .withPropertyName("additionalInputFiles")
        additionalInputsToTrack(additionalInputFiles)

        outputs.dir(generationRoot)
    }

    val dependency: Any = when (dependOnTaskOutput) {
        true -> task
        false -> generationRoot
    }

    if (!commonSourceSet) {
        val javaSourceSets = extensions.getByType<JavaPluginExtension>().sourceSets
        javaSourceSets.named("main") {
            java.srcDirs(dependency)
        }
    } else {
        val kmpSourceSets = extensions.getByType<KotlinMultiplatformExtension>().sourceSets
        kmpSourceSets.named("commonMain") {
            kotlin.srcDirs(dependency)
        }
    }

    apply(plugin = "idea")
    (this as ExtensionAware).extensions.configure<IdeaModel>("idea") {
        this.module.generatedSourceDirs.add(generationRoot.asFile)
    }
    return task
}

