/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.cgen.*
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.ir.allOverriddenFunctions
import org.jetbrains.kotlin.backend.konan.ir.buildSimpleAnnotation
import org.jetbrains.kotlin.backend.konan.ir.getSuperClassNotAny
import org.jetbrains.kotlin.backend.konan.ir.konanLibrary
import org.jetbrains.kotlin.backend.konan.llvm.IntrinsicType
import org.jetbrains.kotlin.backend.konan.llvm.tryGetIntrinsicType
import org.jetbrains.kotlin.backend.konan.serialization.isFromCInteropLibrary
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.objcinterop.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.isSubtypeOf
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.konan.ForeignExceptionMode
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NativeStandardInteropNames.objCActionClassId
import org.jetbrains.kotlin.native.interop.ObjCMethodInfo

internal class InteropLowering(val generationState: NativeGenerationState) : FileLoweringPass, BodyLoweringPass {
    override fun lower(irFile: IrFile) {
        // TODO: merge these lowerings.
        InteropLoweringPart1(generationState).lower(irFile)
        InteropLoweringPart2(generationState).lower(irFile)
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        InteropLoweringPart1(generationState).lower(irBody, container)
        InteropLoweringPart2(generationState).lower(irBody, container)
    }
}

private fun getUniqueName(packageFragment: IrPackageFragment, fileName: String) =
        packageFragment.moduleDescriptor.name.asString().let { it.substring(1, it.lastIndex) } + fileName

private val IrFile.uniqueName: String
    get() = getUniqueName(this, fileEntry.name)

private fun IrDeclaration.getUniqueName(context: Context) =
        getUniqueName(this.getPackageFragment(), context.externalDeclarationFileNameProvider.getExternalDeclarationFileName(this))

private abstract class BaseInteropIrTransformer(
        protected val generationState: NativeGenerationState,
        protected val irFile: IrFile?,
        private val uniqueName: String,
) : IrBuildingTransformer(generationState.context) {
    protected val context = generationState.context
    protected val symbols = context.ir.symbols

    protected inline fun <T : IrDeclaration> generateDeclarationWithStubs(
            owner: IrDeclarationContainer,
            element: IrElement? = null,
            block: KotlinStubs.() -> T
    ): T {
        val addedDeclarations = mutableListOf<IrDeclaration>()
        val result = createKotlinStubs(element) {
            it.parent = owner
            addedDeclarations += it
        }.block()
        addedDeclarations.forEach {
            it.transform(this@BaseInteropIrTransformer, null)
            owner.declarations.add(it)
        }
        return result
    }

    protected inline fun IrBuilderWithScope.generateExpressionWithStubs(
            element: IrElement? = null,
            block: KotlinStubs.() -> IrExpression
    ): IrExpression {
        val addedDeclarations = mutableListOf<IrDeclaration>()
        val result = createKotlinStubs(element) {
            it.parent = parent
            addedDeclarations += it
        }.block()
        return if (addedDeclarations.isEmpty())
            result
        else irBlock {
            addedDeclarations.forEach {
                it.transform(this@BaseInteropIrTransformer, null)
                (it as? IrDeclarationWithVisibility)?.visibility = DescriptorVisibilities.LOCAL
                +it
            }
            +result
        }
    }

    private fun createKotlinStubs(element: IrElement?, addKotlin: (IrDeclaration) -> Unit): KotlinStubs {
        val location = if (element != null && irFile != null) {
            element.getCompilerMessageLocation(irFile)
        } else {
            builder.getCompilerMessageLocation()
        }

        val uniquePrefix = buildString {
            append('_')
            uniqueName.toByteArray().joinTo(this, "") {
                (0xFF and it.toInt()).toString(16).padStart(2, '0')
            }
            append('_')
        }

        return object : KotlinStubs {
            private val context = generationState.context
            private val cStubsManager = generationState.cStubsManager

            override val irBuiltIns get() = context.irBuiltIns
            override val symbols get() = context.ir.symbols
            override val typeSystem: IrTypeSystemContext get() = context.typeSystem

            val klib: KonanLibrary? get() {
                return (element as? IrCall)?.symbol?.owner?.konanLibrary as? KonanLibrary
            }

            override val language: String
                get() = klib?.manifestProperties?.getProperty("language") ?: "C"

            override fun addKotlin(declaration: IrDeclaration) {
                addKotlin(declaration)
            }

            override fun addC(lines: List<String>) {
                cStubsManager.addStub(location, lines, language)
            }

            override fun getUniqueCName(prefix: String) =
                    "$uniquePrefix${cStubsManager.getUniqueName(prefix)}"

            override fun getUniqueKotlinFunctionReferenceClassName(prefix: String) =
                    generationState.fileLowerState.getFunctionReferenceImplUniqueName(prefix)

            override val target get() = context.config.target

            override fun throwCompilerError(element: IrElement?, message: String): Nothing {
                error(irFile, element, message)
            }

            override fun renderCompilerError(element: IrElement?, message: String) =
                    renderCompilerError(irFile, element, message)
        }
    }

    protected fun renderCompilerError(element: IrElement?, message: String = "Failed requirement") =
            renderCompilerError(irFile, element, message)

    // The trick here is to try building a trampoline to the referenced function and see if it gets lowered:
    // if it doesn't, then no trampoline is needed and the reference can be left as is; otherwise, the trampoline
    // is indeed needed and the original reference is replaced to a reference to the trampoline (already lowered).
    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        expression.transformChildrenVoid()

        builder.at(expression)
        val trampoline = tryBuildTrampoline(expression.symbol.owner)
        return if (trampoline == null)
            expression
        else builder.irBlock {
            +trampoline
            +irFunctionReference(expression.type, trampoline.symbol).apply {
                (0..<expression.typeArgumentsCount).forEach { index ->
                    this.putTypeArgument(index, expression.getTypeArgument(index))
                }
                expression.arguments.forEachIndexed { index, argument ->
                    this.arguments[index] = argument
                }
            }
        }
    }

    override fun visitPropertyReference(expression: IrPropertyReference): IrExpression {
        expression.transformChildrenVoid()

        builder.at(expression)
        val getterTrampoline = expression.getter?.let { tryBuildTrampoline(it.owner) }
        val setterTrampoline = expression.setter?.let { tryBuildTrampoline(it.owner) }
        return if (getterTrampoline == null && setterTrampoline == null)
            expression
        else builder.irBlock {
            if (getterTrampoline != null) {
                expression.getter = getterTrampoline.symbol
                +getterTrampoline
            }
            if (setterTrampoline != null) {
                expression.setter = setterTrampoline.symbol
                +setterTrampoline
            }
            +expression
        }
    }

    private fun tryBuildTrampoline(callee: IrFunction): IrSimpleFunction? {
        val typeParametersContainer = when (callee) {
            is IrSimpleFunction -> callee
            is IrConstructor -> callee.constructedClass
        }

        val trampoline = context.irFactory.buildFun {
            startOffset = builder.startOffset
            endOffset = builder.endOffset
            name = callee.name
            visibility = DescriptorVisibilities.LOCAL
        }
        trampoline.parent = builder.parent
        trampoline.copyTypeParametersFrom(typeParametersContainer)
        val typeParametersMap = typeParametersContainer.typeParameters.zip(trampoline.typeParameters).toMap()
        trampoline.returnType = callee.returnType.remapTypeParameters(typeParametersContainer, trampoline, typeParametersMap)
        trampoline.parameters = callee.parameters.map {
            it.copyTo(trampoline, origin = IrDeclarationOrigin.DEFINED, kind = IrParameterKind.Regular, remapTypeMap = typeParametersMap)
        }

        val localBuilder = context.createIrBuilder(trampoline.symbol, trampoline.startOffset, trampoline.endOffset)
        val body = context.irFactory.createExpressionBody(
                when (callee) {
                    is IrConstructor -> localBuilder.irCallConstructor(
                            callee.symbol, typeArguments = trampoline.typeParameters.map { it.defaultType }
                    )
                    is IrSimpleFunction -> localBuilder.irCall(callee).apply {
                        trampoline.typeParameters.forEachIndexed { index, typeParameter ->
                            putTypeArgument(index, typeParameter.defaultType)
                        }
                    }
                }.apply {
                    trampoline.parameters.forEachIndexed { index, parameter ->
                        arguments[index] = localBuilder.irGet(parameter)
                    }
                }
        )
        trampoline.body = body

        val delegatingCall = body.expression
        trampoline.transform(this, null)
        return trampoline.takeUnless { body.expression == delegatingCall }
    }
}

private class InteropLoweringPart1(val generationState: NativeGenerationState) : FileLoweringPass, BodyLoweringPass {
    private val context = generationState.context
    private var topLevelInitializersCounter = 0

    override fun lower(irFile: IrFile) {
        val transformer = InteropTransformerPart1(generationState, irFile, irFile.uniqueName)
        irFile.transformChildrenVoid(transformer)
        transformer.eagerTopLevelInitializersForObjCClasses.forEach {
            irFile.addTopLevelInitializer(it, threadLocal = false, eager = true)
        }
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val transformer = InteropTransformerPart1(generationState, container.fileOrNull, container.getUniqueName(context))
        container.transform(transformer, null)
        require(transformer.eagerTopLevelInitializersForObjCClasses.isEmpty()) { "A local Obj-C class in an inline function is not supported" }
    }

    private fun IrFile.addTopLevelInitializer(expression: IrExpression, threadLocal: Boolean, eager: Boolean) {
        val irField = context.irFactory.createField(
                expression.startOffset,
                expression.endOffset,
                IrDeclarationOrigin.DEFINED,
                "topLevelInitializer${topLevelInitializersCounter++}".synthesizedName,
                DescriptorVisibilities.PRIVATE,
                IrFieldSymbolImpl(),
                expression.type,
                isFinal = true,
                isStatic = true,
        ).apply {
            expression.setDeclarationsParent(this)

            if (threadLocal)
                annotations += buildSimpleAnnotation(context.irBuiltIns, startOffset, endOffset, context.ir.symbols.threadLocal.owner)

            if (eager)
                annotations += buildSimpleAnnotation(context.irBuiltIns, startOffset, endOffset, context.ir.symbols.eagerInitialization.owner)

            initializer = context.irFactory.createExpressionBody(startOffset, endOffset, expression)
        }
        addChild(irField)
    }
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
private class InteropTransformerPart1(
        generationState: NativeGenerationState,
        irFile: IrFile?,
        uniqueName: String,
) : BaseInteropIrTransformer(generationState, irFile, uniqueName) {
    val eagerTopLevelInitializersForObjCClasses = mutableListOf<IrExpression>()

    private fun IrBuilderWithScope.callAlloc(classPtr: IrExpression): IrExpression =
            irCall(symbols.interopAllocObjCObject).apply {
                putValueArgument(0, classPtr)
            }

    private val outerClasses = mutableListOf<IrClass>()

    override fun visitClass(declaration: IrClass): IrStatement {
        if (declaration.isKotlinObjCClass()) {
            lowerKotlinObjCClass(declaration)
        }

        outerClasses.push(declaration)
        try {
            return super.visitClass(declaration)
        } finally {
            outerClasses.pop()
        }
    }

    private fun lowerKotlinObjCClass(irClass: IrClass) {
        checkKotlinObjCClass(irClass)

        irClass.declarations.toList().mapNotNull {
            when {
                it is IrSimpleFunction && it.annotations.hasAnnotation(objCActionClassId.asSingleFqName()) ->
                        generateActionImp(it)

                it is IrProperty && it.annotations.hasAnnotation(InteropFqNames.objCOutlet) ->
                        generateOutletSetterImp(it)

                it is IrConstructor && it.isOverrideInit() ->
                        generateOverrideInit(irClass, it)

                else -> null
            }
        }.let { irClass.addChildren(it) }

        if (irClass.annotations.hasAnnotation(InteropFqNames.exportObjCClass)) {
            val irBuilder = context.createIrBuilder(
                    irFile?.symbol ?: error("No file was supplied while lowering ${irClass.render()}")
            ).at(irClass)
            eagerTopLevelInitializersForObjCClasses.add(irBuilder.getObjCClass(symbols, irClass.symbol))
        }
    }

    private fun IrConstructor.isOverrideInit(): Boolean {
        if (this.origin != IrDeclarationOrigin.DEFINED) {
            // Make best efforts to skip generated stubs that might have got annotations
            // copied from original declarations.
            // For example, default argument stubs (https://youtrack.jetbrains.com/issue/KT-41910).
            return false
        }

        return this.annotations.hasAnnotation(InteropFqNames.objCOverrideInit)
    }

    private fun generateOverrideInit(irClass: IrClass, constructor: IrConstructor): IrSimpleFunction {
        val superClass = irClass.getSuperClassNotAny()!!
        val superConstructors = superClass.constructors.filter {
            constructor.overridesConstructor(it)
        }.toList()

        val superConstructor = superConstructors.singleOrNull()
        require(superConstructor != null) { renderCompilerError(constructor) }

        val initMethod = superConstructor.getObjCInitMethod()!!

        // Remove fake overrides of this init method, also check for explicit overriding:
        irClass.declarations.removeAll {
            if (it is IrSimpleFunction && initMethod.symbol in it.overriddenSymbols) {
                require(it.isFakeOverride) { renderCompilerError(constructor) }
                true
            } else {
                false
            }
        }

        // Generate `override fun init...(...) = this.initBy(...)`:

        return context.irFactory.createSimpleFunction(
                constructor.startOffset,
                constructor.endOffset,
                OVERRIDING_INITIALIZER_BY_CONSTRUCTOR,
                initMethod.name,
                DescriptorVisibilities.PUBLIC,
                isInline = false,
                isExpect = false,
                irClass.defaultType,
                Modality.OPEN,
                IrSimpleFunctionSymbolImpl(),
                isTailrec = false,
                isSuspend = false,
                isOperator = false,
                isInfix = false,
        ).also { result ->
            result.parent = irClass
            result.parameters += result.createDispatchReceiverParameterWithClassParent()
            result.valueParameters += constructor.valueParameters.map { it.copyTo(result) }

            result.overriddenSymbols += initMethod.symbol

            result.body = context.createIrBuilder(result.symbol).irBlockBody(result) {
                +irReturn(
                    irCallWithSubstitutedType(symbols.interopObjCObjectInitBy, listOf(irClass.defaultType)).apply {
                            extensionReceiver = irGet(result.dispatchReceiverParameter!!)
                            putValueArgument(0, irCall(constructor).also {
                                result.valueParameters.forEach { parameter ->
                                    it.putValueArgument(parameter.indexInOldValueParameters, irGet(parameter))
                                }
                            })
                        }
                )
            }

            // Ensure it gets correctly recognized by the compiler.
            require(result.getObjCMethodInfo() != null) { renderCompilerError(constructor) }
        }
    }

    private companion object {
        private val OVERRIDING_INITIALIZER_BY_CONSTRUCTOR by IrDeclarationOriginImpl
    }

    private fun IrConstructor.overridesConstructor(other: IrConstructor): Boolean {
        return this.descriptor.valueParameters.size == other.descriptor.valueParameters.size &&
                this.descriptor.valueParameters.all {
                    val otherParameter = other.descriptor.valueParameters[it.index]
                    it.name == otherParameter.name && it.type == otherParameter.type
                }
    }

    private fun generateActionImp(function: IrSimpleFunction): IrSimpleFunction {
        require(function.extensionReceiverParameter == null) { renderCompilerError(function) }
        require(function.valueParameters.all { it.type.isObjCObjectType() }) { renderCompilerError(function) }
        require(function.returnType.isUnit()) { renderCompilerError(function) }

        return generateFunctionImp(inferObjCSelector(function.descriptor), function)
    }

    private fun generateOutletSetterImp(property: IrProperty): IrSimpleFunction {
        require(property.isVar) { renderCompilerError(property) }
        require(property.getter?.extensionReceiverParameter == null) { renderCompilerError(property) }
        require(property.descriptor.type.isObjCObjectType()) { renderCompilerError(property) }

        val name = property.name.asString()
        val selector = "set${name.replaceFirstChar(Char::uppercaseChar)}:"

        return generateFunctionImp(selector, property.setter!!)
    }

    private fun getMethodSignatureEncoding(function: IrFunction): String {
        require(function.extensionReceiverParameter == null) { renderCompilerError(function) }
        require(function.valueParameters.all { it.type.isObjCObjectType() }) { renderCompilerError(function) }
        require(function.returnType.isUnit()) { renderCompilerError(function) }

        // Note: these values are valid for x86_64 and arm64.
        return when (function.valueParameters.size) {
            0 -> "v16@0:8"
            1 -> "v24@0:8@16"
            2 -> "v32@0:8@16@24"
            else -> error(irFile, function, "Only 0, 1 or 2 parameters are supported here")
        }
    }

    private fun generateFunctionImp(selector: String, function: IrFunction): IrSimpleFunction {
        val signatureEncoding = getMethodSignatureEncoding(function)

        val nativePtrType = context.ir.symbols.nativePtrType

        val parameterTypes = mutableListOf(nativePtrType) // id self

        parameterTypes.add(nativePtrType) // SEL _cmd

        function.valueParameters.mapTo(parameterTypes) { nativePtrType }

        val newFunction =
            context.irFactory.createSimpleFunction(
                    function.startOffset,
                    function.endOffset,
                    // The generated function is called by ObjC and contains Kotlin code, so
                    // it must switch thread state and potentially initialize runtime on this thread.
                    CBridgeOrigin.C_TO_KOTLIN_BRIDGE,
                    ("imp:$selector").synthesizedName,
                    DescriptorVisibilities.PRIVATE,
                    isInline = false,
                    isExpect = false,
                    function.returnType,
                    Modality.FINAL,
                    IrSimpleFunctionSymbolImpl(),
                    isTailrec = false,
                    isSuspend = false,
                    isOperator = false,
                    isInfix = false,
            )

        newFunction.valueParameters += parameterTypes.mapIndexed { index, type ->
            context.irFactory.createValueParameter(
                    startOffset = function.startOffset,
                    endOffset = function.endOffset,
                    origin = IrDeclarationOrigin.DEFINED,
                    name = Name.identifier("p$index"),
                    type = type,
                    isAssignable = false,
                    symbol = IrValueParameterSymbolImpl(),
                    varargElementType = null,
                    isCrossinline = false,
                    isNoinline = false,
                    isHidden = false,
            ).apply {
                parent = newFunction
            }
        }

        // Annotations to be detected in KotlinObjCClassInfoGenerator:

        newFunction.annotations += buildSimpleAnnotation(context.irBuiltIns, function.startOffset, function.endOffset,
                symbols.objCMethodImp.owner, selector, signatureEncoding)

        val builder = context.createIrBuilder(newFunction.symbol)
        newFunction.body = builder.irBlockBody(newFunction) {
            +irCall(function).apply {
                dispatchReceiver = interpretObjCPointer(
                        irGet(newFunction.valueParameters[0]),
                        function.dispatchReceiverParameter!!.type
                )

                function.valueParameters.forEachIndexed { index, parameter ->
                    putValueArgument(index,
                            interpretObjCPointer(
                                    irGet(newFunction.valueParameters[index + 2]),
                                    parameter.type
                            )
                    )
                }
            }
        }

        return newFunction
    }

    private fun IrBuilderWithScope.interpretObjCPointer(expression: IrExpression, type: IrType): IrExpression {
        val callee: IrFunctionSymbol = if (type.isNullable()) {
            symbols.interopInterpretObjCPointerOrNull
        } else {
            symbols.interopInterpretObjCPointer
        }

        return irCallWithSubstitutedType(callee, listOf(type)).apply {
            putValueArgument(0, expression)
        }
    }

    private fun IrClass.hasFields() =
            this.declarations.any {
                when (it) {
                    is IrField ->  it.isReal
                    is IrProperty -> it.isReal && it.backingField != null
                    else -> false
                }
            }

    private fun checkKotlinObjCClass(irClass: IrClass) {
        val kind = irClass.kind
        require(kind == ClassKind.CLASS || kind == ClassKind.OBJECT) { renderCompilerError(irClass) }
        require(irClass.isFinalClass) { renderCompilerError(irClass) }
        require(irClass.companionObject()?.hasFields() != true) { renderCompilerError(irClass) }
        require(irClass.companionObject()?.getSuperClassNotAny()?.hasFields() != true) { renderCompilerError(irClass) }

        var hasObjCClassSupertype = false
        irClass.descriptor.defaultType.constructor.supertypes.forEach {
            val descriptor = it.constructor.declarationDescriptor as ClassDescriptor
            require(descriptor.isObjCClass()) { renderCompilerError(irClass) }

            if (descriptor.kind == ClassKind.CLASS) {
                hasObjCClassSupertype = true
            }
        }

        require(hasObjCClassSupertype) { renderCompilerError(irClass) }

        val methodsOfAny =
                context.ir.symbols.any.owner.declarations.filterIsInstance<IrSimpleFunction>().toSet()

        irClass.declarations.filterIsInstance<IrSimpleFunction>().filter { it.isReal }.forEach { method ->
            val overriddenMethodOfAny = method.allOverriddenFunctions.firstOrNull {
                it in methodsOfAny
            }

            require(overriddenMethodOfAny == null) { renderCompilerError(method) }
        }
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
        expression.transformChildrenVoid()

        builder.at(expression)

        val constructedClass = outerClasses.peek()!!

        if (!constructedClass.isObjCClass()) {
            return expression
        }

        constructedClass.parent.let { parent ->
            if (parent is IrClass && parent.isObjCClass() &&
                    constructedClass.isCompanion) {

                // Note: it is actually not used; getting values of such objects is handled by code generator
                // in [FunctionGenerationContext.getObjectValue].

                return expression
            }
        }

        val delegatingCallConstructingClass = expression.symbol.owner.constructedClass
        if (!constructedClass.isExternalObjCClass() &&
            delegatingCallConstructingClass.isExternalObjCClass()) {

            expression.symbol.owner.getObjCInitMethod()?.let { initMethod ->
                // Calling super constructor from Kotlin Objective-C class.

                require(constructedClass.getSuperClassNotAny() == delegatingCallConstructingClass) { renderCompilerError(expression) }
                require(expression.symbol.owner.objCConstructorIsDesignated()) { renderCompilerError(expression) }
                require(expression.dispatchReceiver == null) { renderCompilerError(expression) }
                require(expression.extensionReceiver == null) { renderCompilerError(expression) }

                val initMethodInfo = initMethod.getExternalObjCMethodInfo()!!

                val initCall = builder.genLoweredObjCMethodCall(
                        initMethodInfo,
                        superQualifier = delegatingCallConstructingClass.symbol,
                        receiver = builder.irGet(constructedClass.thisReceiver!!),
                        arguments = initMethod.valueParameters.map { expression.getValueArgument(it.indexInOldValueParameters) },
                        call = expression,
                        method = initMethod
                )

                val superConstructor = delegatingCallConstructingClass
                        .constructors.single { it.valueParameters.size == 0 }.symbol

                return builder.irBlock(expression) {
                    // Required for the IR to be valid, will be ignored in codegen:
                    +IrDelegatingConstructorCallImpl.fromSymbolDescriptor(
                            startOffset,
                            endOffset,
                            context.irBuiltIns.unitType,
                            superConstructor
                    )
                    +irCall(symbols.interopObjCObjectSuperInitCheck).apply {
                        extensionReceiver = irGet(constructedClass.thisReceiver!!)
                        putValueArgument(0, initCall)
                    }
                }
            }
        }

        return expression
    }

    private fun IrBuilderWithScope.genLoweredObjCMethodCall(
            info: ObjCMethodInfo,
            superQualifier: IrClassSymbol?,
            receiver: IrExpression,
            arguments: List<IrExpression?>,
            call: IrFunctionAccessExpression,
            method: IrSimpleFunction
    ): IrExpression = genLoweredObjCMethodCall(
            info = info,
            superQualifier = superQualifier,
            receiver = ObjCCallReceiver.Regular(rawPtr = getRawPtr(receiver)),
            arguments = arguments,
            call = call,
            method = method
    )

    private fun IrBuilderWithScope.genLoweredObjCMethodCall(
            info: ObjCMethodInfo,
            superQualifier: IrClassSymbol?,
            receiver: ObjCCallReceiver,
            arguments: List<IrExpression?>,
            call: IrFunctionAccessExpression,
            method: IrSimpleFunction
    ): IrExpression = generateExpressionWithStubs(call) {
        if (method.parent !is IrClass) {
            // Category-provided.
            generationState.dependenciesTracker.add(method)
        }

        this.generateObjCCall(
                this@genLoweredObjCMethodCall,
                method,
                info.isStret,
                info.selector,
                info.directSymbol,
                call,
                superQualifier,
                receiver,
                arguments
        )
    }

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        expression.transformChildrenVoid()

        val callee = expression.symbol.owner
        val initMethod = callee.getObjCInitMethod()
        if (initMethod != null) {
            val arguments = callee.valueParameters.map { expression.getValueArgument(it.indexInOldValueParameters) }
            require(expression.extensionReceiver == null) { renderCompilerError(expression) }
            require(expression.dispatchReceiver == null) { renderCompilerError(expression) }

            val constructedClass = callee.constructedClass
            val initMethodInfo = initMethod.getExternalObjCMethodInfo()!!
            return builder.at(expression).run {
                val classPtr = getObjCClass(symbols, constructedClass.symbol)
                ensureObjCReferenceNotNull(callAllocAndInit(classPtr, initMethodInfo, arguments, expression, initMethod))
            }
        }

        return expression
    }

    private fun IrBuilderWithScope.ensureObjCReferenceNotNull(expression: IrExpression): IrExpression =
            if (!expression.type.isNullable()) {
                expression
            } else {
                irBlock(resultType = expression.type) {
                    val temp = irTemporary(expression)
                    +irIfThen(
                            context.irBuiltIns.unitType,
                            irEqeqeq(irGet(temp), irNull()),
                            irCall(symbols.throwNullPointerException)
                    )
                    +irGet(temp)
                }
            }

    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid()

        val callee = expression.symbol.owner

        callee.getObjCFactoryInitMethodInfo()?.let { initMethodInfo ->
            val arguments = (0 until expression.valueArgumentsCount)
                    .map { index -> expression.getValueArgument(index) }

            return builder.at(expression).run {
                val classPtr = getRawPtr(expression.extensionReceiver!!)
                callAllocAndInit(classPtr, initMethodInfo, arguments, expression, callee)
            }
        }

        callee.getExternalObjCMethodInfo()?.let { methodInfo ->
            val isInteropStubsFile =
                    irFile?.annotations?.hasAnnotation(FqName("kotlinx.cinterop.InteropStubs")) == true

            // Special case: bridge from Objective-C method implementation template to Kotlin method;
            // handled in CodeGeneratorVisitor.callVirtual.
            val useKotlinDispatch = isInteropStubsFile &&
                    (builder.scope.scopeOwnerSymbol.owner as? IrAnnotationContainer)
                            ?.hasAnnotation(RuntimeNames.exportForCppRuntime) == true

            if (!useKotlinDispatch) {
                val arguments = callee.valueParameters.map { expression.getValueArgument(it.indexInOldValueParameters) }
                require(expression.dispatchReceiver == null || expression.extensionReceiver == null) { renderCompilerError(expression) }
                require(expression.superQualifierSymbol?.owner?.isObjCMetaClass() != true) { renderCompilerError(expression) }
                require(expression.superQualifierSymbol?.owner?.isInterface != true) { renderCompilerError(expression) }

                builder.at(expression)

                return builder.genLoweredObjCMethodCall(
                        methodInfo,
                        superQualifier = expression.superQualifierSymbol,
                        receiver = expression.dispatchReceiver ?: expression.extensionReceiver!!,
                        arguments = arguments,
                        call = expression,
                        method = callee
                )
            }
        }

        return expression
    }

    override fun visitProperty(declaration: IrProperty): IrStatement {
        val backingField = declaration.backingField
        return if (declaration.isConst && backingField?.isStatic == true && context.config.isInteropStubs) {
            // Transform top-level `const val x = 42` to `val x get() = 42`.
            // Generally this transformation is just an optimization to ensure that interop constants
            // don't require any storage and/or initialization at program startup.
            // Also it is useful due to uncertain design of top-level stored properties in Kotlin/Native.
            val initializer = backingField.initializer!!.expression
            declaration.backingField = null

            val getter = declaration.getter!!
            val getterBody = getter.body!! as IrBlockBody
            getterBody.statements.clear()
            getterBody.statements += IrReturnImpl(
                    declaration.startOffset,
                    declaration.endOffset,
                    context.irBuiltIns.nothingType,
                    getter.symbol,
                    initializer
            )
            // Note: in interop stubs const val initializer is either `IrConst` or quite simple expression,
            // so it is ok to compute it every time.

            require(declaration.setter == null) { renderCompilerError(declaration) }
            require(!declaration.isVar) { renderCompilerError(declaration) }

            declaration.transformChildrenVoid()
            declaration
        } else {
            super.visitProperty(declaration)
        }
    }

    private fun IrBuilderWithScope.callAllocAndInit(
            classPtr: IrExpression,
            initMethodInfo: ObjCMethodInfo,
            arguments: List<IrExpression?>,
            call: IrFunctionAccessExpression,
            initMethod: IrSimpleFunction
    ): IrExpression = genLoweredObjCMethodCall(
            initMethodInfo,
            superQualifier = null,
            receiver = ObjCCallReceiver.Retained(rawPtr = callAlloc(classPtr)),
            arguments = arguments,
            call = call,
            method = initMethod
    )

    private fun IrBuilderWithScope.getRawPtr(receiver: IrExpression) =
            irCall(symbols.interopObjCObjectRawValueGetter).apply {
                extensionReceiver = receiver
            }
}

/**
 * Lowers some interop intrinsic calls.
 */
private class InteropLoweringPart2(val generationState: NativeGenerationState) : FileLoweringPass, BodyLoweringPass {
    private val context = generationState.context

    override fun lower(irFile: IrFile) {
        val transformer = InteropTransformerPart2(generationState, irFile, irFile.uniqueName)
        irFile.transformChildrenVoid(transformer)
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val transformer = InteropTransformerPart2(generationState, container.fileOrNull, container.getUniqueName(context))
        container.transform(transformer, null)
    }
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
private class InteropTransformerPart2(
        generationState: NativeGenerationState,
        irFile: IrFile?,
        uniqueName: String,
) : BaseInteropIrTransformer(generationState, irFile, uniqueName) {
    override fun visitClass(declaration: IrClass): IrStatement {
        super.visitClass(declaration)
        if (declaration.isKotlinObjCClass()) {
            val uniq = mutableSetOf<String>()  // remove duplicates [KT-38234]
            val imps = declaration.simpleFunctions().filter { it.isReal }.flatMap { function ->
                function.overriddenSymbols.mapNotNull {
                    val selector = it.owner.getExternalObjCMethodInfo()?.selector
                    if (selector == null || selector in uniq) {
                        null
                    } else {
                        uniq += selector
                        generateDeclarationWithStubs(declaration, it.owner) {
                            generateCFunctionAndFakeKotlinExternalFunction(
                                    function,
                                    it.owner,
                                    isObjCMethod = true,
                                    location = function
                            )
                        }
                    }
                }
            }
            declaration.addChildren(imps)
        }
        return declaration
    }

    private fun generateCFunctionPointer(function: IrSimpleFunction, expression: IrExpression): IrExpression =
            builder.generateExpressionWithStubs { generateCFunctionPointer(function, function, expression) }

    // ?.foo() part
    fun IrBuilderWithScope.irSafeCall(extensionReceiverExpression: IrExpression, typeArguments: List<IrTypeArgument>, callee: IrSimpleFunctionSymbol): IrExpression =
            irBlock {
                val tmp = irTemporary(extensionReceiverExpression)
                +irIfThenElse(callee.owner.returnType.makeNullable(),
                        irEqeqeq(irGet(tmp), irNull()),
                        irNull(),
                        irCall(callee).apply {
                            extensionReceiver = irGet(tmp)
                            typeArguments.forEachIndexed { index, arg ->
                                this.typeArguments[index] = arg.typeOrNull!!
                            }
                        }
                )
            }

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        expression.transformChildrenVoid(this)

        val callee = expression.symbol.owner
        val inlinedClass = callee.returnType.getInlinedClassNative()
        require(inlinedClass?.symbol != symbols.interopCPointer) { renderCompilerError(expression) }
        require(inlinedClass?.symbol != symbols.nativePointed) { renderCompilerError(expression) }

        val constructedClass = callee.constructedClass
        if (!constructedClass.isObjCClass())
            return expression

        // Calls to other ObjC class constructors must be lowered.
        require(constructedClass.isKotlinObjCClass()) { renderCompilerError(expression) }
        return builder.at(expression).irBlock {
            // Note: using [interopAllocObjCObject] and [interopObjCRelease] here is suboptimal: they switch the thread to Native state
            // and then back to Runnable.
            // TODO: consider calling specialized versions of allocWithZoneImp and releaseImp directly.
            val rawPtr = irTemporary(irCall(symbols.interopAllocObjCObject.owner).apply {
                putValueArgument(0, getObjCClass(symbols, constructedClass.symbol))
            })
            val instance = irTemporary(irCall(symbols.interopInterpretObjCPointer.owner).apply {
                putValueArgument(0, irGet(rawPtr))
            })
            // Balance pointer retained by alloc:
            +irCall(symbols.interopObjCRelease.owner).apply {
                putValueArgument(0, irGet(rawPtr))
            }
            +irCall(symbols.initInstance).apply {
                putValueArgument(0, irGet(instance))
                putValueArgument(1, expression)
            }
            +irGet(instance)
        }
    }

    private fun transformCppConstructorCall(expression: IrConstructorCall): IrExpression {
        val irConstructor = expression.symbol.owner
        if (irConstructor.isPrimary) return expression

        val irClass = irConstructor.constructedClass
        val primaryConstructor = irClass.primaryConstructor!!.symbol

        // TODO: don't use it is deprecated.
        val alloc = symbols.interopAllocType
        val nativeHeap = symbols.nativeHeap
        val interopGetPtr = symbols.interopGetPtr

        val correspondingInit = irClass.companionObject()!!
                .declarations
                .filterIsInstance<IrSimpleFunction>()
                .filter { it.name.toString() == "__init__"}
                .filter { it.valueParameters.size == irConstructor.valueParameters.size + 1}
                .single {
                    it.valueParameters.drop(1).mapIndexed() { index, initParameter ->
                        initParameter.type == irConstructor.valueParameters[index].type
                    }.all{ it }
                }

        val irBlock = builder.at(expression)
                .irBlock {
                    val call = irCall(primaryConstructor).also {
                        val nativePointed = irCall(alloc).apply {
                            extensionReceiver = irGetObject(nativeHeap)
                            putValueArgument(0, irGetObject(irClass.companionObject()!!.symbol))
                        }
                        val nativePtr = irCall(symbols.interopNativePointedGetRawPointer).apply {
                            extensionReceiver = nativePointed
                        }
                        it.putValueArgument(0, nativePtr)
                    }
                    val tmp = irTemporary(call)
                    val initCall = irCall(correspondingInit.symbol).apply {
                        putValueArgument(0,
                                irCall(interopGetPtr).apply {
                                    extensionReceiver = irGet(tmp)
                                    typeArguments[0] =
                                        (correspondingInit.valueParameters.first().type as IrSimpleType).arguments.single().typeOrNull!!
                                }
                        )
                        for (index in 0 until expression.valueArgumentsCount) {
                            putValueArgument(index+1, expression.getValueArgument(index)!!)
                        }
                    }
                    val initCCall = generateCCall(initCall)
                    +initCCall
                    +irGet(tmp)
                }

        return irBlock
    }

    /**
     * Handle `const val`s that come from interop libraries.
     *
     * We extract constant value from the backing field, and replace getter invocation with it.
     */
    private fun tryGenerateInteropConstantRead(expression: IrCall): IrExpression? {
        val function = expression.symbol.owner

        if (!function.isFromCInteropLibrary()) return null
        if (!function.isGetter) return null

        val constantProperty = function.correspondingPropertySymbol?.owner?.takeIf { it.isConst } ?: return null

        val initializer = constantProperty.backingField?.initializer?.expression
        require(initializer is IrConst) { renderCompilerError(expression) }

        // Avoid node duplication
        return initializer.shallowCopy()
    }

    private fun generateCCall(expression: IrCall): IrExpression {
        val function = expression.symbol.owner

        generationState.dependenciesTracker.add(function)
        val exceptionMode = ForeignExceptionMode.byValue(
                function.konanLibrary?.manifestProperties?.getProperty(ForeignExceptionMode.manifestKey)
        )
        return builder.generateExpressionWithStubs(expression) { generateCCall(expression, builder, isInvoke = false, exceptionMode) }
    }

    private fun lowerObjCInitBy(expression: IrCall): IrExpression {
        val argument = expression.getValueArgument(0)!!
        require(argument is IrConstructorCall) { renderCompilerError(argument) }

        val constructedClass = argument.symbol.owner.constructedClass

        val extensionReceiver = expression.extensionReceiver!!
        require(extensionReceiver is IrGetValue &&
                extensionReceiver.symbol.owner.isDispatchReceiverFor(constructedClass)) { renderCompilerError(extensionReceiver) }

        argument.transformChildrenVoid(this)

        return builder.at(expression).irBlock {
            val instance = extensionReceiver.symbol.owner
            +irCall(symbols.initInstance).apply {
                putValueArgument(0, irGet(instance))
                putValueArgument(1, argument)
            }
            +irGet(instance)
        }
    }

    private fun lowerStaticCFunction(expression: IrCall): IrExpression {
        val staticFunctionArgument = unwrapStaticFunctionArgument(expression.getValueArgument(0)!!)
        require(staticFunctionArgument != null && staticFunctionArgument.function is IrSimpleFunction) { renderCompilerError(expression) }
        val targetSymbol = staticFunctionArgument.function.symbol
        val target = targetSymbol.owner
        val signatureTypes = target.allParameters.map { it.type } + target.returnType

        expression.symbol.owner.typeParameters.indices.forEach { index ->
            val typeArgument = expression.typeArguments[index]!!.toKotlinType()
            val signatureType = signatureTypes[index].toKotlinType()

            require(typeArgument.constructor == signatureType.constructor &&
                    typeArgument.isMarkedNullable == signatureType.isMarkedNullable) { renderCompilerError(expression) }
        }

        builder.at(expression)
        val pointer = generateCFunctionPointer(target, expression)
        return if (staticFunctionArgument.defined)
            builder.irBlock {
                +staticFunctionArgument.function
                +pointer
            }
        else
            pointer
    }

    private fun lowerWorkerExecute(expression: IrCall): IrExpression {
        val staticFunctionArgument = unwrapStaticFunctionArgument(expression.getValueArgument(2)!!)
        require(staticFunctionArgument != null) { renderCompilerError(expression) }

        builder.at(expression)
        val targetSymbol = staticFunctionArgument.function.symbol
        val jobPointer = IrRawFunctionReferenceImpl(
                builder.startOffset, builder.endOffset,
                symbols.executeImpl.owner.valueParameters[3].type,
                targetSymbol)

        val executeImplCall = builder.irCall(symbols.executeImpl).apply {
            putValueArgument(0, expression.dispatchReceiver)
            putValueArgument(1, expression.getValueArgument(0))
            putValueArgument(2, expression.getValueArgument(1))
            putValueArgument(3, jobPointer)
        }
        executeImplCall.transformChildrenVoid()

        builder.at(expression)
        return if (staticFunctionArgument.defined)
            builder.irBlock {
                +staticFunctionArgument.function
                +executeImplCall
            }
        else
            executeImplCall
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val intrinsicType = tryGetIntrinsicType(expression)
        // Need to do these intrinsics separately as otherwise [expression.transformChildrenVoid(this)] would be called
        // and some of the child nodes would be transformed which is not what we want.
        when (intrinsicType) {
            IntrinsicType.OBJC_INIT_BY -> return lowerObjCInitBy(expression)
            IntrinsicType.INTEROP_STATIC_C_FUNCTION -> return lowerStaticCFunction(expression)
            IntrinsicType.WORKER_EXECUTE -> return lowerWorkerExecute(expression)
            else -> Unit
        }

        expression.transformChildrenVoid(this)
        builder.at(expression)
        val function = expression.symbol.owner

        if (function.resolveFakeOverrideMaybeAbstract()?.symbol == symbols.interopNativePointedRawPtrGetter) {
            // Replace by the intrinsic call to be handled by code generator:
            return builder.irCall(symbols.interopNativePointedGetRawPointer).apply {
                extensionReceiver = expression.dispatchReceiver
            }
        }

        if (function.annotations.hasAnnotation(RuntimeNames.cCall)) {
            return generateCCall(expression)
        }

        val failCompilation = { msg: String -> error(irFile, expression, msg) }
        tryGenerateInteropMemberAccess(expression, symbols, builder, failCompilation)?.let { return it }

        tryGenerateInteropConstantRead(expression)?.let { return it }

        if (intrinsicType != null) {
            return when (intrinsicType) {
                IntrinsicType.INTEROP_BITS_TO_FLOAT -> {
                    val argument = expression.getValueArgument(0)
                    if (argument is IrConst && argument.kind == IrConstKind.Int) {
                        val floatValue = kotlinx.cinterop.bitsToFloat(argument.value as Int)
                        builder.irFloat(floatValue)
                    } else {
                        expression
                    }
                }
                IntrinsicType.INTEROP_BITS_TO_DOUBLE -> {
                    val argument = expression.getValueArgument(0)
                    if (argument is IrConst && argument.kind == IrConstKind.Long) {
                        val doubleValue = kotlinx.cinterop.bitsToDouble(argument.value as Long)
                        builder.irDouble(doubleValue)
                    } else {
                        expression
                    }
                }
                IntrinsicType.INTEROP_FUNPTR_INVOKE -> {
                    builder.generateExpressionWithStubs { generateCCall(expression, builder, isInvoke = true) }
                }
                IntrinsicType.INTEROP_SIGN_EXTEND, IntrinsicType.INTEROP_NARROW -> {

                    val integerTypePredicates = arrayOf(
                            IrType::isByte, IrType::isShort, IrType::isInt, IrType::isLong
                    )

                    val receiver = expression.extensionReceiver!!
                    val typeOperand = expression.getSingleTypeArgument()

                    val receiverTypeIndex = integerTypePredicates.indexOfFirst { it(receiver.type) }
                    val typeOperandIndex = integerTypePredicates.indexOfFirst { it(typeOperand) }

                    require(receiverTypeIndex >= 0) { renderCompilerError(receiver) }
                    require(typeOperandIndex >= 0) { renderCompilerError(expression) }

                    when (intrinsicType) {
                        IntrinsicType.INTEROP_SIGN_EXTEND ->
                            require(receiverTypeIndex <= typeOperandIndex) { renderCompilerError(expression) }
                        IntrinsicType.INTEROP_NARROW ->
                            require(receiverTypeIndex >= typeOperandIndex) { renderCompilerError(expression) }
                        else -> error(intrinsicType)
                    }

                    val receiverClass = symbols.integerClasses.single {
                        receiver.type.isSubtypeOf(it.owner.defaultType, context.typeSystem)
                    }
                    val targetClass = symbols.integerClasses.single {
                        typeOperand.isSubtypeOf(it.owner.defaultType, context.typeSystem)
                    }

                    val conversionSymbol = receiverClass.functions.single {
                        it.owner.name == Name.identifier("to${targetClass.owner.name}")
                    }

                    builder.irCall(conversionSymbol).apply {
                        dispatchReceiver = receiver
                    }
                }
                IntrinsicType.INTEROP_CONVERT -> {
                    val integerClasses = symbols.allIntegerClasses
                    val typeOperand = expression.typeArguments[0]!!
                    val receiverType = expression.symbol.owner.extensionReceiverParameter!!.type
                    val source = receiverType.classifierOrFail as IrClassSymbol
                    require(source in integerClasses) { renderCompilerError(expression) }
                    require(typeOperand is IrSimpleType && !typeOperand.isNullable() && typeOperand.classifier in integerClasses) {
                        renderCompilerError(expression)
                    }

                    val target = typeOperand.classifier as IrClassSymbol
                    val valueToConvert = expression.extensionReceiver!!

                    if (source in symbols.signedIntegerClasses && target in symbols.unsignedIntegerClasses) {
                        // Default Kotlin signed-to-unsigned widening integer conversions don't follow C rules.
                        val signedTarget = symbols.unsignedToSignedOfSameBitWidth[target]!!
                        val widened = builder.irConvertInteger(source, signedTarget, valueToConvert)
                        builder.irConvertInteger(signedTarget, target, widened)
                    } else {
                        builder.irConvertInteger(source, target, valueToConvert)
                    }
                }
                else -> expression
            }
        }
        return when (function) {
            symbols.interopCPointerRawValue.owner.getter ->
                // Replace by the intrinsic call to be handled by code generator:
                builder.irCall(symbols.interopCPointerGetRawValue).apply {
                    extensionReceiver = expression.dispatchReceiver
                }
            else -> expression
        }
    }

    private fun IrBuilderWithScope.irConvertInteger(
            source: IrClassSymbol,
            target: IrClassSymbol,
            value: IrExpression
    ): IrExpression {
        val conversion = symbols.integerConversions[source to target]!!
        return irCall(conversion.owner).apply {
            if (conversion.owner.dispatchReceiverParameter != null) {
                dispatchReceiver = value
            } else {
                extensionReceiver = value
            }
        }
    }

    private class StaticFunctionArgument(val function: IrFunction, val defined: Boolean)

    private fun unwrapStaticFunctionArgument(argument: IrExpression): StaticFunctionArgument? {
        if (argument is IrFunctionReference) {
            require(argument.arguments.all { it == null }) {
                renderCompilerError(argument, "Interop static function argument should not capture any values")
            }
            return StaticFunctionArgument(argument.symbol.owner, defined = false)
        }

        if (argument !is IrFunctionExpression)
            return null
        if (argument.origin != IrStatementOrigin.LAMBDA && argument.origin != IrStatementOrigin.ANONYMOUS_FUNCTION)
            return null
        argument.function.transform(this, null)
        return StaticFunctionArgument(argument.function, defined = true)
    }

    val IrValueParameter.isDispatchReceiver: Boolean
        get() = when(val parent = this.parent) {
            is IrClass -> true
            is IrFunction -> parent.dispatchReceiverParameter == this
            else -> false
        }

    private fun IrValueDeclaration.isDispatchReceiverFor(irClass: IrClass): Boolean =
        this is IrValueParameter && isDispatchReceiver && type.getClass() == irClass

}

private fun IrCall.getSingleTypeArgument(): IrType {
    val typeParameter = symbol.owner.typeParameters.single()
    return typeArguments[typeParameter.index]!!
}

private fun IrBuilder.irFloat(value: Float) =
        IrConstImpl.float(startOffset, endOffset, context.irBuiltIns.floatType, value)

private fun IrBuilder.irDouble(value: Double) =
        IrConstImpl.double(startOffset, endOffset, context.irBuiltIns.doubleType, value)
