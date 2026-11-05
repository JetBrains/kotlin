/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.importmodels.internal

import com.google.protobuf.Any
import com.google.protobuf.Message
import com.google.protobuf.util.JsonFormat
import org.jetbrains.kotlin.importmodels.proto.Error
import org.jetbrains.kotlin.importmodels.proto.KotlinImportModels
import org.jetbrains.kotlin.importmodels.proto.Result
import org.jetbrains.kotlin.importmodels.proto.error as importModelError
import org.jetbrains.kotlin.importmodels.proto.result as resultModel

/**
 * Serialization of import model [org.jetbrains.kotlin.importmodels.proto.Result] envelopes:
 * packing a model or an error into response bytes, and printing a message as ProtoJSON.
 */
object KotlinImportModelSerialization {
    // The registry lets JsonFormat resolve the google.protobuf.Any payload of a Result
    private val jsonTypeRegistry = JsonFormat.TypeRegistry.newBuilder()
        .add(KotlinImportModels.getDescriptor().messageTypes)
        .build()

    fun modelResult(model: Message): ByteArray = resultModel {
        this.model = Any.pack(model)
    }.toByteArray()

    fun errorResult(type: Error.Type, message: String): ByteArray = resultModel {
        error = importModelError {
            errorType = type
            errorMessage = message
        }
    }.toByteArray()

    /**
     * Parses the ProtoJSON produced by [toJson] back into a [Result].
     */
    fun parseResult(json: String): Result = Result.newBuilder().also { builder ->
        JsonFormat.parser().usingTypeRegistry(jsonTypeRegistry).merge(json, builder)
    }.build()

    /**
     * Returns the message as ProtoJSON with proto field names, printing empty repeated fields;
     * unset singular fields stay omitted (edition-2023 presence).
     */
    fun toJson(message: Message): String = JsonFormat.printer()
        .usingTypeRegistry(jsonTypeRegistry)
        .preservingProtoFieldNames()
        .alwaysPrintFieldsWithNoPresence()
        .print(message)
}
