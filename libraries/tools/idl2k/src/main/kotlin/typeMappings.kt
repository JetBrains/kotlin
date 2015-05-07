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
        "unsignedlong" to "Int",
        "unsignedlonglong" to "Long",
        "longlong" to "Long",
        "unsignedshort" to "Short",
        "void" to "Unit",
        "boolean" to "Boolean",
        "short" to "Short",
        "long" to "Int",
        "double" to "Double",
        "any" to "Any?",
        "DOMTimeStamp" to "Number",
        "object" to "dynamic", // TODO map to Any?
        "EventHandler" to "(Event) -> Unit",
        "WindowProxy" to "Window",
        "Function" to "() -> dynamic",
        "USVString" to "String",
        "DOMString" to "String",
        "ByteString" to "String",
        "DOMError" to "dynamic",
        "Uint8ClampedArray" to "dynamic", // TODO think of native arrays,
        "ArrayBuffer" to "dynamic",
        "Elements" to "dynamic",
        "Date" to "Date",
        "" to "dynamic"
)


fun GenerateTraitOrClass.allSuperTypes(all: Map<String, GenerateTraitOrClass>) = HashSet<GenerateTraitOrClass>().let { result -> allSuperTypesImpl(listOf(this), all, result); result.toList() }

tailRecursive
fun allSuperTypesImpl(roots: List<GenerateTraitOrClass>, all: Map<String, GenerateTraitOrClass>, result: HashSet<GenerateTraitOrClass>) {
    if (roots.isNotEmpty()) {
        allSuperTypesImpl(roots.flatMap { it.superTypes }.map { all[it] }.filterNotNull().filter { result.add(it) }, all, result)
    }
}

fun standardTypes() = typeMapper.values().map {it.dropNullable()}.toSet()
fun String.dynamicIfUnknownType(allTypes: Set<String>, standardTypes: Set<String> = standardTypes()): String = when {
    startsWith("Union<") -> UnionType("", splitUnionType(this)).name.dynamicIfUnknownType(allTypes, standardTypes).copyNullabilityFrom(this)
    endsWith("?") -> this.dropNullable().dynamicIfUnknownType(allTypes, standardTypes).ensureNullable()
    contains("->") -> {
        val (parameters, returnType) = this.split("->".toRegex()).map {it.trim()}.filter { it != "" }

        "(${parameters.removeSurrounding("(", ")").split(',').map {it.dynamicIfUnknownType(allTypes, standardTypes)}.join(",")}) -> ${returnType.dynamicIfUnknownType(allTypes, standardTypes)}"
    }
    this in allTypes -> this
    this in standardTypes -> this
    else -> "dynamic"
}

private fun mapType(repository: Repository, type: String): String =
    when {
        type in typeMapper -> typeMapper[type]!!
        type.endsWith("?") -> mapType(repository, type.dropNullable()).ensureNullable()
        type.endsWith("...") -> mapType(repository, type.substring(0, type.length() - 3))
        type.endsWith("[]") -> "Array<${mapType(repository, type.substring(0, type.length() - 2))}>"
        type.startsWith("unrestricted") -> mapType(repository, type.substring(12))
        type.startsWith("sequence") -> "Any" // TODO how do we handle sequences?
        type in repository.typeDefs -> mapTypedef(repository, type)
        type in repository.enums -> "String"
        type.endsWith("Callback") -> "() -> Unit"
        type.startsWith("Promise<") -> "dynamic"
        repository.interfaces[type].hasExtendedAttribute("NoInterfaceObject") -> "dynamic"
        else -> type
    }

private fun mapTypedef(repository: Repository, type: String): String {
    val typedef = repository.typeDefs[type]!!

    return if (!typedef.types.startsWith("Union<")) mapType(repository, typedef.types)
        else if (splitUnionType(typedef.types).size() == 1) mapType(repository, splitUnionType(typedef.types).first())
        else typedef.name
}

// Union<A, B, C>    ->  [A, B, C]
// Union<A, Union<B>, C>    ->  [A, B, C]
// Union<Union<Union<A, B>>, C>    ->  [A, B, C]
private fun splitUnionType(unionType: String) =
        unionType.replaceAll("Union<", "").replaceAll("[>]+", "").split("\\s*,\\s*".toRegex()).distinct().map {it.replaceAll("\\?$", "")}

private fun GenerateFunction?.allTypes() = if (this != null) sequenceOf(returnType) + arguments.asSequence().map { it.type } else emptySequence()

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
                .map { UnionType(guessPackage(it, allTypes), it) }

private fun guessPackage(types : List<String>, allTypes: Map<String, GenerateTraitOrClass>) =
        types.map { allTypes[it] }
        .map { it?.namespace }
        .filterNotNull()
        .filter { it != "" }
        .distinct()
        .minBy { it.split('.').size() } ?: ""
