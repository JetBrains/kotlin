/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kaptlite.stubs

import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import java.util.*
import kotlin.collections.HashMap

internal const val JAVA_LANG_OBJECT = "java/lang/Object"

class ClassFileLoader(private val inputs: List<GeneratorInput>) {
    private val cached = HashMap<String, Optional<ClassNode>>()

    fun load(internalName: String, cache: Boolean = false): ClassNode? {
        val cachedNode = cached[internalName]
        if (cachedNode != null) {
            return cachedNode.get()
        }

        val file = inputs.firstNotNullResult { it.find(internalName) } ?: return null
        val classNode = file.readClass()

        if (cache) {
            cached[internalName] = Optional.of(classNode)
        }

        return classNode
    }
}