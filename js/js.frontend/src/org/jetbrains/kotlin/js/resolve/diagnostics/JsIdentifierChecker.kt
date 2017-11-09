/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.js.naming.NameSuggestion
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.IdentifierChecker

object JsIdentifierChecker : IdentifierChecker {
    override fun checkIdentifier(identifier: PsiElement?, diagnosticHolder: DiagnosticSink) {
        if (identifier == null) return

        val hasIllegalChars = identifier.text.split('.').any { NameSuggestion.sanitizeName(it) != it }
        if (hasIllegalChars) {
            diagnosticHolder.report(Errors.INVALID_CHARACTERS.on(identifier, "contains illegal characters"))
        }
    }

    override fun checkDeclaration(declaration: KtDeclaration, diagnosticHolder: DiagnosticSink) {}
}
