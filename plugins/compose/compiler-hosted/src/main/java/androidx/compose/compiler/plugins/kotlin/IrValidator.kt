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
import org.jetbrains.kotlin.backend.common.IrValidationError
import org.jetbrains.kotlin.backend.common.performBasicIrValidation
import org.jetbrains.kotlin.config.IrVerificationMode
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.util.render

fun validateIr(fragment: IrModuleFragment, irBuiltIns: IrBuiltIns, mode: IrVerificationMode) {
    if (mode == IrVerificationMode.NONE) return
    performBasicIrValidation(
        fragment,
        irBuiltIns,
        checkProperties = true,
        checkTypes = false, // This should be enabled, the fact this doesn't work is a Compose bug.
    ) { file, element, message ->
        throw IrValidationError(
            "Validation error ($message) for ${element.dumpSrc()}...  ${element.render()} in" +
                    " ${file?.name ?: "???"}"
        )
    }
}
