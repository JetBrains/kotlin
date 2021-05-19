/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.typeProviders.json.inference

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import kotlin.reflect.jvm.jvmErasure
import kotlin.script.experimental.typeProviders.generatedCode.*
import kotlin.script.experimental.typeProviders.generatedCode.impl.*
import kotlin.script.experimental.typeProviders.json.*
import kotlin.script.experimental.typeProviders.json.inference.Inferred.JSONType
import kotlin.script.experimental.typeProviders.json.utils.mergedWith
import kotlin.script.experimental.typeProviders.json.utils.toCamelCase

internal class Inferred(
    val file: File,
    val type: JSONType
) {
    sealed class JSONType {
        abstract fun mergedWith(other: JSONType): JSONType
        abstract fun type(): IdentifiableMember

        data class Object(
            val name: String,
            val properties: Map<String, JSONType>
        ) : JSONType() {
            override fun mergedWith(other: JSONType): JSONType {
                if (other == Null) return optional()
                val otherObject = other.asObjectOrNull() ?: return StandardType.Any.asResolved()

                require(name == otherObject.name)

                val mergedProperties = properties
                    .mergedWith(otherObject.properties) { otherType ->
                        mergedWith(otherType ?: Null) // handle missing key as null
                    }

                return copy(properties = mergedProperties)
            }

            override fun type() = IdentifiableMember(name)

            fun body(baseDirectory: File?) = GeneratedCode {
                dataClass(name) {
                    for ((name, resolved) in properties) {
                        property(name, resolved.type())
                        resolved.body(baseDirectory)?.let { +it }
                    }

                    companionObject {
                        implement(ParsedFactory::class.withParameters(this@dataClass).asInterface()) {
                            if (baseDirectory != null) {
                                constant("baseDirectory", baseDirectory)
                            }
                        }
                    }
                }
            }
        }

        data class Standard(val type: StandardType) : JSONType() {
            override fun mergedWith(other: JSONType): JSONType {
                if (other == Null) return optional()
                val otherType = other.asStandardTypeOrNull() ?: return StandardType.Any.asResolved()

                if (otherType != type) return StandardType.Any.asResolved()

                return this
            }

            override fun type() = IdentifiableMember(type.kClass)
        }

        data class Array(val inferred: JSONType) : JSONType() {
            override fun mergedWith(other: JSONType): JSONType {
                if (other == Null) return optional()
                val otherResolved = other.asArrayOfResolvedOrNull() ?: return StandardType.Any.asResolved()

                return inferred.mergedWith(otherResolved).array()
            }

            override fun type() = List::class.withParameters(inferred.type())
        }

        data class Optional(val inferred: JSONType) : JSONType() {
            override fun mergedWith(other: JSONType): JSONType {
                if (other == Null) return this
                val otherResolved = other.asOptionalOfResolvedOrNull() ?: other

                return inferred.mergedWith(otherResolved).optional()
            }

            override fun type() = inferred.type().optional()
        }

        object Null : JSONType() {
            override fun mergedWith(other: JSONType): JSONType = when (other) {
                is Optional -> other
                is Null -> other
                else -> other.optional()
            }

            override fun type() = Any::class.optional()
        }
    }
}

internal fun JSONType.array() = JSONType.Array(this)

internal fun JSONType.optional() = JSONType.Optional(this)

internal fun JSONType.asObjectOrNull() = this as? JSONType.Object

internal fun JSONType.asStandardTypeOrNull() = (this as? JSONType.Standard)?.type

internal fun JSONType.asArrayOfResolvedOrNull() = (this as? JSONType.Array)?.inferred

internal fun JSONType.asOptionalOfResolvedOrNull(): JSONType? = (this as? JSONType.Optional)?.inferred

private fun JSONType.body(baseDirectory: File?): GeneratedCode? = when (this) {
    is JSONType.Object -> body(baseDirectory)
    is JSONType.Standard -> null
    is JSONType.Array -> inferred.body(baseDirectory)
    is JSONType.Optional -> inferred.body(baseDirectory)
    JSONType.Null -> null
}

internal fun Inferred.generate(
    baseDirectory: File?
): GeneratedCode = GeneratedCode {
    // Include body of type if present
    type.body(baseDirectory)?.let { +it }

    // Include a lazy property with the name of the file with the contents of the file
    val file = file
    lazyProperty(file.nameWithoutExtension.toCamelCase(), type = type.type()) { type ->
        val jacksonMapper = jacksonObjectMapper()
        jacksonMapper.readValue(file, type.jvmErasure.java)
    }
}
