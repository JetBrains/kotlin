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

data class NamedValue<V>(val name: String, val value: V)

data class Repository(
        val interfaces: Map<String, InterfaceDefinition>,
        val typeDefs: Map<String, TypedefDefinition>,
        val externals: Map<String, List<String>>,
        val enums: Map<String, EnumDefinition>
)

enum class AttributeKind {
    VAL, VAR, ARGUMENT
}
data class GenerateAttribute(val name: String, val type: Type, val initializer: String?, val getterSetterNoImpl: Boolean, val kind: AttributeKind, val override: Boolean, var vararg: Boolean, val static: Boolean, val required: Boolean)

val GenerateAttribute.getterNoImpl: Boolean
    get() = getterSetterNoImpl
val GenerateAttribute.setterNoImpl: Boolean
    get() = getterSetterNoImpl && kind == AttributeKind.VAR
val GenerateAttribute.isVal: Boolean
    get() = kind == AttributeKind.VAL
val GenerateAttribute.isVar: Boolean
    get() = kind == AttributeKind.VAR

val Type.typeSignature: String
    get() = when {
        this is FunctionType -> "Function$arity"
        else -> this.toString()
    }

val GenerateAttribute.signature: String
    get() = "$name:${type.typeSignature}"

fun GenerateAttribute.dynamicIfUnknownType(allTypes : Set<String>, standardTypes : Set<Type> = standardTypes()) = copy(type = type.dynamicIfUnknownType(allTypes, standardTypes))
fun List<GenerateAttribute>.dynamicIfUnknownType(allTypes : Set<String>, standardTypes : Set<Type> = standardTypes()) = map { it.dynamicIfUnknownType(allTypes, standardTypes) }

enum class NativeGetterOrSetter {
    NONE,
    GETTER,
    SETTER
}

enum class GenerateDefinitionKind {
    INTERFACE,
    CLASS,
    ABSTRACT_CLASS
}

data class GenerateFunction(
        val name: String,
        val returnType: Type,
        val arguments: List<GenerateAttribute>,
        val nativeGetterOrSetter: NativeGetterOrSetter,
        val static: Boolean,
        val override: Boolean
)

data class ConstructorWithSuperTypeCall(val constructor: GenerateFunction, val constructorAttribute: ExtendedAttribute)

data class GenerateClass(
    val name: String,
    val namespace: String,
    val kind: GenerateDefinitionKind,
    val superTypes: List<String>,
    val memberAttributes: MutableList<GenerateAttribute>,
    val memberFunctions: MutableList<GenerateFunction>,
    val constants: List<GenerateAttribute>,
    val primaryConstructor: ConstructorWithSuperTypeCall?,
    val secondaryConstructors: List<ConstructorWithSuperTypeCall>,
    val generateBuilderFunction: Boolean
)

val GenerateFunction.signature: String
    get() = arguments.map { it.type.typeSignature }.joinToString(", ", "$name(", ")")

fun GenerateFunction.dynamicIfUnknownType(allTypes : Set<String>) = standardTypes().let { standardTypes ->
    copy(returnType = returnType.dynamicIfUnknownType(allTypes, standardTypes), arguments = arguments.map { it.dynamicIfUnknownType(allTypes, standardTypes) })
}

fun InterfaceDefinition.findExtendedAttributes(name: String) = extendedAttributes.filter { it.name == name }
fun InterfaceDefinition.findConstructors() = extendedAttributes.filter { it.call == "Constructor" }

data class GenerateUnionTypes(
    val typeNamesToUnionsMap: Map<String, List<String>>,
    val anonymousUnionsMap: Map<String, GenerateClass>,
    val typedefsMarkersMap: Map<String, GenerateClass>
)
