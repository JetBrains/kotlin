/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.formver.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.FieldAccess
import org.jetbrains.kotlin.formver.embeddings.expression.FieldModification
import org.jetbrains.kotlin.formver.embeddings.expression.withProvenInvariants

class BackingFieldGetter(val field: FieldEmbedding) : GetterEmbedding {
    override fun getValue(receiver: ExpEmbedding, ctx: StmtConversionContext): ExpEmbedding =
        FieldAccess(receiver, field).withProvenInvariants()
}

class BackingFieldSetter(val field: FieldEmbedding) : SetterEmbedding {
    override fun setValue(receiver: ExpEmbedding, value: ExpEmbedding, ctx: StmtConversionContext): ExpEmbedding =
        FieldModification(receiver, field, value)
}

