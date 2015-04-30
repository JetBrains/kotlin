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
        "DOMString" to "String",
        "boolean" to "Boolean",
        "short" to "Short",
        "long" to "Int",
        "double" to "Double",
        "any" to "Any",
        "" to "dynamic",
        "DOMTimeStamp" to "Number",
        "EventHandler" to "() -> Unit",
        "object" to "dynamic",
        "WindowProxy" to "Window",
        "Uint8ClampedArray" to "dynamic", // TODO think of native arrays,
        "Function" to "() -> dynamic",
        "USVString" to "String",
        "ByteString" to "String",
        "DOMError" to "dynamic",
        "SVGMatrix" to "dynamic",
        "ArrayBuffer" to "dynamic",
        "Elements" to "dynamic"
)

private fun fixDynamic(type : String) = if (type == "dynamic?") "dynamic" else type

private fun mapType(repository: Repository, type: String) = fixDynamic(handleSpecialTypes(repository, typeMapper[type] ?: type))

private fun handleSpecialTypes(repository: Repository, type: String): String {
    if (type.endsWith("?")) {
        return mapType(repository, type.substring(0, type.length() - 1)) + "?"
    } else if (type.endsWith("...")) {
        return mapType(repository, type.substring(0, type.length() - 3))
    } else if (type.endsWith("[]")) {
        return "Array<${mapType(repository, type.substring(0, type.length() - 2))}>"
    } else if (type.startsWith("unrestricted")) {
        return mapType(repository, type.substring(12))
    } else if (type.startsWith("sequence")) {
        return "Any" // TODO how do we handle sequences?
    } else if (type in repository.typeDefs) {
        val typedef = repository.typeDefs[type]!!

        return if (!typedef.types.startsWith("Union<")) mapType(repository, typedef.types)
            else if (splitUnionType(typedef.types).size() == 1) mapType(repository, splitUnionType(typedef.types).first())
            else typedef.name
    } else if (type in repository.enums) {
        return "String"
    } else if (type.endsWith("Callback")) {
        return "() -> Unit"
//    } else if (type.startsWith("Union<")) {
//        return "dynamic"
    } else if (type.startsWith("Promise<")) {
        return "dynamic"
    } else if ("NoInterfaceObject" in repository.interfaces[type]?.extendedAttributes?.map {it.call} ?: emptyList()) {
        return "dynamic"
    }

    return type
}

private fun findConstructorAttribute(iface: InterfaceDefinition) = iface.extendedAttributes.firstOrNull { it.call == "Constructor" }
