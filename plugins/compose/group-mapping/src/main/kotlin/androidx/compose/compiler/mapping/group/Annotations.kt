/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.mapping.group

import androidx.compose.compiler.mapping.bytecode.ComposeIds
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.MethodNode

private val MethodNode.allAnnotations: Sequence<AnnotationNode>
    get() = sequence {
        yieldAll(visibleAnnotations.orEmpty())
        yieldAll(invisibleAnnotations.orEmpty())
    }

internal fun MethodNode.readFunctionKeyMetaAnnotation(): Int? {
    val functionKeyAnnotation = allAnnotations.find { annotationNode ->
        annotationNode.desc == ComposeIds.FunctionKeyMeta.classId.descriptor
    } ?: return null

    val keyIndex = functionKeyAnnotation.values.indexOf("key")
    if (keyIndex == -1) {
        return null
    }

    val functionKey = functionKeyAnnotation.values[keyIndex + 1] as? Int ?: return null
    return functionKey
}

internal fun MethodNode.hasComposableAnnotation(): Boolean =
    allAnnotations.any {
        it.desc == ComposeIds.Composable.classId.descriptor
    }

internal fun MethodNode.hasFunctionKeyMetaAnnotation(): Boolean =
    allAnnotations.any {
        it.desc == ComposeIds.FunctionKeyMeta.classId.descriptor
    }