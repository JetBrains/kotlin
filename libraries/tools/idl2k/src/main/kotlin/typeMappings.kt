/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import java.util.*

private val typeMapper = mapOf(
    "unsignedlong" to SimpleType("Int", false),
    "unsignedlonglong" to SimpleType("Int", false),
    "longlong" to SimpleType("Int", false),
    "unsignedshort" to SimpleType("Short", false),
    "unsignedbyte" to SimpleType("Byte", false),
    "octet" to SimpleType("Byte", false),
    "void" to UnitType,
    "boolean" to SimpleType("Boolean", false),
    "byte" to SimpleType("Byte", false),
    "short" to SimpleType("Short", false),
    "long" to SimpleType("Int", false),
    "float" to SimpleType("Float", false),
    "double" to SimpleType("Double", false),
    "any" to AnyType(true),
    "DOMTimeStamp" to SimpleType("Number", false),
    "object" to DynamicType, // TODO map to Any?
    "WindowProxy" to SimpleType("Window", false),
    "USVString" to SimpleType("String", false),
    "DOMString" to SimpleType("String", false),
    "ByteString" to SimpleType("String", false),
    "DOMError" to DynamicType,
    "Elements" to DynamicType,
    "Date" to SimpleType("Date", false),
    "" to DynamicType
)


fun GenerateClass.allSuperTypes(all: Map<String, GenerateClass>) = LinkedHashSet<GenerateClass>().let { result -> allSuperTypesImpl(listOf(this), all, result); result.toList() }

tailrec fun allSuperTypesImpl(roots: List<GenerateClass>, all: Map<String, GenerateClass>, result: MutableSet<GenerateClass>) {
    if (roots.isNotEmpty()) {
        allSuperTypesImpl(roots.flatMap { it.superTypes }.map {
            all[it] ?: all[it.substringBefore("<")]
        }.filterNotNull().filter { result.add(it) }, all, result)
    }
}

fun standardTypes() = typeMapper.values.map { it.dropNullable() }.toSet()
fun Type.dynamicIfUnknownType(allTypes: Set<String>, standardTypes: Set<Type> = standardTypes()): Type = when {
    this is DynamicType || this is UnitType -> this

    this is SimpleType && this.type in allTypes -> this
    this.dropNullable() in standardTypes -> this
    this is ArrayType -> copy(memberType = this.memberType.dynamicIfUnknownType(allTypes, standardTypes))
    this is UnionType -> if (this.name !in allTypes) DynamicType else this
    this is FunctionType -> copy(
        returnType = returnType.dynamicIfUnknownType(allTypes, standardTypes),
        parameterTypes = parameterTypes.map {
            it.copy(
                type = it.type.dynamicIfUnknownType(
                    allTypes,
                    standardTypes
                )
            )
        })
    this is PromiseType ->
        copy(valueType = valueType.dynamicIfUnknownType(allTypes, standardTypes))

    else -> DynamicType
}

private fun Type.dynamicIfAnyType(): Type = if (this is AnyType && this.nullable) DynamicType else this

internal fun mapType(repository: Repository, type: Type): Type = when (type) {
    is SimpleType -> {
        val typeName = type.type
        when {
            typeName in typeMapper -> typeMapper[typeName]!!.withNullability(type.nullable)
            typeName in repository.interfaces -> type
            typeName in repository.typeDefs -> mapTypedef(repository, type)

            else -> type
        }
    }
    is PromiseType -> type.copy(valueType = mapType(repository, type.valueType))
    is ArrayType -> type.copy(memberType = mapType(repository, type.memberType))
    is UnionType -> UnionType(
        type.namespace,
        type.memberTypes.map { mt -> mapType(repository, mt) },
        type.nullable
    ).toSingleTypeIfPossible()
    is FunctionType -> type.copy(
        // TODO: Remove takeWhile { !vararg } when we have varargs supported. See KT-3115
        returnType = mapType(repository, type.returnType).dynamicIfAnyType(),
        parameterTypes = type.parameterTypes.takeWhile { !it.vararg }.map { it.copy(type = mapType(repository, it.type)) }
    )

    is AnyType,
    is UnitType,
    is DynamicType -> type
}

private fun mapTypedef(repository: Repository, type: SimpleType): Type {
    val typedef = repository.typeDefs[type.type]!!

    return when {
        typedef.types is UnionType && typedef.types.memberTypes.size == 1 -> mapType(
            repository,
            typedef.types.memberTypes.single().withNullability(type.nullable)
        )
        typedef.types is UnionType -> SimpleType(typedef.name, type.nullable)
        else -> mapType(repository, typedef.types.withNullability(type.nullable))
    }
}

private fun GenerateFunction?.allTypes() =
    if (this != null) sequenceOf(returnType) + arguments.asSequence().map { it.type } else emptySequence()

internal fun collectUnionTypes(allTypes: Map<String, GenerateClass>) =
        allTypes.values.asSequence()
                .flatMap {
                    it.secondaryConstructors.asSequence().flatMap { it.constructor.allTypes() } +
                    sequenceOf(it.primaryConstructor).filterNotNull().flatMap { it.constructor.allTypes() } +
                            it.memberAttributes.asSequence().map { it.type } +
                            it.memberFunctions.asSequence().flatMap { it.allTypes() }
                }
                .filterIsInstance<UnionType>()
                .map { it.dropNullable() }
                .filter { it.memberTypes.all { unionMember -> unionMember is SimpleType && unionMember.type in allTypes } }
                .distinct()
                .map { it.copy(namespace = guessPackage(it.memberTypes.filterIsInstance<SimpleType>().map { it.type }, allTypes), types = it.memberTypes) }

private fun guessPackage(types : List<String>, allTypes: Map<String, GenerateClass>) =
        types.map { allTypes[it] }
        .map { it?.namespace }
        .filterNotNull()
        .filter { it.isNotEmpty() }
        .distinct()
        .minBy { it.split('.').size } ?: ""
