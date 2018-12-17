/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.translate.general

import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.js.backend.ast.JsNameBinding
import org.jetbrains.kotlin.js.backend.ast.JsProgramFragment
import org.jetbrains.kotlin.js.inline.util.extractImportTag

// TODO this is a hack for `intrinsic:` tags
fun JsProgramFragment.normalizeImportTags() {

    val newImports = mutableMapOf<String, JsExpression>()
    val replacements = mutableMapOf<String, String>()

    imports.entries.retainAll { (tag, statement) ->
        extractImportTag(statement)?.let { newTag ->
            if (newTag != tag) {
                newImports[newTag] = statement
                replacements[tag] = newTag
            }
            newTag == tag
        } ?: true
    }

    imports += newImports

    nameBindings.replaceAll { binding ->
        replacements[binding.key]?.let {
            JsNameBinding(it, binding.name)
        } ?: binding
    }
}