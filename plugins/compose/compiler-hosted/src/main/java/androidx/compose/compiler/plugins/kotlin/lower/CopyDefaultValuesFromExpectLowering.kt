/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.compiler.plugins.kotlin.lower

import androidx.compose.compiler.plugins.kotlin.hasComposableAnnotation
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.multiplatform.findCompatibleExpectsForActual

/**
 * [ComposableFunctionBodyTransformer] relies on presence of default values in
 * Composable functions' parameters.
 * If Composable function is declared as `expect fun` with default value parameter, then
 * [ComposableFunctionBodyTransformer] will not find any default value in `actual fun` - IrFunction.
 *
 * [CopyDefaultValuesFromExpectLowering] sets default values to parameters of actual functions by
 * taking them from their corresponding `expect fun` declarations.
 * This lowering needs to run before [ComposableFunctionBodyTransformer] and
 * before [ComposerParamTransformer].
 *
 * Fixes https://github.com/JetBrains/compose-jb/issues/1407
 */
@OptIn(ObsoleteDescriptorBasedAPI::class)
class CopyDefaultValuesFromExpectLowering : ModuleLoweringPass {

    override fun lower(module: IrModuleFragment) {
        // it uses FunctionDescriptor since current API (findCompatibleExpectedForActual)
        // can return only a descriptor
        val expectComposables = mutableMapOf<FunctionDescriptor, IrFunction>()

        // first pass to find expect functions with default values
        module.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.isExpect && declaration.hasComposableAnnotation()) {
                    val hasDefaultValues = declaration.valueParameters.any {
                        it.defaultValue != null
                    }
                    if (hasDefaultValues) {
                        expectComposables[declaration.descriptor] = declaration
                    }
                }
                return super.visitFunction(declaration)
            }
        })

        // second pass to set corresponding default values
        module.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.descriptor.isActual && declaration.hasComposableAnnotation()) {
                    val compatibleExpects = declaration.descriptor.findCompatibleExpectsForActual {
                        module.descriptor == it
                    }
                    if (compatibleExpects.isNotEmpty()) {
                        val expectFun = compatibleExpects.firstOrNull {
                            it in expectComposables
                        }?.let {
                            expectComposables[it]
                        }

                        if (expectFun != null) {
                            declaration.valueParameters.forEachIndexed { index, it ->
                                it.defaultValue =
                                    it.defaultValue ?: expectFun.valueParameters[index].defaultValue
                            }
                        }
                    }
                }
                return super.visitFunction(declaration)
            }
        })
    }
}