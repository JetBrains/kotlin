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

package org.jetbrains.kotlin.js.resolve.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.PositioningStrategy
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.diagnostics.DiagnosticWithParameters3
import org.jetbrains.kotlin.diagnostics.ParametrizedDiagnostic
import org.jetbrains.kotlin.js.resolve.diagnostics.JsCallData
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.DiagnosticWithParameters1

public object JsCodePositioningStrategy : PositioningStrategy<PsiElement>() {
    override fun markDiagnostic(diagnostic: ParametrizedDiagnostic<out PsiElement>): List<TextRange> {
        [suppress("UNCHECKED_CAST")]
        val diagnosticWithParameters = diagnostic as DiagnosticWithParameters1<JetExpression, JsCallData>
        val textRange = diagnosticWithParameters.getA().reportRange
        return listOf(textRange)
    }
}
