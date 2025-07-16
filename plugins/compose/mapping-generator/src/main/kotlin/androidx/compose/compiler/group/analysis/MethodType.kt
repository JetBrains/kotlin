/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.group.analysis

import org.objectweb.asm.tree.MethodNode

internal fun MethodNode.methodType(): MethodType = when {
    hasComposableAnnotation() -> MethodType.Composable

    // todo: leftover from hot reload, recheck if this is actually needed
    desc.contains("${ComposeIds.Composer.classId.descriptor}I") -> {
        MethodType.Composable
    }

    else -> MethodType.Regular
}

internal enum class MethodType {
    Regular,
    Composable
}