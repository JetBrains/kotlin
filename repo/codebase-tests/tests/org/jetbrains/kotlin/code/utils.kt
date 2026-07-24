/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import java.nio.file.Path
import kotlin.io.path.*

private val root = Path("")

private val rootAbsolute = root.absolute()

internal fun forEachCompiledClass(
    action: (file: Path, ClassNode) -> Unit,
) {
    root.toFile().absoluteFile.walkTopDown()
        .onEnter { it.name != ".git" }
        .map { it.toPath().relativeTo(rootAbsolute) }
        .filter { it.extension == "class" }
        .filter { it.pathString.contains("build/classes") }
        .filterNot { it.pathString.contains("/fakes/") }
        .filterNot { it.pathString.contains("/.") }
        .forEach { file ->
            val classNode = ClassNode()
            ClassReader(file.readBytes()).accept(classNode, ClassReader.SKIP_CODE and ClassReader.SKIP_DEBUG)
            action(file, classNode)
        }
}
