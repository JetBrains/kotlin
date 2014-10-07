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

package org.jetbrains.k2js.inline.util

import com.google.dart.compiler.backend.js.ast.*
import com.google.dart.compiler.backend.js.ast.metadata.staticRef

import java.util.ArrayList
import java.util.IdentityHashMap
import org.jetbrains.k2js.inline.util.IdentitySet
import org.jetbrains.k2js.inline.util.collectors.ReferenceNameCollector
import org.jetbrains.k2js.inline.util.collectors.NameCollector
import org.jetbrains.k2js.inline.util.collectors.InstanceCollector
import org.jetbrains.k2js.inline.util.collectors.FunctionCollector

public fun collectFunctionReferencesInside(scope: JsNode): List<JsName> =
    collectReferencesInside(scope) filter { it.staticRef is JsFunction }

public fun collectReferencesInside(scope: JsNode): List<JsName> {
    return with(ReferenceNameCollector()) {
        accept(scope)
        references
    }
}

public fun collectLocalNames(function: JsFunction): List<JsName> {
    val functionScope = function.getScope()

    return with(NameCollector(functionScope)) {
        accept(function.getBody())
        names.values().toList()
    }
}

public fun collectNamedFunctions(scope: JsNode): IdentityHashMap<JsName, JsFunction> {
    return with(FunctionCollector()) {
        accept(scope)
        functions
    }
}

public fun collectInstances<T : JsNode>(klass: Class<T>, scope: JsNode): List<T> {
    return with(InstanceCollector(klass)) {
        accept(scope)
        collected
    }
}
