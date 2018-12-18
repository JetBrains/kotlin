/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.translate.general

import org.jetbrains.kotlin.js.backend.ast.JsNameBinding
import org.jetbrains.kotlin.js.backend.ast.JsProgramFragment
import org.jetbrains.kotlin.js.backend.ast.JsVars
import org.jetbrains.kotlin.js.inline.util.extractImportTag

// TODO this is a hack for `intrinsic:` and `constant:` tags
fun JsProgramFragment.normalizeImportTags() {

    nameBindings.replaceAll { binding ->
        val (tag, name) = binding

        imports[tag]?.let { import ->
            extractImportTag(JsVars.JsVar(name, imports[tag]))?.let { newtag ->
                if (tag != newtag) {
                    imports[newtag] = import
                    JsNameBinding(newtag, name)
                } else null
            }
        } ?: binding
    }

    val tagSet = nameBindings.asSequence().map { it.key }.toSet()

    imports.entries.retainAll { (tag, _) -> tag in tagSet }
}