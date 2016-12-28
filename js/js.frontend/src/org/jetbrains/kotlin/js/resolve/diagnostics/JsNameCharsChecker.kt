/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.resolve.diagnostics

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.js.naming.NameSuggestion
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.checkers.SimpleDeclarationChecker

class JsNameCharsChecker() : SimpleDeclarationChecker {
    val suggestion = NameSuggestion()

    override fun check(
            declaration: KtDeclaration, descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink, bindingContext: BindingContext
    ) {
        if (descriptor is PropertyAccessorDescriptor && AnnotationsUtils.getJsName(descriptor) == null) return

        val suggestedName = suggestion.suggest(descriptor) ?: return
        if (suggestedName.stable && suggestedName.names.any { NameSuggestion.sanitizeName(it) != it }) {
            diagnosticHolder.report(ErrorsJs.NAME_CONTAINS_ILLEGAL_CHARS.on(declaration))
        }
    }
}