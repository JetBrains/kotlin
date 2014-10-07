/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.k2js.inline.util.collectors

import com.google.dart.compiler.backend.js.ast.JsVisitorWithContextImpl
import com.google.dart.compiler.backend.js.ast.JsName
import com.google.dart.compiler.backend.js.ast.JsNameRef
import com.google.dart.compiler.backend.js.ast.JsContext
import org.jetbrains.k2js

class ReferenceNameCollector : JsVisitorWithContextImpl() {
    private val referenceSet = k2js.inline.util.IdentitySet<JsName>()

    public val references: List<JsName>
        get() = referenceSet.toList()

    override fun endVisit(x: JsNameRef?, ctx: JsContext?) {
        val name = x?.getName()
        if (name != null) {
            referenceSet.add(name)
        }
    }
}