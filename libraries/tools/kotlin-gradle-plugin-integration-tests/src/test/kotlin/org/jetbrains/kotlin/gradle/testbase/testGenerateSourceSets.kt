/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.io.Serializable
import java.security.MessageDigest
import java.util.Base64

data class SourceSetIdentifier(
    val name: String,
) : Serializable {
    fun declaration() = "class ${name}"
    fun consumption() = """
        fun consume_${name}() {
            ${name}()
        }
    """.trimIndent()
}

fun KotlinSourceSet.addIdentifierClass(
    identifier: SourceSetIdentifier
) {
    val identifierPath = project.layout.buildDirectory.dir("produce_${identifier.name}")
    kotlin.srcDir(
        project.tasks.create("produceSourceSetIdentifier_${identifier}") { task ->
            task.outputs.dir(identifierPath)
            task.doLast {
                identifierPath.get().asFile.resolve("${identifier.name}.kt").writeText(identifier.declaration())
            }
        }
    )
}

fun KotlinSourceSet.consumeIdentifierClass(
    identifier: SourceSetIdentifier
) {
    val identifierPath = project.layout.buildDirectory.dir("consume_${identifier.name}")
    kotlin.srcDir(
        project.tasks.create("consumeSourceSetIdentifier_${identifier}") { task ->
            task.outputs.dir(identifierPath)
            task.doLast {
                identifierPath.get().asFile.resolve("${identifier.name}.kt").writeText(identifier.consumption())
            }
        }
    )
}

fun KotlinSourceSet.compileSource(
    @Language("kotlin")
    sourceContent: String
) {
    val identifier = name
    val identifierPath = project.layout.buildDirectory.dir("consume_${identifier}")
    kotlin.srcDir(
        project.tasks.create("consumeSourceIn_${identifier}") { task ->
            task.outputs.dir(identifierPath)
            task.doLast {
                identifierPath.get().asFile.resolve("consumeSourceIn_${identifier}.kt").writeText(sourceContent)
            }
        }
    )
}