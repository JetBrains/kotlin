/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.mapping.bytecode

import androidx.compose.compiler.mapping.ClassId
import androidx.compose.compiler.mapping.MethodId

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

        val currentMarker = MethodId(
            classId,
            methodName = "getCurrentMarker",
            methodDescriptor = "()I"
        )

        val endToMarker = MethodId(
            classId,
            methodName = "endToMarker",
            methodDescriptor = "(I)V"
        )
    }

    object ComposableLambda {
        val classId = ClassId("androidx/compose/runtime/internal/ComposableLambdaKt")
        val nClassId = ClassId("androidx/compose/runtime/internal/ComposableLambdaN_jvmKt")

        val composableLambda = MethodId(
            classId,
            methodName = "composableLambda",
            methodDescriptor = "(Landroidx/compose/runtime/Composer;IZLkotlin/Any;)Landroidx/compose/runtime/internal/ComposableLambda;"
        )

        val rememberComposableLambda = MethodId(
            classId,
            methodName = "rememberComposableLambda",
            methodDescriptor = "(IZLjava/lang/Object;Landroidx/compose/runtime/Composer;I)Landroidx/compose/runtime/internal/ComposableLambda;"
        )

        val composableLambdaInstance = MethodId(
            classId,
            methodName = "composableLambdaInstance",
            methodDescriptor = "(IZLjava/lang/Object;)Landroidx/compose/runtime/internal/ComposableLambda;"
        )

        val composableLambdaN = MethodId(
            nClassId,
            methodName = "composableLambdaN",
            methodDescriptor = "(Landroidx/compose/runtime/Composer;IZILjava/lang/Object;)Landroidx/compose/runtime/internal/ComposableLambdaN;",
        )

        val rememberComposableLambdaN = MethodId(
            nClassId,
            methodName = "rememberComposableLambdaN",
            methodDescriptor = "(IZILjava/lang/Object;Landroidx/compose/runtime/Composer;I)Landroidx/compose/runtime/internal/ComposableLambdaN;"
        )

        val composableLambdaNInstance = MethodId(
            nClassId,
            methodName = "composableLambdaNInstance",
            methodDescriptor = "(IZILjava/lang/Object;)Landroidx/compose/runtime/internal/ComposableLambdaN;"
        )
    }

    object FunctionKeyMeta {
        val classId = ClassId("androidx/compose/runtime/internal/FunctionKeyMeta")
    }
}