/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.formver.conversion.ResultTrackingContext
import org.jetbrains.kotlin.formver.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.embeddings.callables.CallableEmbedding
import org.jetbrains.kotlin.formver.embeddings.callables.insertCall

class CustomGetter(val getterMethod: CallableEmbedding) : GetterEmbedding {
    override fun getValue(receiver: ExpEmbedding, ctx: StmtConversionContext<ResultTrackingContext>): ExpEmbedding =
        getterMethod.insertCall(listOf(receiver), ctx)
}

class CustomSetter(val setterMethod: CallableEmbedding) : SetterEmbedding {
    override fun setValue(receiver: ExpEmbedding, value: ExpEmbedding, ctx: StmtConversionContext<ResultTrackingContext>) {
        setterMethod.insertCall(listOf(receiver, value), ctx)
    }
}
