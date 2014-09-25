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

package org.jetbrains.k2js.translate.utils.ast

import com.google.dart.compiler.backend.js.ast.JsFunction
import com.google.dart.compiler.backend.js.ast.JsStatement
import com.google.dart.compiler.backend.js.ast.JsParameter

public fun JsFunction.addStatement(stmt: JsStatement) {
    getBody().getStatements().add(stmt)
}

public fun JsFunction.addParameter(identifier: String, index: Int? = null): JsParameter {
    val name = getScope().declareFreshName(identifier)
    val parameter = JsParameter(name)

    if (index == null) {
        getParameters().add(parameter)
    } else {
        getParameters().add(index, parameter)
    }

    return parameter
}
