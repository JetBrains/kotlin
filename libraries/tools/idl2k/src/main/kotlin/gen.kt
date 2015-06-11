package org.jetbrains.idl2k

import java.util.*

private fun Operation.getterOrSetter() = this.attributes.map { it.call }.toSet().let { attributes ->
    when {
        "getter" in attributes -> NativeGetterOrSetter.GETTER
        "setter" in attributes -> NativeGetterOrSetter.SETTER
        else -> NativeGetterOrSetter.NONE
    }
}

fun String.isNullable() = endsWith(")?") || (endsWith("?") && !contains("->"))

fun String.ensureNullable() = when {
    this == "dynamic" -> this
    isNullable() -> this
    contains("->") -> "($this)?"
    else -> "$this?"
}

fun String.dropNullable() = when {
    endsWith(")?") -> this.removeSuffix("?").removeSurrounding("(", ")")
    contains("->") -> this
    endsWith("?") -> this.removeSuffix("?")
    else -> this
}

fun String.copyNullabilityFrom(type: String) = when {
    type.isNullable() -> ensureNullable()
    else -> this
}

fun generateFunction(repository: Repository, function: Operation, functionName: String, nativeGetterOrSetter: NativeGetterOrSetter = function.getterOrSetter()): GenerateFunction =
        function.attributes.map { it.call }.toSet().let { attributes ->
            GenerateFunction(
                    name = functionName,
                    returnType = mapType(repository, function.returnType).let { mapped -> if (nativeGetterOrSetter == NativeGetterOrSetter.GETTER) mapped.ensureNullable() else mapped },
                    arguments = function.parameters.map {
                        GenerateAttribute(
                                name = it.name,
                                type = mapType(repository, it.type),
                                initializer = it.defaultValue,
                                getterSetterNoImpl = false,
                                override = false,
                                readOnly = true,
                                vararg = it.vararg,
                                static = it.static
                        )
                    },
                    nativeGetterOrSetter = nativeGetterOrSetter,
                    static = function.static
            )
        }

fun generateFunctions(repository: Repository, function: Operation): List<GenerateFunction> {
    val realFunction = if (function.name == "") null else generateFunction(repository, function, function.name, NativeGetterOrSetter.NONE)
    val getterOrSetterFunction = when (function.getterOrSetter()) {
        NativeGetterOrSetter.NONE -> null
        NativeGetterOrSetter.GETTER -> generateFunction(repository, function, "get")
        NativeGetterOrSetter.SETTER -> generateFunction(repository, function, "set")
    }
    val callbackArgumentsAsLambdas = function.parameters.map {
        val interfaceType = repository.interfaces[it.type.dropNullable()]
        when {
            interfaceType == null -> it
            interfaceType.operations.size() != 1 -> it
            interfaceType.callback -> interfaceType.operations.single().let { callbackFunction ->
                it.copy(type = callbackFunction.parameters
                        .map { it.copy(type = mapType(repository, it.type)) }
                        .map { it.formatFunctionTypePart() }
                        .join(", ", "(", ") -> ${mapType(repository, callbackFunction.returnType)}")
                        .copyNullabilityFrom(it.type))
            }
            else -> it
        }
    }

    val functionWithCallbackOrNull = when {
        callbackArgumentsAsLambdas == function.parameters -> null
        realFunction != null -> generateFunction(repository, function.copy(parameters = callbackArgumentsAsLambdas), function.name, NativeGetterOrSetter.NONE)
        else -> null
    }

    return listOf(realFunction, getterOrSetterFunction, functionWithCallbackOrNull).filterNotNull()
}

fun generateAttribute(putNoImpl: Boolean, repository: Repository, attribute: Attribute): GenerateAttribute =
        GenerateAttribute(attribute.name,
                type = mapType(repository, attribute.type),
                initializer = attribute.defaultValue,
                getterSetterNoImpl = putNoImpl,
                readOnly = attribute.readOnly,
                override = false,
                vararg = attribute.vararg,
                static = attribute.static
        )

private fun InterfaceDefinition.superTypes(repository: Repository) = superTypes.map { repository.interfaces[it] }.filterNotNull()
private fun resolveDefinitionKind(repository: Repository, iface: InterfaceDefinition, constructors: List<ExtendedAttribute> = iface.findConstructors()): GenerateDefinitionKind =
        if (iface.dictionary || constructors.isNotEmpty() || iface.superTypes(repository).any { resolveDefinitionKind(repository, it) == GenerateDefinitionKind.CLASS }) {
            GenerateDefinitionKind.CLASS
        } else {
            GenerateDefinitionKind.TRAIT
        }

private fun InterfaceDefinition.mapAttributes(repository: Repository) = attributes.map { generateAttribute(!dictionary, repository, it) }
private fun InterfaceDefinition.mapOperations(repository: Repository) = operations.flatMap { generateFunctions(repository, it) }
private fun Constant.mapConstant(repository : Repository) = GenerateAttribute(name, mapType(repository, type), value, false, true, false, false, true)
private fun emptyConstructor() = ExtendedAttribute(null, "Constructor", emptyList())

fun generateTrait(repository: Repository, iface: InterfaceDefinition): GenerateTraitOrClass {
    val superClasses = iface.superTypes
            .map { repository.interfaces[it] }
            .filterNotNull()
            .filter { resolveDefinitionKind(repository, it) == GenerateDefinitionKind.CLASS }

    assert(superClasses.size() <= 1) { "Type ${iface.name} should have one or zero super classes but found ${superClasses.map { it.name }}" }
    val superClass = superClasses.singleOrNull()
    val superConstructor = superClass?.findConstructors()?.firstOrNull()

    val declaredConstructors = iface.findConstructors()
    val entityKind = resolveDefinitionKind(repository, iface, declaredConstructors)
    val extensions = repository.externals[iface.name]?.map { repository.interfaces[it] }?.filterNotNull() ?: emptyList()

    val primaryConstructor = when {
        declaredConstructors.size() == 1 -> declaredConstructors.single()
        declaredConstructors.isEmpty() && entityKind == GenerateDefinitionKind.CLASS -> emptyConstructor()
        else -> declaredConstructors.firstOrNull { it.arguments.isEmpty() }
    }
    val secondaryConstructors = declaredConstructors.filter { it != primaryConstructor }

    val primaryConstructorWithCall = if (primaryConstructor != null) {
        val constructorAsFunction = generateConstructorAsFunction(repository, primaryConstructor)
        val superCall = when {
            superClass != null -> superOrPrimaryConstructorCall(constructorAsFunction, superClass.name, superConstructor ?: emptyConstructor())
            else -> null
        }

        ConstructorWithSuperTypeCall(constructorAsFunction, primaryConstructor, superCall)
    } else null

    val secondaryConstructorsWithCall = secondaryConstructors.map { secondaryConstructor ->
        val constructorAsFunction = generateConstructorAsFunction(repository, secondaryConstructor)
        val initCall = when {
            primaryConstructorWithCall != null -> superOrPrimaryConstructorCall(constructorAsFunction, "this", primaryConstructorWithCall.constructorAttribute)
            else -> superOrPrimaryConstructorCall(constructorAsFunction, "super", superConstructor ?: emptyConstructor())
        }

        ConstructorWithSuperTypeCall(constructorAsFunction, secondaryConstructor, initCall)
    }

    return GenerateTraitOrClass(iface.name, iface.namespace, entityKind, iface.superTypes,
            memberAttributes = (iface.mapAttributes(repository) + extensions.flatMap { it.mapAttributes(repository) }).distinct().toList(),
            memberFunctions = (iface.mapOperations(repository) + extensions.flatMap { it.mapOperations(repository) }).distinct().toList(),
            constants = (iface.constants.map { it.mapConstant(repository) } + extensions.flatMap { it.constants.map { it.mapConstant(repository) } }.distinct().toList()),
            primaryConstructor = primaryConstructorWithCall,
            secondaryConstructors = secondaryConstructorsWithCall
    )
}

fun generateConstructorAsFunction(repository: Repository, constructor: ExtendedAttribute) = generateFunction(
        repository,
        Operation("", "Unit", constructor.arguments, emptyList(), false),
        functionName = "",
        nativeGetterOrSetter = NativeGetterOrSetter.NONE)

fun superOrPrimaryConstructorCall(constructorAsFunction: GenerateFunction, superClassName: String, superOrPrimaryConstructor: ExtendedAttribute): GenerateFunctionCall {
    val constructorArgumentNames = constructorAsFunction.arguments.map { it.name }.toSet()
    return GenerateFunctionCall(
            name = superClassName,
            arguments = superOrPrimaryConstructor.arguments.map { arg ->
                if (arg.name in constructorArgumentNames) arg.name else "noImpl"
            }
    )
}

fun mapUnionType(it: UnionType) = GenerateTraitOrClass(
        name = it.name,
        namespace = it.namespace,
        kind = GenerateDefinitionKind.TRAIT,
        superTypes = emptyList(),
        memberAttributes = emptyList(),
        memberFunctions = emptyList(),
        constants = emptyList(),
        primaryConstructor = null,
        secondaryConstructors = emptyList()
)

fun generateUnionTypeTraits(allUnionTypes: Iterable<UnionType>): List<GenerateTraitOrClass> = allUnionTypes.map(::mapUnionType)

fun mapDefinitions(repository: Repository, definitions: Iterable<InterfaceDefinition>) =
        definitions.filter { "NoInterfaceObject" !in it.extendedAttributes.map { it.call } }.map { generateTrait(repository, it) }

fun generateUnions(ifaces: List<GenerateTraitOrClass>, typedefs: Iterable<TypedefDefinition>): GenerateUnionTypes {
    val declaredTypes = ifaces.toMap { it.name }

    val anonymousUnionTypes = collectUnionTypes(declaredTypes)
    val anonymousUnionTypeTraits = generateUnionTypeTraits(anonymousUnionTypes)
    val anonymousUnionsMap = anonymousUnionTypeTraits.toMap { it.name }

    val typedefsToBeGenerated = typedefs.filter { it.types.startsWith("Union<") }
            .map { NamedValue(it.name, UnionType(it.namespace, splitUnionType(it.types))) }
            .filter { it.value.memberTypes.all { type -> type in declaredTypes } }
    val typedefsMarkersMap = typedefsToBeGenerated.groupBy { it.name }.mapValues { mapUnionType(it.value.first().value).copy(name = it.key) }

    val typeNamesToUnions = anonymousUnionTypes.flatMap { unionType -> unionType.memberTypes.map { unionMember -> unionMember to unionType.name } }.toMultiMap() merge
            typedefsToBeGenerated.flatMap { typedef -> typedef.value.memberTypes.map { unionMember -> unionMember to typedef.name } }.toMultiMap()

    return GenerateUnionTypes(
            typeNamesToUnionsMap = typeNamesToUnions,
            anonymousUnionsMap = anonymousUnionsMap,
            typedefsMarkersMap = typedefsMarkersMap
    )
}
