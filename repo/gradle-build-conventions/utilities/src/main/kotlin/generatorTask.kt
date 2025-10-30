/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.tasks.*
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.get
import kotlin.properties.PropertyDelegateProvider
import GeneratorInputKind.RuntimeClasspath
import GeneratorInputKind.SourceSetJar

enum class GeneratorInputKind {
    RuntimeClasspath,
    SourceSetJar,
}

fun Project.generator(
    fqName: String,
    sourceSet: SourceSet,
    inputKind: GeneratorInputKind = SourceSetJar,
    configure: JavaExec.() -> Unit = {}
): PropertyDelegateProvider<Any?, TaskProvider<JavaExec>> = PropertyDelegateProvider { _, property ->
    generator(property.name, fqName, sourceSet, inputKind, configure)
}

fun Project.generator(
    taskName: String,
    fqName: String,
    sourceSet: SourceSet,
    inputKind: GeneratorInputKind,
    configure: JavaExec.() -> Unit = {}
): TaskProvider<JavaExec> = smartJavaExec(
    name = taskName,
    sourceSet = sourceSet,
    mainClass = fqName,
    inputKind,
) {
    group = "Generate"
    workingDir = rootDir
    systemProperty("line.separator", "\n")
    systemProperty("idea.ignore.disabled.plugins", "true")
    if (kotlinBuildProperties.isTeamcityBuild) {
        systemProperty("teamcity", "true")
    }
    configure()
}

/**
 * @param name is the name of the task to be created.
 * @param sourceSet should be the source set containing the main class.
 * @param inputKind determines how the input of the input of the exec task would be coiffured:
 *   - if the [RuntimeClasspath], then the whole runtime classpath of the [sourceSet] would be considered as input
 *   - if the [SourceSetJar], then only output of the jar task of the [sourceSet] will be an input
 *   It's recommended to use [SourceSetJar], as it provides more narrow scope.
 * @param mainClass is the FQN of the main class to be executed.
 */
fun Project.smartJavaExec(
    name: String,
    sourceSet: SourceSet,
    mainClass: String,
    inputKind: GeneratorInputKind,
    configure: JavaExec.() -> Unit
): TaskProvider<JavaExec> {
    val javaExecTaskProvider = tasks.register(name, JavaExec::class.java, configure)
    registerJarTaskForJavaExec(
        javaExecTaskProvider,
        sourceSet,
        mainClass,
        inputKind,
    )

    return javaExecTaskProvider
}

// Moves the classpath into a jar metadata, to shorten the command line length and to avoid hitting the limit on Windows
private fun Project.registerJarTaskForJavaExec(
    javaExec: TaskProvider<JavaExec>,
    sourceSet: SourceSet,
    mainClass: String,
    inputKind: GeneratorInputKind,
) {
    val classpath = sourceSet.runtimeClasspath
    // We write the whole classpath into separate jar to work around CLI length limit on Windows
    val jarWithClasspathTask = project.tasks.register("${javaExec.name}WriteClassPath", Jar::class.java) {
        inputs.files(classpath)
            .withPropertyName("classpathInput")
            .withNormalizer(ClasspathNormalizer::class.java)
        inputs.property("mainClassName", mainClass)

        archiveFileName.set(javaExec.flatMap { task -> task.mainClass.map { "$it.${task.name}.classpath.container.jar" } })
        destinationDirectory.set(temporaryDir)

        doFirst {
            val classPathString = classpath.joinToString(" ") {
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

    val classpathInput = provider {
        when (inputKind) {
            RuntimeClasspath -> classpath
            SourceSetJar -> tasks[sourceSet.jarTaskName].outputs.files
        }
    }

    javaExec.configure {
        inputs.files(jarWithClasspathTask)
            .withPropertyName("jarTaskOutput")
            .withNormalizer(ClasspathNormalizer::class.java)

        inputs.files(classpathInput)
            .withPropertyName("classpathToExecute")
            .withNormalizer(ClasspathNormalizer::class.java)

        this.mainClass.set(mainClass)
        this.classpath = objects.fileCollection().from(jarWithClasspathTask.map { it.outputs.files })
    }
}
