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

package org.jetbrains.k2js.inline.clean

import com.google.dart.compiler.backend.js.ast.*
import org.jetbrains.k2js.inline.util.IdentitySet

private class FunctionRemover(removable: Collection<JsFunction> = listOf()) : NodeRemovingVisitor<JsFunction>(removable) {

    override fun endVisit(x: JsPropertyInitializer?, ctx: JsContext?) {
        if (x == null) return

        val value = x.getValueExpr()
        if (value is JsFunction && shouldRemove(value)) {
            ctx?.removeMe()
        } else {
            super.endVisit(x, ctx)
        }
    }
}
