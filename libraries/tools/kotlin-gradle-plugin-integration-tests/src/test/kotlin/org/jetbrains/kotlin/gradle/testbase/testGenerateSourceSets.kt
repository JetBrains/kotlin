/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.util.UUID

fun KotlinSourceSet.addIdentifierClass() = compileSource("class $name")

fun KotlinSourceSet.compileSource(
    @Language("kotlin")
    sourceContent: String
) {
    val identifier = "${name}_${UUID.randomUUID().toString().replace("-", "_")}"
    val identifierPath = project.layout.buildDirectory.dir("compileSourceIn_${identifier}")
    kotlin.srcDir(
        project.tasks.create("compileSourceIn_${identifier}") { task ->
            task.outputs.dir(identifierPath)
            task.doLast {
                identifierPath.get().asFile.resolve("compileSourceIn_${identifier}.kt").writeText(sourceContent)
            }
        }
    )
}