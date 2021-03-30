/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cir

interface CirExtensionReceiver : CirHasAnnotations {
    val type: CirType

    companion object {
        @Suppress("NOTHING_TO_INLINE")
        inline fun create(
            annotations: List<CirAnnotation>,
            type: CirType
        ): CirExtensionReceiver = CirExtensionReceiverImpl(
            annotations = annotations,
            type = type
        )
    }
}

data class CirExtensionReceiverImpl(
    override val annotations: List<CirAnnotation>,
    override val type: CirType
) : CirExtensionReceiver
