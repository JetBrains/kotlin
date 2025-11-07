/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.ClasspathNormalizer
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import kotlin.properties.PropertyDelegateProvider


fun Project.generator(
    fqName: String,
    sourceSet: SourceSet? = null,
    configure: JavaExec.() -> Unit = {}
): PropertyDelegateProvider<Any?, TaskProvider<JavaExec>> = PropertyDelegateProvider { _, property ->
    generator(property.name, fqName, sourceSet, configure)
}

fun Project.generator(
    taskName: String,
    fqName: String,
    sourceSet: SourceSet? = null,
    configure: JavaExec.() -> Unit = {}
): TaskProvider<JavaExec> = smartJavaExec(
    name = taskName,
    classpath = (sourceSet ?: testSourceSet).runtimeClasspath,
    mainClass = fqName
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
        inputs.files(jarTask.map { it.outputs.files })
            .withPropertyName("jarTaskOutput")
            .withPathSensitivity(PathSensitivity.RELATIVE)

        inputs.files(classpath)
            .withPropertyName("classpathToExecute")
            .withNormalizer(ClasspathNormalizer::class.java)

        this.mainClass.set(mainClass)
        this.classpath = objects.fileCollection().from(jarTask.map { it.outputs.files })
    }
}
