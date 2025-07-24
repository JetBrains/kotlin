import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar

/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun Project.smartJavaExec(
    name: String,
    classpath: FileCollection,
    mainClass: String,
    configure: JavaExec.() -> Unit
): TaskProvider<JavaExec> {
    val javaExecTaskProvider = tasks.register(name, JavaExec::class.java, configure)
    registerJarTaskForJavaExec(javaExecTaskProvider, classpath, mainClass)

    return javaExecTaskProvider
}

// Moves the classpath into a jar metadata, to shorten the command line length and to avoid hitting the limit on Windows
private fun Project.registerJarTaskForJavaExec(
    javaExec: TaskProvider<JavaExec>,
    classpath: FileCollection,
    mainClass: String,
) {
    val jarTask = project.tasks.register("${javaExec.name}WriteClassPath", Jar::class.java) {
        val classpathToConvert = classpath
        dependsOn(classpathToConvert)
        inputs.files(classpathToConvert)
        val main = mainClass
        inputs.property("main", main)

        archiveFileName.set(javaExec.flatMap { task -> task.mainClass.map { "$it.${task.name}.classpath.container.jar" } })
        destinationDirectory.set(temporaryDir)

        doFirst {
            val classPathString = classpathToConvert.joinToString(" ") {
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
        dependsOn(jarTask)
        this.mainClass.set(mainClass)
        this.classpath = objects.fileCollection().from(
            {
                jarTask.get().outputs.files
            }
        )
    }
}
