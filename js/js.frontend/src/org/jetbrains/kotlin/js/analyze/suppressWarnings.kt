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

package org.jetbrains.kotlin.js.analyze

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.name.JsStandardClassIds
import org.jetbrains.kotlin.resolve.checkers.PlatformDiagnosticSuppressor

private val nativeAnnotations = JsStandardClassIds.Annotations.nativeAnnotations.map { it.asSingleFqName() }

private fun DeclarationDescriptor.isLexicallyInsideJsNative(): Boolean {
    var descriptor: DeclarationDescriptor = this
    while (true) {
        val annotations = descriptor.annotations
        if (!annotations.isEmpty() && nativeAnnotations.any(annotations::hasAnnotation)) return true
        descriptor = descriptor.containingDeclaration ?: break
    }
    return false
}

object JsNativeDiagnosticSuppressor : PlatformDiagnosticSuppressor {
    override fun shouldReportUnusedParameter(parameter: VariableDescriptor): Boolean = !parameter.isLexicallyInsideJsNative()

    override fun shouldReportNoBody(descriptor: CallableMemberDescriptor): Boolean = !descriptor.isLexicallyInsideJsNative()
}
