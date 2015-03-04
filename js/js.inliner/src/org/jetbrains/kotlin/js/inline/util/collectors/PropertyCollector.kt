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

package org.jetbrains.kotlin.js.inline.util.collectors

import com.google.dart.compiler.backend.js.ast.*
import org.jetbrains.kotlin.js.translate.expression.InlineMetadata

import java.util.IdentityHashMap

class PropertyCollector : RecursiveJsVisitor() {
    public val properties: IdentityHashMap<JsName, JsExpression> = IdentityHashMap()

    override fun visitPropertyInitializer(x: JsPropertyInitializer?) {
        super.visitPropertyInitializer(x)

        val label = x?.getLabelExpr() as? JsNameRef
        val name = label?.getName()
        if (name == null) return

        val value = x?.getValueExpr()
        properties[name] = value
    }
}
