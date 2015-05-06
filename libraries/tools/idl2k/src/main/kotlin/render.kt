package org.jetbrains.idl2k

import java.math.BigInteger

private fun <O: Appendable> O.indent(level : Int) {
    for (i in 1..level) {
        append("    ")
    }
}

private fun Appendable.renderAttributeDeclaration(allTypes: Set<String>, arg: GenerateAttribute, override: Boolean, level : Int = 1) {
    indent(level)

    if (override) {
        append("override ")
    }

    append(if (arg.readOnly) "val" else "var")
    append(" ")
    append(arg.name)
    append(": ")
    append(arg.type.dynamicIfUnknownType(allTypes))
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
private fun String.replaceWrongConstants(type: String) = if (this == "noImpl" || type == "Int" && parse() > BigInteger.valueOf(Int.MAX_VALUE.toLong())) "noImpl" else this
private fun String.replaceKeywords() = if (this in keywords) this + "_" else this

private fun Appendable.renderArgumentsDeclaration(allTypes: Set<String>, args: List<GenerateAttribute>, omitDefaults: Boolean) =
        args.map {
            StringBuilder {
                if (it.vararg) {
                    append("vararg ")
                }
                append(it.name.replaceKeywords())
                append(": ")
                append(it.type.dynamicIfUnknownType(allTypes))
                if (!omitDefaults && it.initializer != null && it.initializer != "") {
                    append(" = ")
                    append(it.initializer.replaceWrongConstants(it.type))
                }
            }
        }.joinTo(this, ", ")

private fun renderCall(call: GenerateFunctionCall) = "${call.name.replaceKeywords()}(${call.arguments.map {it.replaceKeywords()}.join(", ")})"

private fun Appendable.renderFunctionDeclaration(allTypes: Set<String>, f: GenerateFunction, override: Boolean) {
    indent(1)

    when (f.nativeGetterOrSetter) {
        NativeGetterOrSetter.GETTER -> append("nativeGetter ")
        NativeGetterOrSetter.GETTER -> append("nativeSetter ")
    }

    if (override) {
        append("override ")
    }

    if (f.name in keywords) {
        append("native(\"${f.name}\") ")
    }
    append("fun ${f.name.replaceKeywords()}(")
    renderArgumentsDeclaration(allTypes, f.arguments, override)
    appendln("): ${f.returnType.dynamicIfUnknownType(allTypes)} = noImpl")
}

fun Appendable.render(allTypes: Map<String, GenerateTraitOrClass>, typeNamesToUnions: Map<String, List<String>>, iface: GenerateTraitOrClass, markerAnnotation : Boolean = false) {
    append("native ")
    if (markerAnnotation) {
        append("marker ")
    }
    when (iface.kind) {
        GenerateDefinitionKind.CLASS -> append("open class ")
        GenerateDefinitionKind.TRAIT -> append("trait ")
    }

    append(iface.name)
    if (iface.constructor != null && iface.constructor.arguments.isNotEmpty()) {
        append("(")
        renderArgumentsDeclaration(allTypes.keySet(), iface.constructor.arguments, false)
        append(")")
    }

    val allSuperTypes = iface.allSuperTypes(allTypes)
    val allSuperTypesNames = allSuperTypes.map { it.name }.toSet()

    val superCalls = iface.superConstructorCalls.map { it.name }.toSet()
    val superTypesWithCalls =
            iface.superConstructorCalls.map { renderCall(it) } +
                    iface.superTypes.filter { it !in superCalls && it in allSuperTypesNames } +
                    (typeNamesToUnions[iface.name] ?: emptyList())

    if (superTypesWithCalls.isNotEmpty()) {
        superTypesWithCalls.joinTo(this, ", ", " : ")
    }

    appendln (" {")

    val superAttributes = allSuperTypes.flatMap { it.memberAttributes }.distinct()
    val superFunctions = allSuperTypes.flatMap { it.memberFunctions }.distinct()
    val superSignatures = superAttributes.map { it.signature } merge superFunctions.map { it.signature }

    iface.memberAttributes.filter { it !in superAttributes }.forEach { arg ->
        renderAttributeDeclaration(allTypes.keySet(), arg, arg.signature in superSignatures)
    }
    iface.memberFunctions.filter { it !in superFunctions }.forEach {
        renderFunctionDeclaration(allTypes.keySet(), it, it.signature in superSignatures)
    }
    if (iface.constants.isNotEmpty()) {
        appendln()
        indent(1)
        appendln("companion object {")
        iface.constants.forEach {
            renderAttributeDeclaration(allTypes.keySet(), it, override = false, level = 2)
        }
        indent(1)
        appendln("}")
    }

    appendln("}")
    appendln()
}

fun <K, V> List<Pair<K, V>>.toMultiMap() : Map<K, List<V>> = groupBy { it.first }.mapValues { it.value.map { it.second } }

fun Appendable.render(namespace : String, ifaces: List<GenerateTraitOrClass>, typedefs : Iterable<TypedefDefinition>) {
    val declaredTypes = ifaces.toMap { it.name }

    val anonymousUnionTypes = collectUnionTypes(declaredTypes)
    val anonymousUnionTypeTraits = generateUnionTypeTraits(anonymousUnionTypes)
    val anonymousUnionsMap = anonymousUnionTypeTraits.toMap { it.name }

    val typedefsToBeGenerated = typedefs.filter {it.types.startsWith("Union<")}
            .filter {it.namespace == namespace}
            .map { NamedValue(it.name, UnionType(namespace, splitUnionType(it.types))) }
            .filter { it.value.memberTypes.all { type -> type in declaredTypes} }
    val typedefsMarkerTraits = typedefsToBeGenerated.groupBy { it.name }.mapValues { mapUnionType(it.value.first().value).copy(name = it.key) }

    // TODO better name, extract duplication
    val typeNamesToUnions = anonymousUnionTypes.flatMap { unionType -> unionType.memberTypes.map { unionMember -> unionMember to unionType.name } }.toMultiMap()
            typedefsToBeGenerated.flatMap { typedef -> typedef.value.memberTypes.map { unionMember -> unionMember to typedef.name } }.toMultiMap()

    val allTypes = declaredTypes + anonymousUnionsMap + typedefsMarkerTraits
    declaredTypes.values().filter {it.namespace == namespace}.forEach {
        render(allTypes, typeNamesToUnions, it)
    }

    anonymousUnionTypeTraits.forEach {
        render(allTypes, emptyMap(), it, markerAnnotation = true)
    }

    typedefsMarkerTraits.values().forEach {
        render(allTypes, emptyMap(), it, markerAnnotation = true)
    }
}

