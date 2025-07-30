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

    /**
     * The composable lambda can be generally defined as:
     *
     *  <key>
     *  <parameters>
     *  NEW // only for INVOKESPECIAL
     *  DUP // only for INVOKESPECIAL
     *  <captures>
     *  <lambdaInsn> // can be INVOKEDYNAMIC, INVOKESPECIAL or GETSTATIC
     *  <parameters>
     *  <composableLambdaCall>
     *
     * With that, we can define the lambda as
     * - an offset from call to the lambda insn;
     * - an offset from captures end to the key.
     */
    enum class ComposableLambda(
        val lambdaOffset: Int,
        val keyOffset: Int,
        val methodId: MethodId
    ) {
        RememberComposableLambda(
            lambdaOffset = 3,
            keyOffset = 2,
            methodId = Ids.rememberComposableLambda
        ),
        RememberComposableLambdaN(
            lambdaOffset = 3,
            keyOffset = 3,
            methodId = Ids.rememberComposableLambdaN
        ),
        ComposableLambda(
            lambdaOffset = 1,
            keyOffset = 2,
            methodId = Ids.composableLambda
        ),
        ComposableLambdaN(
            lambdaOffset = 1,
            keyOffset = 3,
            methodId = Ids.composableLambdaN
        ),
        ComposableLambdaInstance(
            lambdaOffset = 1,
            keyOffset = 2,
            methodId = Ids.composableLambdaInstance
        ),
        ComposableLambdaNInstance(
            lambdaOffset = 1,
            keyOffset = 3,
            methodId = Ids.composableLambdaNInstance
        );

        private object Ids {
            val classId = ClassId("androidx/compose/runtime/internal/ComposableLambdaKt")
            val nClassId = ClassId("androidx/compose/runtime/internal/ComposableLambdaN_jvmKt")

            val rememberComposableLambda = MethodId(
                classId,
                methodName = "rememberComposableLambda",
                methodDescriptor = "(IZLjava/lang/Object;Landroidx/compose/runtime/Composer;I)Landroidx/compose/runtime/internal/ComposableLambda;"
            )

            val rememberComposableLambdaN = MethodId(
                nClassId,
                methodName = "rememberComposableLambdaN",
                methodDescriptor = "(IZILjava/lang/Object;Landroidx/compose/runtime/Composer;I)Landroidx/compose/runtime/internal/ComposableLambdaN;"
            )

            val composableLambda = MethodId(
                classId,
                methodName = "composableLambda",
                methodDescriptor = "(Landroidx/compose/runtime/Composer;IZLjava/lang/Object;)Landroidx/compose/runtime/internal/ComposableLambda;"
            )

            val composableLambdaN = MethodId(
                nClassId,
                methodName = "composableLambdaN",
                methodDescriptor = "(Landroidx/compose/runtime/Composer;IZILjava/lang/Object;)Landroidx/compose/runtime/internal/ComposableLambdaN;",
            )

            val composableLambdaInstance = MethodId(
                classId,
                methodName = "composableLambdaInstance",
                methodDescriptor = "(IZLjava/lang/Object;)Landroidx/compose/runtime/internal/ComposableLambda;"
            )

            val composableLambdaNInstance = MethodId(
                nClassId,
                methodName = "composableLambdaNInstance",
                methodDescriptor = "(IZILjava/lang/Object;)Landroidx/compose/runtime/internal/ComposableLambdaN;"
            )
        }
    }

    object FunctionKeyMeta {
        val classId = ClassId("androidx/compose/runtime/internal/FunctionKeyMeta")
    }
}