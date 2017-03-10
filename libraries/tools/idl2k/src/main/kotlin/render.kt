/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.idl2k

import org.jetbrains.idl2k.util.mapEnumConstant
import java.math.BigInteger

private fun <O : Appendable> O.indent(commented: Boolean = false, level: Int) {
    if (commented) {
        append("//")
    }
    for (i in 1..level) {
        append("    ")
    }
}

private fun Appendable.renderAttributeDeclaration(arg: GenerateAttribute, modality: MemberModality, omitDefaults: Boolean = false) {
    if (arg.vararg) {
        append("vararg ")
    }
    else {
        when (modality) {
            MemberModality.OVERRIDE -> append("override ")
            MemberModality.ABSTRACT -> append("abstract ")
            MemberModality.OPEN -> append("open ")
            MemberModality.FINAL -> {}
        }
    }

    append(when(arg.kind) {
        AttributeKind.VAL -> "val "
        AttributeKind.VAR -> "var "
        AttributeKind.ARGUMENT -> ""
    })
    append(arg.name.replaceKeywords())
    append(": ")
    append(arg.type.render())
    if (arg.initializer != null) {
        if (omitDefaults) {
            append(" /*")
        }

        append(" = ")
        append(arg.initializer.replaceWrongConstants(arg.type))

        if (omitDefaults) {
            append(" */")
        }
    }
}

private fun Appendable.renderAttributeDeclarationAsProperty(arg: GenerateAttribute, modality: MemberModality, commented: Boolean, level: Int, omitDefaults: Boolean = false) {
    indent(commented, level)

    if (arg.name in keywords) {
        append("@JsName(\"${arg.name}\") ")
    }

    renderAttributeDeclaration(arg, modality, omitDefaults)

    appendln()
    if (arg.getterNoImpl) {
        indent(commented, level + 1)
        appendln("get() = definedExternally")
    }
    if (arg.setterNoImpl) {
        indent(commented, level + 1)
        appendln("set(value) = definedExternally")
    }
}

private val keywords = setOf("interface", "is", "as")

private fun String.parse() = if (this.startsWith("0x")) BigInteger(this.substring(2), 16) else BigInteger(this)
private fun String.replaceWrongConstants(type: Type) = when {
    this == "null" && type.nullable -> "null"
    this == "definedExternally" || type is SimpleType && type.type == "Int" && parse() > BigInteger.valueOf(Int.MAX_VALUE.toLong()) -> "definedExternally"
    type is SimpleType && type.type == "Double" && this.matches("[0-9]+".toRegex()) -> "${this}.0"
    else -> this
}
private fun String.replaceKeywords() = if (this in keywords) this + "_" else this

private fun Appendable.renderArgumentsDeclaration(args: List<GenerateAttribute>, omitDefaults: Boolean = false) =
        args.joinTo(this, ", ", "(", ")") {
            StringBuilder().apply { renderAttributeDeclaration(it, if (it.override) MemberModality.OVERRIDE else MemberModality.FINAL, omitDefaults) }
        }

private fun renderCall(call: GenerateFunctionCall) = "${call.name.replaceKeywords()}(${call.arguments.joinToString(separator = ", ", transform = String::replaceKeywords)})"

private fun Appendable.renderFunctionDeclaration(owner: String, f: GenerateFunction, override: Boolean, commented: Boolean, level: Int = 1) {
    indent(commented, level)

    if (f.nativeGetterOrSetter != NativeGetterOrSetter.NONE) {
        append("@kotlin.internal.InlineOnly ")
    }
    if (override) {
        append("override ")
    }
    if (f.nativeGetterOrSetter != NativeGetterOrSetter.NONE) {
        append("inline operator ")
    }

    if (f.name in keywords) {
        append("@JsName(\"${f.name}\") ")
    }
    append("fun ")
    if (f.nativeGetterOrSetter != NativeGetterOrSetter.NONE) {
        append("$owner.")
    }
    append(f.name.replaceKeywords())
    renderArgumentsDeclaration(f.arguments, override)
    append(": ${f.returnType.render()}")

    when (f.nativeGetterOrSetter) {
        NativeGetterOrSetter.GETTER -> {
            append(" = asDynamic()[${f.arguments[0].name}]")
        }
        NativeGetterOrSetter.SETTER -> {
            append(" { asDynamic()[${f.arguments[0].name}] = ${f.arguments[1].name}; }")
        }
        NativeGetterOrSetter.NONE -> {}
    }

    appendln()
}

private fun List<GenerateAttribute>.hasNoVars() = none { it.isVar }

private fun GenerateAttribute.isCommented(parent: String) = "$parent.$name" in commentOutDeclarations || "$parent.$name: ${type.render()}" in commentOutDeclarations
private fun GenerateFunction.isCommented(parent: String) =
        "$parent.$name" in commentOutDeclarations || "$parent.$name(${arguments.size})" in commentOutDeclarations
private fun GenerateAttribute.isRequiredFunctionArgument(owner: String, functionName: String) = "$owner.$functionName.$name" in requiredArguments
private fun GenerateFunction.fixRequiredArguments(parent: String) = copy(arguments = arguments.map { arg -> arg.copy(initializer = if (arg.isRequiredFunctionArgument(parent, name)) null else arg.initializer) })

fun Appendable.render(allTypes: Map<String, GenerateTraitOrClass>, enums: List<EnumDefinition>, typeNamesToUnions: Map<String, List<String>>, iface: GenerateTraitOrClass, markerAnnotation: Boolean = false) {
    val allTypesAndEnums = allTypes.keys + enums.map { it.name }

    append("public external ")
    if (markerAnnotation) {
        append("@marker ")
    }
    when (iface.kind) {
        GenerateDefinitionKind.CLASS -> append("open class ")
        GenerateDefinitionKind.ABSTRACT_CLASS -> append("abstract class ")
        GenerateDefinitionKind.INTERFACE -> append("interface ")
    }

    val allSuperTypes = iface.allSuperTypes(allTypes + kotlinBuiltinInterfaces)
    val allSuperTypesNames = allSuperTypes.map { it.name }.toSet()

    append(iface.name)
    val primary = iface.primaryConstructor
    if (primary != null && (primary.constructor.arguments.isNotEmpty() || iface.secondaryConstructors.isNotEmpty())) {
        renderArgumentsDeclaration(primary.constructor.fixRequiredArguments(iface.name).arguments.dynamicIfUnknownType(allTypesAndEnums), false)
    }

    val superTypesExclude = inheritanceExclude[iface.name] ?: emptySet()
    val superTypesWithCalls =
                    iface.superTypes.filter { it in allSuperTypesNames }.filter { it !in superTypesExclude } +
                    (typeNamesToUnions[iface.name] ?: emptyList()) +
                    (iface.superTypes.filter { it.substringBefore("<") in kotlinBuiltinInterfaces }) // TODO in theory we have to parse type but for now it is the only place needs it so let's just cut string

    if (superTypesWithCalls.isNotEmpty()) {
        superTypesWithCalls.joinTo(this, ", ", " : ")
    }

    appendln (" {")

    iface.secondaryConstructors.forEach { secondary ->
        indent(false, 1)
        append("constructor")
        renderArgumentsDeclaration(secondary.constructor.fixRequiredArguments(iface.name).arguments.dynamicIfUnknownType(allTypesAndEnums), false)

        appendln()
    }

    val superAttributes = allSuperTypes.flatMap { it.memberAttributes }.distinct()
    val superAttributesByName = superAttributes.groupBy { it.name }
    val superFunctions = allSuperTypes.flatMap { it.memberFunctions }.distinct()
    val superSignatures = superAttributes.map { it.signature } merge superFunctions.map { it.signature }

    iface.memberAttributes
        .filter { it !in superAttributes && !it.static && (it.isVar || (it.isVal && superAttributesByName[it.name]?.hasNoVars() ?: true)) }
        .map { it.dynamicIfUnknownType(allTypesAndEnums) }
        .groupBy { it.name }
        .mapValues { it.value.filter { "${iface.name}.${it.name}" !in commentOutDeclarations && "${iface.name}.${it.name}: ${it.type.render()}" !in commentOutDeclarations  } }
        .filterValues { it.isNotEmpty() }
        .reduceValues(::merge).values.forEach { attribute ->
            val modality = when {
                attribute.signature in superSignatures -> MemberModality.OVERRIDE
                iface.kind == GenerateDefinitionKind.CLASS && attribute.isVal -> MemberModality.OPEN
                iface.kind == GenerateDefinitionKind.ABSTRACT_CLASS -> MemberModality.OPEN
                else -> MemberModality.FINAL
            }

            if (attribute.name in superAttributesByName && attribute.signature !in superSignatures) {
                System.err.println("Property ${iface.name}.${attribute.name} has different type in super type(s) so will not be generated: ")
                for ((superTypeName, attributes) in allSuperTypes.map { it.name to it.memberAttributes.filter { it.name == attribute.name }.distinct() }) {
                    for (superAttribute in attributes) {
                        System.err.println("  $superTypeName.${attribute.name}: ${superAttribute.type.render()}")
                    }
                }
            } else {
                renderAttributeDeclarationAsProperty(attribute,
                        modality = modality,
                        commented = attribute.isCommented(iface.name),
                        omitDefaults = iface.kind == GenerateDefinitionKind.INTERFACE,
                        level = 1
                )
            }
    }
    val memberFunctions = iface.memberFunctions.filter { (it !in superFunctions || it.override) && !it.static }
            .map { it.dynamicIfUnknownType(allTypesAndEnums) }.groupBy { it.signature }.reduceValues(::betterFunction).values

    fun doRenderFunction(function: GenerateFunction, level: Int = 1) {
        renderFunctionDeclaration(
                iface.name, function.fixRequiredArguments(iface.name),
                function.signature in superSignatures || function.override,
                commented = function.isCommented(iface.name),
                level = level
        )
    }

    memberFunctions.filter { it.nativeGetterOrSetter == NativeGetterOrSetter.NONE }.forEach { doRenderFunction(it) }

    val staticAttributes = iface.memberAttributes.filter { it.static }
    val staticFunctions = iface.memberFunctions.filter { it.static }

    if (iface.constants.isNotEmpty() || staticAttributes.isNotEmpty() || staticFunctions.isNotEmpty()) {
        appendln()
        indent(false, 1)
        appendln("companion object {")
        iface.constants.forEach {
            renderAttributeDeclarationAsProperty(it, MemberModality.FINAL, level = 2, commented = it.isCommented(iface.name))
        }
        staticAttributes.forEach {
            renderAttributeDeclarationAsProperty(it, MemberModality.FINAL, level = 2, commented = it.isCommented(iface.name))
        }
        staticFunctions.forEach {
            renderFunctionDeclaration(iface.name, it.fixRequiredArguments(iface.name), override = false, level = 2, commented = it.isCommented(iface.name))
        }
        indent(false, 1)
        appendln("}")
    }

    appendln("}")
    memberFunctions.filter { it.nativeGetterOrSetter != NativeGetterOrSetter.NONE }.forEach { doRenderFunction(it, 0) }
    appendln()

    if (iface.generateBuilderFunction) {
        renderBuilderFunction(iface, allSuperTypes, allTypesAndEnums)
    }
}

fun Appendable.renderBuilderFunction(dictionary: GenerateTraitOrClass, allSuperTypes: List<GenerateTraitOrClass>, allTypes: Set<String>) {
    val fields = (dictionary.memberAttributes + allSuperTypes.flatMap { it.memberAttributes })
            .distinctBy { it.signature }
            .map { it.copy(kind = AttributeKind.ARGUMENT) }
            .dynamicIfUnknownType(allTypes)
            .map { if (it.initializer == null && (it.type.nullable || it.type == DynamicType) && !it.required) it.copy(initializer = "null") else it }

    appendln("@kotlin.internal.InlineOnly")
    append("public inline fun ${dictionary.name}")
    renderArgumentsDeclaration(fields)
    appendln(": ${dictionary.name} {")

    indent(level = 1)
    appendln("val o = js(\"({})\")")
    appendln()

    for (field in fields) {
        indent(level = 1)
        appendln("o[\"${field.name}\"] = ${field.name.replaceKeywords()}")
    }

    appendln()

    indent(level = 1)
    appendln("return o")

    appendln("}")
    appendln()
}

fun betterFunction(f1: GenerateFunction, f2: GenerateFunction): GenerateFunction =
        f1.copy(
                arguments = f1.arguments
                        .zip(f2.arguments)
                        .map { it.first.copy(type = it.map { it.type }.betterType(), name = it.map { it.name }.betterName()) },
                nativeGetterOrSetter = listOf(f1.nativeGetterOrSetter, f2.nativeGetterOrSetter)
                        .firstOrNull { it != NativeGetterOrSetter.NONE } ?: NativeGetterOrSetter.NONE
        )

private fun <F, T> Pair<F, F>.map(block: (F) -> T) = block(first) to block(second)
private fun Pair<Type, Type>.betterType() = if (first is DynamicType || first is AnyType) first else second
private fun Pair<String, String>.betterName() = if (((0..9).map(Int::toString) + listOf("arg")).none { first.toLowerCase().contains(it) }) first else second

private fun merge(a: AttributeKind, b: AttributeKind): AttributeKind {
    if (a == b) {
        return a
    }

    if (a == AttributeKind.VAR || b == AttributeKind.VAR) {
        return AttributeKind.VAR
    }

    return a
}

private fun merge(a: GenerateAttribute, b: GenerateAttribute): GenerateAttribute {
    require(a.name == b.name)

    val type = when {
        a.type.dropNullable() == b.type.dropNullable() -> a.type.withNullability(a.type.nullable || b.type.nullable)
        else -> DynamicType
    }

    return GenerateAttribute(
            a.name,
            type,
            a.initializer ?: b.initializer,
            a.getterSetterNoImpl || b.getterSetterNoImpl,
            merge(a.kind, b.kind),
            a.override,
            a.vararg,
            a.static,
            a.required || b.required
    )
}

fun <K, V> List<Pair<K, V>>.toMultiMap(): Map<K, List<V>> = groupBy { it.first }.mapValues { it.value.map { it.second } }

fun Appendable.render(enumDefinition: EnumDefinition) {
    appendln("/* please, don't implement this interface! */")
    appendln("public external interface ${enumDefinition.name} {")
    indent(level = 1)
    appendln("companion object")
    appendln("}")

    for (entry in enumDefinition.entries) {
        val entryName = mapEnumConstant(entry)
        appendln("public inline val ${enumDefinition.name}.Companion.$entryName: ${enumDefinition.name} " +
                "get() = \"$entry\".asDynamic().unsafeCast<${enumDefinition.name}>()")
    }

    appendln()
}

fun Appendable.render(namespace: String, ifaces: List<GenerateTraitOrClass>, unions: GenerateUnionTypes, enums: List<EnumDefinition>) {
    val declaredTypes = ifaces.associateBy { it.name }

    val allTypes = declaredTypes + unions.anonymousUnionsMap + unions.typedefsMarkersMap
    declaredTypes.values.filter { it.namespace == namespace }.forEach {
        render(allTypes, enums, unions.typeNamesToUnionsMap, it)
    }

    unions.anonymousUnionsMap.values.filter { it.namespace == "" || it.namespace == namespace }.forEach {
        render(allTypes, enums, emptyMap(), it, markerAnnotation = true)
    }

    unions.typedefsMarkersMap.values.filter { it.namespace == "" || it.namespace == namespace }.forEach {
        render(allTypes, enums, emptyMap(), it, markerAnnotation = true)
    }

    enums.filter { it.namespace == namespace }
            .forEach { render(it) }
}

enum class MemberModality {
    OPEN,
    ABSTRACT,
    OVERRIDE,
    FINAL
}
