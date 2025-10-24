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
import org.jetbrains.kotlin.backend.konan.ir.*
import org.jetbrains.kotlin.backend.konan.IntrinsicType
import org.jetbrains.kotlin.backend.konan.ir.tryGetIntrinsicType
import org.jetbrains.kotlin.backend.konan.serialization.isFromCInteropLibrary
import org.jetbrains.kotlin.config.nativeBinaryOptions.CCallMode
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.IrValueParameterBuilder
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.objcinterop.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.isSubtypeOf
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.konan.ForeignExceptionMode
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NativeStandardInteropNames.objCActionClassId
import org.jetbrains.kotlin.native.interop.ObjCMethodInfo

internal class InteropLowering(val context: Context, val fileLowerState: FileLowerState) : FileLoweringPass, BodyLoweringPass {
    override fun lower(irFile: IrFile) {
        // TODO: merge these lowerings.
        InteropLoweringPart1(context, fileLowerState).lower(irFile)
        InteropLoweringPart2(context, fileLowerState).lower(irFile)
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        InteropLoweringPart1(context, fileLowerState).lower(irBody, container)
        InteropLoweringPart2(context, fileLowerState).lower(irBody, container)
    }

    companion object {
        /**
         * InteropLowering uses this character to designate name placeholders in generated C stubs,
         * so that InteropBridgesNameInventor replaces the placeholders with real names.
         *
         * So, ideally, this character should never be found in valid C code.
         * Otherwise, something that is not a placeholder can be parsed as one (see e.g. KT-81538).
         *
         * It seems that the only reliable choice then is to use the null character.
         * Which is not great because it is not normally displayed, making potential problems
         * harder to diagnose.
         */
        const val NAME_PLACEHOLDER_QUOTE = '\u0000'
    }
}

private class NameCounter {
    private var counter = 0

    fun getNext() = ++counter
}

private abstract class BaseInteropIrTransformer(
        protected val context: Context,
        protected val fileLowerState: FileLowerState,
        protected val irFile: IrFile?,
) : IrBuildingTransformer(context) {
    protected val symbols = context.symbols

    protected inline fun <T : IrDeclaration> generateDeclarationWithStubs(
            nameCounter: NameCounter,
            owner: IrDeclarationContainer,
            element: IrElement? = null,
            block: KotlinStubs.() -> T
    ): T {
        val addedDeclarations = mutableListOf<IrDeclaration>()
        val result = createKotlinStubs(nameCounter, element) {
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
        val result = createKotlinStubs(NameCounter(), element) {
            it.parent = parent
            addedDeclarations += it
        }.block()
        return if (addedDeclarations.isEmpty())
            result
        else irCall(symbols.interopCallMarker, result.type, listOf(result.type)).apply {
            arguments[0] = irBlock {
                addedDeclarations.forEach {
                    it.transform(this@BaseInteropIrTransformer, null)
                    (it as? IrDeclarationWithVisibility)?.visibility = DescriptorVisibilities.LOCAL
                    +it
                }
                +result
            }
        }
    }

    private fun createKotlinStubs(nameCounter: NameCounter, element: IrElement?, addKotlin: (IrDeclaration) -> Unit): KotlinStubs {
        return object : KotlinStubs {

            override val irBuiltIns get() = context.irBuiltIns
            override val symbols get() = context.symbols
            override val typeSystem: IrTypeSystemContext get() = context.typeSystem

            val klib: KonanLibrary? get() {
                return (element as? IrCall)?.symbol?.owner?.konanLibrary as? KonanLibrary
            }

            override val language: String
                get() = klib?.manifestProperties?.getProperty("language") ?: "C"

            override val isSwiftExportEnabled = context.config.swiftExport

            override fun addKotlin(declaration: IrDeclaration) {
                addKotlin(declaration)
            }

            override fun getUniqueCName(prefix: String): String {
                val quote = InteropLowering.NAME_PLACEHOLDER_QUOTE
                return "$quote$prefix${nameCounter.getNext()}$quote"
            }

            override fun getUniqueKotlinFunctionReferenceClassName(prefix: String) =
                    fileLowerState.getFunctionReferenceImplUniqueName(prefix)

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
}

private class InteropLoweringPart1(val context: Context, val fileLowerState: FileLowerState) : FileLoweringPass, BodyLoweringPass {
    private var topLevelInitializersCounter = 0

    override fun lower(irFile: IrFile) {
        val transformer = InteropTransformerPart1(context, fileLowerState, irFile)
        irFile.transformChildrenVoid(transformer)
        transformer.eagerTopLevelInitializersForObjCClasses.forEach {
            irFile.addTopLevelInitializer(it, threadLocal = false, eager = true)
        }
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val transformer = InteropTransformerPart1(context, fileLowerState, container.fileOrNull)
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
                annotations += buildSimpleAnnotation(context.irBuiltIns, startOffset, endOffset, context.symbols.threadLocal.owner)

            if (eager)
                annotations += buildSimpleAnnotation(context.irBuiltIns, startOffset, endOffset, context.symbols.eagerInitialization.owner)

            initializer = context.irFactory.createExpressionBody(startOffset, endOffset, expression)
        }
        addChild(irField)
    }
}

private class InteropTransformerPart1(
        context: Context,
        fileLowerState: FileLowerState,
        irFile: IrFile?,
) : BaseInteropIrTransformer(context, fileLowerState, irFile) {
    val eagerTopLevelInitializersForObjCClasses = mutableListOf<IrExpression>()

    private fun IrBuilderWithScope.callAlloc(classPtr: IrExpression): IrExpression =
            irCall(symbols.interopAllocObjCObject).apply {
                arguments[0] = classPtr
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

        return context.irFactory.buildFun {
            startOffset = constructor.startOffset
            endOffset = constructor.endOffset
            origin = OVERRIDING_INITIALIZER_BY_CONSTRUCTOR
            name = initMethod.name
            modality = Modality.OPEN
            returnType = irClass.defaultType
        }.also { result ->
            result.parent = irClass
            result.parameters += result.createDispatchReceiverParameterWithClassParent()
            result.parameters += constructor.parameters.map { it.copyTo(result) }

            result.overriddenSymbols += initMethod.symbol

            result.body = context.createIrBuilder(result.symbol).irBlockBody(result) {
                +irReturn(
                        irCallWithSubstitutedType(symbols.interopObjCObjectInitBy, listOf(irClass.defaultType)).apply {
                            arguments[0] = irGet(result.parameters[0])
                            arguments[1] = irCall(constructor).also {
                                result.parameters.drop(1).forEachIndexed { index, parameter -> it.arguments[index] = irGet(parameter) }
                            }
                        }
                )
            }

            // Ensure it gets correctly recognized by the compiler.
            require(result.getObjCMethodInfo() != null) { renderCompilerError(constructor) }
        }
    }

    private companion object {
        private val OVERRIDING_INITIALIZER_BY_CONSTRUCTOR by IrDeclarationOriginImpl.Regular
    }

    private fun IrConstructor.overridesConstructor(other: IrConstructor): Boolean {
        return this.parameters.size == other.parameters.size &&
                this.parameters.all {
                    val otherParameter = other.parameters[it.indexInParameters]
                    it.name == otherParameter.name && it.type == otherParameter.type
                }
    }

    private fun generateActionImp(function: IrSimpleFunction): IrSimpleFunction {
        require(function.parameters.all {
            it.kind == IrParameterKind.DispatchReceiver || (it.kind == IrParameterKind.Regular && it.type.isObjCObjectType())
        }) {
            renderCompilerError(function)
        }
        require(function.returnType.isUnit()) { renderCompilerError(function) }

        return generateFunctionImp(inferObjCSelector(function), function)
    }

    private fun generateOutletSetterImp(property: IrProperty): IrSimpleFunction {
        require(property.isVar) { renderCompilerError(property) }
        val getter = property.getter!!
        require(getter.parameters.all { it.kind == IrParameterKind.Regular || it.kind == IrParameterKind.DispatchReceiver }) {
            renderCompilerError(property)
        }
        require(getter.returnType.isObjCObjectType()) { renderCompilerError(property) }

        val name = property.name.asString()
        val selector = "set${name.replaceFirstChar(Char::uppercaseChar)}:"

        return generateFunctionImp(selector, property.setter!!)
    }

    private fun getMethodSignatureEncoding(function: IrFunction): String {
        require(function.parameters.all {
            it.kind == IrParameterKind.DispatchReceiver || (it.kind == IrParameterKind.Regular && it.type.isObjCObjectType())
        }) {
            renderCompilerError(function)
        }
        require(function.returnType.isUnit()) { renderCompilerError(function) }

        // Note: these values are valid for x86_64 and arm64.
        return when (function.parameters.count { it.kind == IrParameterKind.Regular }) {
            0 -> "v16@0:8"
            1 -> "v24@0:8@16"
            2 -> "v32@0:8@16@24"
            else -> error(irFile, function, "Only 0, 1 or 2 parameters are supported here")
        }
    }

    private fun generateFunctionImp(selector: String, function: IrFunction): IrSimpleFunction {
        val signatureEncoding = getMethodSignatureEncoding(function)

        val parameterTypes = (0..function.parameters.size).map { context.symbols.nativePtrType } // id self, SEL _cmd, ...

        val newFunction = context.irFactory.buildFun {
            startOffset = function.startOffset
            endOffset = function.endOffset
            // The generated function is called by ObjC and contains Kotlin code, so
            // it must switch thread state and potentially initialize runtime on this thread.
            origin = CBridgeOrigin.C_TO_KOTLIN_BRIDGE
            name = ("imp:$selector").synthesizedName
            visibility = DescriptorVisibilities.PRIVATE
            returnType = function.returnType
        }

        newFunction.parameters = parameterTypes.mapIndexed { index, parameterType ->
            context.irFactory.buildValueParameter(
                    IrValueParameterBuilder().apply {
                        startOffset = function.startOffset
                        endOffset = function.endOffset
                        name = Name.identifier("p$index")
                        kind = IrParameterKind.Regular
                        type = parameterType
                    },
                    newFunction
            )
        }

        // Annotations to be detected in KotlinObjCClassInfoGenerator:

        newFunction.annotations += buildSimpleAnnotation(context.irBuiltIns, function.startOffset, function.endOffset,
                symbols.objCMethodImp.owner, selector, signatureEncoding)

        val builder = context.createIrBuilder(newFunction.symbol)
        newFunction.body = builder.irBlockBody(newFunction) {
            +irCall(function).apply {
                for (index in function.parameters.indices) {
                    val parameter = if (index == 0)
                        newFunction.parameters[0]
                    else
                        newFunction.parameters[index + 1] // Skip SEL _cmd.
                    arguments[index] = interpretObjCPointer(irGet(parameter), function.parameters[index].type)
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
            arguments[0] = expression
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
        irClass.superTypes.forEach {
            val superClass = it.classOrNull?.owner
            require(superClass != null && superClass.isObjCClass()) { renderCompilerError(irClass) }

            if (superClass.kind == ClassKind.CLASS) {
                hasObjCClassSupertype = true
            }
        }

        require(hasObjCClassSupertype) { renderCompilerError(irClass) }

        val methodsOfAny =
                context.symbols.any.owner.declarations.filterIsInstance<IrSimpleFunction>().toSet()

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

        val constructor = expression.symbol.owner
        val delegatingCallConstructingClass = constructor.constructedClass
        if (!constructedClass.isExternalObjCClass() &&
                delegatingCallConstructingClass.isExternalObjCClass()) {

            constructor.getObjCInitMethod()?.let { initMethod ->
                // Calling super constructor from Kotlin Objective-C class.

                require(constructedClass.getSuperClassNotAny() == delegatingCallConstructingClass) { renderCompilerError(expression) }
                require(constructor.objCConstructorIsDesignated()) { renderCompilerError(expression) }
                require(constructor.parameters.all { it.kind == IrParameterKind.Regular }) { renderCompilerError(expression) }

                val initMethodInfo = initMethod.getExternalObjCMethodInfo()!!

                val initCall = builder.genLoweredObjCMethodCall(
                        initMethodInfo,
                        superQualifier = delegatingCallConstructingClass.symbol,
                        receiver = builder.irGet(constructedClass.thisReceiver!!),
                        arguments = expression.arguments,
                        call = expression,
                        method = initMethod
                )

                val superConstructor = delegatingCallConstructingClass
                        .constructors.single { it.parameters.isEmpty() }

                return builder.irBlock(expression) {
                    // Required for the IR to be valid, will be ignored in codegen:
                    +irDelegatingConstructorCall(superConstructor)
                    +irCall(symbols.interopObjCObjectSuperInitCheck).apply {
                        arguments[0] = irGet(constructedClass.thisReceiver!!)
                        arguments[1] = initCall
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
            val arguments = expression.arguments
            require(callee.parameters.all { it.kind == IrParameterKind.Regular }) { renderCompilerError(expression) }

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
            val arguments = expression.arguments

            return builder.at(expression).run {
                val classPtr = getRawPtr(arguments[0]!!)
                callAllocAndInit(classPtr, initMethodInfo, arguments.drop(1), expression, callee)
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
                val arguments = expression.arguments
                require(callee.parameters.count {
                    it.kind == IrParameterKind.DispatchReceiver || it.kind == IrParameterKind.ExtensionReceiver
                } == 1) {
                    renderCompilerError(expression)
                }
                require(expression.superQualifierSymbol?.owner?.isObjCMetaClass() != true) { renderCompilerError(expression) }
                require(expression.superQualifierSymbol?.owner?.isInterface != true) { renderCompilerError(expression) }

                builder.at(expression)

                return builder.genLoweredObjCMethodCall(
                        methodInfo,
                        superQualifier = expression.superQualifierSymbol,
                        receiver = arguments[0]!!,
                        arguments = arguments.drop(1),
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
                arguments[0] = receiver
            }
}

/**
 * Lowers some interop intrinsic calls.
 */
private class InteropLoweringPart2(val context: Context, val fileLowerState: FileLowerState) : FileLoweringPass, BodyLoweringPass {
    override fun lower(irFile: IrFile) {
        val transformer = InteropTransformerPart2(context, fileLowerState, irFile)
        irFile.transformChildrenVoid(transformer)
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val transformer = InteropTransformerPart2(context, fileLowerState, container.fileOrNull)
        container.transform(transformer, null)
    }
}

private class InteropTransformerPart2(
        context: Context,
        fileLowerState: FileLowerState,
        irFile: IrFile?,
) : BaseInteropIrTransformer(context, fileLowerState, irFile) {
    override fun visitClass(declaration: IrClass): IrStatement {
        super.visitClass(declaration)
        if (declaration.isKotlinObjCClass()) {
            val nameCounter = NameCounter()
            val uniq = mutableSetOf<String>()  // remove duplicates [KT-38234]
            val imps = declaration.simpleFunctions().filter { it.isReal }.flatMap { function ->
                function.overriddenSymbols.mapNotNull {
                    val selector = it.owner.getExternalObjCMethodInfo()?.selector
                    if (selector == null || selector in uniq) {
                        null
                    } else {
                        uniq += selector
                        generateDeclarationWithStubs(nameCounter, declaration, it.owner) {
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
                arguments[0] = getObjCClass(symbols, constructedClass.symbol)
            })
            val instance = irTemporary(irCallWithSubstitutedType(symbols.interopInterpretObjCPointer.owner, listOf(expression.type)).apply {
                arguments[0] = irGet(rawPtr)
            })
            // Balance pointer retained by alloc:
            +irCall(symbols.interopObjCRelease.owner).apply {
                arguments[0] = irGet(rawPtr)
            }
            +irCall(symbols.initInstance).apply {
                arguments[0] = irGet(instance)
                arguments[1] = expression
            }
            +irGet(instance)
        }
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

    private fun generateCCall(expression: IrCall, direct: Boolean): IrExpression {
        val function = expression.symbol.owner

        val exceptionMode = ForeignExceptionMode.byValue(
                function.konanLibrary?.manifestProperties?.getProperty(ForeignExceptionMode.manifestKey)
        )
        return builder.generateExpressionWithStubs(expression) {
            generateCCall(
                    expression,
                    builder,
                    isInvoke = false,
                    exceptionMode,
                    direct = direct
            )
        }
    }

    private fun tryGenerateIndirectCCall(expression: IrCall): IrExpression? =
            if (expression.symbol.owner.hasAnnotation(RuntimeNames.cCall)) {
                generateCCall(expression, direct = false)
            } else {
                null
            }

    private fun tryGenerateDirectCCall(expression: IrCall): IrExpression? =
            if (expression.symbol.owner.hasAnnotation(RuntimeNames.cCallDirect)) {
                generateCCall(expression, direct = true)
            } else {
                null
            }

    private fun generateCFunctionCallOrGlobalAccess(expression: IrCall): IrExpression = when (context.config.cCallMode) {
        CCallMode.Indirect -> tryGenerateIndirectCCall(expression)
        CCallMode.IndirectOrDirect -> tryGenerateIndirectCCall(expression) ?: tryGenerateDirectCCall(expression)
        CCallMode.DirectOrIndirect -> tryGenerateDirectCCall(expression) ?: tryGenerateIndirectCCall(expression)
        CCallMode.Direct -> tryGenerateDirectCCall(expression)
    } ?: error(renderCompilerError(expression, "the call is incompatible with cCallMode=${context.config.cCallMode}"))

    private fun lowerObjCInitBy(expression: IrCall): IrExpression {
        val argument = expression.arguments[1]!!
        require(argument is IrConstructorCall) { renderCompilerError(argument) }

        val constructedClass = argument.symbol.owner.constructedClass

        val extensionReceiver = expression.arguments[0]
        require(extensionReceiver is IrGetValue &&
                extensionReceiver.symbol.owner.isDispatchReceiverFor(constructedClass)) { renderCompilerError(extensionReceiver) }

        argument.transformChildrenVoid(this)

        return builder.at(expression).irBlock {
            val instance = extensionReceiver.symbol.owner
            +irCall(symbols.initInstance).apply {
                arguments[0] = irGet(instance)
                arguments[1] = argument
            }
            +irGet(instance)
        }
    }

    private fun lowerStaticCFunction(expression: IrCall): IrExpression {
        val staticFunctionArgument = unwrapStaticFunctionArgument(expression.arguments[0]!!)
        require(staticFunctionArgument != null && staticFunctionArgument.function is IrSimpleFunction) { renderCompilerError(expression) }
        val targetSymbol = staticFunctionArgument.function.symbol
        val target = targetSymbol.owner
        val signatureTypes = target.allParameters.map { it.type } + target.returnType

        expression.symbol.owner.typeParameters.indices.forEach { index ->
            val typeArgument = expression.typeArguments[index]!!
            val signatureType = signatureTypes[index]

            require(typeArgument.erasedUpperBound == signatureType.erasedUpperBound &&
                    typeArgument.isNullable() == signatureType.isNullable()) { renderCompilerError(expression) }
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
        val staticFunctionArgument = unwrapStaticFunctionArgument(expression.arguments[3]!!)
        require(staticFunctionArgument != null) { renderCompilerError(expression) }

        builder.at(expression)
        val targetSymbol = staticFunctionArgument.function.symbol
        val jobPointer = IrRawFunctionReferenceImpl(
                builder.startOffset, builder.endOffset,
                symbols.executeImpl.owner.parameters[3].type,
                targetSymbol)

        val executeImplCall = builder.irCall(symbols.executeImpl).apply {
            arguments[0] = expression.arguments[0]
            arguments[1] = expression.arguments[1]
            arguments[2] = expression.arguments[2]
            arguments[3] = jobPointer
        }.implicitCastTo(expression.type)
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
                arguments[0] = expression.arguments[0]
            }
        }

        if (function.isCFunctionOrGlobalAccessor()) {
            return generateCFunctionCallOrGlobalAccess(expression)
        }

        val failCompilation = { msg: String -> error(irFile, expression, msg) }
        tryGenerateInteropMemberAccess(expression, symbols, builder, failCompilation)?.let { return it }

        tryGenerateInteropConstantRead(expression)?.let { return it }

        if (intrinsicType != null) {
            return when (intrinsicType) {
                IntrinsicType.INTEROP_BITS_TO_FLOAT -> {
                    val argument = expression.arguments[0]
                    if (argument is IrConst && argument.kind == IrConstKind.Int) {
                        val floatValue = kotlinx.cinterop.bitsToFloat(argument.value as Int)
                        builder.irFloat(floatValue)
                    } else {
                        expression
                    }
                }
                IntrinsicType.INTEROP_BITS_TO_DOUBLE -> {
                    val argument = expression.arguments[0]
                    if (argument is IrConst && argument.kind == IrConstKind.Long) {
                        val doubleValue = kotlinx.cinterop.bitsToDouble(argument.value as Long)
                        builder.irDouble(doubleValue)
                    } else {
                        expression
                    }
                }
                IntrinsicType.INTEROP_FUNPTR_INVOKE -> {
                    builder.generateExpressionWithStubs { generateCCall(expression, builder, isInvoke = true, direct = false) }
                }
                IntrinsicType.INTEROP_SIGN_EXTEND, IntrinsicType.INTEROP_NARROW -> {

                    val integerTypePredicates = arrayOf(
                            IrType::isByte, IrType::isShort, IrType::isInt, IrType::isLong
                    )

                    val receiver = expression.arguments[0]!!
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
                    val receiverType = expression.symbol.owner.parameters[0].type
                    val source = receiverType.classifierOrFail as IrClassSymbol
                    require(source in integerClasses) { renderCompilerError(expression) }
                    require(typeOperand is IrSimpleType && !typeOperand.isNullable() && typeOperand.classifier in integerClasses) {
                        renderCompilerError(expression)
                    }

                    val target = typeOperand.classifier as IrClassSymbol
                    val valueToConvert = expression.arguments[0]!!

                    if (source in symbols.signedIntegerClasses && target in symbols.unsignedIntegerClasses) {
                        // Default Kotlin signed-to-unsigned widening integer conversions don't follow C rules.
                        val signedTarget = symbols.unsignedToSignedOfSameBitWidth[target]!!
                        val widened = builder.irConvertInteger(source, signedTarget, valueToConvert)
                        builder.irConvertInteger(signedTarget, target, widened)
                    } else {
                        builder.irConvertInteger(source, target, valueToConvert)
                    }
                }
                IntrinsicType.BLOCK_PTR_TO_FUNCTION_OBJECT -> {
                    builder.generateExpressionWithStubs {
                        val blockPtr = expression.arguments.single()!!
                        val functionType = expression.typeArguments[0]!!
                        val state = CBridgeGenState(this)
                        state.convertBlockPtrToKotlinFunction(builder, blockPtr, functionType).also {
                            require(state.getC().isEmpty()) { "No C code generation is expected" }
                        }
                    }
                }
                else -> expression
            }
        }
        return when (function) {
            symbols.interopCPointerRawValueGetter.owner ->
                // Replace by the intrinsic call to be handled by code generator:
                builder.irCall(symbols.interopCPointerGetRawValue).apply {
                    arguments[0] = expression.arguments[0]
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
        return irCall(conversion.owner).apply { arguments[0] = value }
    }

    private class StaticFunctionArgument(val function: IrFunction, val defined: Boolean)

    private fun unwrapStaticFunctionArgument(argument: IrExpression) =
            (argument as? IrRichFunctionReference)?.invokeFunction?.let { invokeFunction ->
                invokeFunction.transform(this, data = null)
                StaticFunctionArgument(invokeFunction, defined = true)
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
