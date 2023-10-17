/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.conversion.ResultTrackingContext
import org.jetbrains.kotlin.formver.conversion.StmtConversionContext

// We assume that thanks to the checks done by the Kotlin compiler, a property with a
// missing getter or setter will never be accessed.
class ClassPropertyAccess(val receiver: ExpEmbedding, val property: PropertyEmbedding) :
    PropertyAccessEmbedding {
    override fun getValue(ctx: StmtConversionContext<ResultTrackingContext>, source: KtSourceElement?): ExpEmbedding = property.getter!!.getValue(receiver, ctx, source)

    override fun setValue(value: ExpEmbedding, ctx: StmtConversionContext<ResultTrackingContext>, source: KtSourceElement?) {
        property.setter!!.setValue(receiver, value, ctx, source)
    }
}
