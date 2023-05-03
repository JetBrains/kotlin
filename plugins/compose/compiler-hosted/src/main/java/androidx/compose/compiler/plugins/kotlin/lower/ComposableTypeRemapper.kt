/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.compose.compiler.plugins.kotlin.ComposeFqNames
import androidx.compose.compiler.plugins.kotlin.lower.decoys.isDecoy
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.peek
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.declarations.copyAttributes
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrIfThenElseImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeAbbreviation
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrTypeAbbreviationImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.types.isClassWithFqName
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.SymbolRemapper
import org.jetbrains.kotlin.ir.util.SymbolRenamer
import org.jetbrains.kotlin.ir.util.TypeRemapper
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.Variance

internal class DeepCopyIrTreeWithRemappedComposableTypes(
    private val context: IrPluginContext,
    private val symbolRemapper: DeepCopySymbolRemapper,
    typeRemapper: TypeRemapper,
    symbolRenamer: SymbolRenamer = SymbolRenamer.DEFAULT
) : DeepCopyPreservingMetadata(symbolRemapper, typeRemapper, symbolRenamer) {

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrSimpleFunction {
        if (declaration.symbol.isRemappedAndBound()) {
            return symbolRemapper.getReferencedSimpleFunction(declaration.symbol).owner
        }
        if (declaration.symbol.isBoundButNotRemapped()) {
            symbolRemapper.visitSimpleFunction(declaration)
        }
        return super.visitSimpleFunction(declaration).also {
            it.correspondingPropertySymbol = declaration.correspondingPropertySymbol
        }
    }
    override fun visitProperty(declaration: IrProperty): IrProperty {
        return super.visitProperty(declaration).also {
            it.copyAttributes(declaration)
        }
    }

    override fun visitFile(declaration: IrFile): IrFile {
        includeFileNameInExceptionTrace(declaration) {
            return super.visitFile(declaration)
        }
    }

    override fun visitWhen(expression: IrWhen): IrWhen {
        if (expression is IrIfThenElseImpl) {
            return IrIfThenElseImpl(
                expression.startOffset,
                expression.endOffset,
                expression.type.remapType(),
                mapStatementOrigin(expression.origin),
            ).also {
                expression.branches.mapTo(it.branches) { branch ->
                    branch.transform()
                }
            }.copyAttributes(expression)
        }
        return super.visitWhen(expression)
    }

    override fun visitConstructorCall(expression: IrConstructorCall): IrConstructorCall {
        if (!expression.symbol.isBound)
            (context as IrPluginContextImpl).linker.getDeclaration(expression.symbol)
        val ownerFn = expression.symbol.owner as? IrConstructor
        // If we are calling an external constructor, we want to "remap" the types of its signature
        // as well, since if it they are @Composable it will have its unmodified signature. These
        // types won't be traversed by default by the DeepCopyIrTreeWithSymbols so we have to
        // do it ourself here.
        if (
            ownerFn != null &&
            ownerFn.origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB &&
            ownerFn.needsComposableRemapping()
        ) {
            if (symbolRemapper.getReferencedConstructor(ownerFn.symbol) == ownerFn.symbol) {
                // Not remapped yet, so remap now.
                // Remap only once to avoid IdSignature clash (on k/js 1.7.20).
                symbolRemapper.visitConstructor(ownerFn)
                super.visitConstructor(ownerFn).also {
                    it.patchDeclarationParents(ownerFn.parent)
                }
            }
            val newCallee = symbolRemapper.getReferencedConstructor(ownerFn.symbol)

            return IrConstructorCallImpl(
                expression.startOffset, expression.endOffset,
                expression.type.remapType(),
                newCallee,
                expression.typeArgumentsCount,
                expression.constructorTypeArgumentsCount,
                expression.valueArgumentsCount,
                mapStatementOrigin(expression.origin)
            ).apply {
                copyRemappedTypeArgumentsFrom(expression)
                transformValueArguments(expression)
            }.copyAttributes(expression)
        }
        return super.visitConstructorCall(expression)
    }

    private fun IrFunction.needsComposableRemapping(): Boolean {
        if (
            needsComposableRemapping(dispatchReceiverParameter?.type) ||
            needsComposableRemapping(extensionReceiverParameter?.type) ||
            needsComposableRemapping(returnType)
        ) return true

        for (param in valueParameters) {
            if (needsComposableRemapping(param.type)) return true
        }
        return false
    }

    private fun needsComposableRemapping(type: IrType?): Boolean {
        if (type == null) return false
        if (type !is IrSimpleType) return false
        if (type.isComposable()) return true
        if (type.arguments.any { needsComposableRemapping(it.typeOrNull) }) return true
        return false
    }

    override fun visitCall(expression: IrCall): IrCall {
        val ownerFn = expression.symbol.owner as? IrSimpleFunction
        val containingClass = ownerFn?.parentClassOrNull

        // Any virtual calls on composable functions we want to make sure we update the call to
        // the right function base class (of n+1 arity). The most often virtual call to make on
        // a function instance is `invoke`, which we *already* do in the ComposeParamTransformer.
        // There are others that can happen though as well, such as `equals` and `hashCode`. In this
        // case, we want to update those calls as well.
        if (
            containingClass != null &&
            ownerFn.origin == IrDeclarationOrigin.FAKE_OVERRIDE &&
            containingClass.defaultType.isFunction() &&
            expression.dispatchReceiver?.type?.isComposable() == true
        ) {
            val realParams = containingClass.typeParameters.size - 1
            // with composer and changed
            val newArgsSize = realParams + 1 + changedParamCount(realParams, 0)
            val newFnClass = context.function(newArgsSize).owner

            var newFn = newFnClass
                .functions
                .first { it.name == ownerFn.name }

            if (symbolRemapper.getReferencedSimpleFunction(newFn.symbol) == newFn.symbol) {
                // Not remapped yet, so remap now.
                // Remap only once to avoid IdSignature clash (on k/js 1.7.20).
                symbolRemapper.visitSimpleFunction(newFn)
                newFn = super.visitSimpleFunction(newFn).also { fn ->
                    fn.overriddenSymbols = ownerFn.overriddenSymbols.map { it }
                    fn.dispatchReceiverParameter = ownerFn.dispatchReceiverParameter
                    fn.extensionReceiverParameter = ownerFn.extensionReceiverParameter
                    newFn.valueParameters.forEach { p ->
                        fn.addValueParameter(p.name.identifier, p.type)
                    }
                    fn.patchDeclarationParents(newFnClass)
                    assert(fn.body == null) { "expected body to be null" }
                }
            }

            val newCallee = symbolRemapper.getReferencedSimpleFunction(newFn.symbol)
            return shallowCopyCall(expression, newCallee).apply {
                copyRemappedTypeArgumentsFrom(expression)
                transformValueArguments(expression)
            }
        }

        // If we are calling an external function, we want to "remap" the types of its signature
        // as well, since if it is @Composable it will have its unmodified signature. These
        // functions won't be traversed by default by the DeepCopyIrTreeWithSymbols so we have to
        // do it ourself here.
        //
        // When an external declaration for a property getter/setter is transformed, we need to
        // also transform the corresponding property so that we maintain the relationship
        // `getterFun.correspondingPropertySymbol.owner.getter == getterFun`. If we do not
        // maintain this relationship inline class getters will be incorrectly compiled.
        if (
            ownerFn != null &&
            ownerFn.origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
        ) {
            if (ownerFn.correspondingPropertySymbol != null) {
                val property = ownerFn.correspondingPropertySymbol!!.owner
                // avoid java properties since they go through a different lowering and it is
                // also impossible for them to have composable types
                if (property.origin != IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB &&
                    property.getter?.needsComposableRemapping() == true
                ) {
                    if (symbolRemapper.getReferencedProperty(property.symbol) == property.symbol) {
                        // Not remapped yet, so remap now.
                        // Remap only once to avoid IdSignature clash (on k/js 1.7.20).
                        symbolRemapper.visitProperty(property)
                        visitProperty(property).also {
                            it.getter?.correspondingPropertySymbol = it.symbol
                            it.setter?.correspondingPropertySymbol = it.symbol
                            it.patchDeclarationParents(ownerFn.parent)
                            it.copyAttributes(property)
                        }
                    }
                }
            } else if (ownerFn.needsComposableRemapping()) {
                if (symbolRemapper.getReferencedSimpleFunction(ownerFn.symbol) == ownerFn.symbol) {
                    // Not remapped yet, so remap now.
                    // Remap only once to avoid IdSignature clash (on k/js 1.7.20).
                    symbolRemapper.visitSimpleFunction(ownerFn)
                    visitSimpleFunction(ownerFn).also {
                        it.correspondingPropertySymbol = null
                        it.patchDeclarationParents(ownerFn.parent)
                    }
                }
            }
            val newCallee = symbolRemapper.getReferencedSimpleFunction(ownerFn.symbol)
            return shallowCopyCall(expression, newCallee).apply {
                copyRemappedTypeArgumentsFrom(expression)
                transformValueArguments(expression)
            }
        }

        if (
            ownerFn != null &&
            ownerFn.needsComposableRemapping()
        ) {
            val newFn = visitSimpleFunction(ownerFn).also {
                it.overriddenSymbols = ownerFn.overriddenSymbols.map { override ->
                    if (override.isBound) {
                        visitSimpleFunction(override.owner).apply {
                            patchDeclarationParents(override.owner.parent)
                        }.symbol
                    } else {
                        override
                    }
                }
                it.patchDeclarationParents(ownerFn.parent)
            }
            val newCallee = symbolRemapper.getReferencedSimpleFunction(newFn.symbol)
            return shallowCopyCall(expression, newCallee).apply {
                copyRemappedTypeArgumentsFrom(expression)
                transformValueArguments(expression)
            }
        }

        return super.visitCall(expression)
    }

    private fun IrSimpleFunctionSymbol.isBoundButNotRemapped(): Boolean {
        return this.isBound && symbolRemapper.getReferencedFunction(this) == this
    }

    private fun IrSimpleFunctionSymbol.isRemappedAndBound(): Boolean {
        val symbol = symbolRemapper.getReferencedFunction(this)
        return symbol.isBound && symbol != this
    }

    /* copied verbatim from DeepCopyIrTreeWithSymbols, except with newCallee as a parameter */
    private fun shallowCopyCall(expression: IrCall, newCallee: IrSimpleFunctionSymbol): IrCall {
        return IrCallImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            newCallee,
            expression.typeArgumentsCount,
            expression.valueArgumentsCount,
            mapStatementOrigin(expression.origin),
            symbolRemapper.getReferencedClassOrNull(expression.superQualifierSymbol)
        ).apply {
            copyRemappedTypeArgumentsFrom(expression)
        }.copyAttributes(expression)
    }

    /* copied verbatim from DeepCopyIrTreeWithSymbols */
    private fun IrMemberAccessExpression<*>.copyRemappedTypeArgumentsFrom(
        other: IrMemberAccessExpression<*>
    ) {
        assert(typeArgumentsCount == other.typeArgumentsCount) {
            "Mismatching type arguments: $typeArgumentsCount vs ${other.typeArgumentsCount} "
        }
        for (i in 0 until typeArgumentsCount) {
            putTypeArgument(i, other.getTypeArgument(i)?.remapType())
        }
    }

    /* copied verbatim from DeepCopyIrTreeWithSymbols */
    private fun <T : IrMemberAccessExpression<*>> T.transformValueArguments(original: T) {
        transformReceiverArguments(original)
        for (i in 0 until original.valueArgumentsCount) {
            putValueArgument(i, original.getValueArgument(i)?.transform())
        }
    }

    /* copied verbatim from DeepCopyIrTreeWithSymbols */
    private fun <T : IrMemberAccessExpression<*>> T.transformReceiverArguments(original: T): T =
        apply {
            dispatchReceiver = original.dispatchReceiver?.transform()
            extensionReceiver = original.extensionReceiver?.transform()
        }

    private fun IrType.isComposable(): Boolean {
        return annotations.hasAnnotation(ComposeFqNames.Composable)
    }
}

class ComposerTypeRemapper(
    private val context: IrPluginContext,
    private val symbolRemapper: SymbolRemapper,
    private val composerType: IrType
) : TypeRemapper {

    lateinit var deepCopy: IrElementTransformerVoid

    private val scopeStack = mutableListOf<IrTypeParametersContainer>()

    override fun enterScope(irTypeParametersContainer: IrTypeParametersContainer) {
        scopeStack.add(irTypeParametersContainer)
    }

    override fun leaveScope() {
        scopeStack.pop()
    }

    private fun IrType.isComposable(): Boolean {
        return annotations.hasAnnotation(ComposeFqNames.Composable)
    }

    private val IrConstructorCall.annotationClass
        get() = this.symbol.owner.returnType.classifierOrNull

    private fun List<IrConstructorCall>.hasAnnotation(fqName: FqName): Boolean =
        any { it.annotationClass?.isClassWithFqName(fqName.toUnsafe()) ?: false }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun IrType.isFunction(): Boolean {
        val classifier = classifierOrNull ?: return false
        val name = classifier.descriptor.name.asString()
        if (!name.startsWith("Function")) return false
        classifier.descriptor.name
        return true
    }

    override fun remapType(type: IrType): IrType {
        if (type !is IrSimpleType) return type
        if (!type.isFunction()) return underlyingRemapType(type)
        if (!type.isComposable()) return underlyingRemapType(type)
        // do not convert types for decoys
        if (scopeStack.peek()?.isDecoy() == true) {
            return underlyingRemapType(type)
        }

        val oldIrArguments = type.arguments
        val realParams = oldIrArguments.size - 1
        var extraArgs = listOf(
            // composer param
            makeTypeProjection(
                composerType,
                Variance.INVARIANT
            )
        )
        val changedParams = changedParamCount(realParams, 1)
        extraArgs = extraArgs + (0 until changedParams).map {
            makeTypeProjection(context.irBuiltIns.intType, Variance.INVARIANT)
        }
        val newIrArguments =
            oldIrArguments.subList(0, oldIrArguments.size - 1) +
                extraArgs +
                oldIrArguments.last()

        val newArgSize = oldIrArguments.size - 1 + extraArgs.size
        val functionCls = context.function(newArgSize)

        return IrSimpleTypeImpl(
            null,
            functionCls,
            type.nullability,
            newIrArguments.map { remapTypeArgument(it) },
            type.annotations.filter { !it.isComposableAnnotation() }.map {
                it.transform(deepCopy, null) as IrConstructorCall
            },
            null
        )
    }

    private fun underlyingRemapType(type: IrSimpleType): IrType {
        return IrSimpleTypeImpl(
            null,
            symbolRemapper.getReferencedClassifier(type.classifier),
            type.nullability,
            type.arguments.map { remapTypeArgument(it) },
            type.annotations.map { it.transform(deepCopy, null) as IrConstructorCall },
            type.abbreviation?.remapTypeAbbreviation()
        )
    }

    private fun remapTypeArgument(typeArgument: IrTypeArgument): IrTypeArgument =
        if (typeArgument is IrTypeProjection)
            makeTypeProjection(this.remapType(typeArgument.type), typeArgument.variance)
        else
            typeArgument

    private fun IrTypeAbbreviation.remapTypeAbbreviation() =
        IrTypeAbbreviationImpl(
            symbolRemapper.getReferencedTypeAlias(typeAlias),
            hasQuestionMark,
            arguments.map { remapTypeArgument(it) },
            annotations
        )
}

private fun IrConstructorCall.isComposableAnnotation() =
    this.symbol.owner.parent.fqNameForIrSerialization == ComposeFqNames.Composable
