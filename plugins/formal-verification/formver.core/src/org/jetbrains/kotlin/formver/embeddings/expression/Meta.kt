/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.linearization.LinearizationContext

data class WithPosition(override val inner: ExpEmbedding, val source: KtSourceElement) : PassthroughExpEmbedding {
    override fun <R> withPassthroughHook(ctx: LinearizationContext, action: LinearizationContext.() -> R): R =
        ctx.withPosition(source, action)

    override fun ignoringMetaNodes(): ExpEmbedding = inner.ignoringMetaNodes()
    override fun ignoringCastsAndMetaNodes(): ExpEmbedding = inner.ignoringCastsAndMetaNodes()
}

