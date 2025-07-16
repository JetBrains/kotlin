/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.group.analysis

import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.MethodNode

private val MethodNode.allAnnotations: List<AnnotationNode>
    get() = buildList {
        addAll(visibleAnnotations.orEmpty())
        addAll(invisibleAnnotations.orEmpty())
    }

internal fun MethodNode.readFunctionKeyMetaAnnotation(): ComposeGroupKey? {
    val functionKeyAnnotation = allAnnotations.find { annotationNode ->
        annotationNode.desc == ComposeIds.FunctionKeyMeta.classId.descriptor
    } ?: return null

    val keyIndex = functionKeyAnnotation.values.indexOf("key")
    if (keyIndex == -1) {
        return null
    }

    val functionKey = functionKeyAnnotation.values[keyIndex + 1] as? Int ?: return null
    return ComposeGroupKey(functionKey)
}

internal fun MethodNode.hasComposableAnnotation(): Boolean {
    return allAnnotations.any {
        ClassId.fromDesc(it.desc) in COMPOSE_FUNCTION_ANNOTATIONS
    }
}

private val COMPOSE_FUNCTION_ANNOTATIONS = setOf(
    ComposeIds.Composable.classId,
    ComposeIds.FunctionKeyMeta.classId,
)