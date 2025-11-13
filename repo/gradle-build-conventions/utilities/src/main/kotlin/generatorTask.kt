/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvableConfiguration
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.ClasspathNormalizer
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import kotlin.properties.PropertyDelegateProvider

fun Project.generator(
    mainClassFqName: String,
    sourceSet: SourceSet,
    configure: JavaExec.() -> Unit = {}
): PropertyDelegateProvider<Any?, TaskProvider<*>> = PropertyDelegateProvider { _, property ->
    val taskName = property.name
    generator(
        taskName,
        mainClassFqName,
        setupConfigurationForGeneratorRuntimeClasspath(taskName, sourceSet.runtimeClasspath),
        generatorInput = null,
        generationRoot = null,
        configure
    )
}

/**
 * Creates `<taskName>GeneratorRuntimeClasspath` with the given set of [dependencies]
 */
fun Project.setupConfigurationForGeneratorRuntimeClasspath(
    taskName: String,
    vararg dependencies: Any,
): NamedDomainObjectProvider<ResolvableConfiguration> {
    return setupConfigurationForGeneratorRuntimeTask(
        taskName,
        suffix = "RuntimeClasspath",
        *dependencies
    )
}

/**
 * Creates `<taskName>GeneratorClasspathInput` with the given set of [dependencies]
 */
fun Project.setupConfigurationForGeneratorClasspathInput(
    taskName: String,
    vararg dependencies: Any,
): NamedDomainObjectProvider<ResolvableConfiguration> {
    return setupConfigurationForGeneratorRuntimeTask(
        taskName,
        suffix = "ClasspathInput",
        *dependencies
    )
}

private fun Project.setupConfigurationForGeneratorRuntimeTask(
    taskName: String,
    suffix: String,
    vararg dependencies: Any,
): NamedDomainObjectProvider<ResolvableConfiguration> {
    val generatorDependencies = configurations.dependencyScope("${taskName}Generator${suffix}Dependencies")
    val generatorClasspath = configurations.resolvable("${taskName}Generator${suffix}") {
        extendsFrom(generatorDependencies.get())
    }
    for (dependency in dependencies) {
        this@setupConfigurationForGeneratorRuntimeTask.dependencies.add(generatorDependencies.name, dependency)
    }

    return generatorClasspath
}

/**
 * @param taskName is the name of the task to be created.
 * @param mainClassFqName is the FQN of the main class to be executed.
 * @param generatorClasspath is a configuration containing all the necessary libraries to run the generator.
 * @param generatorInput allows specifying an input of the generator task in case it differs from the [generatorClasspath].
 *        If null is passed, then [generatorClasspath] is used as the generator input.
 * @param generationRoot is the output directory in which the generator will generate sources
 */
fun Project.generator(
    taskName: String,
    mainClassFqName: String,
    generatorClasspath: NamedDomainObjectProvider<ResolvableConfiguration>,
    generatorInput: NamedDomainObjectProvider<ResolvableConfiguration>?,
    generationRoot: Directory?,
    configure: JavaExec.() -> Unit = {}
): TaskProvider<JavaExec> {
    val javaExec = tasks.register(taskName, JavaExec::class.java)
    val jarWithClasspathTask = createSyntheticClasspathJarTask(generatorClasspath, generatorInput, mainClassFqName, javaExec)

    javaExec.configure {
        group = "Generate"
        workingDir = rootDir
        systemProperty("line.separator", "\n")
        systemProperty("idea.ignore.disabled.plugins", "true")
        if (kotlinBuildProperties.isTeamcityBuild) {
            systemProperty("teamcity", "true")
        }
        mainClass.set(mainClassFqName)
        classpath = objects.fileCollection().from(jarWithClasspathTask.map { it.outputs.files })

        inputs.files(jarWithClasspathTask)
            .withPropertyName("jarTaskOutput")
            .withNormalizer(ClasspathNormalizer::class.java)

        inputs.files(generatorInput ?: generatorClasspath)
            .withPropertyName("classpathToExecute")
            .withNormalizer(ClasspathNormalizer::class.java)

        generationRoot?.let { outputs.dir(it) }

        configure()
    }
    return javaExec
}

/**
 * Moves the classpath into a jar metadata, to shorten the command line length and to avoid hitting the limit on Windows.
 * This hack could be removed when https://github.com/gradle/gradle/issues/1989 will be fixed.
 */
private fun Project.createSyntheticClasspathJarTask(
    classpath: Provider<out FileCollection>,
    classpathInput: Provider<out FileCollection>?,
    mainClass: String,
    javaExec: TaskProvider<JavaExec>,
): TaskProvider<Jar> {
    return project.tasks.register("${javaExec.name}WriteClassPath", Jar::class.java) {
        inputs.files(classpathInput ?: classpath)
            .withPropertyName("classpathInput")
            .withNormalizer(ClasspathNormalizer::class.java)
        inputs.property("mainClassName", mainClass)

        archiveFileName.set(javaExec.flatMap { task -> task.mainClass.map { "$it.${task.name}.classpath.container.jar" } })
        destinationDirectory.set(temporaryDir)

        doFirst {
            val classPathString = classpath.get().joinToString(" ") {
                it.toURI().toString()
            }
            manifest {
                attributes(
                    mapOf(
                        "Class-Path" to classPathString,
                        "Main-Class" to mainClass
                    )
                )
            }
        }
    }
}
