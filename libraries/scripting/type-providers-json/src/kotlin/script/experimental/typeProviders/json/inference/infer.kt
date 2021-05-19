/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.typeProviders.json.inference

import kotlinx.serialization.json.*
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.typeProviders.json.*
import kotlin.script.experimental.typeProviders.json.utils.toCamelCase
import kotlin.script.experimental.typeProviders.json.utils.toUpperCamelCase

internal fun JSON.infer(baseDirectory: File?, location: SourceCode.LocationWithId?): ResultWithDiagnostics<Inferred> {
    val file = file(baseDirectory, location).valueOr { return it }
    val typeName = typeName.takeIf { it.isNotBlank() } ?: file.nameWithoutExtension
    return file.json(location).onSuccess { Inferred(file, it.infer(typeName = typeName)).asSuccess() }
}

private fun JSON.file(baseDirectory: File?, location: SourceCode.LocationWithId?): ResultWithDiagnostics<File> {
    return File(baseDirectory, filePath).takeIf { it.exists() }?.asSuccess()
        ?: File(filePath).takeIf { it.exists() }?.asSuccess()
        ?: return makeFailureResult("File $filePath does not exist", location)
}

private fun File.json(location: SourceCode.LocationWithId?): ResultWithDiagnostics<JsonElement> {
    val content = inputStream().reader().readText()
    return try {
        Json(JsonConfiguration.Stable).parseJson(content).asSuccess()
    } catch (exception: Throwable) {
        makeFailureResult("Failed to parse JSON from $path due to: ${exception.message}", location)
    }
}

private fun JsonElement.infer(typeName: String): Inferred.JSONType = when (this) {
    is JsonLiteral -> infer().asResolved()
    JsonNull -> Inferred.JSONType.Null
    is JsonObject -> Inferred.JSONType.Object(
        name = typeName.toUpperCamelCase(),
        properties = content
            .mapKeys { it.key.toCamelCase() }
            .mapValues { entry ->
                entry.value.infer(entry.key.toUpperCamelCase())
            }
    )
    is JsonArray -> content.infer(typeName).array()
}

private fun JsonLiteral.infer(): StandardType {
    if (isString) return StandardType.String
    if (primitive.booleanOrNull != null) return StandardType.Boolean
    if (primitive.intOrNull != null) return StandardType.Int
    if (primitive.doubleOrNull != null) return StandardType.Double
    return StandardType.Any
}

private fun Collection<JsonElement>.infer(typeName: String): Inferred.JSONType {
    if (isEmpty()) return StandardType.Any.asResolved()
    return map { it.infer(typeName = typeName) }.reduce { acc, resolved -> acc.mergedWith(resolved) }
}