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

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.isObject
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsImport
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext

object JsImportOptionsChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        val trace = context.trace

        val jsImportName = AnnotationsUtils.getImportName(descriptor)
        val hasJsImportDefault = AnnotationsUtils.hasImportDefault(descriptor)
        val hasJsImportNamespace = AnnotationsUtils.hasImportNamespace(descriptor)

        if (jsImportName == null && !hasJsImportDefault && !hasJsImportNamespace) return

        val thereIsMixOfOptions = listOf(jsImportName != null, hasJsImportDefault, hasJsImportNamespace).count { it } > 1
        if (thereIsMixOfOptions) {
            trace.report(ErrorsJs.JS_IMPORT_DEFAULT_AND_NAMED.on(declaration))
        }

        if (hasJsImportNamespace && (descriptor !is ClassDescriptor || !descriptor.kind.isObject)) {
            trace.report(ErrorsJs.JS_IMPORT_NAMESPACE_ON_NON_OBJECT_DECLARATION.on(declaration))
        }

        val hasJsImport = AnnotationsUtils.getImportSource(descriptor) != null

        if (!hasJsImport && !AnnotationsUtils.getContainingFileAnnotations(context.trace.bindingContext, descriptor)
                .any { JsImport.asSingleFqName() == it.fqName }
        ) {
            trace.report(ErrorsJs.JS_IMPORT_OPTION_WITHOUT_JS_IMPORT.on(declaration))
            return
        }

        if (hasJsImportNamespace && !hasJsImport) {
            trace.report(ErrorsJs.JS_IMPORT_NAMESPACE_WITH_FILE_JS_IMPORT.on(declaration))
        }
    }
}
