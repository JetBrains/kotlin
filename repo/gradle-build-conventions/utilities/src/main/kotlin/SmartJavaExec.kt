/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.ClasspathNormalizer
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar

/**
 * @param name is the name of the task to be created.
 * @param classpath determines the runtime classpath of the exec task.
 * @param classpathInput determines the Gradle input of the task. By default, it's a [classpath], but it could be configured.
 * @param mainClass is the FQN of the main class to be executed.
 */
fun Project.smartJavaExec(
    name: String,
    classpath: FileCollection,
    classpathInput: Provider<FileCollection>? = null,
    mainClass: String,
    configure: JavaExec.() -> Unit
): TaskProvider<JavaExec> {
    val javaExecTaskProvider = tasks.register(name, JavaExec::class.java, configure)
    registerJarTaskForJavaExec(
        javaExecTaskProvider,
        classpath,
        classpathInput ?: project.provider { classpath },
        mainClass
    )

    return javaExecTaskProvider
}

// Moves the classpath into a jar metadata, to shorten the command line length and to avoid hitting the limit on Windows
private fun Project.registerJarTaskForJavaExec(
    javaExec: TaskProvider<JavaExec>,
    classpath: FileCollection,
    classpathInput: Provider<FileCollection>,
    mainClass: String,
) {
    val jarTask = project.tasks.register("${javaExec.name}WriteClassPath", Jar::class.java) {
        dependsOn(classpathInput)
        inputs.files(classpathInput).withPropertyName("classpathInput")
        val main = mainClass
        inputs.property("main", main)

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
                        "Main-Class" to main
                    )
                )
            }
        }
    }

    javaExec.configure {
        inputs.files(jarTask.map { it.outputs.files })
            .withPropertyName("jarTaskOutput")
            .withPathSensitivity(PathSensitivity.RELATIVE)

        inputs.files(classpathInput)
            .withPropertyName("classpathToExecute")
            .withNormalizer(ClasspathNormalizer::class.java)

        this.mainClass.set(mainClass)
        this.classpath = objects.fileCollection().from(jarTask.map { it.outputs.files })
    }
}
