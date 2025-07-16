/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.group.analysis

internal object ComposeIds {
    object Composable {
        val classId = ClassId("androidx/compose/runtime/Composable")
    }

    object Composer {
        val classId = ClassId("androidx/compose/runtime/Composer")

        val startRestartGroup = MethodId(
            classId,
            methodName = "startRestartGroup",
            methodDescriptor = "(I)Landroidx/compose/runtime/Composer;"
        )

        val startReplaceGroup = MethodId(
            classId,
            methodName = "startReplaceGroup",
            methodDescriptor = "(I)V"
        )

        val startReplaceableGroup = MethodId(
            classId,
            methodName = "startReplaceableGroup",
            methodDescriptor = "(I)V"
        )

        val endReplaceGroup = MethodId(
            classId,
            methodName = "endReplaceGroup",
            methodDescriptor = "()V"
        )

        val endReplaceableGroup = MethodId(
            classId,
            methodName = "endReplaceableGroup",
            methodDescriptor = "()V"
        )

        val endRestartGroup = MethodId(
            classId,
            methodName = "endRestartGroup",
            methodDescriptor = "()Landroidx/compose/runtime/ScopeUpdateScope;"
        )
    }

    object FunctionKeyMeta {
        val classId = ClassId("androidx/compose/runtime/internal/FunctionKeyMeta")
    }
}
