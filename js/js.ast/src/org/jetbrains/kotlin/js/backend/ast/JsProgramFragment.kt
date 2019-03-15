// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast

import java.util.*

class JsProgramFragment(val scope: JsScope, val packageFqn: String) {
    val importedModules = mutableListOf<JsImportedModule>()
    val imports: MutableMap<String, JsExpression> = LinkedHashMap()
    val declarationBlock = JsGlobalBlock()
    val exportBlock = JsGlobalBlock()
    val initializerBlock = JsGlobalBlock()
    val nameBindings = mutableListOf<JsNameBinding>()
    val classes: MutableMap<JsName, JsClassModel> = LinkedHashMap()
    val inlineModuleMap: MutableMap<String, JsExpression> = LinkedHashMap()
    var tests: JsStatement? = null
    var mainFunction: JsStatement? = null
    val inlinedLocalDeclarations = mutableMapOf<String, JsGlobalBlock>()
}
