/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.jetbrains.kotlin.importmodels.internal.protobuf.com.google.protobuf.TextFormat
import org.jetbrains.kotlin.importmodels.internal.protobuf.com.google.protobuf.TypeRegistry
import org.jetbrains.kotlin.importmodels.proto.KotlinImportModels
import org.jetbrains.kotlin.importmodels.proto.Result

/**
 * TextFormat printing and parsing of import model [Result] envelopes for the golden harness.
 * The embeddable `kotlin-import-models` jar ships the Protobuf runtime relocated under
 * `org.jetbrains.kotlin.importmodels.internal.protobuf`, and the harness reads the models through
 * that jar's classes, so the printer and parser come from the same relocated runtime.
 */
internal object ImportModelsTextFormat {
    // The registry lets TextFormat expand the google.protobuf.Any payload of a Result
    private val typeRegistry = TypeRegistry.newBuilder()
        .add(KotlinImportModels.getDescriptor().messageTypes)
        .build()

    private val parser = TextFormat.Parser.newBuilder().setTypeRegistry(typeRegistry).build()

    /**
     * Parses the text produced by [printResult] back into a [Result].
     */
    fun parseResult(text: String): Result = Result.newBuilder().also { builder ->
        parser.merge(text, builder)
    }.build()

    /**
     * Returns the message as readable TextFormat, with the `Any` payload expanded into its fields.
     */
    fun printResult(result: Result): String = TextFormat.printer()
        .usingTypeRegistry(typeRegistry)
        .printToString(result)
}
