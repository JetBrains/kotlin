/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package org.jetbrains.kotlin.scripting.compiler.plugin.irLowerings

import org.jetbrains.kotlin.backend.common.extensions.ExperimentalAPIForScriptingPlugin
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.ClosureAnnotator
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.jvm.JvmInnerClassesSupport
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmBackendErrors
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

internal data class ScriptLikeToClassTransformerContext(
    val valueParameterForScriptThis: IrValueParameterSymbol?,
    val fieldForScriptThis: IrFieldSymbol?,
    val valueParameterForFieldReceiver: IrValueParameterSymbol?,
    val isInScriptConstructor: Boolean,
    val topLevelDeclaration: IrDeclaration? = null,
) {
    companion object {
        fun makeRootContext(
            valueParameterForScriptThis: IrValueParameterSymbol?,
            isInScriptConstructor: Boolean,
            topLevelDeclaration: IrDeclaration? = null
        ) =
            ScriptLikeToClassTransformerContext(
                valueParameterForScriptThis = valueParameterForScriptThis,
                fieldForScriptThis = null,
                valueParameterForFieldReceiver = null,
                isInScriptConstructor = isInScriptConstructor,
                topLevelDeclaration = topLevelDeclaration
            )
    }
}

internal data class ScriptFixLambdasTransformerContext(
    val insideTopLevelDestructuringDeclaration: Boolean = false,
    val valueParameterToReplaceWithScript: IrValueParameter? = null
)

internal open class ScriptLikeAccessCallsGenerator(
    val context: IrPluginContext,
    val targetClassReceiver: IrValueParameter,
    val implicitReceiversFieldsWithParameters: Collection<Pair<IrField, IrValueParameter>>,
) {

    internal fun getDispatchReceiverExpression(
        data: ScriptLikeToClassTransformerContext,
        expression: IrDeclarationReference,
        receiverType: IrType,
        origin: IrStatementOrigin?,
        originalReceiverParameter: IrValueParameter?,
    ): IrExpression? {
        return if (receiverType == targetClassReceiver.type) {
            getAccessCallForSelf(data, expression.startOffset, expression.endOffset, origin, originalReceiverParameter)
        } else {
            getAccessCallForImplicitReceiver(data, expression, receiverType, origin, originalReceiverParameter)
        }
    }

    internal fun getAccessCallForSelf(
        data: ScriptLikeToClassTransformerContext,
        startOffset: Int,
        endOffset: Int,
        origin: IrStatementOrigin?,
        originalReceiverParameter: IrValueParameter?
    ): IrExpression? = when {
        // do not touch receiver of a different type
        originalReceiverParameter != null && originalReceiverParameter.type != targetClassReceiver.type ->
            null

        data.fieldForScriptThis != null ->
            IrGetFieldImpl(
                startOffset, endOffset,
                data.fieldForScriptThis,
                targetClassReceiver.type,
                origin
            ).apply {
                receiver =
                    IrGetValueImpl(
                        startOffset, endOffset,
                        data.valueParameterForFieldReceiver!!.owner.type,
                        data.valueParameterForFieldReceiver,
                        origin
                    )
            }

        data.valueParameterForScriptThis != null ->
            IrGetValueImpl(
                startOffset, endOffset,
                targetClassReceiver.type,
                data.valueParameterForScriptThis,
                origin
            )

        else -> error("Unexpected script transformation state: $data")
    }

    open fun getAccessCallForImplicitReceiver(
        data: ScriptLikeToClassTransformerContext,
        expression: IrDeclarationReference,
        receiverType: IrType,
        expressionOrigin: IrStatementOrigin?,
        originalReceiverParameter: IrValueParameter?
    ): IrExpression? {
        // implicit receivers have priority (as per descriptor outer scopes)
        implicitReceiversFieldsWithParameters.firstOrNull {
            if (originalReceiverParameter != null) it.second == originalReceiverParameter
            else it.second.type == receiverType
        }?.let { (field, param) ->
            val builder = context.irBuiltIns.createIrBuilder(expression.symbol)
            return if (data.isInScriptConstructor) {
                builder.irGet(param.type, param.symbol)
            } else {
                val selfReceiver =
                    getAccessCallForSelf(data, expression.startOffset, expression.endOffset, expressionOrigin, null)
                builder.irGetField(selfReceiver, field)
            }
        }
        return null
    }
}

internal abstract class ScriptLikeToClassTransformer(
    val context: IrPluginContext,
    val irScriptLike: IrDeclaration,
    val irTargetClass: IrClass,
    val targetClassReceiver: IrValueParameter,
    val typeRemapper: TypeRemapper,
    open val accessCallsGenerator: ScriptLikeAccessCallsGenerator,
    val capturingClasses: Set<IrClassImpl>,
    val needsReceiverProcessing: Boolean,
) : IrTransformer<ScriptLikeToClassTransformerContext>() {

    private val innerClassesSupport = JvmInnerClassesSupport(context.irFactory)

    private fun IrType.remapType() = typeRemapper.remapType(this)

    val capturingClassesConstructors = mutableMapOf<IrConstructor, IrClassImpl>().apply {
        capturingClasses.forEach { c ->
            c.declarations.forEach { d ->
                if (d is IrConstructor) {
                    put(d, c)
                }
            }
        }
    }

    private fun IrDeclaration.transformParent() {
        if (parent == irScriptLike) {
            parent = irTargetClass
        }
    }

    private fun IrMutableAnnotationContainer.transformAnnotations(data: ScriptLikeToClassTransformerContext) {
        annotations = annotations.transform(data)
    }

    private inline fun <reified T : IrElement> T.transform(data: ScriptLikeToClassTransformerContext) =
        transform(this@ScriptLikeToClassTransformer, data) as T

    private inline fun <reified T : IrElement> List<T>.transform(data: ScriptLikeToClassTransformerContext) =
        map { it.transform(data) }

    private fun <T : IrFunction> T.transformFunctionChildren(data: ScriptLikeToClassTransformerContext): T =
        apply {
            transformAnnotations(data)
            typeRemapper.withinScope(this) {
                val newDispatchReceiverParameter = dispatchReceiverParameter?.transform(data) ?: run {
                    if (this is IrSimpleFunction && this.isCurrentScriptTopLevelDeclaration(data)) {
                        createThisReceiverParameter(context, IrDeclarationOrigin.SCRIPT_THIS_RECEIVER, targetClassReceiver.type)
                    } else null
                }
                val isInScriptConstructor =
                    this@transformFunctionChildren is IrConstructor && (parent == irScriptLike || parent == irTargetClass)
                val dataForChildren =
                    when {
                        newDispatchReceiverParameter == null -> data

                        newDispatchReceiverParameter.type == targetClassReceiver.type ->
                            ScriptLikeToClassTransformerContext(newDispatchReceiverParameter.symbol, null, null, isInScriptConstructor)

                        newDispatchReceiverParameter.type == data.valueParameterForFieldReceiver?.owner?.type ->
                            ScriptLikeToClassTransformerContext(
                                null,
                                data.fieldForScriptThis,
                                newDispatchReceiverParameter.symbol,
                                isInScriptConstructor
                            )

                        else -> data
                    }
                dispatchReceiverParameter = newDispatchReceiverParameter
                extensionReceiverParameter = extensionReceiverParameter?.transform(dataForChildren)
                returnType = returnType.remapType()
                valueParameters = valueParameters.transform(dataForChildren)
                body = body?.transform(dataForChildren)
            }
        }

    private fun IrTypeParameter.remapSuperTypes(): IrTypeParameter = apply {
        superTypes = superTypes.map { it.remapType() }
    }

    private fun unexpectedElement(element: IrElement): Nothing =
        throw IllegalArgumentException("Unsupported element type: $element")

    override fun visitModuleFragment(declaration: IrModuleFragment, data: ScriptLikeToClassTransformerContext): IrModuleFragment =
        unexpectedElement(declaration)

    override fun visitExternalPackageFragment(declaration: IrExternalPackageFragment, data: ScriptLikeToClassTransformerContext) =
        unexpectedElement(declaration)

    override fun visitFile(declaration: IrFile, data: ScriptLikeToClassTransformerContext): IrFile = unexpectedElement(declaration)
    override fun visitScript(declaration: IrScript, data: ScriptLikeToClassTransformerContext): IrStatement = unexpectedElement(declaration)

    override fun visitDeclaration(declaration: IrDeclarationBase, data: ScriptLikeToClassTransformerContext): IrStatement =
        declaration.apply {
            transformParent()
            transformAnnotations(data)
            transformChildren(this@ScriptLikeToClassTransformer, data)
        }

    override fun visitClass(declaration: IrClass, data: ScriptLikeToClassTransformerContext): IrClass = declaration.apply {
        superTypes = superTypes.map {
            it.remapType()
        }
        transformParent()
        var dataForChildren = data
        (declaration as? IrClassImpl)?.let {
            if (it in capturingClasses) {
                it.isInner = true
                dataForChildren =
                    ScriptLikeToClassTransformerContext(
                        null, innerClassesSupport.getOuterThisField(it).symbol, it.thisReceiver?.symbol, false
                    )
            }
        }
        transformAnnotations(dataForChildren)
        transformChildren(this@ScriptLikeToClassTransformer, dataForChildren)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: ScriptLikeToClassTransformerContext): IrSimpleFunction =
        declaration.apply {
            transformParent()
            transformFunctionChildren(data)
        }

    override fun visitConstructor(declaration: IrConstructor, data: ScriptLikeToClassTransformerContext): IrConstructor =
        declaration.apply {
            if (declaration in capturingClassesConstructors) {
                declaration.dispatchReceiverParameter =
                    declaration.createThisReceiverParameter(context, IrDeclarationOrigin.INSTANCE_RECEIVER, targetClassReceiver.type)
            }
            transformParent()
            transformFunctionChildren(data)
        }

    override fun visitVariable(declaration: IrVariable, data: ScriptLikeToClassTransformerContext): IrVariable = declaration.apply {
        type = type.remapType()
        visitDeclaration(declaration, data)
    }

    override fun visitTypeParameter(declaration: IrTypeParameter, data: ScriptLikeToClassTransformerContext): IrTypeParameter =
        declaration.apply {
            remapSuperTypes()
            visitDeclaration(declaration, data)
        }

    override fun visitValueParameter(declaration: IrValueParameter, data: ScriptLikeToClassTransformerContext): IrValueParameter =
        declaration.apply {
            type = type.remapType()
            varargElementType = varargElementType?.remapType()
            visitDeclaration(declaration, data)
        }

    override fun visitTypeAlias(declaration: IrTypeAlias, data: ScriptLikeToClassTransformerContext): IrTypeAlias = declaration.apply {
        expandedType = expandedType.remapType()
        visitDeclaration(declaration, data)
    }

    override fun visitVararg(expression: IrVararg, data: ScriptLikeToClassTransformerContext): IrVararg = expression.apply {
        type = type.remapType()
        varargElementType = varargElementType.remapType()
        transformChildren(this@ScriptLikeToClassTransformer, data)
    }

    override fun visitSpreadElement(spread: IrSpreadElement, data: ScriptLikeToClassTransformerContext): IrSpreadElement = spread.apply {
        transformChildren(this@ScriptLikeToClassTransformer, data)
    }

    override fun visitExpression(expression: IrExpression, data: ScriptLikeToClassTransformerContext): IrExpression = expression.apply {
        type = type.remapType()
        transformChildren(this@ScriptLikeToClassTransformer, data)
    }

    override fun visitClassReference(expression: IrClassReference, data: ScriptLikeToClassTransformerContext): IrClassReference =
        expression.apply {
            type = type.remapType()
            classType = classType.remapType()
            transformChildren(this@ScriptLikeToClassTransformer, data)
        }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: ScriptLikeToClassTransformerContext): IrTypeOperatorCall =
        expression.apply {
            type = type.remapType()
            typeOperand = typeOperand.remapType()
            transformChildren(this@ScriptLikeToClassTransformer, data)
        }

    override fun visitMemberAccess(expression: IrMemberAccessExpression<*>, data: ScriptLikeToClassTransformerContext): IrExpression {
        for (i in expression.typeArguments.indices) {
            expression.typeArguments[i] = expression.typeArguments[i]?.remapType()
        }
        if (expression.dispatchReceiver == null && (expression.symbol.owner as? IrDeclaration)?.needsScriptReceiver() == true) {
            val memberAccessTargetReceiverType = when (val callee = expression.symbol.owner) {
                is IrFunction -> callee.dispatchReceiverParameter?.type
                is IrProperty -> callee.getter?.dispatchReceiverParameter?.type
                else -> null
            }
            val dispatchReceiver =
                if (memberAccessTargetReceiverType != null && memberAccessTargetReceiverType != targetClassReceiver.type)
                    accessCallsGenerator.getAccessCallForImplicitReceiver(
                        data, expression, memberAccessTargetReceiverType, expression.origin, originalReceiverParameter = null
                    )
                else
                    accessCallsGenerator.getAccessCallForSelf(
                        data, expression.startOffset, expression.endOffset, expression.origin, originalReceiverParameter = null
                    )
            expression.insertDispatchReceiver(dispatchReceiver)
        }
        return super.visitMemberAccess(expression, data) as IrExpression
    }

    override fun visitFieldAccess(expression: IrFieldAccessExpression, data: ScriptLikeToClassTransformerContext): IrExpression {
        if (expression.receiver == null && expression.symbol.owner.parent.let { it == irScriptLike || it == irTargetClass }) {
            expression.receiver =
                accessCallsGenerator.getAccessCallForSelf(
                    data, expression.startOffset, expression.endOffset, expression.origin, originalReceiverParameter = null
                )
        }
        return super.visitFieldAccess(expression, data)
    }

    override fun visitConstructorCall(expression: IrConstructorCall, data: ScriptLikeToClassTransformerContext): IrExpression {
        if (expression.dispatchReceiver == null) {
            // the first part of the expression triggers if the ctor itself is transformed before call,
            // but the second part is not enough by itself if capturing class is defined in an earlier snippet (we are not keeping
            // capturing classes from earlier snippets)
            val ctorDispatchReceiverType = expression.symbol.owner.dispatchReceiverParameter?.type
                ?: if (capturingClassesConstructors.keys.any { it.symbol == expression.symbol }) targetClassReceiver.type else null
            if (ctorDispatchReceiverType != null) {
                accessCallsGenerator.getDispatchReceiverExpression(
                    data, expression, ctorDispatchReceiverType, expression.origin, null
                )?.let {
                    expression.insertDispatchReceiver(it)
                }
            }
        }
        return super.visitConstructorCall(expression, data) as IrExpression
    }

    override fun visitGetValue(expression: IrGetValue, data: ScriptLikeToClassTransformerContext): IrExpression {
        val correspondingValueParameter = expression.symbol.owner as? IrValueParameter
        when {
            correspondingValueParameter != null && needsReceiverProcessing && isValidNameForReceiver(correspondingValueParameter.name) -> {
                val newExpression = accessCallsGenerator.getDispatchReceiverExpression(
                    data, expression, correspondingValueParameter.type, expression.origin, correspondingValueParameter
                )
                if (newExpression != null) {
                    return super.visitExpression(newExpression, data)
                }
            }
        }
        return super.visitGetValue(expression, data)
    }

    protected open fun isValidNameForReceiver(name: Name) = name == SpecialNames.THIS

    private fun IrDeclaration.isCurrentScriptTopLevelDeclaration(data: ScriptLikeToClassTransformerContext): Boolean {
        if (data.topLevelDeclaration == null || (parent != irScriptLike && parent != irTargetClass)) return false
        val declarationToCompare = if (this is IrFunction) this.propertyIfAccessor else this
        // TODO: might be fragile, if we'll start to use transformed declaration on either side, try to find a way to detect or avoid (KT-72943)
        return declarationToCompare == data.topLevelDeclaration
    }

    private fun IrDeclaration.needsScriptReceiver() =
        when (this) {
            is IrFunction -> this.dispatchReceiverParameter
            is IrProperty -> {
                this.getter?.takeIf {
                    // without this exception, the PropertyReferenceLowering generates `clinit` with an attempt to use script as receiver
                    // TODO: find whether it is a valid exception and maybe how to make it more obvious (KT-72942)
                    it.origin != IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR
                }?.dispatchReceiverParameter
            }
            else -> null
        }?.origin == IrDeclarationOrigin.SCRIPT_THIS_RECEIVER
}

internal class ScriptFixLambdasTransformer(val irScriptClass: IrClass) : IrTransformer<ScriptFixLambdasTransformerContext>() {

    private fun unexpectedElement(element: IrElement): Nothing =
        throw IllegalArgumentException("Unsupported element type: $element")

    override fun visitModuleFragment(declaration: IrModuleFragment, data: ScriptFixLambdasTransformerContext): IrModuleFragment =
        unexpectedElement(declaration)

    override fun visitExternalPackageFragment(
        declaration: IrExternalPackageFragment,
        data: ScriptFixLambdasTransformerContext
    ): IrExternalPackageFragment =
        unexpectedElement(declaration)

    override fun visitFile(declaration: IrFile, data: ScriptFixLambdasTransformerContext): IrFile = unexpectedElement(declaration)
    override fun visitScript(declaration: IrScript, data: ScriptFixLambdasTransformerContext): IrScript = unexpectedElement(declaration)

    override fun visitGetValue(expression: IrGetValue, data: ScriptFixLambdasTransformerContext): IrExpression {
        if (data.valueParameterToReplaceWithScript == expression.symbol.owner) {
            val newGetValue = IrGetValueImpl(
                expression.startOffset, expression.endOffset,
                expression.type,
                irScriptClass.thisReceiver!!.symbol,
                expression.origin
            )
            return super.visitGetValue(newGetValue, data)
        } else return super.visitGetValue(expression, data)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: ScriptFixLambdasTransformerContext): IrSimpleFunction =
        with(declaration) {
            if (data.insideTopLevelDestructuringDeclaration && origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA) {
                visibility = DescriptorVisibilities.LOCAL
                val dataForChildren =
                    if (dispatchReceiverParameter?.type == irScriptClass.defaultType) {
                        val oldDispatchReceiver = dispatchReceiverParameter
                        dispatchReceiverParameter = null
                        data.copy(valueParameterToReplaceWithScript = oldDispatchReceiver)
                    } else data
                super.visitSimpleFunction(this, dataForChildren)
            } else {
                super.visitSimpleFunction(this, data)
            }
        } as IrSimpleFunction

    override fun visitComposite(expression: IrComposite, data: ScriptFixLambdasTransformerContext): IrComposite {
        val dataForChildren =
            if (expression.origin == IrStatementOrigin.DESTRUCTURING_DECLARATION &&
                expression.statements.firstIsInstanceOrNull<IrDeclaration>()?.parent == irScriptClass
            ) {
                data.copy(insideTopLevelDestructuringDeclaration = true)
            } else {
                data
            }
        return super.visitComposite(expression, dataForChildren) as IrComposite
    }
}


internal fun IrDeclarationParent.createThisReceiverParameter(
    context: IrPluginContext,
    origin: IrDeclarationOrigin,
    type: IrType
): IrValueParameter =
    context.irFactory.createValueParameter(
        startOffset = startOffset,
        endOffset = endOffset,
        origin = origin,
        kind = IrParameterKind.DispatchReceiver,
        name = SpecialNames.THIS,
        type = type,
        isAssignable = false,
        symbol = IrValueParameterSymbolImpl(),
        varargElementType = null,
        isCrossinline = false,
        isNoinline = false,
        isHidden = false,
    ).also {
        it.parent = this
    }

internal fun patchDeclarationsDispatchReceiver(statements: List<IrStatement>, context: IrPluginContext, scriptClassReceiverType: IrType) {

    fun IrFunction.addScriptDispatchReceiverIfNeeded() {
        if (dispatchReceiverParameter == null) {
            dispatchReceiverParameter =
                createThisReceiverParameter(context, IrDeclarationOrigin.SCRIPT_THIS_RECEIVER, scriptClassReceiverType)
        }
    }

    statements.forEach { scriptStatement ->
        when (scriptStatement) {
            is IrProperty -> {
                scriptStatement.getter?.addScriptDispatchReceiverIfNeeded()
                scriptStatement.setter?.addScriptDispatchReceiverIfNeeded()
            }
            is IrFunction -> scriptStatement.addScriptDispatchReceiverIfNeeded()
        }
    }
}

@OptIn(ExperimentalAPIForScriptingPlugin::class)
internal fun Collection<IrClass>.collectCapturersByReceivers(
    context: IrPluginContext,
    parentDeclaration: IrDeclaration,
    externalReceivers: Set<IrType>,
): Set<IrClassImpl> {
    val annotator = ClosureAnnotator(parentDeclaration, parentDeclaration)
    val capturingClasses = mutableSetOf<IrClassImpl>()

    val collector = object : IrVisitorVoid() {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitClass(declaration: IrClass) {
            if (declaration is IrClassImpl && !declaration.isInner) {
                val closure = annotator.getClassClosure(declaration)
                if (closure.capturedValues.any { it.owner.type in externalReceivers }) {
                    fun reportError(factory: KtDiagnosticFactory1<String>, name: Name? = null) {
                        context.diagnosticReporter.at(declaration).report(factory, (name ?: declaration.name).asString())
                    }
                    when {
                        declaration.isInterface -> reportError(JvmBackendErrors.SCRIPT_CAPTURING_INTERFACE)
                        declaration.isEnumClass -> reportError(JvmBackendErrors.SCRIPT_CAPTURING_ENUM)
                        declaration.isEnumEntry -> reportError(JvmBackendErrors.SCRIPT_CAPTURING_ENUM_ENTRY)
                        // TODO: ClosureAnnotator is not catching companion's closures, so the following reporting never happens. Make it work or drop
                        declaration.isCompanion -> reportError(
                            JvmBackendErrors.SCRIPT_CAPTURING_OBJECT, SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
                        )
                        declaration.kind.isSingleton -> reportError(JvmBackendErrors.SCRIPT_CAPTURING_OBJECT)

                        declaration.isClass ->
                            if (declaration.parent != parentDeclaration) {
                                if ((declaration.parent as? IrClass)?.isInner == false) {
                                    context.diagnosticReporter.at(declaration).report(
                                        JvmBackendErrors.SCRIPT_CAPTURING_NESTED_CLASS,
                                        declaration.name.asString(),
                                        ((declaration.parent as? IrDeclarationWithName)?.name
                                            ?: SpecialNames.NO_NAME_PROVIDED).asString()
                                    )
                                }
                            } else {
                                capturingClasses.add(declaration)
                            }
                    }
                }
            }
            super.visitClass(declaration)
        }
    }
    forEach(collector::visitClass)
    return capturingClasses
}
