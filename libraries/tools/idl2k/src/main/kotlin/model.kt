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

import java.util.HashSet

data class NamedValue<V>(val name: String, val value: V)

data class Repository(
        val interfaces: Map<String, InterfaceDefinition>,
        val typeDefs: Map<String, TypedefDefinition>,
        val externals: Map<String, List<String>>,
        val enums: Map<String, EnumDefinition>
)

data class GenerateAttribute(val name: String, val type: String, val initializer: String?, val getterSetterNoImpl: Boolean, val readOnly: Boolean, val override: Boolean, var vararg: Boolean)

val GenerateAttribute.getterNoImpl: Boolean
    get() = getterSetterNoImpl
val GenerateAttribute.setterNoImpl: Boolean
    get() = getterSetterNoImpl && !readOnly

val String.typeSignature: String
    get() = if (contains("->")) "Function${FunctionType(this).arity}" else this

val GenerateAttribute.signature: String
    get() = "$name:${type.typeSignature}"

fun GenerateAttribute.dynamicIfUnknownType(allTypes : Set<String>, standardTypes : Set<String> = standardTypes()) = copy(type = type.dynamicIfUnknownType(allTypes, standardTypes))

enum class NativeGetterOrSetter {
    NONE
    GETTER
    SETTER
}

enum class GenerateDefinitionKind {
    TRAIT
    CLASS
}

class UnionType(val namespace: String, types: Collection<String>) {
    val memberTypes = HashSet(types)
    val name = "Union${this.memberTypes.sort().joinToString("Or")}"

    fun contains(type: String) = type in memberTypes

    override fun equals(other: Any?): Boolean = other is UnionType && name == other.name
    override fun hashCode(): Int = name.hashCode()
    override fun toString(): String = name
}

data class GenerateFunctionCall(val name: String, val arguments: List<String>)
data class GenerateFunction(
        val name: String,
        val returnType: String,
        val arguments: List<GenerateAttribute>,
        val nativeGetterOrSetter: NativeGetterOrSetter
)

data class GenerateTraitOrClass(
        val name: String,
        val namespace: String,
        val kind: GenerateDefinitionKind,
        val superTypes: List<String>,
        val memberAttributes: List<GenerateAttribute>,
        val memberFunctions: List<GenerateFunction>,
        val constants: List<GenerateAttribute>,
        val constructor: GenerateFunction?,
        val superConstructorCalls: List<GenerateFunctionCall>
) {
    init {
        assert(superConstructorCalls.size() <= 1, "It shoould be zero or one super constructors")
    }
}


val GenerateFunction.signature: String
    get() = arguments.map { it.type.typeSignature }.joinToString(", ", "$name(", ")")

fun GenerateFunction.dynamicIfUnknownType(allTypes : Set<String>) = standardTypes().let { standardTypes ->
    copy(returnType = returnType.dynamicIfUnknownType(allTypes, standardTypes), arguments = arguments.map { it.dynamicIfUnknownType(allTypes, standardTypes) })
}

fun InterfaceDefinition.findExtendedAttribute(name: String) = extendedAttributes.firstOrNull { it.call == name }
fun InterfaceDefinition?.hasExtendedAttribute(name: String) = this?.findExtendedAttribute(name) ?: null != null
fun InterfaceDefinition.findConstructor() = findExtendedAttribute("Constructor")

data class GenerateUnionTypes(
        val typeNamesToUnionsMap: Map<String, List<String>>,
        val anonymousUnionsMap: Map<String, GenerateTraitOrClass>,
        val typedefsMarkersMap: Map<String, GenerateTraitOrClass>
)
