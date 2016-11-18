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

package org.jetbrains.kotlin.kapt3.stubs

import org.jetbrains.kotlin.kapt3.util.isEnum
import org.jetbrains.kotlin.kapt3.util.isStatic
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.AnnotationNode
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import java.util.*

internal class ParameterInfo(
        val flags: Long,
        val name: String,
        val type: Type,
        val visibleAnnotations: List<AnnotationNode>?,
        val invisibleAnnotations: List<AnnotationNode>?)

internal fun MethodNode.getParametersInfo(containingClass: ClassNode): List<ParameterInfo> {
    val localVariables = this.localVariables ?: emptyList()
    val parameters = this.parameters ?: emptyList()
    val isStatic = isStatic(access)

    // First and second parameters in enum constructors are synthetic, we should ignore them
    val isEnumConstructor = (name == "<init>") && containingClass.isEnum()
    val startParameterIndex = if (isEnumConstructor) 2 else 0

    val parameterTypes = Type.getArgumentTypes(desc)

    val parameterInfos = ArrayList<ParameterInfo>(parameterTypes.size - startParameterIndex)
    for (index in startParameterIndex..parameterTypes.lastIndex) {
        val type = parameterTypes[index]
        var name = parameters.getOrNull(index - startParameterIndex)?.name
                   ?: localVariables.getOrNull(index + (if (isStatic) 0 else 1))?.name
                   ?: "p${index - startParameterIndex}"

        // Property setters has bad parameter names
        if (name.startsWith("<") && name.endsWith(">")) {
            name = "p${index - startParameterIndex}"
        }

        val visibleAnnotations = visibleParameterAnnotations?.get(index)
        val invisibleAnnotations = invisibleParameterAnnotations?.get(index)
        parameterInfos += ParameterInfo(0, name, type, visibleAnnotations, invisibleAnnotations)
    }
    return parameterInfos
}