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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.descriptorUtils.getKotlinOrJsName
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module

class JsExportedDeclarationNameClashChecker : DeclarationChecker {
    private val alreadyUsedExportedNames = mutableMapOf<ExportedName, Pair<KtDeclaration, DeclarationDescriptor>>()

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (!descriptor.isTopLevelInPackage() || descriptor.hasJsExportIgnore()) return

        val containingFile = descriptor.containingDeclaration as? PackageFragmentDescriptor

        if (!descriptor.hasJsExport() && containingFile?.hasJsExport() != true) return

        val trace = context.trace
        val exportedName = descriptor.calculateExportedName()
        val declarationWithTheSameExportedName = alreadyUsedExportedNames[exportedName]

        if (declarationWithTheSameExportedName != null) {
            val (duplicateDeclaration, duplicateDescriptor) = declarationWithTheSameExportedName
            trace.report(ErrorsJs.EXPORTED_NAME_CLASH.on(declaration, exportedName.name, duplicateDescriptor))
            trace.report(ErrorsJs.EXPORTED_NAME_CLASH.on(duplicateDeclaration, exportedName.name, descriptor))
            return
        }

        alreadyUsedExportedNames[exportedName] = declaration to descriptor
    }

    private fun DeclarationDescriptor.hasJsExport(): Boolean = AnnotationsUtils.getJsExportAnnotation(this) != null
    private fun DeclarationDescriptor.hasJsExportIgnore(): Boolean = AnnotationsUtils.getJsExportIgnoreAnnotation(this) != null

    private fun DeclarationDescriptor.calculateExportedName(): ExportedName {
        return ExportedName(module, getKotlinOrJsName())
    }

    private data class ExportedName(val module: ModuleDescriptor, val name: String)
}
