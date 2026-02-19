/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.matrix.cases

import org.jetbrains.kotlinx.serialization.matrix.SerializerKind.*
import org.jetbrains.kotlinx.serialization.matrix.TypeLocation.FILE_ROOT
import org.jetbrains.kotlinx.serialization.matrix.TypeLocation.NESTED
import org.jetbrains.kotlinx.serialization.matrix.*
import org.jetbrains.kotlinx.serialization.matrix.impl.*
import org.jetbrains.kotlinx.serialization.matrix.impl.elements
import org.jetbrains.kotlinx.serialization.matrix.impl.serialName

fun CombinationContext.enumsTestMatrix() {
    val enums = defineEnums(
        SerializerKind.entries.toSet(),
        setOf(FILE_ROOT, NESTED)
    ) {
        entries("A", "B")
        descriptorAccessing(*DescriptorAccessing.entries.toTypedArray())
    }

    val enumsWithAnnotations = defineEnums(
        setOf(GENERATED),
        setOf(FILE_ROOT)
    ) {
        entries("A", "B")
        serialInfo(*SerialInfo.entries.toTypedArray())
    }

    val allTypes = enums + enumsWithAnnotations

    val withCompanion = allTypes.filter(::hasFactoryFun)
    val lookupTypes = allTypes.filter(::canBeUsedInLookup)

    val moduleLookupTypes = allTypes.filter(::canBeUsedInModuleLookup)

    val annotatedTypes = allTypes.filter(::hasAnnotationOnType)
    val annotatedElementsTypes = allTypes.filter(::hasAnnotationOnElement)

    box() {
        line("val module = SerializersModule {")
        allTypes.filter(::shouldAddContextualSerializerToModule).forEach { type ->
            line("    contextual(${type.named.serializerName})")
        }
        line("}")
        line()

        allTypes.forEach { type ->
            line("serializer<${type.named.classUsage}>().checkElements(${
                type.elements.joinToString(", ") { "\"$it\"" }
            })")
        }
        line()

        line("// Call serializer factory function in companion")
        withCompanion.forEach { type ->
            line("${type.named.classUsage}.serializer().checkSerialName(\"${type.named.serialName}\")?.let { return it }")
        }
        line()

        line("// Serializer lookup by generic parameter")
        lookupTypes.forEach { type ->
            val serialName = if (type.features.serializer == CLASS_USE_SERIALIZER || type.features.serializer == USE_CONTEXTUAL) {
                line("// generated serializer used in lookup for the empty module  in case of specifying of @file:UseContextualSerialization or @UseSerializers")
                type.named.classUsage
            } else {
                type.named.serialName
            }
            line("serializer<${type.named.classUsage}>().checkSerialName(\"$serialName\")?.let { return it }")
        }
        line()

        line("// Serializer lookup by typeOf function")
        lookupTypes.forEach { type ->
            val serialName = if (type.features.serializer == CLASS_USE_SERIALIZER || type.features.serializer == USE_CONTEXTUAL) {
                line("// generated serializer used in lookup for the empty module  in case of specifying of @file:UseContextualSerialization or @UseSerializers")
                type.named.classUsage
            } else {
                type.named.serialName
            }
            line("serializer(typeOf<${type.named.classUsage}>()).checkSerialName(\"$serialName\")?.let { return it }")
        }
        line()

        line("// Serializer lookup by generic parameter in custom module")
        moduleLookupTypes.forEach { type ->
            val serialName = if (type.features.serializer == CLASS_USE_SERIALIZER || type.features.serializer == USE_CONTEXTUAL) {
                line("// !!! for some reason, the generated serializer is still lookup for the custom module in case of specifying of @file:UseContextualSerialization or @UseSerializers")
                type.named.classUsage
            } else {
                type.named.serialName
            }
            line("module.serializer<${type.named.classUsage}>().checkSerialName(\"$serialName\")?.let { return it }")
        }
        line()

        line("// Serializer lookup by typeOf function in custom module")
        moduleLookupTypes.forEach { type ->
            val serialName = if (type.features.serializer == CLASS_USE_SERIALIZER || type.features.serializer == USE_CONTEXTUAL) {
                line("// !!! for some reason, the generated serializer is still lookup for the custom module in case of specifying of @file:UseContextualSerialization or @UseSerializers")
                type.named.classUsage
            } else {
                type.named.serialName
            }
            line("module.serializer(typeOf<${type.named.classUsage}>()).checkSerialName(\"$serialName\")?.let { return it }")
        }
        line()

        line("// Annotation on type should have value same as a class name")
        annotatedTypes.forEach { type ->
            line("serializer<${type.named.classUsage}>().checkAnnotation(\"${type.named.classUsage}\")")
        }
        line()

        line("// Annotation on enum entries should have value same as a entry names")
        annotatedElementsTypes.forEach { type ->
            line("serializer<${type.named.classUsage}>().checkElementAnnotations(${
                type.elements.joinToString(", ") { "\"$it\"" }
            })")
        }
        line()
    }
}