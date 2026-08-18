/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import org.junit.jupiter.api.parallel.ResourceLock
import java.nio.file.Path
import kotlin.io.path.*

/**
 * Can be used to mark a test as 'Uses Gradle', using a [ResourceLock] under the hood.
 * Only one test can be executed at a time, which uses Gradle.
 */
@ResourceLock("Gradle")
internal annotation class GradleLock

private val root = Path("")

private val rootAbsolute = root.absolute()

/**
 * Performs the provided [action] on each class compiled by the kotlin project (found in build/classes).
 * Actions will be launched in parallel
 */
internal suspend fun forEachCompiledClass(
    action: suspend (file: Path, ClassNode) -> Unit,
) {
    coroutineScope {
        root.toFile().absoluteFile.walkTopDown()
            .onEnter { it.name != ".git" }
            .map { it.toPath().relativeTo(rootAbsolute) }
            .filter { it.extension == "class" }
            .filter { it.pathString.contains("build/classes") }
            .filterNot { it.pathString.contains("/fakes/") }
            .filterNot { it.pathString.contains("/.") }
            .forEach { file ->
                launch {
                    val classBytes = withContext(Dispatchers.IO) { file.readBytes() }
                    val classNode = ClassNode()
                    ClassReader(classBytes).accept(classNode, ClassReader.SKIP_CODE and ClassReader.SKIP_DEBUG)
                    action(file, classNode)
                }
            }
    }
}
