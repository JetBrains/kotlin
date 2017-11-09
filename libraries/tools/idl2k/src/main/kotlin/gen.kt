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

private fun Operation.getterOrSetter() = this.attributes.map { it.call }.toSet().let { attributes ->
    when {
        "getter" in attributes -> NativeGetterOrSetter.GETTER
        "setter" in attributes -> NativeGetterOrSetter.SETTER
        else -> NativeGetterOrSetter.NONE
    }
}

fun generateFunction(repository: Repository, function: Operation, functionName: String, nativeGetterOrSetter: NativeGetterOrSetter = function.getterOrSetter()): GenerateFunction =
        function.attributes.map { it.call }.toSet().let {
            GenerateFunction(
                    name = functionName,
                    returnType = mapType(repository, function.returnType).let { mapped -> if (nativeGetterOrSetter == NativeGetterOrSetter.GETTER) mapped.toNullableIfNonPrimitive() else mapped },
                    arguments = function.parameters.map {
                        val mappedType = mapType(repository, it.type)

                        GenerateAttribute(
                                name = it.name,
                                type = mappedType,
                                initializer = if (it.defaultValue != null) "definedExternally" else null,
                                getterSetterNoImpl = false,
                                override = false,
                                kind = AttributeKind.ARGUMENT,
                                vararg = it.vararg,
                                static = it.static,
                                required = it.required
                        )
                    },
                    nativeGetterOrSetter = nativeGetterOrSetter,
                    static = function.static,
                    override = false
            )
        }

fun generateFunctions(repository: Repository, function: Operation): List<GenerateFunction> {
    val realFunction = when {
        function.name == "" -> null
        function.getterOrSetter() == NativeGetterOrSetter.NONE -> generateFunction(repository, function, function.name, NativeGetterOrSetter.NONE)
        function.name == "get" || function.name == "set" -> null
        else -> generateFunction(repository, function, function.name, NativeGetterOrSetter.NONE)
    }
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

fun generateAttribute(putNoImpl: Boolean, repository: Repository, attribute: Attribute, nullableAttributes: Boolean): GenerateAttribute =
        GenerateAttribute(attribute.name,
                type = mapType(repository, attribute.type).let { if (nullableAttributes) it.toNullable() else it },
                initializer =
                    if (putNoImpl && !attribute.static) {
                        mapLiteral(attribute.defaultValue, mapType(repository, attribute.type), repository.enums)
                    }
                    else if (attribute.defaultValue != null) {
                        "definedExternally"
                    }
                    else {
                        null
                    },
                getterSetterNoImpl = putNoImpl,
                kind = if (attribute.readOnly) AttributeKind.VAL else AttributeKind.VAR,
                override = false,
                vararg = attribute.vararg,
                static = attribute.static,
                required = attribute.required
        )

private fun InterfaceDefinition.superTypes(repository: Repository) = superTypes.map { repository.interfaces[it] }.filterNotNull()
private fun resolveDefinitionKind(repository: Repository, iface: InterfaceDefinition, constructors: List<ExtendedAttribute> = iface.findConstructors()): GenerateDefinitionKind =
        when {
            iface.dictionary -> GenerateDefinitionKind.INTERFACE
            iface.extendedAttributes.any { it.call == "NoInterfaceObject" } -> GenerateDefinitionKind.INTERFACE
            constructors.isNotEmpty() || iface.superTypes(repository).any { resolveDefinitionKind(repository, it) == GenerateDefinitionKind.CLASS } -> {
                GenerateDefinitionKind.CLASS
            }
            iface.callback -> GenerateDefinitionKind.INTERFACE
            else -> GenerateDefinitionKind.ABSTRACT_CLASS
        }

private fun InterfaceDefinition.mapAttributes(repository: Repository)
        = attributes.map { generateAttribute(putNoImpl = dictionary, repository = repository, attribute = it, nullableAttributes = dictionary) }
private fun InterfaceDefinition.mapOperations(repository: Repository) = operations.flatMap { generateFunctions(repository, it) }
private fun Constant.mapConstant(repository : Repository) = GenerateAttribute(name, mapType(repository, type), null, false, AttributeKind.VAL, false, false, true, false)
private val EMPTY_CONSTRUCTOR = ExtendedAttribute(null, "Constructor", emptyList())

fun generateTrait(repository: Repository, iface: InterfaceDefinition): GenerateTraitOrClass {
    val superClasses = iface.superTypes
            .mapNotNull { repository.interfaces[it] }
            .filter {
                when (resolveDefinitionKind(repository, it)) {
                    GenerateDefinitionKind.CLASS,
                    GenerateDefinitionKind.ABSTRACT_CLASS -> true
                    else -> false
                }
            }

    assert(superClasses.size <= 1) { "Type ${iface.name} should have one or zero super classes but found ${superClasses.map { it.name }}" }

    val declaredConstructors = iface.findConstructors()
    val entityKind = resolveDefinitionKind(repository, iface, declaredConstructors)
    val extensions = repository.externals[iface.name]?.mapNotNull { repository.interfaces[it] } ?: emptyList()

    val primaryConstructor = when {
        declaredConstructors.size == 1 -> declaredConstructors.single()
        declaredConstructors.isEmpty() && (entityKind == GenerateDefinitionKind.CLASS || entityKind == GenerateDefinitionKind.ABSTRACT_CLASS)  -> EMPTY_CONSTRUCTOR
        else -> declaredConstructors.firstOrNull { it.arguments.isEmpty() }
    }
    val secondaryConstructors = declaredConstructors.filter { it != primaryConstructor }

    val primaryConstructorWithCall = primaryConstructor?.let { constructor ->
        val constructorAsFunction = generateConstructorAsFunction(repository, constructor)

        ConstructorWithSuperTypeCall(constructorAsFunction, constructor)
    }

    val secondaryConstructorsWithCall = secondaryConstructors.map { secondaryConstructor ->
        val constructorAsFunction = generateConstructorAsFunction(repository, secondaryConstructor)

        ConstructorWithSuperTypeCall(constructorAsFunction, secondaryConstructor)
    }

    val result = GenerateTraitOrClass(iface.name, iface.namespace, entityKind, (iface.superTypes + extensions.map { it.name }).distinct(),
            memberAttributes = iface.mapAttributes(repository).toMutableList(),
            memberFunctions = iface.mapOperations(repository).toMutableList(),
            constants = (iface.constants.map { it.mapConstant(repository) } + extensions.flatMap { it.constants.map { it.mapConstant(repository) } }.distinct().toList()),
            primaryConstructor = primaryConstructorWithCall,
            secondaryConstructors = secondaryConstructorsWithCall,
            generateBuilderFunction = iface.dictionary
    )

    return markAsArrayLikeIfApplicable(result)
}

fun markAsArrayLikeIfApplicable(iface: GenerateTraitOrClass): GenerateTraitOrClass {
    fun isInt(type: Type) = type is SimpleType && type.type == "Int"

    val lengthProperty = iface.memberAttributes.singleOrNull { it.name == "length" && isInt(it.type)  }
    val itemAccessFunction = iface.memberFunctions.singleOrNull { it.name == "item" && it.arguments.map { isInt(it.type) } == listOf(true) && it.returnType != UnitType }

    if (lengthProperty == null || itemAccessFunction == null) return iface

    return iface.copy(superTypes = iface.superTypes + "ItemArrayLike<${itemAccessFunction.returnType.dropNullable().render()}>")
}

fun generateConstructorAsFunction(repository: Repository, constructor: ExtendedAttribute) = generateFunction(
        repository,
        Operation("constructor", UnitType, constructor.arguments, emptyList(), false),
        functionName = "constructor",
        nativeGetterOrSetter = NativeGetterOrSetter.NONE)


fun mapUnionType(it: UnionType) = GenerateTraitOrClass(
        name = it.name,
        namespace = it.namespace,
        kind = GenerateDefinitionKind.INTERFACE,
        superTypes = emptyList(),
        memberAttributes = mutableListOf(),
        memberFunctions = mutableListOf(),
        constants = emptyList(),
        primaryConstructor = null,
        secondaryConstructors = emptyList(),
        generateBuilderFunction = false
)

fun generateUnionTypeTraits(allUnionTypes: Sequence<UnionType>): Sequence<GenerateTraitOrClass> = allUnionTypes.map(::mapUnionType)

fun mapDefinitions(repository: Repository, definitions: Iterable<InterfaceDefinition>) =
        definitions.map { generateTrait(repository, it) }

fun generateUnions(ifaces: List<GenerateTraitOrClass>, typedefs: Iterable<TypedefDefinition>): GenerateUnionTypes {
    val declaredTypes = ifaces.associateBy { it.name }

    val anonymousUnionTypes = collectUnionTypes(declaredTypes)
    val anonymousUnionTypeTraits = generateUnionTypeTraits(anonymousUnionTypes)
    val anonymousUnionsMap = anonymousUnionTypeTraits.associateBy { it.name }

    val typedefsToBeGenerated = typedefs.filter { it.types is UnionType }
            .map { NamedValue(it.name, it.types as UnionType) }
            .filter { it.value.memberTypes.all { type -> type is SimpleType && type.type in declaredTypes } }

    val typedefsMarkersMap = typedefsToBeGenerated.groupBy { it.name }.mapValues { mapUnionType(it.value.first().value).copy(name = it.key) }

    val typeNamesToUnions = anonymousUnionTypes
            .toList()
            .flatMap { unionType ->
                unionType.memberTypes
                        .filterIsInstance<SimpleType>()
                        .map { unionMember -> unionMember.type to unionType.name }
            }.toMultiMap()
            .merge(typedefsToBeGenerated
                    .flatMap { typedef ->
                        typedef.value.memberTypes
                                .filterIsInstance<SimpleType>()
                                .map { unionMember -> unionMember.type to typedef.name }
                    }.toMultiMap())

    return GenerateUnionTypes(
            typeNamesToUnionsMap = typeNamesToUnions,
            anonymousUnionsMap = anonymousUnionsMap,
            typedefsMarkersMap = typedefsMarkersMap
    )
}

private fun mapLiteral(literal: String?, expectedType: Type = DynamicType, enums: Map<String, EnumDefinition>) =
    if (literal != null && expectedType is SimpleType && expectedType.type in enums.keys) {
        expectedType.type + "." + mapEnumConstant(literal.removeSurrounding("\"", "\""))
    }
    else {
        when (literal) {
            "[]" -> when {
                expectedType == DynamicType -> "arrayOf<dynamic>()"
                expectedType is AnyType -> "arrayOf<dynamic>()"
                expectedType is UnionType -> "arrayOf<dynamic>()"
                else -> "arrayOf()"
            }
            else -> literal
        }
    }

fun implementInterfaces(declarations: List<GenerateTraitOrClass>) {
    val unimplementedMemberMap = getUnimplementedMembers(declarations)
    val nonAbstractDeclarations = declarations.filter { it.kind == GenerateDefinitionKind.CLASS }
    for (declaration in nonAbstractDeclarations) {
        val unimplementedMembers = unimplementedMemberMap[declaration.name] ?: continue

        for (attribute in unimplementedMembers.attributes) {
            declaration.memberAttributes += attribute.copy(override = true)
        }
        for (function in unimplementedMembers.functions) {
            declaration.memberFunctions += function.copy(override = true)
        }
    }
}

private fun getUnimplementedMembers(declarations: List<GenerateTraitOrClass>): Map<String, UnimplementedMembers> {
    val declarationMap = declarations.associate { it.name to it }
    val unimplementedMemberCache = mutableMapOf<String, UnimplementedMembers>()

    fun getForClass(className: String): UnimplementedMembers = unimplementedMemberCache.getOrPut(className) {
        val declaration = declarationMap[className] ?: return@getOrPut UnimplementedMembers(emptyList(), emptyList())
        val unimplementedInSuperClasses = declaration.superTypes.map { getForClass(it) }
        val attributeMap = unimplementedInSuperClasses
                .flatMap { it.attributes }
                .associate { it.name to it }
                .toMutableMap()
        val functionMap = unimplementedInSuperClasses
                .flatMap { it.functions }
                .associate { "${it.name}(${it.signature})" to it }
                .toMutableMap()

        val (implementedAttributes, unimplementedAttributes) = declaration.memberAttributes
                .filter { !it.static }
                .partition { declaration.kind != GenerateDefinitionKind.INTERFACE && !it.getterSetterNoImpl }
        val (implementedFunctions, unimplementedFunctions) = declaration.memberFunctions
                .filter { !it.static }
                .partition { declaration.kind != GenerateDefinitionKind.INTERFACE }

        attributeMap += unimplementedAttributes.map { it.name to it }
        attributeMap.keys -= implementedAttributes.map { it.name }
        functionMap += unimplementedFunctions.map { "${it.name}(${it.signature})" to it }
        functionMap.keys -= implementedFunctions.map { "${it.name}(${it.signature})" }

        UnimplementedMembers(attributeMap.values.toList(), functionMap.values.toList())
    }

    for (declaration in declarations) {
        getForClass(declaration.name)
    }

    return unimplementedMemberCache
}

private class UnimplementedMembers(val attributes: List<GenerateAttribute>, val functions: List<GenerateFunction>)