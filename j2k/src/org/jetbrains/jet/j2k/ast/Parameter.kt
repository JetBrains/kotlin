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

package org.jetbrains.jet.j2k.ast

import org.jetbrains.jet.j2k.ast.types.Type
import org.jetbrains.jet.j2k.ast.types.VarArg

public open class Parameter(val identifier: Identifier, val `type`: Type, val readOnly: Boolean = true) : Expression() {
    public override fun toKotlin(): String {
        val vararg: String = (if (`type` is VarArg)
            "vararg "
        else
            "")
        val `var`: String? = (if (readOnly)
            ""
        else
            "var ")
        return vararg + `var` + identifier.toKotlin() + " : " + `type`.toKotlin()
    }
}
