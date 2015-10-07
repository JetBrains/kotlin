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

private fun Appendable.renderAttributeDeclaration(arg: GenerateAttribute, override: Boolean, open: Boolean, omitDefaults: Boolean = false) {
    when {
        override -> append("override ")
        open -> append("open ")
        arg.vararg -> append("vararg ")
    }

    append(when(arg.kind) {
        AttributeKind.VAL -> "val "
        AttributeKind.VAR -> "var "
        AttributeKind.ARGUMENT -> ""
    })
    append(arg.name.replaceKeywords())
    append(": ")
    append(arg.type.render())
    if (arg.initializer != null && !omitDefaults) {
        append(" = ")
        append(arg.initializer.replaceWrongConstants(arg.type))
    }
}

private fun Appendable.renderAttributeDeclarationAsProperty(arg: GenerateAttribute, override: Boolean, open: Boolean, commented: Boolean, level: Int, omitDefaults: Boolean = false) {
    indent(commented, level)

    renderAttributeDeclaration(arg, override, open, omitDefaults)

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

private val keywords = setOf("interface")

private fun String.parse() = if (this.startsWith("0x")) BigInteger(this.substring(2), 16) else BigInteger(this)
private fun String.replaceWrongConstants(type: Type) = when {
    this == "noImpl" || type is SimpleType && type.type == "Int" && parse() > BigInteger.valueOf(Int.MAX_VALUE.toLong()) -> "noImpl"
    type is SimpleType && type.type == "Double" && this.matches("[0-9]+".toRegex()) -> "${this}.0"
    else -> this
}
private fun String.replaceKeywords() = if (this in keywords) this + "_" else this

private fun Appendable.renderArgumentsDeclaration(args: List<GenerateAttribute>, omitDefaults: Boolean = false) =
        args.map {
            StringBuilder {
                renderAttributeDeclaration(it, it.override, false, omitDefaults)
            }
        }.joinTo(this, ", ", "(", ")")

private fun renderCall(call: GenerateFunctionCall) = "${call.name.replaceKeywords()}(${call.arguments.map { it.replaceKeywords() }.join(", ")})"

private fun Appendable.renderFunctionDeclaration(f: GenerateFunction, override: Boolean, commented: Boolean, level: Int = 1) {
    indent(commented, level)

    when (f.nativeGetterOrSetter) {
        NativeGetterOrSetter.GETTER -> append("@nativeGetter ")
        NativeGetterOrSetter.SETTER -> append("@nativeSetter ")
        NativeGetterOrSetter.NONE -> {}
    }

    if (override) {
        append("override ")
    }

    if (f.name in keywords) {
        append("@native(\"${f.name}\") ")
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
    append("@native public ")
    if (markerAnnotation) {
        append("@marker ")
    }
    when (iface.kind) {
        GenerateDefinitionKind.CLASS -> append("open class ")
        GenerateDefinitionKind.TRAIT -> append("interface ")
    }

    val allSuperTypes = iface.allSuperTypes(allTypes)
    val allSuperTypesNames = allSuperTypes.map { it.name }.toSet()

    append(iface.name)
    val primary = iface.primaryConstructor
    if (primary != null && (primary.constructor.arguments.isNotEmpty() || iface.secondaryConstructors.isNotEmpty())) {
        renderArgumentsDeclaration(primary.constructor.fixRequiredArguments(iface.name).arguments.dynamicIfUnknownType(allTypes.keySet()), false)
    }

    val superCallName = primary?.initTypeCall?.name
    val superTypesWithCalls =
            (primary?.initTypeCall?.let { listOf(renderCall(it)) } ?: emptyList()) +
                    iface.superTypes.filter { it != superCallName && it in allSuperTypesNames } +
                    (typeNamesToUnions[iface.name] ?: emptyList())

    if (superTypesWithCalls.isNotEmpty()) {
        superTypesWithCalls.joinTo(this, ", ", " : ")
    }

    appendln (" {")

    iface.secondaryConstructors.forEach { secondary ->
        indent(false, 1)
        append("constructor")
        renderArgumentsDeclaration(secondary.constructor.fixRequiredArguments(iface.name).arguments.dynamicIfUnknownType(allTypes.keySet()), false)

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
            .map { it.dynamicIfUnknownType(allTypes.keySet()) }
            .groupBy { it.signature }.reduceValues().values().forEach { arg ->
        renderAttributeDeclarationAsProperty(arg,
                override = arg.signature in superSignatures,
                open = iface.kind == GenerateDefinitionKind.CLASS && arg.isVal,
                commented = arg.isCommented(iface.name),
                omitDefaults = iface.kind == GenerateDefinitionKind.TRAIT,
                level = 1
        )
    }
    iface.memberFunctions.filter { it !in superFunctions && !it.static }.map { it.dynamicIfUnknownType(allTypes.keySet()) }.groupBy { it.signature }.reduceValues(::betterFunction).values().forEach {
        renderFunctionDeclaration(it.fixRequiredArguments(iface.name), it.signature in superSignatures, commented = it.isCommented(iface.name))
    }

    val staticAttributes = iface.memberAttributes.filter { it.static }
    val staticFunctions = iface.memberFunctions.filter { it.static }

    if (iface.constants.isNotEmpty() || staticAttributes.isNotEmpty() || staticFunctions.isNotEmpty()) {
        appendln()
        indent(false, 1)
        appendln("companion object {")
        iface.constants.forEach {
            renderAttributeDeclarationAsProperty(it, override = false, open = false, level = 2, commented = it.isCommented(iface.name))
        }
        staticAttributes.forEach {
            renderAttributeDeclarationAsProperty(it, override = false, open = false, level = 2, commented = it.isCommented(iface.name))
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
        renderBuilderFunction(iface, allSuperTypes, allTypes.keySet())
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
                        .map { it.first.copy(type = it.map { it.type }.betterType(), name = it.map { it.name }.betterName()) }
        )

private fun <F, T> Pair<F, F>.map(block: (F) -> T) = block(first) to block(second)
private fun Pair<Type, Type>.betterType() = if (first is DynamicType || first is AnyType) first else second
private fun Pair<String, String>.betterName() = if (((0..9).map { it.toString() } + listOf("arg")).none { first.toLowerCase().contains(it) }) first else second

fun <K, V> List<Pair<K, V>>.toMultiMap(): Map<K, List<V>> = groupBy { it.first }.mapValues { it.value.map { it.second } }

fun Appendable.render(namespace: String, ifaces: List<GenerateTraitOrClass>, unions : GenerateUnionTypes) {
    val declaredTypes = ifaces.toMap { it.name }

    val allTypes = declaredTypes + unions.anonymousUnionsMap + unions.typedefsMarkersMap
    declaredTypes.values().filter { it.namespace == namespace }.forEach {
        render(allTypes, unions.typeNamesToUnionsMap, it)
    }

    unions.anonymousUnionsMap.values().filter { it.namespace == "" || it.namespace == namespace }.forEach {
        render(allTypes, emptyMap(), it, markerAnnotation = true)
    }

    unions.typedefsMarkersMap.values().filter { it.namespace == "" || it.namespace == namespace }.forEach {
        render(allTypes, emptyMap(), it, markerAnnotation = true)
    }
}

