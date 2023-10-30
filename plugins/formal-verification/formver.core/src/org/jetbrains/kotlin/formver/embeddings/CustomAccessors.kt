/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.conversion.ResultTrackingContext
import org.jetbrains.kotlin.formver.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.embeddings.callables.CallableEmbedding
import org.jetbrains.kotlin.formver.embeddings.callables.FunctionEmbedding
import org.jetbrains.kotlin.formver.embeddings.callables.insertCall
import org.jetbrains.kotlin.formver.embeddings.expression.ExpEmbedding

class CustomGetter(val getterMethod: FunctionEmbedding) : GetterEmbedding {
    override fun getValue(
        receiver: ExpEmbedding,
        ctx: StmtConversionContext<ResultTrackingContext>,
        source: KtSourceElement?,
    ): ExpEmbedding =
        getterMethod.insertCall(listOf(receiver), ctx, source)
}

class CustomSetter(val setterMethod: CallableEmbedding) : SetterEmbedding {
    override fun setValue(
        receiver: ExpEmbedding,
        value: ExpEmbedding,
        ctx: StmtConversionContext<ResultTrackingContext>,
        source: KtSourceElement?,
    ) {
        setterMethod.insertCall(listOf(receiver, value), ctx, source)
    }
}
