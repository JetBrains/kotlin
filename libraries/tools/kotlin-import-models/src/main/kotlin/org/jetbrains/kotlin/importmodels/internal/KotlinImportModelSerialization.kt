/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.importmodels.internal

import com.google.protobuf.Any
import com.google.protobuf.InvalidProtocolBufferException
import com.google.protobuf.Message
import com.google.protobuf.util.JsonFormat
import org.jetbrains.kotlin.importmodels.proto.CompilationUnitModel
import org.jetbrains.kotlin.importmodels.proto.CompilerArgumentsModel
import org.jetbrains.kotlin.importmodels.proto.CompilationUnitId
import org.jetbrains.kotlin.importmodels.proto.DependenciesModel
import org.jetbrains.kotlin.importmodels.proto.Error
import org.jetbrains.kotlin.importmodels.proto.error as importModelError
import org.jetbrains.kotlin.importmodels.proto.result as resultModel

object KotlinImportModelSerialization {
    fun modelResult(model: Message): ByteArray = resultModel { this.model = Any.pack(model) }.toByteArray()

    fun errorResult(type: Error.Type, message: String): ByteArray = resultModel {
        error = importModelError {
            errorType = type
            errorMessage = message
        }
    }.toByteArray()

    fun parseCompilationUnitId(bytes: ByteArray): CompilationUnitId? =
        parseParameters(bytes, CompilationUnitModel.Parameters::parseFrom)
            ?.takeIf { it.hasCompilationUnitId() && it.compilationUnitId.value.isNotEmpty() }
            ?.compilationUnitId

    fun parseCompilerArgumentsCompilationUnitId(bytes: ByteArray): CompilationUnitId? =
        parseParameters(bytes, CompilerArgumentsModel.Parameters::parseFrom)
            ?.takeIf { it.hasCompilationUnitId() && it.compilationUnitId.value.isNotEmpty() }
            ?.compilationUnitId

    fun parseDependenciesParameters(bytes: ByteArray): DependenciesModel.Parameters? =
        parseParameters(bytes, DependenciesModel.Parameters::parseFrom)
            ?.takeIf { it.hasCompilationUnitId() && it.compilationUnitId.value.isNotEmpty() }

    private fun <P> parseParameters(bytes: ByteArray, parser: (ByteArray) -> P): P? = try {
        parser(bytes)
    } catch (_: InvalidProtocolBufferException) {
        null
    }

    fun toJson(model: Message): String = JsonFormat.printer()
        .preservingProtoFieldNames()
        .alwaysPrintFieldsWithNoPresence()
        .print(model)
}
