/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.formver.conversion.ResultTrackingContext
import org.jetbrains.kotlin.formver.conversion.StmtConversionContext

interface SetterEmbedding {
    fun setValue(receiver: ExpEmbedding, value: ExpEmbedding, ctx: StmtConversionContext<ResultTrackingContext>)
}