package org.jetbrains.idl2k

import java.math.BigInteger

private fun <O : Appendable> O.indent(level: Int) {
    for (i in 1..level) {
        append("    ")
    }
}

private fun Appendable.renderAttributeDeclaration(arg: GenerateAttribute, override: Boolean, open: Boolean, level: Int = 1) {
    indent(level)

    when {
        override -> append("override ")
        open -> append("open ")
    }

    append(if (arg.readOnly) "val" else "var")
    append(" ")
    append(arg.name)
    append(": ")
    append(arg.type.render())
    if (arg.initializer != null) {
        append(" = ")
        append(arg.initializer.replaceWrongConstants(arg.type))
    }

    appendln()
    if (arg.getterNoImpl) {
        indent(level + 1)
        appendln("get() = noImpl")
    }
    if (arg.setterNoImpl) {
        indent(level + 1)
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

private fun Appendable.renderArgumentsDeclaration(args: List<GenerateAttribute>, omitDefaults: Boolean) =
        args.map {
            StringBuilder {
                if (it.vararg) {
                    append("vararg ")
                }
                append(it.name.replaceKeywords())
                append(": ")
                append(it.type.render())
                if (!omitDefaults && it.initializer != null && it.initializer != "") {
                    append(" = ")
                    append(it.initializer.replaceWrongConstants(it.type))
                }
            }
        }.joinTo(this, ", ", "(", ")")

private fun renderCall(call: GenerateFunctionCall) = "${call.name.replaceKeywords()}(${call.arguments.map { it.replaceKeywords() }.join(", ")})"

private fun Appendable.renderFunctionDeclaration(f: GenerateFunction, override: Boolean, level: Int = 1) {
    indent(level)

    when (f.nativeGetterOrSetter) {
        NativeGetterOrSetter.GETTER -> append("nativeGetter ")
        NativeGetterOrSetter.SETTER -> append("nativeSetter ")
        NativeGetterOrSetter.NONE -> {}
    }

    if (override) {
        append("override ")
    }

    if (f.name in keywords) {
        append("native(\"${f.name}\") ")
    }
    append("fun ${f.name.replaceKeywords()}")
    renderArgumentsDeclaration(f.arguments, override)
    appendln(": ${f.returnType.render()} = noImpl")
}

private fun List<GenerateAttribute>.hasNoVars() = none { it.isVar }
private val GenerateAttribute.isVal: Boolean
    get() = readOnly
private val GenerateAttribute.isVar: Boolean
    get() = !readOnly

fun Appendable.render(allTypes: Map<String, GenerateTraitOrClass>, typeNamesToUnions: Map<String, List<String>>, iface: GenerateTraitOrClass, markerAnnotation: Boolean = false) {
    append("native public ")
    if (markerAnnotation) {
        append("marker ")
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
        renderArgumentsDeclaration(primary.constructor.arguments.dynamicIfUnknownType(allTypes.keySet()), false)
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
        indent(1)
        append("constructor")
        renderArgumentsDeclaration(secondary.constructor.arguments.dynamicIfUnknownType(allTypes.keySet()), false)

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
        renderAttributeDeclaration(arg, override = arg.signature in superSignatures, open = iface.kind == GenerateDefinitionKind.CLASS && arg.readOnly)
    }
    iface.memberFunctions.filter { it !in superFunctions && !it.static }.map { it.dynamicIfUnknownType(allTypes.keySet()) }.groupBy { it.signature }.reduceValues(::betterFunction).values().forEach {
        renderFunctionDeclaration(it, it.signature in superSignatures)
    }

    val staticAttributes = iface.memberAttributes.filter { it.static }
    val staticFunctions = iface.memberFunctions.filter { it.static }

    if (iface.constants.isNotEmpty() || staticAttributes.isNotEmpty() || staticFunctions.isNotEmpty()) {
        appendln()
        indent(1)
        appendln("companion object {")
        iface.constants.forEach {
            renderAttributeDeclaration(it, override = false, open = false, level = 2)
        }
        staticAttributes.forEach {
            renderAttributeDeclaration(it, override = false, open = false, level = 2)
        }
        staticFunctions.forEach {
            renderFunctionDeclaration(it, override = false, level = 2)
        }
        indent(1)
        appendln("}")
    }

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

