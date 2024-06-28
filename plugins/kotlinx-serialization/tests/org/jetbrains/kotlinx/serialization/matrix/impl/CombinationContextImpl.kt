/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.matrix.impl

import org.jetbrains.kotlinx.serialization.matrix.*

internal class CombinationContextImpl : CombinationContext {
    private val typesByFeatures = mutableMapOf<TypeFeatures, MutableList<TypeVariant>>()
    private val types: MutableMap<TypeVariant, String> = mutableMapOf()
    private val functions: MutableMap<String, FunctionContext.() -> Unit> = mutableMapOf()

    override fun defineEnums(
        serializers: Set<SerializerKind>,
        locations: Set<TypeLocation>,
        optionsConfig: EnumOptionsBuilder.() -> Unit,
    ): Set<EnumVariant> {
        val enumTypes: MutableSet<EnumVariant> = mutableSetOf()
        val options = EnumOptionsBuilderImpl().also { it.optionsConfig() }.build()
        for (serializer in serializers) {
            for (location in locations) {
                val features = EnumFeatures(serializer, location)
                enumTypes += EnumVariant(features, options)
            }
        }
        addTypes(enumTypes)
        return enumTypes
    }

    override fun function(name: String, block: FunctionContext.() -> Unit) {
        functions[name] = block
    }

    override fun box(block: FunctionContext.() -> Unit) {
        functions["box(): String"] = {
            block()
            line("return \"OK\"")
        }
    }

    override fun generate(appendable: Appendable, generator: String) {
        val namedTypes = types.map { (type, name) -> NamedTypeVariant(name, type) }
        appendable.writeHeader(namedTypes, generator)
        appendable.writeTypes(namedTypes)

        functions.forEach { (signature, builder) ->
            appendable.writeFunction(signature, builder)
        }

        appendable.writeUtils()
    }

    override val TypeVariant.named: NamedTypeVariant
        get() = NamedTypeVariant(types[this] ?: throw Exception("Type variant wasn't defined properly $this"), this)

    private fun addTypes(types: Iterable<TypeVariant>) {
        for (type in types) {
            val typesForFeatures = typesByFeatures.getOrPut(type.features) { mutableListOf() }
            val className = type.className + if (typesForFeatures.size > 0) typesForFeatures.size.toString() else ""
            typesForFeatures += type
            this.types[type] = className
        }
    }
}

private class EnumOptionsBuilderImpl : EnumOptionsBuilder {
    private val serialInfoP: MutableSet<SerialInfo> = mutableSetOf()
    private val descriptorAccessingP: MutableSet<DescriptorAccessing> = mutableSetOf()
    private val entriesP: MutableSet<String> = mutableSetOf()


    override fun serialInfo(vararg serialInfo: SerialInfo) {
        serialInfo.forEach { serialInfoP.add(it) }
    }

    override fun descriptorAccessing(vararg descriptorAccessing: DescriptorAccessing) {
        descriptorAccessing.forEach { descriptorAccessingP.add(it) }
    }

    override fun entries(vararg entries: String) {
        entries.forEach { entriesP.add(it) }
    }

    fun build(): EnumOptions {
        return EnumOptions(serialInfoP, descriptorAccessingP, entriesP)
    }

}