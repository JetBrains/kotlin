/*
 * Copyright 2000-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.evaluate.compilation

import com.intellij.debugger.SourcePosition
import org.jetbrains.kotlin.idea.debugger.evaluate.classLoading.ClassToLoad
import org.jetbrains.org.objectweb.asm.Type

data class CompiledDataDescriptor(
    val classes: List<ClassToLoad>,
    val parameters: List<CodeFragmentParameter.Dumb>,
    val crossingBounds: Set<CodeFragmentParameter.Dumb>,
    val mainMethodSignature: MethodSignature,
    val sourcePosition: SourcePosition?
) {
    data class MethodSignature(val parameterTypes: List<Type>, val returnType: Type)
}

val CompiledDataDescriptor.mainClass: ClassToLoad
    get() = classes.first { it.isMainClass }