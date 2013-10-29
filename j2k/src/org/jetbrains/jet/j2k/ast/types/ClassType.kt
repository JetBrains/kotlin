/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.j2k.ast.types

import org.jetbrains.jet.j2k.ast.Element
import org.jetbrains.jet.j2k.ast.Identifier
import java.util.ArrayList

public open class ClassType(val `type`: Identifier, val parameters: List<Element>, nullable: Boolean) : Type(nullable) {
    public override fun toKotlin(): String {
        // TODO change to map() when KT-2051 is fixed
        val parametersToKotlin = ArrayList<String>()
        for (param in parameters) {
            parametersToKotlin.add(param.toKotlin())
        }
        var params: String = if (parametersToKotlin.size() == 0)
            ""
        else
            "<" + parametersToKotlin.makeString(", ") + ">"
        return `type`.toKotlin() + params + isNullableStr()
    }


    public override fun convertedToNotNull(): Type = ClassType(`type`, parameters, false)
}
