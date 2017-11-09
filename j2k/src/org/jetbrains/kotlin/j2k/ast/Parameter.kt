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

package org.jetbrains.kotlin.j2k.ast

import org.jetbrains.kotlin.j2k.CodeBuilder

abstract class Parameter(val identifier: Identifier, val type: Type?) : Element()

class FunctionParameter(identifier: Identifier,
                        type: Type?,
                        val varVal: FunctionParameter.VarValModifier,
                        val annotations: Annotations,
                        val modifiers: Modifiers,
                        val defaultValue: DeferredElement<Expression>? = null
): Parameter(identifier, type) {
    enum class VarValModifier {
        None,
        Val,
        Var
    }

    override fun generateCode(builder: CodeBuilder) {
        builder.append(annotations).appendWithSpaceAfter(modifiers)

        if (type is VarArgType) {
            builder.append("vararg ")
        }

        when (varVal) {
            VarValModifier.Var -> builder.append("var ")
            VarValModifier.Val -> builder.append("val ")
            VarValModifier.None -> {}
        }

        builder.append(identifier)

        if (type != null) {
            builder append ":" append type
        }

        if (defaultValue != null) {
            builder append " = " append defaultValue
        }
    }
}

class LambdaParameter(identifier: Identifier, type: Type?) : Parameter(identifier, type) {
    override fun generateCode(builder: CodeBuilder) {
        builder append identifier

        if (type != null) {
            builder append ":" append type
        }
    }
}