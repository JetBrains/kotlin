/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.matrix.impl

import org.jetbrains.kotlinx.serialization.matrix.*
import org.jetbrains.kotlinx.serialization.matrix.SerializerKind.*

internal const val CLASS_FOR_NESTED = "Container"

internal const val CLASS_FOR_INNER = "Outer"

internal const val SERIAL_ANNOTATION = "Extra"


internal val TypeVariant.className: String
    get() = when (this) {
        is EnumVariant -> features.location.namePart + "Enum" + features.serializer.namePart
    }

internal val TypeVariant.elements: List<String>
    get() = when (this) {
        is EnumVariant -> options.entries.toList()
    }

internal val NamedTypeVariant.serialName: String
    get() = when (variant.features.serializer) {
        CUSTOM_CLASS, CUSTOM_OBJECT -> "custom|$classUsage"
        CONTEXTUAL, USE_CONTEXTUAL -> "contextual|$classUsage"
        CLASS_USE_SERIALIZER -> "useSerializer|$classUsage"
        else -> classUsage
    }


internal val NamedTypeVariant.useSerializer: String?
    get() {
        if (variant.features.serializer != CLASS_USE_SERIALIZER) return null
        return serializerName
    }

internal val NamedTypeVariant.useContextualSerializer: String?
    get() {
        if (variant.features.serializer != USE_CONTEXTUAL) return null
        return serializerName
    }

internal val NamedTypeVariant.serializerName: String
    get() = when (variant.features.serializer) {
        CUSTOM_CLASS, CUSTOM_OBJECT, CLASS_USE_SERIALIZER, CONTEXTUAL, USE_CONTEXTUAL -> name + "Serializer"
        BY_DEFAULT -> throw IllegalStateException("No named serializer for type serializable by default '$name'")
        GENERATED -> throw IllegalStateException("No named serializer for type '$name' with automatically generated serializer")
    }

private val TypeLocation.namePart: String
    get() = when (this) {
        TypeLocation.FILE_ROOT -> ""
        TypeLocation.LOCAL -> "Local"
        TypeLocation.NESTED -> "Nested"
    }

private val SerializerKind.namePart: String
    get() = when (this) {
        BY_DEFAULT -> "WithDef"
        GENERATED -> ""
        CUSTOM_OBJECT -> "WithCustom"
        CUSTOM_CLASS -> "WithCustomCl"
        CONTEXTUAL -> "WithContextual"
        USE_CONTEXTUAL -> "WithUseContextual"
        CLASS_USE_SERIALIZER -> "WithUse"
    }
