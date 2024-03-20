/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin

import androidx.compose.compiler.plugins.kotlin.lower.dumpSrc
import org.jetbrains.kotlin.backend.common.CheckIrElementVisitor
import org.jetbrains.kotlin.backend.common.IrValidatorConfig
import org.jetbrains.kotlin.backend.common.ScopeValidator
import org.jetbrains.kotlin.backend.common.checkDeclarationParents
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

fun validateIr(fragment: IrModuleFragment, irBuiltIns: IrBuiltIns) {
    val validatorConfig = IrValidatorConfig(
        abortOnError = true,
        ensureAllNodesAreDifferent = true,
        checkTypes = false, // This should be enabled, the fact this doesn't work is a Compose bug.
        checkDescriptors = false,
        checkProperties = true,
        checkScopes = false
    )
    fragment.accept(IrValidator(irBuiltIns, validatorConfig), null)
    fragment.checkDeclarationParents()
}

// TODO: Replace with IrValidator validator when upstream is compatible with PluginContext
class IrValidator(val irBuiltIns: IrBuiltIns, val config: IrValidatorConfig) :
    IrElementVisitorVoid {

    var currentFile: IrFile? = null

    override fun visitFile(declaration: IrFile) {
        currentFile = declaration
        super.visitFile(declaration)
        if (config.checkScopes) {
            ScopeValidator(this::error).check(declaration)
        }
    }

    private fun error(element: IrElement, message: String) {
        throw Error(
            "Validation error ($message) for ${element.dumpSrc()}...  ${element.render()} in" +
                " ${currentFile?.name ?: "???"}"
        )
    }

    private val elementChecker = CheckIrElementVisitor(irBuiltIns, this::error, config)

    override fun visitElement(element: IrElement) {
        element.acceptVoid(elementChecker)
        element.acceptChildrenVoid(this)
    }
}
