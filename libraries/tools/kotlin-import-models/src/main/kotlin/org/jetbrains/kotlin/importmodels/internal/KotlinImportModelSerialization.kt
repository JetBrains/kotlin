/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.importmodels.internal

import com.google.protobuf.Any
import com.google.protobuf.Message
import org.jetbrains.kotlin.importmodels.proto.Error
import org.jetbrains.kotlin.importmodels.proto.Result
import org.jetbrains.kotlin.importmodels.proto.error as importModelError
import org.jetbrains.kotlin.importmodels.proto.result as resultModel

/**
 * Serialization of import model [Result] envelopes: packing a model or an error into response bytes.
 */
object KotlinImportModelSerialization {
    fun modelResult(model: Message): ByteArray = resultModel {
        this.model = Any.pack(model)
    }.toByteArray()

    fun errorResult(type: Error.Type, message: String): ByteArray = resultModel {
        error = importModelError {
            errorType = type
            errorMessage = message
        }
    }.toByteArray()
}
