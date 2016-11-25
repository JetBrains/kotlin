package org.jetbrains.idl2k

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
        appendln("get() = noImpl")
    }
    if (arg.setterNoImpl) {
        indent(commented, level + 1)
        appendln("set(value) = noImpl")
    }
}

private val keywords = setOf("interface", "is", "as")

private fun String.parse() = if (this.startsWith("0x")) BigInteger(this.substring(2), 16) else BigInteger(this)
private fun String.replaceWrongConstants(type: Type) = when {
    this == "noImpl" || type is SimpleType && type.type == "Int" && parse() > BigInteger.valueOf(Int.MAX_VALUE.toLong()) -> "noImpl"
    type is SimpleType && type.type == "Double" && this.matches("[0-9]+".toRegex()) -> "${this}.0"
    else -> this
}
private fun String.replaceKeywords() = if (this in keywords) this + "_" else this

private fun Appendable.renderArgumentsDeclaration(args: List<GenerateAttribute>, omitDefaults: Boolean = false) =
        args.joinTo(this, ", ", "(", ")") {
            StringBuilder().apply { renderAttributeDeclaration(it, if (it.override) MemberModality.OVERRIDE else MemberModality.FINAL, omitDefaults) }
        }

private fun renderCall(call: GenerateFunctionCall) = "${call.name.replaceKeywords()}(${call.arguments.joinToString(separator = ", ", transform = String::replaceKeywords)})"

private fun Appendable.renderFunctionDeclaration(f: GenerateFunction, override: Boolean, commented: Boolean, level: Int = 1) {
    indent(commented, level)

    if (f.nativeGetterOrSetter == NativeGetterOrSetter.GETTER
            && !f.returnType.nullable
            && f.returnType != DynamicType) {
        appendln("@Suppress(\"NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE\")")
        indent(commented, level)
    }

    when (f.nativeGetterOrSetter) {
        NativeGetterOrSetter.GETTER -> { appendln("@nativeGetter"); indent(commented, level); append("operator ") }
        NativeGetterOrSetter.SETTER -> { appendln("@nativeSetter"); indent(commented, level); append("operator ") }
        NativeGetterOrSetter.NONE -> {}
    }

    if (override) {
        append("override ")
    }

    if (f.name in keywords) {
        append("@JsName(\"${f.name}\") ")
    }
    append("fun ${f.name.replaceKeywords()}")
    renderArgumentsDeclaration(f.arguments, override)
    appendln(": ${f.returnType.render()} = noImpl")
}

private fun List<GenerateAttribute>.hasNoVars() = none { it.isVar }

private fun GenerateAttribute.isCommented(parent: String) = "$parent.$name" in commentOutDeclarations || "$parent.$name: ${type.render()}" in commentOutDeclarations
private fun GenerateFunction.isCommented(parent: String) =
        "$parent.$name" in commentOutDeclarations || "$parent.$name(${arguments.size})" in commentOutDeclarations
private fun GenerateAttribute.isRequiredFunctionArgument(owner: String, functionName: String) = "$owner.$functionName.$name" in requiredArguments
private fun GenerateFunction.fixRequiredArguments(parent: String) = copy(arguments = arguments.map { arg -> arg.copy(initializer = if (arg.isRequiredFunctionArgument(parent, name)) null else arg.initializer) })

fun Appendable.render(allTypes: Map<String, GenerateTraitOrClass>, typeNamesToUnions: Map<String, List<String>>, iface: GenerateTraitOrClass, markerAnnotation: Boolean = false) {
    append("public external ")
    if (markerAnnotation) {
        append("@marker ")
    }
    when (iface.kind) {
        GenerateDefinitionKind.CLASS -> append("open class ")
        GenerateDefinitionKind.ABSTRACT_CLASS -> append("abstract class ")
        GenerateDefinitionKind.INTERFACE -> append("interface ")
    }

    val allSuperTypes = iface.allSuperTypes(allTypes)
    val allSuperTypesNames = allSuperTypes.map { it.name }.toSet()

    append(iface.name)
    val primary = iface.primaryConstructor
    if (primary != null && (primary.constructor.arguments.isNotEmpty() || iface.secondaryConstructors.isNotEmpty())) {
        renderArgumentsDeclaration(primary.constructor.fixRequiredArguments(iface.name).arguments.dynamicIfUnknownType(allTypes.keys), false)
    }

    val superTypesExclude = inheritanceExclude[iface.name] ?: emptySet()
    val superCallName = primary?.initTypeCall?.name
    val superTypesWithCalls =
            (primary?.initTypeCall?.let { listOf(renderCall(it)) } ?: emptyList()) +
                    iface.superTypes.filter { it != superCallName && it in allSuperTypesNames }.filter { it !in superTypesExclude } +
                    (typeNamesToUnions[iface.name] ?: emptyList())

    if (superTypesWithCalls.isNotEmpty()) {
        superTypesWithCalls.joinTo(this, ", ", " : ")
    }

    appendln (" {")

    iface.secondaryConstructors.forEach { secondary ->
        indent(false, 1)
        append("constructor")
        renderArgumentsDeclaration(secondary.constructor.fixRequiredArguments(iface.name).arguments.dynamicIfUnknownType(allTypes.keys), false)

        if (secondary.initTypeCall != null) {
            append(" : ")
            append(renderCall(secondary.initTypeCall))
        }

        appendln()
    }

    val superAttributes = allSuperTypes.flatMap { it.memberAttributes }.distinct()
    val superAttributesByName = superAttributes.groupBy { it.name }
    val superFunctions = allSuperTypes.flatMap { it.memberFunctions }.distinct()
    val superSignatures = superAttributes.map { it.signature } merge superFunctions.map { it.signature }

    iface.memberAttributes
        .filter { it !in superAttributes && !it.static && (it.isVar || (it.isVal && superAttributesByName[it.name]?.hasNoVars() ?: true)) }
        .map { it.dynamicIfUnknownType(allTypes.keys) }
        .groupBy { it.name }
        .mapValues { it.value.filter { "${iface.name}.${it.name}" !in commentOutDeclarations && "${iface.name}.${it.name}: ${it.type.render()}" !in commentOutDeclarations  } }
        .filterValues { it.isNotEmpty() }
        .reduceValues(::merge).values.forEach { attribute ->
            val modality = when {
                attribute.signature in superSignatures -> MemberModality.OVERRIDE
                iface.kind == GenerateDefinitionKind.CLASS && attribute.isVal -> MemberModality.OPEN
                iface.kind == GenerateDefinitionKind.ABSTRACT_CLASS -> when {
                    attribute.initializer != null || attribute.getterSetterNoImpl -> MemberModality.OPEN
                    else -> MemberModality.ABSTRACT
                }
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
    iface.memberFunctions.filter { it !in superFunctions && !it.static }.map { it.dynamicIfUnknownType(allTypes.keys) }.groupBy { it.signature }.reduceValues(::betterFunction).values.forEach {
        renderFunctionDeclaration(it.fixRequiredArguments(iface.name), it.signature in superSignatures, commented = it.isCommented(iface.name))
    }

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
            renderFunctionDeclaration(it.fixRequiredArguments(iface.name), override = false, level = 2, commented = it.isCommented(iface.name))
        }
        indent(false, 1)
        appendln("}")
    }

    appendln("}")
    appendln()

    if (iface.generateBuilderFunction) {
        renderBuilderFunction(iface, allSuperTypes, allTypes.keys)
    }
}

fun Appendable.renderBuilderFunction(dictionary: GenerateTraitOrClass, allSuperTypes: List<GenerateTraitOrClass>, allTypes: Set<String>) {
    val fields = (dictionary.memberAttributes + allSuperTypes.flatMap { it.memberAttributes }).distinctBy { it.signature }.map { it.copy(kind = AttributeKind.ARGUMENT) }.dynamicIfUnknownType(allTypes)

    appendln("@Suppress(\"NOTHING_TO_INLINE\")")
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
            a.static
    )
}

fun <K, V> List<Pair<K, V>>.toMultiMap(): Map<K, List<V>> = groupBy { it.first }.mapValues { it.value.map { it.second } }

fun Appendable.render(namespace: String, ifaces: List<GenerateTraitOrClass>, unions : GenerateUnionTypes) {
    val declaredTypes = ifaces.associateBy { it.name }

    val allTypes = declaredTypes + unions.anonymousUnionsMap + unions.typedefsMarkersMap
    declaredTypes.values.filter { it.namespace == namespace }.forEach {
        render(allTypes, unions.typeNamesToUnionsMap, it)
    }

    unions.anonymousUnionsMap.values.filter { it.namespace == "" || it.namespace == namespace }.forEach {
        render(allTypes, emptyMap(), it, markerAnnotation = true)
    }

    unions.typedefsMarkersMap.values.filter { it.namespace == "" || it.namespace == namespace }.forEach {
        render(allTypes, emptyMap(), it, markerAnnotation = true)
    }
}

enum class MemberModality {
    OPEN,
    ABSTRACT,
    OVERRIDE,
    FINAL
}
