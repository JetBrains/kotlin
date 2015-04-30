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

data class Repository(
        val interfaces: Map<String, InterfaceDefinition>,
        val typeDefs: Map<String, TypedefDefinition>,
        val externals: Map<String, List<String>>,
        val enums: Map<String, EnumDefinition>
)

data class GenerateAttribute(val name: String, val type: String, val initializer: String?, val getterSetterNoImpl: Boolean, val readOnly: Boolean, val override: Boolean, var vararg : Boolean) {
    val getterNoImpl: Boolean
        get() = getterSetterNoImpl
    val setterNoImpl: Boolean
        get() = getterSetterNoImpl && !readOnly
}

val GenerateAttribute.proto : String
    get() = "$name:$type"

enum class NativeGetterOrSetter {
    NONE
    GETTER
    SETTER
}

enum class GenerateDefinitionType {
    TRAIT
    CLASS
}

data class GenerateFunction(val name: String, val returnType: String, val arguments: List<GenerateAttribute>, val native : NativeGetterOrSetter)
data class GenerateFunctionCall(val name: String, val arguments: List<String>)
data class GenerateTraitOrClass(val name: String,
                           val type : GenerateDefinitionType,
                           val superTypes: List<String>,
                           val memberAttributes: List<GenerateAttribute>,
                           val memberFunctions: List<GenerateFunction>,
                           val constnats : List<GenerateAttribute>,
                           val constructor: GenerateFunction?,
                           val superConstructorCalls: List<GenerateFunctionCall>)
val GenerateFunction.proto : String
    get() = arguments.map {it.type}.joinToString(", ", "$name(", ")")