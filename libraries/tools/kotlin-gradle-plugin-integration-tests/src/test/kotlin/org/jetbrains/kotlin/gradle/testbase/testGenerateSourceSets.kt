/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.internal.extensions.core.extra
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import java.util.UUID

fun KotlinSourceSet.addIdentifierClass() = compileSource("class $name")

fun KotlinSourceSet.compileSource(
    @Language("kotlin")
    sourceContent: String,
) {
    val identifier = "${name}_${project.generateIdentifier()}"
    val identifierPath = project.layout.buildDirectory.dir("generatedSourceDir_${identifier}")
    kotlin.srcDir(
        project.tasks.create("generateSourceIn_${identifier}") { task ->
            task.outputs.dir(identifierPath)
            task.doLast {
                identifierPath.get().asFile.resolve("generatedSource_${identifier}.kt").writeText(sourceContent)
            }
        }
    )
}

fun SourceSet.compileJavaSource(
    project: Project,
    className: String,
    @Language("java")
    sourceContent: String,
) {
    val identifier = "${name}_${project.generateIdentifier()}"
    val identifierPath = project.layout.buildDirectory.dir("generatedJavaSourceDir_${identifier}")
    java.srcDir(
        project.tasks.create("generateJavaSourceIn_${identifier}") { task ->
            task.outputs.dir(identifierPath)
            task.doLast {
                identifierPath.get().asFile.resolve("${className}.java").writeText(sourceContent)
            }
        }
    )
}

private fun Project.generateIdentifier(): String {
    val counter = "counterIdentifier"
    if (!extraProperties.has(counter)) extraProperties.set(counter, 0)
    val count = extraProperties.get(counter) as Int
    extraProperties.set(counter, count + 1)
    return count.toString()
}
