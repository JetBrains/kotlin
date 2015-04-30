package org.jetbrains.idl2k

import java.math.BigInteger
import java.util.*
import kotlin.support.AbstractIterator

private fun Operation.getterOrSetter() = this.attributes.map { it.call }.toSet().let { attributes ->
    when {
        "getter" in attributes -> NativeGetterOrSetter.GETTER
        "setter" in attributes -> NativeGetterOrSetter.SETTER
        else -> NativeGetterOrSetter.NONE
    }
}

private fun String.ensureNullable() = if (this.endsWith("?") || this == "dynamic") this else this + "?"

fun generateFunction1(repository: Repository, function: Operation, functionName: String, nativeGetterOrSetter: Boolean): GenerateFunction =
        function.attributes.map { it.call }.toSet().let { attributes ->
            GenerateFunction(
                    name = functionName,
                    returnType = mapType(repository, function.returnType).let { mapped -> if (nativeGetterOrSetter) mapped.ensureNullable() else mapped },
                    arguments = function.parameters.map {
                        GenerateAttribute(
                                name = it.name,
                                type = mapType(repository, it.type),
                                initializer = it.defaultValue,
                                getterSetterNoImpl = false,
                                override = false,
                                readOnly = true,
                                vararg = it.vararg
                        )
                    },
                    native = if (nativeGetterOrSetter) function.getterOrSetter() else NativeGetterOrSetter.NONE
            )
        }

fun generateFunction(repository: Repository, function: Operation): List<GenerateFunction> {
    val realFunction = if (function.name == "") null else generateFunction1(repository, function, function.name, false)
    val getterOrSetterFunction = when (function.getterOrSetter()) {
        NativeGetterOrSetter.NONE -> null
        NativeGetterOrSetter.GETTER -> generateFunction1(repository, function, "get", true)
        NativeGetterOrSetter.SETTER -> generateFunction1(repository, function, "set", true)
    }

    return listOf(realFunction, getterOrSetterFunction).filterNotNull()
}

fun generateAttribute(putNoImpl: Boolean, repository: Repository, attribute: Attribute): GenerateAttribute =
        GenerateAttribute(attribute.name,
                type = mapType(repository, attribute.type),
                initializer = attribute.defaultValue,
                getterSetterNoImpl = putNoImpl,
                readOnly = attribute.readOnly,
                override = false,
                vararg = attribute.vararg
        )

private fun InterfaceDefinition.findConstructor() = extendedAttributes.firstOrNull { it.call == "Constructor" }

private fun InterfaceDefinition.superTypes(repository: Repository) = superTypes.map { repository.interfaces[it] }.filterNotNull()
private fun resolveDefinitionType(repository: Repository, iface: InterfaceDefinition, constructor: ExtendedAttribute? = findConstructorAttribute(iface)): GenerateDefinitionType =
        if (iface.dictionary || constructor != null || iface.superTypes(repository).any { resolveDefinitionType(repository, it) == GenerateDefinitionType.CLASS }) GenerateDefinitionType.CLASS
        else GenerateDefinitionType.TRAIT

private fun InterfaceDefinition.mapAttributes(repository: Repository) = attributes.map { generateAttribute(!dictionary, repository, it) }
private fun InterfaceDefinition.mapOperations(repository: Repository) = operations.flatMap { generateFunction(repository, it) }
private fun Constant.mapConstant(repository : Repository) = GenerateAttribute(name, mapType(repository, type), value, false, true, false, false)

fun generateTrait(repository: Repository, iface: InterfaceDefinition): GenerateTraitOrClass {
    val constructor = iface.findConstructor()
    val parentTypes = iface.superTypes.map { repository.interfaces[it] }.filterNotNull().map { it to resolveDefinitionType(repository, it) }
    val constructorFunction = generateFunction1(repository, Operation("", "Unit", constructor?.arguments ?: emptyList(), emptyList()), "", false)
    val constructorArgumentNames = constructorFunction.arguments.map { it.name }.toSet()

    val constructorSuperCalls = parentTypes.filter { it.second == GenerateDefinitionType.CLASS }.map { it.first }.map {
        val superConstructor = it.findConstructor()
        GenerateFunctionCall(name = it.name,
                arguments = if (superConstructor == null) emptyList() else
                    superConstructor.arguments.map { arg ->
                        if (arg.name in constructorArgumentNames) arg.name
                        else "noImpl"
                    }
        )
    }

    val entityType = resolveDefinitionType(repository, iface, constructor)
    val extensions = repository.externals[iface.name]?.map { repository.interfaces[it] }?.filterNotNull() ?: emptyList()

    return GenerateTraitOrClass(iface.name, entityType, iface.superTypes,
            memberAttributes = (iface.mapAttributes(repository) + extensions.flatMap { it.mapAttributes(repository) }).distinct().toList(),
            memberFunctions = (iface.mapOperations(repository) + extensions.flatMap { it.mapOperations(repository) }).distinct().toList(),
            constnats = (iface.constants.map {it.mapConstant(repository)} + extensions.flatMap { it.constants.map {it.mapConstant(repository)} }.distinct().toList()),
            constructor = constructorFunction,
            superConstructorCalls = constructorSuperCalls
    )
}

private fun splitUnionType(unionType: String) =
        unionType.split("[>]*,(Union<)*".toRegex()).toList().filter { it != "" }.map { it.removePrefix("Union<").replaceAll(">*$", "") }.distinct()

private fun GenerateFunction?.allTypes() = if (this != null) sequenceOf(returnType) + arguments.asSequence().map { it.type } else emptySequence()

class UnionType(types : Collection<String>) {
    val types = HashSet(types)
    val name = "Union${this.types.sort().joinToString("Or")}"

    fun contains(type : String) = type in types

    override fun equals(other: Any?): Boolean = other is UnionType && name == other.name
    override fun hashCode(): Int = name.hashCode()
    override fun toString(): String = name
}

private fun collectUnionTypes(allTypes: Map<String, GenerateTraitOrClass>) =
        allTypes.values().asSequence()
                .flatMap {
                    it.constructor.allTypes() +
                            it.memberAttributes.asSequence().map { it.type } +
                            it.memberFunctions.asSequence().flatMap { it.allTypes() }
                }
                .filter { it.startsWith("Union<") }
                .map { splitUnionType(it) }
                .filter { it.all { unionMember -> unionMember in allTypes } }
                .toSet()
                .map { UnionType(it) }

fun mapUnionType(it : UnionType) = GenerateTraitOrClass(
        name = it.name,
        type = GenerateDefinitionType.TRAIT,
        superTypes = emptyList(),
        memberAttributes = emptyList(),
        memberFunctions = emptyList(),
        constnats = emptyList(),
        constructor = null,
        superConstructorCalls = emptyList()
)

fun generateUnionTypeTraits(allUnionTypes : Iterable<UnionType>): List<GenerateTraitOrClass> = allUnionTypes.map(::mapUnionType)

fun mapDefinitions(repository: Repository, definitions: Iterable<InterfaceDefinition>) =
        definitions.filter { "NoInterfaceObject" !in it.extendedAttributes.map { it.call } }.map { generateTrait(repository, it) }


private fun <O: Appendable> O.indent(level : Int) {
    for (i in 1..level) {
        append("    ")
    }
}

private fun <O : Appendable> O.renderAttributeDeclaration(allTypes: Set<String>, arg: GenerateAttribute, override: Boolean, level : Int = 1) {
    indent(level)

    if (override) {
        append("override ")
    }

    append(if (arg.readOnly) "val" else "var")
    append(" ")
    append(arg.name)
    append(" : ")
    append(arg.type.mapUnknownType(allTypes))
    if (arg.initializer != null) {
        append(" = ")
        append(arg.initializer)
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


fun GenerateTraitOrClass.allSuperTypes(all: Map<String, GenerateTraitOrClass>) = HashSet<GenerateTraitOrClass>().let { result -> allSuperTypesImpl(listOf(this), all, result); result.toList() }

tailRecursive
private fun allSuperTypesImpl(roots: List<GenerateTraitOrClass>, all: Map<String, GenerateTraitOrClass>, result: HashSet<GenerateTraitOrClass>) {
    if (roots.isNotEmpty()) {
        allSuperTypesImpl(roots.flatMap { it.superTypes }.map { all[it] }.filterNotNull().filter { result.add(it) }, all, result)
    }
}

private fun String.mapUnknownType(allTypes: Set<String>, standardTypes: Set<String> = typeMapper.values().toSet()): String =
        if (this.endsWith("?")) (this.substring(0, length() - 1).mapUnknownType(allTypes, standardTypes) + "?").replace("dynamic?", "dynamic")
        else if (this.startsWith("Union<")) UnionType(splitUnionType(this)).name.mapUnknownType(allTypes, standardTypes)
        else if (this in allTypes || this in standardTypes) this else "dynamic"

private fun String.parse() = if (this.startsWith("0x")) BigInteger(this.substring(2), 16) else BigInteger(this)
private fun String.replaceInitializer(type: String) = if (this == "noImpl" || type == "Int" && parse() > BigInteger.valueOf(Int.MAX_VALUE.toLong())) "noImpl" else this

private fun <O : Appendable> O.renderArgumentsDeclaration(allTypes: Set<String>, args: List<GenerateAttribute>, omitDefaults: Boolean) =
        args.map { "${if (it.vararg) "vararg " else ""}${it.name} : ${it.type.mapUnknownType(allTypes)}${if (omitDefaults || it.initializer == null) "" else " = ${it.initializer.replaceInitializer(it.type)}"}" }.joinTo(this, ", ")

private fun renderCall(call: GenerateFunctionCall) = "${call.name}(${call.arguments.join(", ")})"

private fun <O : Appendable> O.renderFunction(allTypes: Set<String>, f: GenerateFunction, override: Boolean) {
    indent(1)

    when (f.native) {
        NativeGetterOrSetter.GETTER -> append("nativeGetter ")
        NativeGetterOrSetter.GETTER -> append("nativeSetter ")
    }

    if (override) {
        append("override ")
    }

    append("fun ${f.name}(")
    renderArgumentsDeclaration(allTypes, f.arguments, override)
    appendln(") : ${f.returnType.mapUnknownType(allTypes)} = noImpl")
}

fun <O : Appendable> O.render(allTypes: Map<String, GenerateTraitOrClass>, classesToUnions : Map<String, List<String>>, iface: GenerateTraitOrClass, markerAnnotation : Boolean = false) {
    val superTypes = iface.allSuperTypes(allTypes).filter { it.name != "" }
    val superTypesNames = superTypes.map { it.name }.toSet()

    append("native ")
    if (markerAnnotation) {
        append("marker ")
    }
    when (iface.type) {
        GenerateDefinitionType.CLASS -> append("open class ")
        GenerateDefinitionType.TRAIT -> append("trait ")
    }

    append(iface.name)
    if (iface.constructor != null && iface.constructor.arguments.isNotEmpty()) {
        append("(")
        renderArgumentsDeclaration(allTypes.keySet(), iface.constructor.arguments, false)
        append(")")
    }

    val superCalls = iface.superConstructorCalls.map { it.name }.toSet()
    val superTypesWithCalls =
            iface.superConstructorCalls.filter { it.name in superTypesNames }.map { renderCall(it) } +
                    iface.superTypes.filter { it !in superCalls && it in superTypesNames } +
                    (classesToUnions[iface.name] ?: emptyList())

    if (superTypesWithCalls.isNotEmpty()) {
        superTypesWithCalls.joinTo(this, ", ", " : ")
    }

    appendln (" {")

    val superAttributes = superTypes.flatMap { it.memberAttributes }.distinct()
    val superFunctions = superTypes.flatMap { it.memberFunctions }.distinct()
    val superProtos = superAttributes.map { it.proto } merge superFunctions.map { it.proto }

    iface.memberAttributes.filter { it !in superAttributes }.forEach { arg ->
        renderAttributeDeclaration(allTypes.keySet(), arg, arg.proto in superProtos)
    }
    iface.memberFunctions.filter { it !in superFunctions }.forEach {
        renderFunction(allTypes.keySet(), it, it.proto in superProtos)
    }
    if (iface.constnats.isNotEmpty()) {
        indent(1)
        appendln("companion object {")
        iface.constnats.forEach {
            renderAttributeDeclaration(allTypes.keySet(), it, override = false, level = 2)
        }
        indent(1)
        appendln("}")
    }

    appendln("}")
}

fun <O : Appendable> O.render(ifaces: List<GenerateTraitOrClass>, typedefs : Iterable<TypedefDefinition>) {
    val all = ifaces.groupBy { it.name }.mapValues { it.getValue().single() }
    val unionTypes = collectUnionTypes(all)
    val unionTypeTraits = generateUnionTypeTraits(unionTypes)
    val allUnions = unionTypeTraits.groupBy { it.name }.mapValues { it.getValue().single() }
    val typedefsToBeGenerated = typedefs.filter {it.types.startsWith("Union<")}
            .map { UnionType(splitUnionType(it.types)) to it.name }
            .filter { it.first.types.all { type -> type in all} }
    val typedefsClasses = typedefsToBeGenerated.groupBy { it.second }.mapValues { mapUnionType(it.value.first().first).copy(name = it.key) }

    val classesToUnions = unionTypes.flatMap { unionType -> unionType.types.map { it to unionType } }.groupBy { it.first }.mapValues { it.getValue().map { it.second.name} } +
        typedefsToBeGenerated.flatMap { typedef -> typedef.first.types.map { it to typedef.second }  }.groupBy { it.first }.mapValues { it.getValue().map {it.second} }

    val allTypes = all + allUnions + typedefsClasses
    ifaces.forEach {
        render(allTypes, classesToUnions, it)
    }

    unionTypeTraits.forEach {
        render(allTypes, emptyMap(), it, markerAnnotation = true)
    }

    typedefsClasses.values().forEach {
        render(allTypes, emptyMap(), it, markerAnnotation = true)
    }
}
