/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.formver.conversion.AccessPolicy
import org.jetbrains.kotlin.formver.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.embeddings.expression.*

class BackingFieldGetter(val field: FieldEmbedding) : GetterEmbedding {
    override fun getValue(receiver: ExpEmbedding, ctx: StmtConversionContext): ExpEmbedding {
        return if (field.accessPolicy == AccessPolicy.ALWAYS_READABLE) {
            FieldAccess(receiver, field)
        } else {
            FieldAccess(receiver, field).withInvariants {
                proven = true
                access = true
            }
        }
    }
}

class BackingFieldSetter(val field: FieldEmbedding) : SetterEmbedding {
    override fun setValue(receiver: ExpEmbedding, value: ExpEmbedding, ctx: StmtConversionContext): ExpEmbedding =
        FieldModification(receiver, field, value)
}

