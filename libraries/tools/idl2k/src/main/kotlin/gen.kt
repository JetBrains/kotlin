package org.jetbrains.idl2k

private fun Operation.getterOrSetter() = this.attributes.map { it.call }.toSet().let { attributes ->
    when {
        "getter" in attributes -> NativeGetterOrSetter.GETTER
        "setter" in attributes -> NativeGetterOrSetter.SETTER
        else -> NativeGetterOrSetter.NONE
    }
}

fun generateFunction(repository: Repository, function: Operation, functionName: String, nativeGetterOrSetter: NativeGetterOrSetter = function.getterOrSetter()): GenerateFunction =
        function.attributes.map { it.call }.toSet().let { attributes ->
            GenerateFunction(
                    name = functionName,
                    returnType = mapType(repository, function.returnType).let { mapped -> if (nativeGetterOrSetter == NativeGetterOrSetter.GETTER) mapped.toNullable() else mapped },
                    arguments = function.parameters.map {
                        GenerateAttribute(
                                name = it.name,
                                type = mapType(repository, it.type),
                                initializer = it.defaultValue,
                                getterSetterNoImpl = false,
                                override = false,
                                kind = AttributeKind.ARGUMENT,
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
        val parameterType = mapType(repository, it.type) as? SimpleType
        val interfaceType = repository.interfaces[parameterType?.type]
        when {
            interfaceType == null -> it
            interfaceType.operations.size != 1 -> it
            interfaceType.callback -> interfaceType.operations.single().let { callbackFunction ->
                it.copy(type = FunctionType(callbackFunction.parameters.map { it.copy(type = mapType(repository, it.type)) }, mapType(repository, callbackFunction.returnType), parameterType?.nullable ?: false))
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
                kind = if (attribute.readOnly) AttributeKind.VAL else AttributeKind.VAR,
                override = false,
                vararg = attribute.vararg,
                static = attribute.static
        )

private fun InterfaceDefinition.superTypes(repository: Repository) = superTypes.map { repository.interfaces[it] }.filterNotNull()
private fun resolveDefinitionKind(repository: Repository, iface: InterfaceDefinition, constructors: List<ExtendedAttribute> = iface.findConstructors()): GenerateDefinitionKind =
        if (constructors.isNotEmpty() || iface.superTypes(repository).any { resolveDefinitionKind(repository, it) == GenerateDefinitionKind.CLASS }) {
            GenerateDefinitionKind.CLASS
        } else {
            GenerateDefinitionKind.TRAIT
        }

private fun InterfaceDefinition.mapAttributes(repository: Repository) = attributes.map { generateAttribute(!dictionary, repository, it) }
private fun InterfaceDefinition.mapOperations(repository: Repository) = operations.flatMap { generateFunctions(repository, it) }
private fun Constant.mapConstant(repository : Repository) = GenerateAttribute(name, mapType(repository, type), value, false, AttributeKind.VAL, false, false, true)
private val EMPTY_CONSTRUCTOR = ExtendedAttribute(null, "Constructor", emptyList())

fun generateTrait(repository: Repository, iface: InterfaceDefinition): GenerateTraitOrClass {
    val superClasses = iface.superTypes
            .map { repository.interfaces[it] }
            .filterNotNull()
            .filter { resolveDefinitionKind(repository, it) == GenerateDefinitionKind.CLASS }

    assert(superClasses.size <= 1) { "Type ${iface.name} should have one or zero super classes but found ${superClasses.map { it.name }}" }
    val superClass = superClasses.singleOrNull()
    val superConstructor = superClass?.findConstructors()?.firstOrNull() ?: EMPTY_CONSTRUCTOR

    val declaredConstructors = iface.findConstructors()
    val entityKind = resolveDefinitionKind(repository, iface, declaredConstructors)
    val extensions = repository.externals[iface.name]?.map { repository.interfaces[it] }?.filterNotNull() ?: emptyList()

    val primaryConstructor = when {
        declaredConstructors.size == 1 -> declaredConstructors.single()
        declaredConstructors.isEmpty() && entityKind == GenerateDefinitionKind.CLASS -> EMPTY_CONSTRUCTOR
        else -> declaredConstructors.firstOrNull { it.arguments.isEmpty() }
    }
    val secondaryConstructors = declaredConstructors.filter { it != primaryConstructor }

    val primaryConstructorWithCall = primaryConstructor?.let { constructor ->
        val constructorAsFunction = generateConstructorAsFunction(repository, constructor)
        val superCall = when {
            superClass != null -> superOrPrimaryConstructorCall(constructorAsFunction, superClass.name, superConstructor)
            else -> null
        }

        ConstructorWithSuperTypeCall(constructorAsFunction, constructor, superCall)
    }

    val secondaryConstructorsWithCall = secondaryConstructors.map { secondaryConstructor ->
        val constructorAsFunction = generateConstructorAsFunction(repository, secondaryConstructor)
        val initCall = when {
            primaryConstructorWithCall != null -> superOrPrimaryConstructorCall(constructorAsFunction, "this", primaryConstructorWithCall.constructorAttribute)
            superClass != null -> superOrPrimaryConstructorCall(constructorAsFunction, "super", superConstructor)
            else -> null
        }

        ConstructorWithSuperTypeCall(constructorAsFunction, secondaryConstructor, initCall)
    }

    return GenerateTraitOrClass(iface.name, iface.namespace, entityKind, iface.superTypes,
            memberAttributes = (iface.mapAttributes(repository) + extensions.flatMap { it.mapAttributes(repository) }).distinct().toList(),
            memberFunctions = (iface.mapOperations(repository) + extensions.flatMap { it.mapOperations(repository) }).distinct().toList(),
            constants = (iface.constants.map { it.mapConstant(repository) } + extensions.flatMap { it.constants.map { it.mapConstant(repository) } }.distinct().toList()),
            primaryConstructor = primaryConstructorWithCall,
            secondaryConstructors = secondaryConstructorsWithCall,
            generateBuilderFunction = iface.dictionary
    )
}

fun generateConstructorAsFunction(repository: Repository, constructor: ExtendedAttribute) = generateFunction(
        repository,
        Operation("constructor", UnitType, constructor.arguments, emptyList(), false),
        functionName = "constructor",
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
        secondaryConstructors = emptyList(),
        generateBuilderFunction = false
)

fun generateUnionTypeTraits(allUnionTypes: Sequence<UnionType>): Sequence<GenerateTraitOrClass> = allUnionTypes.map(::mapUnionType)

fun mapDefinitions(repository: Repository, definitions: Iterable<InterfaceDefinition>) =
        definitions.filter { "NoInterfaceObject" !in it.extendedAttributes.map { it.call } }.map { generateTrait(repository, it) }

fun generateUnions(ifaces: List<GenerateTraitOrClass>, typedefs: Iterable<TypedefDefinition>): GenerateUnionTypes {
    val declaredTypes = ifaces.toMap { it.name }

    val anonymousUnionTypes = collectUnionTypes(declaredTypes)
    val anonymousUnionTypeTraits = generateUnionTypeTraits(anonymousUnionTypes)
    val anonymousUnionsMap = anonymousUnionTypeTraits.toMap { it.name }

    val typedefsToBeGenerated = typedefs.filter { it.types is UnionType }
            .map { NamedValue(it.name, it.types as UnionType) }
            .filter { it.value.memberTypes.all { type -> type is SimpleType && type.type in declaredTypes } }

    val typedefsMarkersMap = typedefsToBeGenerated.groupBy { it.name }.mapValues { mapUnionType(it.value.first().value).copy(name = it.key) }

    val typeNamesToUnions = anonymousUnionTypes.toList().flatMap { unionType -> unionType.memberTypes.filterIsInstance<SimpleType>().map { unionMember -> unionMember.type to unionType.name } }.toMultiMap() merge
            typedefsToBeGenerated.flatMap { typedef -> typedef.value.memberTypes.filterIsInstance<SimpleType>().map { unionMember -> unionMember.type to typedef.name } }.toMultiMap()

    return GenerateUnionTypes(
            typeNamesToUnionsMap = typeNamesToUnions,
            anonymousUnionsMap = anonymousUnionsMap,
            typedefsMarkersMap = typedefsMarkersMap
    )
}
