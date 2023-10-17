/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.conversion.ResultTrackingContext
import org.jetbrains.kotlin.formver.conversion.StmtConversionContext

interface PropertyAccessEmbedding {
    fun getValue(ctx: StmtConversionContext<ResultTrackingContext>, source: KtSourceElement?): ExpEmbedding
    fun setValue(value: ExpEmbedding, ctx: StmtConversionContext<ResultTrackingContext>, source: KtSourceElement?)
}

fun ExpEmbedding.asPropertyAccess() = when (this) {
    is PropertyAccessEmbedding -> this
    else -> object : PropertyAccessEmbedding {
        override fun getValue(ctx: StmtConversionContext<ResultTrackingContext>, source: KtSourceElement?): ExpEmbedding =
            this@asPropertyAccess

        override fun setValue(value: ExpEmbedding, ctx: StmtConversionContext<ResultTrackingContext>, source: KtSourceElement?) {
            throw IllegalStateException("Property does not have a settable value")
        }
    }
}