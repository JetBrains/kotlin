/*
 * Copyright 2023 The Android Open Source Project
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

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrLocalDelegatedProperty
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.DeepCopyIrTreeWithSymbols
import org.jetbrains.kotlin.ir.util.SymbolRemapper
import org.jetbrains.kotlin.ir.util.SymbolRenamer
import org.jetbrains.kotlin.ir.util.TypeRemapper

internal open class DeepCopyPreservingMetadata(
    symbolRemapper: SymbolRemapper,
    typeRemapper: TypeRemapper,
    symbolRenamer: SymbolRenamer
) : DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper, symbolRenamer) {
    override fun visitFile(declaration: IrFile): IrFile =
        super.visitFile(declaration).apply {
            metadata = declaration.metadata
        }

    override fun visitClass(declaration: IrClass): IrClass =
        super.visitClass(declaration).apply {
            metadata = declaration.metadata
        }

    override fun visitConstructor(declaration: IrConstructor): IrConstructor =
        super.visitConstructor(declaration).apply {
            metadata = declaration.metadata
        }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrSimpleFunction =
        super.visitSimpleFunction(declaration).apply {
            metadata = declaration.metadata
        }

    override fun visitProperty(declaration: IrProperty): IrProperty =
        super.visitProperty(declaration).apply {
            metadata = declaration.metadata
        }

    override fun visitField(declaration: IrField): IrField =
        super.visitField(declaration).apply {
            metadata = declaration.metadata
        }

    override fun visitLocalDelegatedProperty(
        declaration: IrLocalDelegatedProperty
    ): IrLocalDelegatedProperty =
        super.visitLocalDelegatedProperty(declaration).apply {
            metadata = declaration.metadata
        }
}