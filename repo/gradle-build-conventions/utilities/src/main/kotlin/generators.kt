/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
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
    configure()
}

