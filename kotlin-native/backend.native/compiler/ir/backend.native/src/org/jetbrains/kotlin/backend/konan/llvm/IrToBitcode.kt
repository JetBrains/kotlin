/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.backend.common.ir.allParameters
import org.jetbrains.kotlin.backend.common.ir.allParametersCount
import org.jetbrains.kotlin.backend.common.ir.ir2string
import org.jetbrains.kotlin.backend.common.lower.inline.InlinerExpressionLocationHint
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.cgen.CBridgeOrigin
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.backend.konan.ir.*
import org.jetbrains.kotlin.backend.konan.llvm.coverage.LLVMCoverageInstrumentation
import org.jetbrains.kotlin.backend.konan.lower.*
import org.jetbrains.kotlin.backend.konan.lower.DECLARATION_ORIGIN_FILE_GLOBAL_INITIALIZER
import org.jetbrains.kotlin.backend.konan.lower.DECLARATION_ORIGIN_FILE_STANDALONE_THREAD_LOCAL_INITIALIZER
import org.jetbrains.kotlin.backend.konan.lower.DECLARATION_ORIGIN_FILE_THREAD_LOCAL_INITIALIZER
import org.jetbrains.kotlin.backend.konan.lower.DECLARATION_ORIGIN_MODULE_GLOBAL_INITIALIZER
import org.jetbrains.kotlin.backend.konan.lower.DECLARATION_ORIGIN_MODULE_THREAD_LOCAL_INITIALIZER
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.konan.ForeignExceptionMode
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.classId

internal enum class FieldStorageKind {
    GLOBAL, // In the old memory model these are only accessible from the "main" thread.
    SHARED_FROZEN,
    THREAD_LOCAL
}

internal enum class ObjectStorageKind {
    PERMANENT,
    THREAD_LOCAL,
    SHARED
}

// TODO: maybe unannotated singleton objects shall be accessed from main thread only as well?
internal val IrField.storageKind: FieldStorageKind get() {
    // TODO: Is this correct?
    val annotations = correspondingPropertySymbol?.owner?.annotations ?: annotations
    return when {
        annotations.hasAnnotation(KonanFqNames.threadLocal) -> FieldStorageKind.THREAD_LOCAL
        !isFinal -> FieldStorageKind.GLOBAL
        annotations.hasAnnotation(KonanFqNames.sharedImmutable) -> FieldStorageKind.SHARED_FROZEN
        // TODO: simplify, once IR types are fully there.
        (type.classifierOrNull?.owner as? IrAnnotationContainer)
                ?.annotations?.hasAnnotation(KonanFqNames.frozen) == true -> FieldStorageKind.SHARED_FROZEN
        else -> FieldStorageKind.GLOBAL
    }
}

internal fun IrField.needsGCRegistration(context: Context) =
        context.memoryModel == MemoryModel.EXPERIMENTAL && // only for the new MM
                type.binaryTypeIsReference() && // only for references
                (hasNonConstInitializer || // which are initialized from heap object
                        !isFinal) // or are not final

internal fun IrClass.storageKind(context: Context): ObjectStorageKind = when {
    this.annotations.hasAnnotation(KonanFqNames.threadLocal) &&
            context.config.threadsAreAllowed -> ObjectStorageKind.THREAD_LOCAL
    this.hasConstStateAndNoSideEffects(context) -> ObjectStorageKind.PERMANENT
    else -> ObjectStorageKind.SHARED
}

val IrField.isGlobalNonPrimitive get() = when  {
        type.computePrimitiveBinaryTypeOrNull() != null -> false
        else -> storageKind == FieldStorageKind.GLOBAL
    }


internal fun IrField.shouldBeFrozen(context: Context): Boolean =
        this.storageKind == FieldStorageKind.SHARED_FROZEN &&
                (context.memoryModel != MemoryModel.EXPERIMENTAL || context.config.freezing.freezeImplicit)

internal class RTTIGeneratorVisitor(context: Context) : IrElementVisitorVoid {
    val generator = RTTIGenerator(context)

    val kotlinObjCClassInfoGenerator = KotlinObjCClassInfoGenerator(context)

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(declaration: IrClass) {
        super.visitClass(declaration)
        if (declaration.requiresRtti()) {
            generator.generate(declaration)
        }
        if (declaration.isKotlinObjCClass()) {
            kotlinObjCClassInfoGenerator.generate(declaration)
        }
    }

    fun dispose() {
        generator.dispose()
    }
}

//-------------------------------------------------------------------------//


/**
 * Defines how to generate context-dependent operations.
 */
private interface CodeContext {

    /**
     * Generates `return` [value] operation.
     *
     * @param value may be null iff target type is `Unit`.
     */
    fun genReturn(target: IrSymbolOwner, value: LLVMValueRef?)

    fun genBreak(destination: IrBreak)

    fun genContinue(destination: IrContinue)

    val exceptionHandler: ExceptionHandler

    val stackLocalsManager: StackLocalsManager

    /**
     * Declares the variable.
     * @return index of declared variable.
     */
    fun genDeclareVariable(variable: IrVariable, value: LLVMValueRef?, variableLocation: VariableDebugLocation?): Int

    /**
     * @return index of value declared before, or -1 if no such variable has been declared yet.
     */
    fun getDeclaredValue(value: IrValueDeclaration): Int

    /**
     * Generates the code to obtain a value available in this context.
     *
     * @return the requested value
     */
    fun genGetValue(value: IrValueDeclaration): LLVMValueRef

    /**
     * Returns owning function scope.
     *
     * @return the requested value
     */
    fun functionScope(): CodeContext?

    /**
     * Returns owning file scope.
     *
     * @return the requested value if in the file scope or null.
     */
    fun fileScope(): CodeContext?

    /**
     * Returns owning class scope [ClassScope].
     *
     * @returns the requested value if in the class scope or null.
     */
    fun classScope(): CodeContext?

    fun addResumePoint(bbLabel: LLVMBasicBlockRef): Int

    /**
     * Returns owning returnable block scope [ReturnableBlockScope].
     *
     * @returns the requested value if in the returnableBlockScope scope or null.
     */
    fun returnableBlockScope(): CodeContext?

    /**
     * Returns location information for given source location [LocationInfo].
     */
    fun location(offset: Int): LocationInfo?

    /**
     * Returns [DIScopeOpaqueRef] instance for corresponding scope.
     */
    fun scope(): DIScopeOpaqueRef?
}

//-------------------------------------------------------------------------//

internal class CodeGeneratorVisitor(val context: Context, val lifetimes: Map<IrElement, Lifetime>) : IrElementVisitorVoid {

    val codegen = CodeGenerator(context)

    // TODO: consider eliminating mutable state
    private var currentCodeContext: CodeContext = TopLevelCodeContext

    private val intrinsicGeneratorEnvironment = object : IntrinsicGeneratorEnvironment {
        override val codegen: CodeGenerator
            get() = this@CodeGeneratorVisitor.codegen

        override val functionGenerationContext: FunctionGenerationContext
            get() = this@CodeGeneratorVisitor.functionGenerationContext

        override fun calculateLifetime(element: IrElement): Lifetime =
                resultLifetime(element)

        override val continuation: LLVMValueRef
            get() = getContinuation()

        override val exceptionHandler: ExceptionHandler
            get() = currentCodeContext.exceptionHandler

        override val stackLocalsManager: StackLocalsManager
            get() = currentCodeContext.stackLocalsManager

        override fun evaluateCall(function: IrFunction, args: List<LLVMValueRef>, resultLifetime: Lifetime, superClass: IrClass?) =
                evaluateSimpleFunctionCall(function, args, resultLifetime, superClass)

        override fun evaluateExplicitArgs(expression: IrFunctionAccessExpression): List<LLVMValueRef> =
                this@CodeGeneratorVisitor.evaluateExplicitArgs(expression)

        override fun evaluateExpression(value: IrExpression): LLVMValueRef =
                this@CodeGeneratorVisitor.evaluateExpression(value)
    }

    private val intrinsicGenerator = IntrinsicGenerator(intrinsicGeneratorEnvironment)

    /**
     * Fake [CodeContext] that doesn't support any operation.
     *
     * During function code generation [FunctionScope] should be set up.
     */
    private object TopLevelCodeContext : CodeContext {
        private fun unsupported(any: Any? = null): Nothing = throw UnsupportedOperationException(if (any is IrElement) any.render() else any?.toString() ?: "")

        override fun genReturn(target: IrSymbolOwner, value: LLVMValueRef?) = unsupported(target)

        override fun genBreak(destination: IrBreak) = unsupported()

        override fun genContinue(destination: IrContinue) = unsupported()

        override val exceptionHandler get() = unsupported()

        override val stackLocalsManager: StackLocalsManager get() = unsupported()

        override fun genDeclareVariable(variable: IrVariable, value: LLVMValueRef?, variableLocation: VariableDebugLocation?) = unsupported(variable)

        override fun getDeclaredValue(value: IrValueDeclaration) = -1

        override fun genGetValue(value: IrValueDeclaration) = unsupported(value)

        override fun functionScope(): CodeContext? = null

        override fun fileScope(): CodeContext? = null

        override fun classScope(): CodeContext? = null

        override fun addResumePoint(bbLabel: LLVMBasicBlockRef) = unsupported(bbLabel)

        override fun returnableBlockScope(): CodeContext? = null

        override fun location(offset: Int): LocationInfo? = unsupported()

        override fun scope(): DIScopeOpaqueRef? = unsupported()
    }

    /**
     * The [CodeContext] which can define some operations and delegate other ones to [outerContext]
     */
    private abstract class InnerScope(val outerContext: CodeContext) : CodeContext by outerContext

    /**
     * Convenient [InnerScope] implementation that is bound to the [currentCodeContext].
     */
    private abstract inner class InnerScopeImpl : InnerScope(currentCodeContext)
    /**
     * Executes [block] with [codeContext] substituted as [currentCodeContext].
     */
    private inline fun <R> using(codeContext: CodeContext?, block: () -> R): R {
        val oldCodeContext = currentCodeContext
        if (codeContext != null) {
            currentCodeContext = codeContext
        }
        try {
            return block()
        } finally {
            currentCodeContext = oldCodeContext
        }
    }

    private fun <T:IrElement> findCodeContext(entry: T, context:CodeContext?, predicate: CodeContext.(T) -> Boolean): CodeContext? {
        if(context == null)
            //TODO: replace `return null` with `throw NoContextFound()` ASAP.
            return null
        if (context.predicate(entry))
            return context
        return findCodeContext(entry, (context as? InnerScope)?.outerContext, predicate)
    }


    private inline fun <R> switchSymbolizationContextTo(symbol: IrFunctionSymbol, block: () -> R): R? {
        val functionContext = findCodeContext(symbol.owner, currentCodeContext) {
            val declaration = (this as? FunctionScope)?.declaration
            val returnableBlock = (this as? ReturnableBlockScope)?.returnableBlock
            val inlinedFunction = returnableBlock?.inlineFunctionSymbol?.owner
            declaration == it || inlinedFunction == it
        } ?: return null

        /**
         * We can't switch context safely, only for symbolzation needs: location, scope detection.
         */
        using(object: InnerScopeImpl() {
            override fun location(offset: Int): LocationInfo? = functionContext.location(offset)

            override fun scope(): DIScopeOpaqueRef? = functionContext.scope()

        }) {
            return block()
        }
    }
    private fun appendCAdapters() {
        context.cAdapterGenerator.generateBindings(codegen)
    }

    private fun FunctionGenerationContext.initThreadLocalField(irField: IrField) {
        val initializer = irField.initializer ?: return
        val address = context.llvmDeclarations.forStaticField(irField).storageAddressAccess.getAddress(this)
        storeAny(evaluateExpression(initializer.expression), address, false)
    }

    private fun FunctionGenerationContext.initGlobalField(irField: IrField) {
        val address = context.llvmDeclarations.forStaticField(irField).storageAddressAccess.getAddress(this)
        val initialValue = if (irField.hasNonConstInitializer) {
            val initialization = evaluateExpression(irField.initializer!!.expression)
            if (irField.shouldBeFrozen(context))
                freeze(initialization, currentCodeContext.exceptionHandler)
            initialization
        } else {
            null
        }
        if (irField.needsGCRegistration(context)) {
            call(context.llvm.initAndRegisterGlobalFunction, listOf(address, initialValue
                    ?: kNullObjHeaderPtr))
        } else if (initialValue != null) {
            storeAny(initialValue, address, false)
        }
    }

    private fun runAndProcessInitializers(konanLibrary: KotlinLibrary?, f: () -> Unit) {
        // TODO: collect those two in one place.
        context.llvm.fileUsesThreadLocalObjects = false
        context.llvm.globalSharedObjects.clear()

        context.llvm.initializersGenerationState.reset()

        f()

        context.llvm.initializersGenerationState.globalInitFunction?.let { fileInitFunction ->
            generateFunction(codegen, fileInitFunction, fileInitFunction.location(start = true), fileInitFunction.location(start = false)) {
                using(FunctionScope(fileInitFunction, this)) {
                    val parameterScope = ParameterScope(fileInitFunction, functionGenerationContext)
                    using(parameterScope) usingParameterScope@{
                        using(VariableScope()) usingVariableScope@{
                            context.llvm.initializersGenerationState.topLevelFields
                                    .filter { it.storageKind != FieldStorageKind.THREAD_LOCAL }
                                    .filterNot { it.shouldBeInitializedEagerly }
                                    .forEach { initGlobalField(it) }
                            ret(null)
                        }
                    }
                }
            }
        }

        context.llvm.initializersGenerationState.threadLocalInitFunction?.let { fileInitFunction ->
            generateFunction(codegen, fileInitFunction, fileInitFunction.location(start = true), fileInitFunction.location(start = false)) {
                using(FunctionScope(fileInitFunction, this)) {
                    val parameterScope = ParameterScope(fileInitFunction, functionGenerationContext)
                    using(parameterScope) usingParameterScope@{
                        using(VariableScope()) usingVariableScope@{
                            context.llvm.initializersGenerationState.topLevelFields
                                    .filter { it.storageKind == FieldStorageKind.THREAD_LOCAL }
                                    .filterNot { it.shouldBeInitializedEagerly }
                                    .forEach { initThreadLocalField(it) }
                            ret(null)
                        }
                    }
                }
            }
        }

        if (!context.llvm.fileUsesThreadLocalObjects && context.llvm.globalSharedObjects.isEmpty()
                && context.llvm.initializersGenerationState.isEmpty()) {
            return
        }

        // Create global initialization records.
        val initNode = createInitNode(createInitBody())
        context.llvm.irStaticInitializers.add(IrStaticInitializer(konanLibrary, createInitCtor(initNode)))
    }

    //-------------------------------------------------------------------------//

    override fun visitElement(element: IrElement) {
        TODO(ir2string(element))
    }

    //-------------------------------------------------------------------------//
    override fun visitModuleFragment(declaration: IrModuleFragment) {
        context.log{"visitModule                    : ${ir2string(declaration)}"}

        context.coverage.collectRegions(declaration)

        initializeCachedBoxes(context)
        declaration.acceptChildrenVoid(this)

        runAndProcessInitializers(null) {
            // Note: it is here because it also generates some bitcode.
            context.objCExport.generate(codegen)

            codegen.objCDataGenerator?.finishModule()

            context.coverage.writeRegionInfo()
            setRuntimeConstGlobals()
            overrideRuntimeGlobals()
            appendLlvmUsed("llvm.used", context.llvm.usedFunctions + context.llvm.usedGlobals)
            appendLlvmUsed("llvm.compiler.used", context.llvm.compilerUsedGlobals)
            if (context.isNativeLibrary) {
                appendCAdapters()
            }
        }

        appendStaticInitializers()
    }

    //-------------------------------------------------------------------------//

    val kVoidFuncType = functionType(voidType)
    val kNodeInitType = LLVMGetTypeByName(context.llvmModule, "struct.InitNode")!!
    val kMemoryStateType = LLVMGetTypeByName(context.llvmModule, "struct.MemoryState")!!
    val kInitFuncType = functionType(voidType, false, int32Type, pointerType(kMemoryStateType))

    //-------------------------------------------------------------------------//

    // Must be synchronized with Runtime.cpp
    val ALLOC_THREAD_LOCAL_GLOBALS = 0
    val INIT_GLOBALS = 1
    val INIT_THREAD_LOCAL_GLOBALS = 2
    val DEINIT_GLOBALS = 3

    val FILE_NOT_INITIALIZED = 0
    val FILE_INITIALIZED = 2

    private fun createInitBody(): LLVMValueRef {
        val initFunction = addLlvmFunctionWithDefaultAttributes(
                context,
                context.llvmModule!!,
                "",
                kInitFuncType
        )
        LLVMSetLinkage(initFunction, LLVMLinkage.LLVMPrivateLinkage)
        generateFunction(codegen, initFunction) {
            using(FunctionScope(initFunction, this)) {
                val bbInit = basicBlock("init", null)
                val bbLocalInit = basicBlock("local_init", null)
                val bbLocalAlloc = basicBlock("local_alloc", null)
                val bbGlobalDeinit = basicBlock("global_deinit", null)
                val bbDefault = basicBlock("default", null) {
                    unreachable()
                }

                switch(LLVMGetParam(initFunction, 0)!!,
                        listOf(Int32(INIT_GLOBALS).llvm                to bbInit,
                               Int32(INIT_THREAD_LOCAL_GLOBALS).llvm   to bbLocalInit,
                               Int32(ALLOC_THREAD_LOCAL_GLOBALS).llvm  to bbLocalAlloc,
                               Int32(DEINIT_GLOBALS).llvm              to bbGlobalDeinit),
                        bbDefault)

                // Globals initializers may contain accesses to objects, so visit them first.
                appendingTo(bbInit) {
                    context.llvm.initializersGenerationState.topLevelFields
                            .filter { !context.useLazyFileInitializers() || it.shouldBeInitializedEagerly }
                            .filterNot { it.storageKind == FieldStorageKind.THREAD_LOCAL }
                            .forEach { initGlobalField(it) }
                    context.llvm.initializersGenerationState.moduleGlobalInitializers.forEach {
                        evaluateSimpleFunctionCall(it, emptyList(), Lifetime.IRRELEVANT)
                    }
                    ret(null)
                }

                appendingTo(bbLocalInit) {
                    context.llvm.initializersGenerationState.threadLocalInitState?.let {
                        val address = it.getAddress(functionGenerationContext)
                        store(Int32(FILE_NOT_INITIALIZED).llvm, address)
                        LLVMSetInitializer(address, Int32(FILE_NOT_INITIALIZED).llvm)
                    }
                    context.llvm.initializersGenerationState.topLevelFields
                            .filter { !context.useLazyFileInitializers() || it.shouldBeInitializedEagerly }
                            .filter { it.storageKind == FieldStorageKind.THREAD_LOCAL }
                            .forEach { initThreadLocalField(it) }
                    context.llvm.initializersGenerationState.moduleThreadLocalInitializers.forEach {
                        evaluateSimpleFunctionCall(it, emptyList(), Lifetime.IRRELEVANT)
                    }
                    ret(null)
                }

                appendingTo(bbLocalAlloc) {
                    if (context.llvm.tlsCount > 0) {
                        val memory = LLVMGetParam(initFunction, 1)!!
                        call(context.llvm.addTLSRecord, listOf(memory, context.llvm.tlsKey,
                                Int32(context.llvm.tlsCount).llvm))
                    }
                    ret(null)
                }

                appendingTo(bbGlobalDeinit) {
                    context.llvm.initializersGenerationState.topLevelFields
                            // Only if a subject for memory management.
                            .forEach { irField ->
                                if (irField.type.binaryTypeIsReference() && irField.storageKind != FieldStorageKind.THREAD_LOCAL) {
                                    val address = context.llvmDeclarations.forStaticField(irField).storageAddressAccess.getAddress(
                                            functionGenerationContext
                                    )
                                    storeHeapRef(codegen.kNullObjHeaderPtr, address)
                                }
                            }
                    context.llvm.globalSharedObjects.forEach { address ->
                        storeHeapRef(codegen.kNullObjHeaderPtr, address)
                    }
                    context.llvm.initializersGenerationState.globalInitState?.let {
                        store(Int32(FILE_NOT_INITIALIZED).llvm, it)
                    }
                    ret(null)
                }
            }
        }
        return initFunction
    }

    //-------------------------------------------------------------------------//
    // Creates static struct InitNode $nodeName = {$initName, NULL};

    private fun createInitNode(initFunction: LLVMValueRef): LLVMValueRef {
        val nextInitNode = LLVMConstNull(pointerType(kNodeInitType))
        val argList = cValuesOf(initFunction, nextInitNode)
        // Create static object of class InitNode.
        val initNode = LLVMConstNamedStruct(kNodeInitType, argList, 2)!!
        // Create global variable with init record data.
        return context.llvm.staticData.placeGlobal(
                "init_node", constPointer(initNode), isExported = false).llvmGlobal
    }

    //-------------------------------------------------------------------------//

    private fun createInitCtor(initNodePtr: LLVMValueRef): LLVMValueRef {
        val ctorFunction = generateFunctionNoRuntime(codegen, kVoidFuncType, "") {
            call(context.llvm.appendToInitalizersTail, listOf(initNodePtr))
            ret(null)
        }
        LLVMSetLinkage(ctorFunction, LLVMLinkage.LLVMPrivateLinkage)
        return ctorFunction
    }

    //-------------------------------------------------------------------------//

    override fun visitFile(declaration: IrFile) {
        @Suppress("UNCHECKED_CAST")
        using(FileScope(declaration)) {
            runAndProcessInitializers(declaration.konanLibrary) {
                declaration.acceptChildrenVoid(this)
            }
        }
    }

    //-------------------------------------------------------------------------//

    private inner class LoopScope(val loop: IrLoop) : InnerScopeImpl() {
        val loopExit  = functionGenerationContext.basicBlock("loop_exit", loop.endLocation)
        val loopCheck = functionGenerationContext.basicBlock("loop_check", loop.condition.startLocation)

        override fun genBreak(destination: IrBreak) {
            if (destination.loop == loop)
                functionGenerationContext.br(loopExit)
            else
                super.genBreak(destination)
        }

        override fun genContinue(destination: IrContinue) {
            if (destination.loop == loop) {
                functionGenerationContext.br(loopCheck)
            } else
                super.genContinue(destination)
        }

        override val stackLocalsManager: StackLocalsManager =
            object : StackLocalsManager by functionGenerationContext.stackLocalsManager { }
    }

    //-------------------------------------------------------------------------//

    fun evaluateBreak(destination: IrBreak): LLVMValueRef {
        currentCodeContext.genBreak(destination)
        return codegen.kNothingFakeValue
    }

    //-------------------------------------------------------------------------//

    fun evaluateContinue(destination: IrContinue): LLVMValueRef {
        currentCodeContext.genContinue(destination)
        return codegen.kNothingFakeValue
    }

    //-------------------------------------------------------------------------//

    override fun visitConstructor(declaration: IrConstructor) {
        context.log{"visitConstructor               : ${ir2string(declaration)}"}
        if (declaration.constructedClass.isInlined()) {
            // Do not generate any ctors for value types.
            return
        }

        if (declaration.isObjCConstructor) {
            // Do not generate any ctors for external Objective-C classes.
            return
        }

        visitFunction(declaration)
    }

    //-------------------------------------------------------------------------//

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer) {
        context.log{"visitAnonymousInitializer      : ${ir2string(declaration)}"}
    }

    //-------------------------------------------------------------------------//

    /**
     * The scope of variable visibility.
     */
    private inner class VariableScope : InnerScopeImpl() {

        override fun genDeclareVariable(variable: IrVariable, value: LLVMValueRef?, variableLocation: VariableDebugLocation?): Int {
            return functionGenerationContext.vars.createVariable(variable, value, variableLocation)
        }

        override fun getDeclaredValue(value: IrValueDeclaration): Int {
            val index = functionGenerationContext.vars.indexOf(value)
            return if (index < 0) super.getDeclaredValue(value) else index
        }

        override fun genGetValue(value: IrValueDeclaration): LLVMValueRef {
            val index = functionGenerationContext.vars.indexOf(value)
            if (index < 0) {
                return super.genGetValue(value)
            } else {
                return functionGenerationContext.vars.load(index)
            }
        }
    }

    /**
     * The scope of parameter visibility.
     */
    private open inner class ParameterScope(
            function: IrFunction?,
            private val functionGenerationContext: FunctionGenerationContext): InnerScopeImpl() {

        val parameters = bindParameters(function)

        init {
            if (function != null) {
                parameters.forEach{
                    val parameter = it.key
                    val local = functionGenerationContext.vars.createParameter(
                            parameter, debugInfoIfNeeded(function, parameter))
                    functionGenerationContext.mapParameterForDebug(local, it.value)
                }
            }
        }

        override fun genGetValue(value: IrValueDeclaration): LLVMValueRef {
            val index = functionGenerationContext.vars.indexOf(value)
            if (index < 0) {
                return super.genGetValue(value)
            } else {
                return functionGenerationContext.vars.load(index)
            }
        }
    }

    /**
     * The [CodeContext] enclosing the entire function body.
     */
    private inner class FunctionScope private constructor(
            val functionGenerationContext: FunctionGenerationContext,
            val declaration: IrFunction?,
            val llvmFunction: LLVMValueRef) : InnerScopeImpl() {

        constructor(declaration: IrFunction, functionGenerationContext: FunctionGenerationContext) :
                this(functionGenerationContext, declaration, codegen.llvmFunction(declaration).llvmValue)

        constructor(llvmFunction: LLVMValueRef, functionGenerationContext: FunctionGenerationContext) :
                this(functionGenerationContext, null, llvmFunction)

        val coverageInstrumentation: LLVMCoverageInstrumentation? =
                context.coverage.tryGetInstrumentation(declaration) { function, args -> functionGenerationContext.call(function, args) }

        override fun genReturn(target: IrSymbolOwner, value: LLVMValueRef?) {
            if (declaration == null || target == declaration) {
                if ((target as IrFunction).returnsUnit()) {
                    functionGenerationContext.ret(null)
                } else {
                    functionGenerationContext.ret(value!!)
                }
            } else {
                super.genReturn(target, value)
            }
        }

        override val exceptionHandler: ExceptionHandler
            get() = ExceptionHandler.Caller

        override val stackLocalsManager get() = functionGenerationContext.stackLocalsManager

        override fun functionScope(): CodeContext = this


        private val scope by lazy {
            if (!context.shouldContainLocationDebugInfo() || declaration == null)
                return@lazy null
            declaration.scope() ?: llvmFunction.scope(0, subroutineType(context, codegen.llvmTargetData, listOf(context.irBuiltIns.intType)))
        }

        private val fileScope = (fileScope() as? FileScope)
        override fun location(offset: Int) = scope?.let { scope -> fileScope?.let{LocationInfo(scope, it.file.fileEntry.line(offset), it.file.fileEntry.column(offset)) } }

        override fun scope() = scope
    }

    private val functionGenerationContext
            get() = (currentCodeContext.functionScope() as FunctionScope).functionGenerationContext
    /**
     * Binds LLVM function parameters to IR parameter descriptors.
     */
    private fun bindParameters(function: IrFunction?): Map<IrValueParameter, LLVMValueRef> {
        if (function == null) return emptyMap()
        return function.allParameters.mapIndexed { i, irParameter ->
            val parameter = codegen.param(function, i)
            assert(codegen.getLLVMType(irParameter.type) == parameter.type)
            irParameter to parameter
        }.toMap()
    }

    private fun getGlobalInitStateFor(file: IrFile): LLVMValueRef =
            context.llvm.initializersGenerationState.fileGlobalInitStates.getOrPut(file) {
                codegen.addGlobal("state_global$${file.fileEntry.name}", int32Type, false).also {
                    LLVMSetInitializer(it, Int32(FILE_NOT_INITIALIZED).llvm)
                }
            }

    private fun getThreadLocalInitStateFor(file: IrFile): AddressAccess =
            context.llvm.initializersGenerationState.fileThreadLocalInitStates.getOrPut(file) {
                codegen.addKotlinThreadLocal("state_thread_local$${file.fileEntry.name}", int32Type)
            }

    override fun visitFunction(declaration: IrFunction) {
        context.log{"visitFunction                  : ${ir2string(declaration)}"}

        val body = declaration.body

        if (declaration.origin == DECLARATION_ORIGIN_FILE_GLOBAL_INITIALIZER) {
            require(context.llvm.initializersGenerationState.globalInitFunction == null) { "There can only be at most one global file initializer" }
            require(body == null) { "The body of file initializer should be null" }
            require(declaration.valueParameters.isEmpty()) { "File initializer must be parameterless" }
            require(declaration.returnsUnit()) { "File initializer must return Unit" }
            context.llvm.initializersGenerationState.globalInitFunction = declaration
            context.llvm.initializersGenerationState.globalInitState = getGlobalInitStateFor(declaration.parent as IrFile)
        }
        if (declaration.origin == DECLARATION_ORIGIN_FILE_THREAD_LOCAL_INITIALIZER
                || declaration.origin == DECLARATION_ORIGIN_FILE_STANDALONE_THREAD_LOCAL_INITIALIZER) {
            require(context.llvm.initializersGenerationState.threadLocalInitFunction == null) { "There can only be at most one thread local file initializer" }
            require(body == null) { "The body of file initializer should be null" }
            require(declaration.valueParameters.isEmpty()) { "File initializer must be parameterless" }
            require(declaration.returnsUnit()) { "File initializer must return Unit" }
            context.llvm.initializersGenerationState.threadLocalInitFunction = declaration
            context.llvm.initializersGenerationState.threadLocalInitState = getThreadLocalInitStateFor(declaration.parent as IrFile)
        }
        if (declaration.origin == DECLARATION_ORIGIN_MODULE_GLOBAL_INITIALIZER) {
            require(declaration.valueParameters.isEmpty()) { "Module initializer must be a parameterless function" }
            require(declaration.returnsUnit()) { "Module initializer must return Unit" }
            context.llvm.initializersGenerationState.moduleGlobalInitializers.add(declaration)
        }
        if (declaration.origin == DECLARATION_ORIGIN_MODULE_THREAD_LOCAL_INITIALIZER) {
            require(declaration.valueParameters.isEmpty()) { "Module initializer must be a parameterless function" }
            require(declaration.returnsUnit()) { "Module initializer must return Unit" }
            context.llvm.initializersGenerationState.moduleThreadLocalInitializers.add(declaration)
        }

        if (declaration.hasAnnotation(RuntimeNames.symbolNameAnnotation) && context.memoryModel == MemoryModel.EXPERIMENTAL) {
            context.reportCompilationError("@SymbolName annotation on function ${declaration.kotlinFqName} can't be used with experimental memory model.")
        }

        if ((declaration as? IrSimpleFunction)?.modality == Modality.ABSTRACT
                || declaration.isExternal
                || body == null)
            return
        val isNotInlinedLambda = declaration.origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
        val file = ((declaration as? IrSimpleFunction)?.attributeOwnerId as? IrSimpleFunction)?.file.takeIf {
            it ?: return@takeIf false
            (currentCodeContext.fileScope() as FileScope).file != it && isNotInlinedLambda
        }
        val scope = file?.let {
            FileScope(it)
        }
        using(scope) {
            generateFunction(codegen, declaration,
                    declaration.location(start = true),
                    declaration.location(start = false)) {
                using(FunctionScope(declaration, this)) {
                    val parameterScope = ParameterScope(declaration, functionGenerationContext)
                    using(parameterScope) usingParameterScope@{
                        using(VariableScope()) usingVariableScope@{
                            recordCoverage(body)
                            if (declaration.isReifiedInline) {
                                callDirect(context.ir.symbols.throwIllegalStateExceptionWithMessage.owner,
                                        listOf(context.llvm.staticData.kotlinStringLiteral(
                                                "unsupported call of reified inlined function `${declaration.fqNameForIrSerialization}`").llvm),
                                        Lifetime.IRRELEVANT)
                                return@usingVariableScope
                            }
                            when (body) {
                                is IrBlockBody -> body.statements.forEach { generateStatement(it) }
                                is IrExpressionBody -> error("IrExpressionBody should've been lowered")
                                is IrSyntheticBody -> throw AssertionError("Synthetic body ${body.kind} has not been lowered")
                                else -> TODO(ir2string(body))
                            }
                        }
                    }
                }
            }
        }


        if (declaration.retainAnnotation(context.config.target)) {
            context.llvm.usedFunctions.add(codegen.llvmFunction(declaration).llvmValue)
        }

        if (context.shouldVerifyBitCode())
            verifyModule(context.llvmModule!!,
                "${declaration.descriptor.containingDeclaration}::${ir2string(declaration)}")
    }

    private fun IrFunction.location(start: Boolean) =
            if (context.shouldContainLocationDebugInfo() && startOffset != UNDEFINED_OFFSET) LocationInfo(
                scope = scope()!!,
                line = if (start) startLine() else endLine(),
                column = if (start) startColumn() else endColumn())
            else null

    //-------------------------------------------------------------------------//

    override fun visitClass(declaration: IrClass) {
        context.log{"visitClass                     : ${ir2string(declaration)}"}

        if (!declaration.requiresCodeGeneration()) {
            // For non-generated annotation classes generate only nested classes.
            declaration.declarations
                    .filterIsInstance<IrClass>()
                    .forEach { it.acceptVoid(this) }
            return
        }
        using(ClassScope(declaration)) {
            declaration.declarations.forEach {
                it.acceptVoid(this)
            }
        }

        if (declaration.kind.isSingleton && !declaration.isUnit()) {
            val singleton = context.llvmDeclarations.forSingleton(declaration)
            val access = singleton.instanceStorage
            if (access is GlobalAddressAccess) {
                // Global objects are kept in a data segment and can be accessed by any module (if exported) and also
                // they need to be initialized statically.
                LLVMSetInitializer(access.getAddress(null), if (declaration.storageKind(context) == ObjectStorageKind.PERMANENT)
                    context.llvm.staticData.createConstKotlinObject(declaration,
                            *computeFields(declaration)).llvm else codegen.kNullObjHeaderPtr)
            } else {
                // Thread local objects are kept in a special map, so they need a getter function to be accessible
                // by other modules.
                val isObjCCompanion = declaration.isCompanion && declaration.parentAsClass.isObjCClass()
                // If can be exported and can be instantiated.
                if (declaration.isExported() && !isObjCCompanion &&
                        declaration.constructors.singleOrNull() { it.valueParameters.size == 0 } != null) {
                    val valueGetterName = declaration.threadLocalObjectStorageGetterSymbolName
                    generateFunction(codegen,
                            functionType(codegen.kObjHeaderPtrPtr, false),
                            valueGetterName) {
                        val value = access.getAddress(this)
                        ret(value)
                    }
                    // Getter uses TLS object, so need to ensure that this file's (de)initializer function
                    // inits and deinits TLS.
                    context.llvm.fileUsesThreadLocalObjects = true
                }
            }
        }
    }

    private fun computeFields(declaration: IrClass): Array<ConstValue> {
        val fields = context.getLayoutBuilder(declaration).fields
        return Array(fields.size) { index ->
            val initializer = fields[index].irField!!.initializer!!.expression as IrConst<*>
            evaluateConst(initializer)
        }
    }

    override fun visitProperty(declaration: IrProperty) {
        declaration.getter?.acceptVoid(this)
        declaration.setter?.acceptVoid(this)
        declaration.backingField?.acceptVoid(this)
    }

    //-------------------------------------------------------------------------//

    override fun visitField(declaration: IrField) {
        context.log{"visitField                     : ${ir2string(declaration)}"}
        debugFieldDeclaration(declaration)
        if (context.needGlobalInit(declaration)) {
            val type = codegen.getLLVMType(declaration.type)
            val globalPropertyAccess = context.llvmDeclarations.forStaticField(declaration).storageAddressAccess
            val initializer = declaration.initializer?.expression
            val globalProperty = (globalPropertyAccess as? GlobalAddressAccess)?.getAddress(null)
            if (globalProperty != null) {
                LLVMSetInitializer(globalProperty, when (initializer) {
                    is IrConst<*>, is IrConstantValue -> evaluateExpression(initializer)
                    else -> LLVMConstNull(type)
                })
                // (Cannot do this before the global is initialized).
                LLVMSetLinkage(globalProperty, LLVMLinkage.LLVMInternalLinkage)
            }
            context.llvm.initializersGenerationState.topLevelFields.add(declaration)
        }
    }

    private fun recordCoverage(irElement: IrElement) {
        val scope = currentCodeContext.functionScope()
        if (scope is FunctionScope) {
            scope.coverageInstrumentation?.instrumentIrElement(irElement)
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateExpression(value: IrExpression): LLVMValueRef {
        updateBuilderDebugLocation(value)
        recordCoverage(value)
        when (value) {
            is IrTypeOperatorCall    -> return evaluateTypeOperator           (value)
            is IrCall                -> return evaluateCall                   (value)
            is IrDelegatingConstructorCall ->
                                        return evaluateCall                   (value)
            is IrConstructorCall     -> return evaluateCall                   (value)
            is IrInstanceInitializerCall ->
                                        return evaluateInstanceInitializerCall(value)
            is IrGetValue            -> return evaluateGetValue               (value)
            is IrSetValue            -> return evaluateSetValue               (value)
            is IrGetField            -> return evaluateGetField               (value)
            is IrSetField            -> return evaluateSetField               (value)
            is IrConst<*>            -> return evaluateConst                  (value).llvm
            is IrReturn              -> return evaluateReturn                 (value)
            is IrWhen                -> return evaluateWhen                   (value)
            is IrThrow               -> return evaluateThrow                  (value)
            is IrTry                 -> return evaluateTry                    (value)
            is IrReturnableBlock     -> return evaluateReturnableBlock        (value)
            is IrContainerExpression -> return evaluateContainerExpression    (value)
            is IrWhileLoop           -> return evaluateWhileLoop              (value)
            is IrDoWhileLoop         -> return evaluateDoWhileLoop            (value)
            is IrVararg              -> return evaluateVararg                 (value)
            is IrBreak               -> return evaluateBreak                  (value)
            is IrContinue            -> return evaluateContinue               (value)
            is IrGetObjectValue      -> return evaluateGetObjectValue         (value)
            is IrFunctionReference   -> return evaluateFunctionReference      (value)
            is IrSuspendableExpression ->
                                        return evaluateSuspendableExpression  (value)
            is IrSuspensionPoint     -> return evaluateSuspensionPoint        (value)
            is IrClassReference ->      return evaluateClassReference         (value)
            is IrConstantValue ->       return evaluateConstantValue          (value).llvm
            else                     -> {
                TODO(ir2string(value))
            }
        }
    }

    private fun generateStatement(statement: IrStatement) {
        when (statement) {
            is IrExpression -> evaluateExpression(statement)
            is IrVariable -> generateVariable(statement)
            else -> TODO(ir2string(statement))
        }
    }

    private fun IrStatement.generate() = generateStatement(this)

    //-------------------------------------------------------------------------//

    private fun evaluateGetObjectValue(value: IrGetObjectValue): LLVMValueRef =
        functionGenerationContext.getObjectValue(
                value.symbol.owner,
                currentCodeContext.exceptionHandler,
                value.startLocation,
                value.endLocation
        )


    //-------------------------------------------------------------------------//

    private fun evaluateExpressionAndJump(expression: IrExpression, destination: ContinuationBlock) {
        val result = evaluateExpression(expression)

        // It is possible to check here whether the generated code has the normal continuation path
        // and do not generate any jump if not;
        // however such optimization can lead to phi functions with zero entries, which is not allowed by LLVM;
        // TODO: find the better solution.

        functionGenerationContext.jump(destination, result)
    }

    //-------------------------------------------------------------------------//

    /**
     * Represents the basic block which may expect a value:
     * when generating a [jump] to this block, one should provide the value.
     * Inside the block that value is accessible as [valuePhi].
     *
     * This class is designed to be used to generate Kotlin expressions that have a value and require branching.
     *
     * [valuePhi] may be `null`, which would mean `Unit` value is passed.
     */
    private data class ContinuationBlock(val block: LLVMBasicBlockRef, val valuePhi: LLVMValueRef?)

    private val ContinuationBlock.value: LLVMValueRef
        get() = this.valuePhi ?: codegen.theUnitInstanceRef.llvm

    /**
     * Jumps to [target] passing [value].
     */
    private fun FunctionGenerationContext.jump(target: ContinuationBlock, value: LLVMValueRef?) {
        val entry = target.block
        br(entry)
        if (target.valuePhi != null) {
            assignPhis(target.valuePhi to value!!)
        }
    }

    /**
     * Creates new [ContinuationBlock] that receives the value of given Kotlin type
     * and generates [code] starting from its beginning.
     */
    private fun continuationBlock(
            type: IrType, locationInfo: LocationInfo?, code: (ContinuationBlock) -> Unit = {}): ContinuationBlock {

        val entry = functionGenerationContext.basicBlock("continuation_block", locationInfo)

        functionGenerationContext.appendingTo(entry) {
            val valuePhi = if (type.isUnit()) {
                null
            } else {
                functionGenerationContext.phi(codegen.getLLVMType(type))
            }

            val result = ContinuationBlock(entry, valuePhi)
            code(result)
            return result
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateVararg(value: IrVararg): LLVMValueRef {
        val elements = value.elements.map {
            if (it is IrExpression) {
                val mapped = evaluateExpression(it)
                if (mapped.isConst) {
                    return@map mapped
                }
            }

            throw IllegalStateException("IrVararg neither was lowered nor can be statically evaluated")
        }

        val arrayClass = value.type.getClass()!!

        // Note: even if all elements are const, they aren't guaranteed to be statically initialized.
        // E.g. an element may be a pointer to lazy-initialized object (aka singleton).
        // However it is guaranteed that all elements are already initialized at this point.
        return codegen.staticData.createConstKotlinArray(arrayClass, elements)
    }

    //-------------------------------------------------------------------------//

    private fun evaluateThrow(expression: IrThrow): LLVMValueRef {
        val exception = evaluateExpression(expression.value)
        currentCodeContext.exceptionHandler.genThrow(functionGenerationContext, exception)
        return codegen.kNothingFakeValue
    }

    //-------------------------------------------------------------------------//

    /**
     * The [CodeContext] that catches exceptions.
     */
    private inner abstract class CatchingScope : InnerScopeImpl() {

        /**
         * The LLVM `landingpad` such that if an invoked function throws an exception,
         * then this exception is passed to [handler].
         */
        private val landingpad: LLVMBasicBlockRef by lazy {
            using(outerContext) {
                functionGenerationContext.basicBlock("landingpad", endLocationInfoFromScope()) {
                    genLandingpad()
                }
            }
        }

        /**
         * The Kotlin exception handler, i.e. the [ContinuationBlock] which gets started
         * when the exception is caught, receiving this exception as its value.
         */
        private val handler by lazy {
            using(outerContext) {
                continuationBlock(context.ir.symbols.throwable.owner.defaultType, endLocationInfoFromScope()) {
                    genHandler(it.value)
                }
            }
        }

        private fun endLocationInfoFromScope(): LocationInfo? {
            val functionScope = currentCodeContext.functionScope()
            val irFunction = functionScope?.let {
                (functionScope as FunctionScope).declaration
            }
            return irFunction?.endLocation
        }

        private fun FunctionGenerationContext.jumpToHandler(exception: LLVMValueRef) {
            jump(handler, exception)
        }

        /**
         * Generates the LLVM `landingpad` that catches C++ exception with type `KotlinException`,
         * unwraps the Kotlin exception object and jumps to [handler].
         *
         * This method generates nearly the same code as `clang++` does for the following:
         * ```
         * catch (KotlinException& e) {
         *     KRef exception = e.exception_;
         *     return exception;
         * }
         * ```
         * except that our code doesn't check exception `typeid`.
         *
         * TODO: why does `clang++` check `typeid` even if there is only one catch clause?
         */
        private fun genLandingpad() {
            with(functionGenerationContext) {
                val exceptionPtr = catchKotlinException()
                jumpToHandler(exceptionPtr)
            }
        }

        override val exceptionHandler: ExceptionHandler
            get() = object : ExceptionHandler.Local() {
                override val unwind get() = landingpad

                override fun genThrow(functionGenerationContext: FunctionGenerationContext, kotlinException: LLVMValueRef) {
                    // Super class implementation would do too, so this is just an optimization:
                    // use local jump instead of wrapping to C++ exception, throwing, catching and unwrapping it:
                    functionGenerationContext.jumpToHandler(kotlinException)
                }
            }

        protected abstract fun genHandler(exception: LLVMValueRef)
    }

    /**
     * The [CatchingScope] that handles exceptions using Kotlin `catch` clauses.
     *
     * @param success the block to be used when the exception is successfully handled;
     * expects `catch` expression result as its value.
     */
    private inner class CatchScope(private val catches: List<IrCatch>,
                                   private val success: ContinuationBlock) : CatchingScope() {

        override fun genHandler(exception: LLVMValueRef) {

            for (catch in catches) {
                fun genCatchBlock() {
                    using(VariableScope()) {
                        currentCodeContext.genDeclareVariable(catch.catchParameter, exception, null)
                        functionGenerationContext.generateFrameCheck()
                        evaluateExpressionAndJump(catch.result, success)
                    }
                }

                if (catch.catchParameter.descriptor.type == context.builtIns.throwable.defaultType) {
                    genCatchBlock()
                    return      // Remaining catch clauses are unreachable.
                } else {
                    val isInstance = genInstanceOf(exception, catch.catchParameter.type.getClass()!!)
                    val body = functionGenerationContext.basicBlock("catch", catch.startLocation)
                    val nextCheck = functionGenerationContext.basicBlock("catchCheck", catch.endLocation)
                    functionGenerationContext.condBr(isInstance, body, nextCheck)

                    functionGenerationContext.appendingTo(body) {
                        genCatchBlock()
                    }

                    functionGenerationContext.positionAtEnd(nextCheck)
                }
            }
            // rethrow the exception if no clause can handle it.
            outerContext.exceptionHandler.genThrow(functionGenerationContext, exception)
        }
    }

    private fun evaluateTry(expression: IrTry): LLVMValueRef {
        // TODO: does basic block order influence machine code order?
        // If so, consider reordering blocks to reduce exception tables size.

        assert (expression.finallyExpression == null, { "All finally blocks should've been lowered" })

        val continuation = continuationBlock(expression.type, expression.endLocation)

        val catchScope = if (expression.catches.isEmpty())
                             null
                         else
                             CatchScope(expression.catches, continuation)
        using(catchScope) {
            evaluateExpressionAndJump(expression.tryResult, continuation)
        }
        functionGenerationContext.positionAtEnd(continuation.block)

        return continuation.value
    }

    //-------------------------------------------------------------------------//
    /* FIXME. Fix "when" type in frontend.
     * For the following code:
     *  fun foo(x: Int) {
     *      when (x) {
     *          0 -> 0
     *      }
     *  }
     *  we cannot determine if the result of when is assigned or not.
     */
    private inner class WhenEmittingContext(val expression: IrWhen) {
        val needsPhi = expression.branches.last().isUnconditional() && !expression.type.isUnit()
        val llvmType = codegen.getLLVMType(expression.type)

        val bbExit = lazy { functionGenerationContext.basicBlock("when_exit", expression.endLocation) }

        val resultPhi = lazy {
            functionGenerationContext.appendingTo(bbExit.value) {
                functionGenerationContext.phi(llvmType)
            }
        }
    }

    private fun evaluateWhen(expression: IrWhen): LLVMValueRef {
        context.log{"evaluateWhen                   : ${ir2string(expression)}"}

        val whenEmittingContext = WhenEmittingContext(expression)

        generateDebugTrambolineIf("when", expression)
        expression.branches.forEach {
            val bbNext = if (it == expression.branches.last())
                             null
                         else
                             functionGenerationContext.basicBlock("when_next", it.startLocation, it.endLocation)
            generateWhenCase(whenEmittingContext, it, bbNext)
        }

        if (whenEmittingContext.bbExit.isInitialized())
            functionGenerationContext.positionAtEnd(whenEmittingContext.bbExit.value)

        return when {
            expression.type.isUnit() -> functionGenerationContext.theUnitInstanceRef.llvm
            expression.type.isNothing() -> functionGenerationContext.kNothingFakeValue
            whenEmittingContext.resultPhi.isInitialized() -> whenEmittingContext.resultPhi.value
            else -> LLVMGetUndef(whenEmittingContext.llvmType)!!
        }
    }

    private fun generateDebugTrambolineIf(name: String, expression: IrExpression) {
        val generationContext = (currentCodeContext.functionScope() as? FunctionScope)?.functionGenerationContext
                .takeIf { context.config.generateDebugTrampoline }
        generationContext?.basicBlock(name, expression.startLocation)?.let {
            generationContext.br(it)
            generationContext.positionAtEnd(it)
        }
    }

    private fun generateWhenCase(whenEmittingContext: WhenEmittingContext, branch: IrBranch, bbNext: LLVMBasicBlockRef?) {
        val brResult = if (branch.isUnconditional())
            evaluateExpression(branch.result)
        else {
            val bbCase = functionGenerationContext.basicBlock("when_case", branch.startLocation, branch.endLocation)
            val condition = evaluateExpression(branch.condition)
            functionGenerationContext.condBr(condition, bbCase, bbNext ?: whenEmittingContext.bbExit.value)
            functionGenerationContext.positionAtEnd(bbCase)
            evaluateExpression(branch.result)
        }
        if (!functionGenerationContext.isAfterTerminator()) {
            if (whenEmittingContext.needsPhi)
                functionGenerationContext.assignPhis(whenEmittingContext.resultPhi.value to brResult)
            functionGenerationContext.br(whenEmittingContext.bbExit.value)
        }
        if (bbNext != null)
            functionGenerationContext.positionAtEnd(bbNext)
    }

    //-------------------------------------------------------------------------//

    private fun evaluateWhileLoop(loop: IrWhileLoop): LLVMValueRef {
        val loopScope = LoopScope(loop)
        using(loopScope) {
            val loopBody = functionGenerationContext.basicBlock("while_loop", loop.startLocation)
            functionGenerationContext.br(loopScope.loopCheck)

            functionGenerationContext.positionAtEnd(loopScope.loopCheck)
            val condition = evaluateExpression(loop.condition)
            functionGenerationContext.condBr(condition, loopBody, loopScope.loopExit)

            functionGenerationContext.positionAtEnd(loopBody)
            if (context.memoryModel == MemoryModel.EXPERIMENTAL)
                call(context.llvm.Kotlin_mm_safePointWhileLoopBody, emptyList())
            loop.body?.generate()

            functionGenerationContext.br(loopScope.loopCheck)
            functionGenerationContext.positionAtEnd(loopScope.loopExit)

        }

        assert(loop.type.isUnit())
        return functionGenerationContext.theUnitInstanceRef.llvm
    }

    //-------------------------------------------------------------------------//

    private fun evaluateDoWhileLoop(loop: IrDoWhileLoop): LLVMValueRef {
        val loopScope = LoopScope(loop)
        using(loopScope) {
            val loopBody = functionGenerationContext.basicBlock("do_while_loop", loop.body?.startLocation ?: loop.startLocation)
            functionGenerationContext.br(loopBody)

            functionGenerationContext.positionAtEnd(loopBody)
            if (context.memoryModel == MemoryModel.EXPERIMENTAL)
                call(context.llvm.Kotlin_mm_safePointWhileLoopBody, emptyList())
            loop.body?.generate()
            functionGenerationContext.br(loopScope.loopCheck)

            functionGenerationContext.positionAtEnd(loopScope.loopCheck)
            val condition = evaluateExpression(loop.condition)
            functionGenerationContext.condBr(condition, loopBody, loopScope.loopExit)

            functionGenerationContext.positionAtEnd(loopScope.loopExit)
        }

        assert(loop.type.isUnit())
        return functionGenerationContext.theUnitInstanceRef.llvm
    }

    //-------------------------------------------------------------------------//

    private fun evaluateGetValue(value: IrGetValue): LLVMValueRef {
        context.log{"evaluateGetValue               : ${ir2string(value)}"}
        return currentCodeContext.genGetValue(value.symbol.owner)
    }

    //-------------------------------------------------------------------------//

    private fun evaluateSetValue(value: IrSetValue): LLVMValueRef {
        context.log{"evaluateSetValue               : ${ir2string(value)}"}
        val result = evaluateExpression(value.value)
        val variable = currentCodeContext.getDeclaredValue(value.symbol.owner)
        functionGenerationContext.vars.store(result, variable)
        assert(value.type.isUnit())
        return functionGenerationContext.theUnitInstanceRef.llvm
    }

    //-------------------------------------------------------------------------//
    private fun debugInfoIfNeeded(function: IrFunction?, element: IrElement): VariableDebugLocation? {
        if (function == null || !element.needDebugInfo(context) || currentCodeContext.scope() == null) return null
        val locationInfo = element.startLocation ?: return null
        val location = codegen.generateLocationInfo(locationInfo)
        val file = (currentCodeContext.fileScope() as FileScope).file.file()
        return when (element) {
            is IrVariable -> if (shouldGenerateDebugInfo(element)) debugInfoLocalVariableLocation(
                    builder       = context.debugInfo.builder,
                    functionScope = locationInfo.scope,
                    diType        = element.type.diType(context, codegen.llvmTargetData),
                    name          = element.debugNameConversion(),
                    file          = file,
                    line          = locationInfo.line,
                    location      = location)
                    else null
            is IrValueParameter -> debugInfoParameterLocation(
                    builder       = context.debugInfo.builder,
                    functionScope = locationInfo.scope,
                    diType        = element.type.diType(context, codegen.llvmTargetData),
                    name          = element.debugNameConversion(),
                    argNo         = function.allParameters.indexOf(element) + 1,
                    file          = file,
                    line          = locationInfo.line,
                    location      = location)
            else -> throw Error("Unsupported element type: ${ir2string(element)}")
        }
    }

    private fun shouldGenerateDebugInfo(variable: IrVariable) = when(variable.origin) {
        IrDeclarationOrigin.FOR_LOOP_IMPLICIT_VARIABLE,
        IrDeclarationOrigin.FOR_LOOP_ITERATOR,
        IrDeclarationOrigin.IR_TEMPORARY_VARIABLE -> false
        else -> true
    }

    private fun generateVariable(variable: IrVariable) {
        context.log{"generateVariable               : ${ir2string(variable)}"}
        val value = variable.initializer?.let {
            val callSiteOrigin = (it as? IrBlock)?.origin as? InlinerExpressionLocationHint
            val inlineAtFunctionSymbol = callSiteOrigin?.inlineAtSymbol as? IrFunctionSymbol
            inlineAtFunctionSymbol?.run {
                switchSymbolizationContextTo(inlineAtFunctionSymbol) {
                    evaluateExpression(it)
                }
            } ?: evaluateExpression(it)
        }
        currentCodeContext.genDeclareVariable(
                variable, value, debugInfoIfNeeded(
                (currentCodeContext.functionScope() as FunctionScope).declaration, variable))
    }

    //-------------------------------------------------------------------------//

    private fun evaluateTypeOperator(value: IrTypeOperatorCall): LLVMValueRef {
        return when (value.operator) {
            IrTypeOperator.CAST                      -> evaluateCast(value)
            IrTypeOperator.IMPLICIT_INTEGER_COERCION -> evaluateIntegerCoercion(value)
            IrTypeOperator.IMPLICIT_CAST             -> evaluateExpression(value.argument)
            IrTypeOperator.IMPLICIT_NOTNULL          -> TODO(ir2string(value))
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> {
                evaluateExpression(value.argument)
                functionGenerationContext.theUnitInstanceRef.llvm
            }
            IrTypeOperator.SAFE_CAST                 -> throw IllegalStateException("safe cast wasn't lowered")
            IrTypeOperator.INSTANCEOF                -> evaluateInstanceOf(value)
            IrTypeOperator.NOT_INSTANCEOF            -> evaluateNotInstanceOf(value)
            IrTypeOperator.SAM_CONVERSION            -> TODO(ir2string(value))
            IrTypeOperator.IMPLICIT_DYNAMIC_CAST     -> TODO(ir2string(value))
            IrTypeOperator.REINTERPRET_CAST          -> TODO(ir2string(value))
        }
    }

    //-------------------------------------------------------------------------//

    private fun IrType.isPrimitiveInteger(): Boolean {
        return this.isPrimitiveType() &&
               !this.isBoolean() &&
               !this.isFloat() &&
               !this.isDouble() &&
               !this.isChar()
    }

    private fun IrType.isUnsignedInteger(): Boolean =
            this is IrSimpleType && !this.hasQuestionMark &&
                    UnsignedType.values().any { it.classId == this.getClass()?.descriptor?.classId }

    private fun evaluateIntegerCoercion(value: IrTypeOperatorCall): LLVMValueRef {
        context.log{"evaluateIntegerCoercion        : ${ir2string(value)}"}
        val type = value.typeOperand
        assert(type.isPrimitiveInteger() || type.isUnsignedInteger())
        val result = evaluateExpression(value.argument)
        assert(value.argument.type.isInt())
        val llvmSrcType = codegen.getLLVMType(value.argument.type)
        val llvmDstType = codegen.getLLVMType(type)
        val srcWidth    = LLVMGetIntTypeWidth(llvmSrcType)
        val dstWidth    = LLVMGetIntTypeWidth(llvmDstType)
        return when {
            srcWidth == dstWidth           -> result
            srcWidth > dstWidth            -> LLVMBuildTrunc(functionGenerationContext.builder, result, llvmDstType, "")!!
            else /* srcWidth < dstWidth */ -> LLVMBuildSExt(functionGenerationContext.builder, result, llvmDstType, "")!!
        }
    }

    //-------------------------------------------------------------------------//
    //   table of conversion with llvm for primitive types
    //   to be used in replacement fo primitive.toX() calls with
    //   translator intrinsics.
    //            | byte     short   int     long     float     double
    //------------|----------------------------------------------------
    //    byte    |   x       sext   sext    sext     sitofp    sitofp
    //    short   | trunc      x     sext    sext     sitofp    sitofp
    //    int     | trunc    trunc    x      sext     sitofp    sitofp
    //    long    | trunc    trunc   trunc     x      sitofp    sitofp
    //    float   | fptosi   fptosi  fptosi  fptosi      x      fpext
    //    double  | fptosi   fptosi  fptosi  fptosi   fptrunc      x

    private fun evaluateCast(value: IrTypeOperatorCall): LLVMValueRef {
        context.log{"evaluateCast                   : ${ir2string(value)}"}
        val dstClass = value.typeOperand.getClass()
                ?: error("No class for ${value.typeOperand.render()} from \n${functionGenerationContext.irFunction?.render()}")

        val srcArg = evaluateExpression(value.argument)
        assert(srcArg.type == codegen.kObjHeaderPtr)

        with(functionGenerationContext) {
            ifThen(not(genInstanceOf(srcArg, dstClass))) {
                if (dstClass.defaultType.isObjCObjectType()) {
                    callDirect(
                            context.ir.symbols.throwTypeCastException.owner,
                            emptyList(),
                            Lifetime.GLOBAL
                    )
                } else {
                    val dstTypeInfo = functionGenerationContext.bitcast(kInt8Ptr, codegen.typeInfoValue(dstClass))
                    callDirect(
                            context.ir.symbols.throwClassCastException.owner,
                            listOf(srcArg, dstTypeInfo),
                            Lifetime.GLOBAL
                    )
                }
            }
        }
        return srcArg
    }

    //-------------------------------------------------------------------------//

    private fun evaluateInstanceOf(value: IrTypeOperatorCall): LLVMValueRef {
        context.log{"evaluateInstanceOf             : ${ir2string(value)}"}

        val type     = value.typeOperand
        val srcArg   = evaluateExpression(value.argument)     // Evaluate src expression.

        val bbExit       = functionGenerationContext.basicBlock("instance_of_exit", value.startLocation)
        val bbInstanceOf = functionGenerationContext.basicBlock("instance_of_notnull", value.startLocation)
        val bbNull       = functionGenerationContext.basicBlock("instance_of_null", value.startLocation)

        val condition = functionGenerationContext.icmpEq(srcArg, codegen.kNullObjHeaderPtr)
        functionGenerationContext.condBr(condition, bbNull, bbInstanceOf)

        functionGenerationContext.positionAtEnd(bbNull)
        val resultNull = if (type.containsNull()) kTrue else kFalse
        functionGenerationContext.br(bbExit)

        functionGenerationContext.positionAtEnd(bbInstanceOf)
        val typeOperandClass = value.typeOperand.getClass()
        val resultInstanceOf = if (typeOperandClass != null) {
            genInstanceOf(srcArg, typeOperandClass)
        } else {
            // E.g. when generating type operation with reified type parameter in the original body of inline function.
            kTrue
            // TODO: this code should be unreachable, recheck.
        }
        functionGenerationContext.br(bbExit)
        val bbInstanceOfResult = functionGenerationContext.currentBlock

        functionGenerationContext.positionAtEnd(bbExit)
        val result = functionGenerationContext.phi(kBoolean)
        functionGenerationContext.addPhiIncoming(result, bbNull to resultNull, bbInstanceOfResult to resultInstanceOf)
        return result
    }

    //-------------------------------------------------------------------------//

    private fun genInstanceOf(obj: LLVMValueRef, dstClass: IrClass): LLVMValueRef {
        if (dstClass.defaultType.isObjCObjectType()) {
            return genInstanceOfObjC(obj, dstClass)
        }

        val srcObjInfoPtr = functionGenerationContext.bitcast(codegen.kObjHeaderPtr, obj)

        return if (!context.ghaEnabled()) {
            call(context.llvm.isInstanceFunction, listOf(srcObjInfoPtr, codegen.typeInfoValue(dstClass)))
        } else {
            val dstHierarchyInfo = context.getLayoutBuilder(dstClass).hierarchyInfo
            if (!dstClass.isInterface) {
                call(context.llvm.isInstanceOfClassFastFunction,
                        listOf(srcObjInfoPtr, Int32(dstHierarchyInfo.classIdLo).llvm, Int32(dstHierarchyInfo.classIdHi).llvm))
            } else {
                // Essentially: typeInfo.itable[place(interfaceId)].id == interfaceId
                val interfaceId = dstHierarchyInfo.interfaceId
                val typeInfo = functionGenerationContext.loadTypeInfo(srcObjInfoPtr)
                with(functionGenerationContext) {
                    val interfaceTableRecord = lookupInterfaceTableRecord(typeInfo, interfaceId)
                    icmpEq(load(structGep(interfaceTableRecord, 0 /* id */)), Int32(interfaceId).llvm)
                }
            }
        }
    }

    private fun genInstanceOfObjC(obj: LLVMValueRef, dstClass: IrClass): LLVMValueRef {
        val objCObject = callDirect(
                context.ir.symbols.interopObjCObjectRawValueGetter.owner,
                listOf(obj),
                Lifetime.IRRELEVANT
        )

        return if (dstClass.isObjCClass()) {
            if (dstClass.isInterface) {
                val isMeta = if (dstClass.isObjCMetaClass()) kTrue else kFalse
                call(
                        context.llvm.Kotlin_Interop_DoesObjectConformToProtocol,
                        listOf(
                                objCObject,
                                genGetObjCProtocol(dstClass),
                                isMeta
                        )
                )
            } else {
                call(
                        context.llvm.Kotlin_Interop_IsObjectKindOfClass,
                        listOf(objCObject, genGetObjCClass(dstClass))
                )
            }.let {
                functionGenerationContext.icmpNe(it, kFalse)
            }


        } else {
            // e.g. ObjCObject, ObjCObjectBase etc.
            if (dstClass.isObjCMetaClass()) {
                val isClassProto = LlvmFunctionProto(
                        "object_isClass",
                        LlvmRetType(int8Type),
                        listOf(LlvmParamType(int8TypePtr)),
                        origin = context.standardLlvmSymbolsOrigin
                )
                val isClass = context.llvm.externalFunction(isClassProto)
                call(isClass, listOf(objCObject)).let {
                    functionGenerationContext.icmpNe(it, Int8(0).llvm)
                }
            } else if (dstClass.isObjCProtocolClass()) {
                // Note: it is not clear whether this class should be looked up this way.
                // clang does the same, however swiftc uses dynamic lookup.
                val protocolClass =
                        functionGenerationContext.getObjCClass("Protocol", context.standardLlvmSymbolsOrigin)
                call(
                        context.llvm.Kotlin_Interop_IsObjectKindOfClass,
                        listOf(objCObject, protocolClass)
                )
            } else {
                kTrue
            }
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateNotInstanceOf(value: IrTypeOperatorCall): LLVMValueRef {
        val instanceOfResult = evaluateInstanceOf(value)
        return functionGenerationContext.not(instanceOfResult)
    }

    //-------------------------------------------------------------------------//

    private fun evaluateGetField(value: IrGetField): LLVMValueRef {
        context.log { "evaluateGetField               : ${ir2string(value)}" }
        return if (!value.symbol.owner.isStatic) {
            val thisPtr = evaluateExpression(value.receiver!!)
            functionGenerationContext.loadSlot(
                    fieldPtrOfClass(thisPtr, value.symbol.owner), !value.symbol.owner.isFinal)
        } else {
            assert(value.receiver == null)
            if (value.symbol.owner.correspondingPropertySymbol?.owner?.isConst == true) {
                evaluateConst(value.symbol.owner.initializer?.expression as IrConst<*>).llvm
            } else {
                if (context.config.threadsAreAllowed && value.symbol.owner.isGlobalNonPrimitive) {
                    functionGenerationContext.checkGlobalsAccessible(currentCodeContext.exceptionHandler)
                }
                val ptr = context.llvmDeclarations.forStaticField(value.symbol.owner).storageAddressAccess.getAddress(
                        functionGenerationContext
                )
                functionGenerationContext.loadSlot(ptr, !value.symbol.owner.isFinal)
            }
        }.also {
            if (value.type.classifierOrNull?.isClassWithFqName(vectorType) == true)
                LLVMSetAlignment(it, 8)

        }
    }

    //-------------------------------------------------------------------------//
    private fun needMutationCheck(irClass: IrClass): Boolean {
        // For now we omit mutation checks on immutable types, as this allows initialization in constructor
        // and it is assumed that API doesn't allow to change them.
        return !irClass.isFrozen
    }

    private fun isZeroConstValue(value: IrExpression): Boolean {
        if (value !is IrConst<*>) return false
        return when (value.kind) {
            IrConstKind.Null -> true
            IrConstKind.Boolean -> (value.value as Boolean) == false
            IrConstKind.Byte -> (value.value as Byte) == 0.toByte()
            IrConstKind.Char -> (value.value as Char) == 0.toChar()
            IrConstKind.Short -> (value.value as Short) == 0.toShort()
            IrConstKind.Int -> (value.value as Int) == 0
            IrConstKind.Long -> (value.value as Long) == 0L
            IrConstKind.Float -> (value.value as Float).toRawBits() == 0
            IrConstKind.Double -> (value.value as Double).toRawBits() == 0L
            IrConstKind.String -> false
        }
    }

    private fun evaluateSetField(value: IrSetField): LLVMValueRef {
        context.log{"evaluateSetField               : ${ir2string(value)}"}
        if (value.origin == IrStatementOrigin.INITIALIZE_FIELD
                && isZeroConstValue(value.value)) {
            check(value.receiver is IrGetValue) { "Only IrGetValue expected for receiver of a field initializer" }
            // All newly allocated objects are zeroed out, so it is redundant to initialize their
            // fields with the default values. This is also aligned with the Kotlin/JVM behavior.
            // See https://youtrack.jetbrains.com/issue/KT-39100 for details.
            return codegen.theUnitInstanceRef.llvm
        }

        val valueToAssign = evaluateExpression(value.value)
        val store = if (!value.symbol.owner.isStatic) {
            val thisPtr = evaluateExpression(value.receiver!!)
            assert(thisPtr.type == codegen.kObjHeaderPtr) {
                LLVMPrintTypeToString(thisPtr.type)?.toKString().toString()
            }
            if (needMutationCheck(value.symbol.owner.parentAsClass)) {
                functionGenerationContext.call(context.llvm.mutationCheck,
                        listOf(functionGenerationContext.bitcast(codegen.kObjHeaderPtr, thisPtr)),
                        Lifetime.IRRELEVANT, currentCodeContext.exceptionHandler)

                if (functionGenerationContext.isObjectType(valueToAssign.type))
                    functionGenerationContext.call(context.llvm.checkLifetimesConstraint, listOf(thisPtr, valueToAssign))
            }
            functionGenerationContext.storeAny(valueToAssign, fieldPtrOfClass(thisPtr, value.symbol.owner), false)
        } else {
            assert(value.receiver == null)
            val globalAddress = context.llvmDeclarations.forStaticField(value.symbol.owner).storageAddressAccess.getAddress(
                    functionGenerationContext
            )
            if (context.config.threadsAreAllowed && value.symbol.owner.storageKind == FieldStorageKind.GLOBAL)
                functionGenerationContext.checkGlobalsAccessible(currentCodeContext.exceptionHandler)
            if (value.symbol.owner.shouldBeFrozen(context))
                functionGenerationContext.freeze(valueToAssign, currentCodeContext.exceptionHandler)
            functionGenerationContext.storeAny(valueToAssign, globalAddress, false)
        }
        if (store != null && value.value.type.classifierOrNull?.isClassWithFqName(vectorType) == true) {
            LLVMSetAlignment(store, 8)
        }

        assert (value.type.isUnit())
        return codegen.theUnitInstanceRef.llvm
    }

    private val vectorType = FqName("kotlin.native.Vector128").toUnsafe()

    //-------------------------------------------------------------------------//
    private fun fieldPtrOfClass(thisPtr: LLVMValueRef, value: IrField): LLVMValueRef {
        val fieldInfo = context.llvmDeclarations.forField(value)

        val typePtr = pointerType(fieldInfo.classBodyType)

        val typedBodyPtr = functionGenerationContext.bitcast(typePtr, thisPtr)
        val fieldPtr = LLVMBuildStructGEP(functionGenerationContext.builder, typedBodyPtr, fieldInfo.index, "")
        return fieldPtr!!
    }

    //-------------------------------------------------------------------------//
    private fun evaluateStringConst(value: IrConst<String>) =
            context.llvm.staticData.kotlinStringLiteral(value.value)

    private fun evaluateConst(value: IrConst<*>): ConstValue {
        context.log{"evaluateConst                  : ${ir2string(value)}"}
        /* This suppression against IrConst<String> */
        @Suppress("UNCHECKED_CAST")
        when (value.kind) {
            IrConstKind.Null    -> return constPointer(codegen.kNullObjHeaderPtr)
            IrConstKind.Boolean -> when (value.value) {
                true  -> return Int1(true)
                false -> return Int1(false)
            }
            IrConstKind.Char   -> return Char16(value.value as Char)
            IrConstKind.Byte   -> return Int8(value.value as Byte)
            IrConstKind.Short  -> return Int16(value.value as Short)
            IrConstKind.Int    -> return Int32(value.value as Int)
            IrConstKind.Long   -> return Int64(value.value as Long)
            IrConstKind.String -> return evaluateStringConst(value as IrConst<String>)
            IrConstKind.Float  -> return Float32(value.value as Float)
            IrConstKind.Double -> return Float64(value.value as Double)
        }
        TODO(ir2string(value))
    }

    //-------------------------------------------------------------------------//

    private class IrConstValueCacheKey(val value: IrConstantValue) {
        override fun equals(other: Any?): Boolean {
            if (other !is IrConstValueCacheKey) return false
            return value.contentEquals(other.value)
        }

        override fun hashCode(): Int {
            return value.contentHashCode()
        }
    }

    private val constantValuesCache = mutableMapOf<IrConstValueCacheKey, ConstValue>()

    private fun evaluateConstantValue(value: IrConstantValue): ConstValue =
            constantValuesCache.getOrPut(IrConstValueCacheKey(value)) {
                evaluateConstantValueImpl(value)
            }

    private fun evaluateConstantValueImpl(value: IrConstantValue): ConstValue {
        val symbols = context.ir.symbols
        return when (value) {
            is IrConstantPrimitive -> {
                val constructedType = value.value.type
                if (context.ir.symbols.getTypeConversion(constructedType, value.type) != null) {
                    if (value.value.kind == IrConstKind.Null) {
                        Zero(codegen.getLLVMType(value.type))
                    } else {
                        require(codegen.getLLVMType(value.type) == codegen.kObjHeaderPtr) {
                            "Can't wrap ${value.value.kind.asString} constant to type ${value.type.render()}"
                        }
                        context.llvm.staticData.createConstKotlinObject(
                                constructedType.getClass()!!,
                                evaluateConst(value.value)
                        )
                    }
                } else {
                    evaluateConst(value.value)
                }
            }
            is IrConstantArray -> {
                val clazz = value.type.getClass()!!
                require(clazz.symbol == symbols.array || clazz.symbol in symbols.primitiveTypesToPrimitiveArrays.values) {
                    "Statically initialized array should have array type"
                }
                context.llvm.staticData.createConstKotlinArray(
                        value.type.getClass()!!,
                        value.elements.map { evaluateConstantValue(it) }
                )
            }
            is IrConstantObject -> {
                val constructedType = value.constructor.owner.constructedClassType
                val constructedClass = constructedType.getClass()!!
                val needUnBoxing = constructedType.getInlinedClassNative() != null &&
                        context.ir.symbols.getTypeConversion(constructedType, value.type) == null
                if (needUnBoxing) {
                    val unboxed = value.valueArguments.singleOrNull()
                            ?: error("Inlined class should have exactly one constructor argument")
                    return evaluateConstantValue(unboxed)
                }
                val fields = if (value.constructor.owner.isConstantConstructorIntrinsic) {
                    intrinsicGenerator.evaluateConstantConstructorFields(value, value.valueArguments.map { evaluateConstantValue(it) })
                } else {
                    context.getLayoutBuilder(constructedClass).fields.map { field ->
                        val index = value.constructor.owner.valueParameters
                                .indexOfFirst { it.name.toString() == field.name }
                                .takeIf { it >= 0 }
                                ?: error("Bad statically initialized object: field ${field.name} value not set in ${constructedClass.name}")
                        evaluateConstantValue(value.valueArguments[index])
                    }.also {
                        require(it.size == value.valueArguments.size) { "Bad statically initialized object: too many fields" }
                    }
                }

                require(codegen.getLLVMType(value.type) == codegen.kObjHeaderPtr) { "Constant object is not an object, but ${value.type.render()}" }
                context.llvm.staticData.createConstKotlinObject(
                        constructedClass,
                        *fields.toTypedArray()
                )
            }
            else -> TODO("Unimplemented IrConstantValue subclass ${value::class.qualifiedName}")
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateReturn(expression: IrReturn): LLVMValueRef {
        context.log{"evaluateReturn                 : ${ir2string(expression)}"}
        val value = expression.value

        val evaluated = evaluateExpression(value)

        val target = expression.returnTargetSymbol.owner

        currentCodeContext.genReturn(target, evaluated)
        return codegen.kNothingFakeValue
    }

    //-------------------------------------------------------------------------//
    private inner class ReturnableBlockScope(val returnableBlock: IrReturnableBlock) :
            FileScope(returnableBlock.inlineFunctionSymbol?.owner?.let {
                context.specialDeclarationsFactory.loweredInlineFunctions[it]?.irFile ?: it.fileOrNull
            }
                    ?: (currentCodeContext.fileScope() as? FileScope)?.file
                    ?: error("returnable block should belong to current file at least")) {

        var bbExit : LLVMBasicBlockRef? = null
        var resultPhi : LLVMValueRef? = null
        private val functionScope by lazy {
            returnableBlock.inlineFunctionSymbol?.owner?.let {
                it.scope(file().fileEntry.line(context.specialDeclarationsFactory.loweredInlineFunctions[it]?.startOffset ?: it.startOffset))
            }
        }

        private fun getExit(): LLVMBasicBlockRef {
            val location = returnableBlock.inlineFunctionSymbol?.owner?.let {
                location(context.specialDeclarationsFactory.loweredInlineFunctions[it]?.endOffset ?: it.endOffset)
            } ?: returnableBlock.statements.lastOrNull()?.let {
                location(it.endOffset)
            }
            if (bbExit == null) bbExit = functionGenerationContext.basicBlock("returnable_block_exit", location)
            return bbExit!!
        }

        private fun getResult(): LLVMValueRef {
            if (resultPhi == null) {
                val bbCurrent = functionGenerationContext.currentBlock
                functionGenerationContext.positionAtEnd(getExit())
                resultPhi = functionGenerationContext.phi(codegen.getLLVMType(returnableBlock.type))
                functionGenerationContext.positionAtEnd(bbCurrent)
            }
            return resultPhi!!
        }

        override fun genReturn(target: IrSymbolOwner, value: LLVMValueRef?) {
            if (target != returnableBlock) {                                    // It is not our "local return".
                super.genReturn(target, value)
                return
            }
                                                                                // It is local return from current function.
            functionGenerationContext.br(getExit())                                               // Generate branch on exit block.

            if (!returnableBlock.type.isUnit()) {                               // If function returns more then "unit"
                functionGenerationContext.assignPhis(getResult() to value!!)                      // Assign return value to result PHI node.
            }
        }

        override fun returnableBlockScope(): CodeContext? = this

        override fun location(offset: Int): LocationInfo? {
            return if (returnableBlock.inlineFunctionSymbol != null) {
                val diScope = functionScope ?: return null
                val inlinedAt = outerContext.location(returnableBlock.startOffset) ?: return null
                LocationInfo(diScope, file.fileEntry.line(offset), file.fileEntry.column(offset), inlinedAt)
            } else {
                outerContext.location(offset)
            }
        }

        /**
         * Note: DILexicalBlocks aren't nested, they should be scoped with the parent function.
         */
        private val scope by lazy {
            if (!context.shouldContainLocationDebugInfo() || returnableBlock.startOffset == UNDEFINED_OFFSET)
                return@lazy null
            val lexicalBlockFile = DICreateLexicalBlockFile(context.debugInfo.builder, functionScope()!!.scope(), super.file.file())
            DICreateLexicalBlock(context.debugInfo.builder, lexicalBlockFile, super.file.file(), returnableBlock.startLine(), returnableBlock.startColumn())!!
        }

        override fun scope() = scope

    }

    //-------------------------------------------------------------------------//

    private open inner class FileScope(val file: IrFile) : InnerScopeImpl() {
        override fun fileScope(): CodeContext? = this

        override fun location(offset: Int) = scope()?.let { LocationInfo(it, file.fileEntry.line(offset), file.fileEntry.column(offset)) }

        @Suppress("UNCHECKED_CAST")
        private val scope by lazy {
            if (!context.shouldContainLocationDebugInfo())
                return@lazy null
            file.file() as DIScopeOpaqueRef?
        }

        override fun scope() = scope
    }

    //-------------------------------------------------------------------------//

    private inner class ClassScope(val clazz:IrClass) : InnerScopeImpl() {
        val isExported
            get() = clazz.isExported()
        var offsetInBits = 0L
        val members = mutableListOf<DIDerivedTypeRef>()
        @Suppress("UNCHECKED_CAST")
        val scope = if (isExported && context.shouldContainDebugInfo())
            context.debugInfo.objHeaderPointerType
        else null
        override fun classScope(): CodeContext? = this
    }

    //-------------------------------------------------------------------------//
    private fun evaluateReturnableBlock(value: IrReturnableBlock): LLVMValueRef {
        context.log{"evaluateReturnableBlock         : ${value.statements.forEach { ir2string(it) }}"}

        val returnableBlockScope = ReturnableBlockScope(value)
        generateDebugTrambolineIf("inline", value)
        using(returnableBlockScope) {
            using(VariableScope()) {
                value.statements.forEach {
                    generateStatement(it)
                }
            }
        }

        val bbExit = returnableBlockScope.bbExit
        if (bbExit != null) {
            if (!functionGenerationContext.isAfterTerminator()) {                 // TODO should we solve this problem once and for all
                functionGenerationContext.unreachable()
            }
            functionGenerationContext.positionAtEnd(bbExit)
        }

        return returnableBlockScope.resultPhi ?: if (value.type.isUnit()) {
            codegen.theUnitInstanceRef.llvm
        } else {
            LLVMGetUndef(codegen.getLLVMType(value.type))!!
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateContainerExpression(value: IrContainerExpression): LLVMValueRef {
        context.log{"evaluateContainerExpression    : ${value.statements.forEach { ir2string(it) }}"}

        val scope = if (value.isTransparentScope) {
            null
        } else {
            VariableScope()
        }

        using(scope) {
            value.statements.dropLast(1).forEach {
                generateStatement(it)
            }
            value.statements.lastOrNull()?.let {
                if (it is IrExpression) {
                    return evaluateExpression(it)
                } else {
                    generateStatement(it)
                }
            }

            assert(value.type.isUnit())
            return codegen.theUnitInstanceRef.llvm
        }
    }

    private fun evaluateInstanceInitializerCall(expression: IrInstanceInitializerCall): LLVMValueRef {
        assert (expression.type.isUnit())
        return codegen.theUnitInstanceRef.llvm
    }

    //-------------------------------------------------------------------------//
    private fun evaluateCall(value: IrFunctionAccessExpression): LLVMValueRef {
        context.log{"evaluateCall                   : ${ir2string(value)}"}

        intrinsicGenerator.tryEvaluateSpecialCall(value)?.let { return it }

        val args = evaluateExplicitArgs(value)

        updateBuilderDebugLocation(value)
        return when (value) {
            is IrDelegatingConstructorCall -> delegatingConstructorCall(value.symbol.owner, args)
            is IrConstructorCall -> evaluateConstructorCall(value, args)
            else -> evaluateFunctionCall(value as IrCall, args, resultLifetime(value))
        }
    }

    //-------------------------------------------------------------------------//
    private fun file() = (currentCodeContext.fileScope() as FileScope).file

    //-------------------------------------------------------------------------//
    private fun updateBuilderDebugLocation(element: IrElement) {
        if (!context.shouldContainLocationDebugInfo() || currentCodeContext.functionScope() == null || element.startLocation == null) return
        functionGenerationContext.debugLocation(element.startLocation!!, element.endLocation!!)
    }

    private val IrElement.startLocation: LocationInfo?
        get() = if (!context.shouldContainLocationDebugInfo() || startOffset == UNDEFINED_OFFSET) null
            else currentCodeContext.location(startOffset)

    private val IrElement.endLocation: LocationInfo?
        get() = if (!context.shouldContainLocationDebugInfo() || startOffset == UNDEFINED_OFFSET) null
            else currentCodeContext.location(endOffset)

    //-------------------------------------------------------------------------//
    private fun IrElement.startLine() = file().fileEntry.line(this.startOffset)

    //-------------------------------------------------------------------------//
    private fun IrElement.startColumn() = file().fileEntry.column(this.startOffset)

    //-------------------------------------------------------------------------//
    private fun IrElement.endLine() = file().fileEntry.line(this.endOffset)

    //-------------------------------------------------------------------------//
    private fun IrElement.endColumn() = file().fileEntry.column(this.endOffset)

    //-------------------------------------------------------------------------//
    private fun debugFieldDeclaration(expression: IrField) {
        val scope = currentCodeContext.classScope() as? ClassScope ?: return
        if (!scope.isExported || !context.shouldContainDebugInfo()) return
        val irFile = (currentCodeContext.fileScope() as FileScope).file
        val sizeInBits = expression.type.size(context)
        scope.offsetInBits += sizeInBits
        val alignInBits = expression.type.alignment(context)
        scope.offsetInBits = alignTo(scope.offsetInBits, alignInBits)
        @Suppress("UNCHECKED_CAST")
        scope.members.add(DICreateMemberType(
                refBuilder   = context.debugInfo.builder,
                refScope     = scope.scope as DIScopeOpaqueRef,
                name         = expression.computeSymbolName(),
                file         = irFile.file(),
                lineNum      = expression.startLine(),
                sizeInBits   = sizeInBits,
                alignInBits  = alignInBits,
                offsetInBits = scope.offsetInBits,
                flags        = 0,
                type         = expression.type.diType(context, codegen.llvmTargetData)
        )!!)
    }


    //-------------------------------------------------------------------------//
    private fun IrFile.file(): DIFileRef {
        return context.debugInfo.files.getOrPut(this.fileEntry.name) {
            val path = this.fileEntry.name.toFileAndFolder(context)
            DICreateFile(context.debugInfo.builder, path.file, path.folder)!!
        }
    }

    //-------------------------------------------------------------------------//

    // Saved calculated IrFunction scope which is used several time for getting locations and generating debug info.
    private var irFunctionSavedScope: Pair<IrFunction, DIScopeOpaqueRef?>? = null

    private fun IrFunction.scope(): DIScopeOpaqueRef? = if (startOffset != UNDEFINED_OFFSET) (
            if (irFunctionSavedScope != null && this == irFunctionSavedScope!!.first)
                irFunctionSavedScope!!.second
            else
                this.scope(startLine()).also { irFunctionSavedScope = Pair(this, it) }
            ) else null

    private val IrFunction.isReifiedInline:Boolean
        get() = isInline && typeParameters.any { it.isReified }

    @Suppress("UNCHECKED_CAST")
    private fun IrFunction.scope(startLine:Int): DIScopeOpaqueRef? {
        if (!context.shouldContainLocationDebugInfo())
            return null
        val functionLlvmValue =
                // TODO: May be tie up inline lambdas to their outer function?
                if (codegen.isExternal(this) && !KonanBinaryInterface.isExported(this))
                    null
                else
                    codegen.llvmFunctionOrNull(this)?.llvmValue
        return if (!isReifiedInline && functionLlvmValue != null) {
            context.debugInfo.subprograms.getOrPut(functionLlvmValue) {
                memScoped {
                    val subroutineType = subroutineType(context, codegen.llvmTargetData)
                    val llvmFunction = codegen.llvmFunction(this@scope).llvmValue
                    diFunctionScope(name.asString(), llvmFunction.name!!, startLine, subroutineType).also {
                        if (!this@scope.isInline)
                            DIFunctionAddSubprogram(llvmFunction, it)
                    }
                }
            } as DIScopeOpaqueRef
        } else {
            context.debugInfo.inlinedSubprograms.getOrPut(this) {
                memScoped {
                    val subroutineType = subroutineType(context, codegen.llvmTargetData)
                    diFunctionScope(name.asString(), "<inlined-out:$name>", startLine, subroutineType)
                }
            } as DIScopeOpaqueRef
        }

    }

    @Suppress("UNCHECKED_CAST")
    private fun LLVMValueRef.scope(startLine:Int, subroutineType: DISubroutineTypeRef): DIScopeOpaqueRef? {
        return context.debugInfo.subprograms.getOrPut(this) {
            diFunctionScope(name!!, name!!, startLine, subroutineType).also {
                DIFunctionAddSubprogram(this@scope, it)
            }
        }  as DIScopeOpaqueRef
    }

    @Suppress("UNCHECKED_CAST")
    private fun diFunctionScope(name: String, linkageName: String, startLine: Int, subroutineType: DISubroutineTypeRef) = DICreateFunction(
                builder = context.debugInfo.builder,
                scope = context.debugInfo.compilationUnit,
                name = name,
                linkageName = linkageName,
                file = file().file(),
                lineNo = startLine,
                type = subroutineType,
                //TODO: need more investigations.
                isLocal = 0,
                isDefinition = 1,
                scopeLine = 0)!!

    //-------------------------------------------------------------------------//

    private fun getContinuation(): LLVMValueRef {
        val caller = functionGenerationContext.irFunction!!
        return if (caller.isSuspend)
            codegen.param(caller, caller.allParametersCount)    // The last argument.
        else {
            // Suspend call from non-suspend function - must be [invokeSuspend].
            assert ((caller as IrSimpleFunction).overrides(context.ir.symbols.invokeSuspendFunction.owner),
                    { "Expected 'BaseContinuationImpl.invokeSuspend' but was '$caller'" })
            currentCodeContext.genGetValue(caller.dispatchReceiverParameter!!)
        }
    }

    private fun IrFunction.returnsUnit() = returnType.isUnit() && !isSuspend

    /**
     * Evaluates all arguments of [expression] that are explicitly represented in the IR.
     * Returns results in the same order as LLVM function expects, assuming that all explicit arguments
     * exactly correspond to a tail of LLVM parameters.
     */
    private fun evaluateExplicitArgs(expression: IrFunctionAccessExpression): List<LLVMValueRef> {
        val result = expression.getArgumentsWithIr().map { (_, argExpr) ->
            evaluateExpression(argExpr)
        }
        val explicitParametersCount = expression.symbol.owner.explicitParametersCount
        if (result.size != explicitParametersCount) {
            error("Number of arguments explicitly represented in the IR ${result.size} differs from expected " +
                    "$explicitParametersCount in ${ir2string(expression)}")
        }
        return result
    }

    //-------------------------------------------------------------------------//

    private fun evaluateFunctionReference(expression: IrFunctionReference): LLVMValueRef {
        // TODO: consider creating separate IR element for pointer to function.
        assert (expression.type.getClass()?.descriptor == context.interopBuiltIns.cPointer) {
            "assert: ${expression.type.getClass()?.descriptor} == ${context.interopBuiltIns.cPointer}"
        }

        assert (expression.getArguments().isEmpty())

        val function = expression.symbol.owner
        assert (function.dispatchReceiverParameter == null)

        return codegen.functionEntryPointAddress(function)
    }

    //-------------------------------------------------------------------------//

    private inner class SuspendableExpressionScope(val resumePoints: MutableList<LLVMBasicBlockRef>) : InnerScopeImpl() {
        override fun addResumePoint(bbLabel: LLVMBasicBlockRef): Int {
            val result = resumePoints.size
            resumePoints.add(bbLabel)
            return result
        }
    }

    private fun evaluateSuspendableExpression(expression: IrSuspendableExpression): LLVMValueRef {
        val suspensionPointId = evaluateExpression(expression.suspensionPointId)
        val bbStart = functionGenerationContext.basicBlock("start", expression.result.startLocation)
        val bbDispatch = functionGenerationContext.basicBlock("dispatch", expression.suspensionPointId.startLocation)

        val resumePoints = mutableListOf<LLVMBasicBlockRef>()
        using (SuspendableExpressionScope(resumePoints)) {
            functionGenerationContext.condBr(functionGenerationContext.icmpEq(suspensionPointId, kNullInt8Ptr), bbStart, bbDispatch)

            functionGenerationContext.positionAtEnd(bbStart)
            val result = evaluateExpression(expression.result)

            functionGenerationContext.appendingTo(bbDispatch) {
                if (context.config.indirectBranchesAreAllowed)
                    functionGenerationContext.indirectBr(suspensionPointId, resumePoints)
                else {
                    val bbElse = functionGenerationContext.basicBlock("else", null) {
                        functionGenerationContext.unreachable()
                    }

                    val cases = resumePoints.withIndex().map { Int32(it.index + 1).llvm to it.value }
                    functionGenerationContext.switch(functionGenerationContext.ptrToInt(suspensionPointId, int32Type), cases, bbElse)
                }
            }
            return result
        }
    }

    private inner class SuspensionPointScope(val suspensionPointId: IrVariable,
                                             val bbResume: LLVMBasicBlockRef,
                                             val bbResumeId: Int): InnerScopeImpl() {
        override fun genGetValue(value: IrValueDeclaration): LLVMValueRef {
            if (value == suspensionPointId) {
                return if (context.config.indirectBranchesAreAllowed)
                           functionGenerationContext.blockAddress(bbResume)
                       else
                           functionGenerationContext.intToPtr(Int32(bbResumeId + 1).llvm, int8TypePtr)
            }
            return super.genGetValue(value)
        }
    }

    private fun evaluateSuspensionPoint(expression: IrSuspensionPoint): LLVMValueRef {
        val bbResume = functionGenerationContext.basicBlock("resume", expression.resumeResult.startLocation)
        val id = currentCodeContext.addResumePoint(bbResume)

        using (SuspensionPointScope(expression.suspensionPointIdParameter, bbResume, id)) {
            continuationBlock(expression.type, expression.result.startLocation).run {
                val normalResult = evaluateExpression(expression.result)
                functionGenerationContext.jump(this, normalResult)

                functionGenerationContext.positionAtEnd(bbResume)
                val resumeResult = evaluateExpression(expression.resumeResult)
                functionGenerationContext.jump(this, resumeResult)

                functionGenerationContext.positionAtEnd(this.block)
                return this.value
            }
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateClassReference(classReference: IrClassReference): LLVMValueRef {
        val typeInfoPtr = codegen.typeInfoValue(classReference.symbol.owner as IrClass)
        return functionGenerationContext.bitcast(int8TypePtr, typeInfoPtr)
    }

    //-------------------------------------------------------------------------//

    private fun evaluateFunctionCall(callee: IrCall, args: List<LLVMValueRef>,
                                     resultLifetime: Lifetime): LLVMValueRef {
        val function = callee.symbol.owner

        val argsWithContinuationIfNeeded = if (function.isSuspend)
                                               args + getContinuation()
                                           else args
        return when {
            function.isTypedIntrinsic -> intrinsicGenerator.evaluateCall(callee, args)
            function.isBuiltInOperator -> evaluateOperatorCall(callee, argsWithContinuationIfNeeded)
            function.origin == DECLARATION_ORIGIN_FILE_GLOBAL_INITIALIZER -> evaluateFileGlobalInitializerCall(function)
            function.origin == DECLARATION_ORIGIN_FILE_THREAD_LOCAL_INITIALIZER -> evaluateFileThreadLocalInitializerCall(function)
            function.origin == DECLARATION_ORIGIN_FILE_STANDALONE_THREAD_LOCAL_INITIALIZER -> evaluateFileStandaloneThreadLocalInitializerCall(function)
            else -> evaluateSimpleFunctionCall(function, argsWithContinuationIfNeeded, resultLifetime, callee.superQualifierSymbol?.owner)
        }
    }

    private fun evaluateFileGlobalInitializerCall(fileInitializer: IrFunction) = with(functionGenerationContext) {
        val statePtr = getGlobalInitStateFor(fileInitializer.parent as IrFile)
        val initializerPtr = with(codegen) { fileInitializer.llvmFunction.llvmValue }

        val bbInit = basicBlock("label_init", null)
        val bbExit = basicBlock("label_continue", null)
        moveBlockAfterEntry(bbExit)
        moveBlockAfterEntry(bbInit)
        // TODO: Is it ok to use non-volatile read here since once value is FILE_INITIALIZED, it is no longer change?
        val state = load(statePtr)
        LLVMSetVolatile(state, 1)
        condBr(icmpEq(state, Int32(FILE_INITIALIZED).llvm), bbExit, bbInit)
        positionAtEnd(bbInit)
        call(context.llvm.callInitGlobalPossiblyLock, listOf(statePtr, initializerPtr),
                exceptionHandler = currentCodeContext.exceptionHandler)
        br(bbExit)
        positionAtEnd(bbExit)
        codegen.theUnitInstanceRef.llvm
    }

    private fun evaluateFileThreadLocalInitializerCall(fileInitializer: IrFunction) = with(functionGenerationContext) {
        val globalStatePtr = getGlobalInitStateFor(fileInitializer.parent as IrFile)
        val localState = getThreadLocalInitStateFor(fileInitializer.parent as IrFile)
        val localStatePtr = localState.getAddress(functionGenerationContext)
        val initializerPtr = with(codegen) { fileInitializer.llvmFunction.llvmValue }

        val bbInit = basicBlock("label_init", null)
        val bbCheckLocalState = basicBlock("label_check_local", null)
        val bbExit = basicBlock("label_continue", null)
        moveBlockAfterEntry(bbExit)
        moveBlockAfterEntry(bbCheckLocalState)
        moveBlockAfterEntry(bbInit)
        val globalState = load(globalStatePtr)
        LLVMSetVolatile(globalState, 1)
        // Make sure we're not in the middle of global initializer invocation -
        // thread locals can be initialized only after all shared globals have been initialized.
        condBr(icmpNe(globalState, Int32(FILE_INITIALIZED).llvm), bbExit, bbCheckLocalState)
        positionAtEnd(bbCheckLocalState)
        condBr(icmpNe(load(localStatePtr), Int32(FILE_INITIALIZED).llvm), bbInit, bbExit)
        positionAtEnd(bbInit)
        call(context.llvm.callInitThreadLocal, listOf(globalStatePtr, localStatePtr, initializerPtr),
                exceptionHandler = currentCodeContext.exceptionHandler)
        br(bbExit)
        positionAtEnd(bbExit)
        codegen.theUnitInstanceRef.llvm
    }

    private fun evaluateFileStandaloneThreadLocalInitializerCall(fileInitializer: IrFunction) = with(functionGenerationContext) {
        val state = getThreadLocalInitStateFor(fileInitializer.parent as IrFile)
        val statePtr = state.getAddress(functionGenerationContext)
        val initializerPtr = with(codegen) { fileInitializer.llvmFunction.llvmValue }

        val bbInit = basicBlock("label_init", null)
        val bbExit = basicBlock("label_continue", null)
        moveBlockAfterEntry(bbExit)
        moveBlockAfterEntry(bbInit)
        condBr(icmpEq(load(statePtr), Int32(FILE_INITIALIZED).llvm), bbExit, bbInit)
        positionAtEnd(bbInit)
        call(context.llvm.callInitThreadLocal, listOf(kNullInt32Ptr, statePtr, initializerPtr),
                exceptionHandler = currentCodeContext.exceptionHandler)
        br(bbExit)
        positionAtEnd(bbExit)
        codegen.theUnitInstanceRef.llvm
    }

    //-------------------------------------------------------------------------//

    private fun evaluateSimpleFunctionCall(
            function: IrFunction, args: List<LLVMValueRef>,
            resultLifetime: Lifetime, superClass: IrClass? = null): LLVMValueRef {
        //context.log{"evaluateSimpleFunctionCall : $tmpVariableName = ${ir2string(value)}"}
        if (superClass == null && function is IrSimpleFunction && function.isOverridable)
            return callVirtual(function, args, resultLifetime)
        else
            return callDirect(function, args, resultLifetime)
    }

    //-------------------------------------------------------------------------//
    private fun resultLifetime(callee: IrElement): Lifetime {
        return lifetimes.getOrElse(callee) { /* TODO: make IRRELEVANT */ Lifetime.GLOBAL }
    }

    private fun evaluateConstructorCall(callee: IrConstructorCall, args: List<LLVMValueRef>): LLVMValueRef {
        context.log{"evaluateConstructorCall        : ${ir2string(callee)}"}
        return memScoped {
            val constructedClass = callee.symbol.owner.constructedClass
            val thisValue = when {
                constructedClass.isArray -> {
                    assert(args.isNotEmpty() && args[0].type == int32Type)
                    functionGenerationContext.allocArray(constructedClass, args[0],
                            resultLifetime(callee), currentCodeContext.exceptionHandler)
                }
                constructedClass == context.ir.symbols.string.owner -> {
                    // TODO: consider returning the empty string literal instead.
                    assert(args.isEmpty())
                    functionGenerationContext.allocArray(constructedClass, count = kImmZero,
                            lifetime = resultLifetime(callee), exceptionHandler = currentCodeContext.exceptionHandler)
                }

                constructedClass.isObjCClass() -> error("Call should've been lowered: ${callee.dump()}")

                else -> functionGenerationContext.allocInstance(constructedClass, resultLifetime(callee),
                        currentCodeContext.stackLocalsManager)
            }
            evaluateSimpleFunctionCall(callee.symbol.owner,
                    listOf(thisValue) + args, Lifetime.IRRELEVANT /* constructor doesn't return anything */)
            thisValue
        }
    }

    private fun genGetObjCClass(irClass: IrClass): LLVMValueRef {
        return functionGenerationContext.getObjCClass(irClass, currentCodeContext.exceptionHandler)
    }

    private fun genGetObjCProtocol(irClass: IrClass): LLVMValueRef {
        // Note: this function will return the same result for Obj-C protocol and corresponding meta-class.

        assert(irClass.isInterface)
        assert(irClass.isExternalObjCClass())

        val annotation = irClass.annotations.findAnnotation(externalObjCClassFqName)!!
        val protocolGetterName = annotation.getAnnotationStringValue("protocolGetter")
        val protocolGetterProto = LlvmFunctionProto(
                protocolGetterName,
                LlvmRetType(int8TypePtr),
                origin = irClass.llvmSymbolOrigin,
                independent = true // Protocol is header-only declaration.
        )
        val protocolGetter = context.llvm.externalFunction(protocolGetterProto)

        return call(protocolGetter, emptyList())
    }

    //-------------------------------------------------------------------------//
    private val kImmZero = Int32(0).llvm
    private val kImmOne  = Int32(1).llvm
    private val kTrue    = Int1(true).llvm
    private val kFalse   = Int1(false).llvm

    // TODO: Intrinsify?
    private fun evaluateOperatorCall(callee: IrCall, args: List<LLVMValueRef>): LLVMValueRef {
        context.log{"evaluateOperatorCall           : origin:${ir2string(callee)}"}
        val function = callee.symbol.owner
        val ib = context.irModule!!.irBuiltins

        with(functionGenerationContext) {
            val functionSymbol = function.symbol
            return when (functionSymbol) {
                ib.eqeqeqSymbol -> icmpEq(args[0], args[1])
                ib.booleanNotSymbol -> icmpNe(args[0], kTrue)
                else -> {
                    val isFloatingPoint = args[0].type.isFloatingPoint()
                    // LLVM does not distinguish between signed/unsigned integers, so we must check
                    // the parameter type.
                    val shouldUseUnsignedComparison = function.valueParameters[0].type.isChar()
                    when {
                        functionSymbol.isComparisonFunction(ib.greaterFunByOperandType) -> {
                            when {
                                isFloatingPoint -> fcmpGt(args[0], args[1])
                                shouldUseUnsignedComparison -> icmpUGt(args[0], args[1])
                                else -> icmpGt(args[0], args[1])
                            }
                        }
                        functionSymbol.isComparisonFunction(ib.greaterOrEqualFunByOperandType) -> {
                            when {
                                isFloatingPoint -> fcmpGe(args[0], args[1])
                                shouldUseUnsignedComparison -> icmpUGe(args[0], args[1])
                                else -> icmpGe(args[0], args[1])
                            }
                        }
                        functionSymbol.isComparisonFunction(ib.lessFunByOperandType) -> {
                            when {
                                isFloatingPoint -> fcmpLt(args[0], args[1])
                                shouldUseUnsignedComparison -> icmpULt(args[0], args[1])
                                else -> icmpLt(args[0], args[1])
                            }
                        }
                        functionSymbol.isComparisonFunction(ib.lessOrEqualFunByOperandType) -> {
                            when {
                                isFloatingPoint -> fcmpLe(args[0], args[1])
                                shouldUseUnsignedComparison -> icmpULe(args[0], args[1])
                                else -> icmpLe(args[0], args[1])
                            }
                        }
                        functionSymbol == context.irBuiltIns.illegalArgumentExceptionSymbol -> {
                            callDirect(
                                    context.ir.symbols.throwIllegalArgumentExceptionWithMessage.owner,
                                    args,
                                    Lifetime.GLOBAL
                            )
                        }
                        else -> TODO(function.name.toString())
                    }
                }
            }
        }
    }

    //-------------------------------------------------------------------------//

    fun callDirect(function: IrFunction, args: List<LLVMValueRef>, resultLifetime: Lifetime): LLVMValueRef {
        val functionDeclarations = codegen.llvmFunction(function.target)
        return call(function, functionDeclarations, args, resultLifetime)
    }

    //-------------------------------------------------------------------------//

    fun callVirtual(function: IrFunction, args: List<LLVMValueRef>, resultLifetime: Lifetime): LLVMValueRef {
        val functionDeclarations = functionGenerationContext.lookupVirtualImpl(args.first(), function)
        return call(function, functionDeclarations, args, resultLifetime)
    }

    //-------------------------------------------------------------------------//

    private val IrFunction.needsNativeThreadState: Boolean
        get() {
            // We assume that call site thread state switching is required for interop calls only.
            val result = context.memoryModel == MemoryModel.EXPERIMENTAL && origin == CBridgeOrigin.KOTLIN_TO_C_BRIDGE
            if (result) {
                check(isExternal)
                // TODO: this should be separate annotation
                // check(!annotations.hasAnnotation(KonanFqNames.gcUnsafeCall))
                check(annotations.hasAnnotation(RuntimeNames.filterExceptions))
            }
            return result
        }

    private fun call(function: IrFunction, llvmCallable: LlvmCallable, args: List<LLVMValueRef>,
                     resultLifetime: Lifetime): LLVMValueRef {
        check(!function.isTypedIntrinsic)

        val needsNativeThreadState = function.needsNativeThreadState
        val exceptionHandler = function.annotations.findAnnotation(RuntimeNames.filterExceptions)?.let {
            val foreignExceptionMode = ForeignExceptionMode.byValue(it.getAnnotationValueOrNull<String>("mode"))
            functionGenerationContext.filteringExceptionHandler(
                    currentCodeContext.exceptionHandler,
                    foreignExceptionMode,
                    needsNativeThreadState
            )
        } ?: currentCodeContext.exceptionHandler

        if (needsNativeThreadState) {
            functionGenerationContext.switchThreadState(ThreadState.Native)
        }

        val result = call(llvmCallable, args, resultLifetime, exceptionHandler)

        when {
            !function.isSuspend && function.returnType.isNothing() ->
                functionGenerationContext.unreachable()
            needsNativeThreadState ->
                functionGenerationContext.switchThreadState(ThreadState.Runnable)
        }

        if (llvmCallable.returnType == voidType) {
            return codegen.theUnitInstanceRef.llvm
        }

        return result
    }

    private fun call(
            function: LlvmCallable, args: List<LLVMValueRef>,
            resultLifetime: Lifetime = Lifetime.IRRELEVANT,
            exceptionHandler: ExceptionHandler = currentCodeContext.exceptionHandler,
    ): LLVMValueRef {
        return functionGenerationContext.call(function, args, resultLifetime, exceptionHandler)
    }

    //-------------------------------------------------------------------------//

    private fun delegatingConstructorCall(constructor: IrConstructor, args: List<LLVMValueRef>): LLVMValueRef {

        val constructedClass = functionGenerationContext.constructedClass!!
        val thisPtr = currentCodeContext.genGetValue(constructedClass.thisReceiver!!)

        if (constructor.constructedClass.isExternalObjCClass() || constructor.constructedClass.isAny()) {
            assert(args.isEmpty())
            return codegen.theUnitInstanceRef.llvm
        }

        val thisPtrArgType = codegen.getLLVMType(constructor.allParameters[0].type)
        val thisPtrArg = if (thisPtr.type == thisPtrArgType) {
            thisPtr
        } else {
            // e.g. when array constructor calls super (i.e. Any) constructor.
            functionGenerationContext.bitcast(thisPtrArgType, thisPtr)
        }

        return callDirect(constructor, listOf(thisPtrArg) + args,
                Lifetime.IRRELEVANT /* no value returned */)
    }

    //-------------------------------------------------------------------------//

    private fun appendLlvmUsed(name: String, args: List<LLVMValueRef>) {
        if (args.isEmpty()) return

        val argsCasted = args.map { it -> constPointer(it).bitcast(int8TypePtr) }
        val llvmUsedGlobal =
                context.llvm.staticData.placeGlobalArray(name, int8TypePtr, argsCasted)

        LLVMSetLinkage(llvmUsedGlobal.llvmGlobal, LLVMLinkage.LLVMAppendingLinkage)
        LLVMSetSection(llvmUsedGlobal.llvmGlobal, "llvm.metadata")
    }

    // Globals set this way will be const, but can only be built into runtime-containing module. Which
    // means they are set at stdlib-cache compilation time.
    private fun setRuntimeConstGlobal(name: String, value: ConstValue) {
        val global = context.llvm.staticData.placeGlobal(name, value)
        global.setConstant(true)
        global.setLinkage(LLVMLinkage.LLVMExternalLinkage)
    }

    private fun setRuntimeConstGlobals() {
        if (!context.producedLlvmModuleContainsStdlib)
            return

        setRuntimeConstGlobal("KonanNeedDebugInfo", Int32(if (context.shouldContainDebugInfo()) 1 else 0))
        setRuntimeConstGlobal("Kotlin_runtimeAssertsMode", Int32(context.config.runtimeAssertsMode.value))
        val runtimeLogs = context.config.runtimeLogs?.let {
            context.llvm.staticData.cStringLiteral(it)
        } ?: NullPointer(int8Type)
        setRuntimeConstGlobal("Kotlin_runtimeLogs", runtimeLogs)
    }

    // Globals set this way cannot be const, but are overridable when producing final executable.
    private fun overrideRuntimeGlobal(name: String, value: ConstValue) {
        // TODO: A similar mechanism is used in `ObjCExportCodeGenerator`. Consider merging them.
        if (context.llvmModuleSpecification.importsKotlinDeclarationsFromOtherSharedLibraries()) {
            // When some dynamic caches are used, we consider that stdlib is in the dynamic cache as well.
            // Runtime is linked into stdlib module only, so import runtime global from it.
            val global = codegen.importGlobal(name, value.llvmType, context.standardLlvmSymbolsOrigin)
            val initializer = generateFunctionNoRuntime(codegen, functionType(voidType, false), "") {
                store(value.llvm, global)
                ret(null)
            }

            LLVMSetLinkage(initializer, LLVMLinkage.LLVMPrivateLinkage)

            context.llvm.otherStaticInitializers += initializer
        } else {
            context.llvmImports.add(context.standardLlvmSymbolsOrigin)
            // Define a strong runtime global. It'll overrule a weak global defined in a statically linked runtime.
            val global = context.llvm.staticData.placeGlobal(name, value, true)

            if (context.llvmModuleSpecification.importsKotlinDeclarationsFromOtherObjectFiles()) {
                context.llvm.usedGlobals += global.llvmGlobal
                LLVMSetVisibility(global.llvmGlobal, LLVMVisibility.LLVMHiddenVisibility)
            }
        }
    }

    private fun overrideRuntimeGlobals() {
        if (!context.config.produce.isFinalBinary)
            return

        overrideRuntimeGlobal("Kotlin_destroyRuntimeMode", Int32(context.config.destroyRuntimeMode.value))
        overrideRuntimeGlobal("Kotlin_gcAggressive", Int32(if (context.config.gcAggressive) 1 else 0))
        overrideRuntimeGlobal("Kotlin_workerExceptionHandling", Int32(context.config.workerExceptionHandling.value))
        overrideRuntimeGlobal("Kotlin_freezingEnabled", Int32(if (context.config.freezing.enableFreezeAtRuntime) 1 else 0))
        val getSourceInfoFunctionName = when (context.config.sourceInfoType) {
            SourceInfoType.NOOP -> null
            SourceInfoType.LIBBACKTRACE -> "Kotlin_getSourceInfo_libbacktrace"
            SourceInfoType.CORESYMBOLICATION -> "Kotlin_getSourceInfo_core_symbolication"
        }
        if (getSourceInfoFunctionName != null) {
            val getSourceInfoFunction = LLVMGetNamedFunction(context.llvmModule, getSourceInfoFunctionName)
                    ?: LLVMAddFunction(context.llvmModule, getSourceInfoFunctionName,
                            functionType(int32Type, false, int8TypePtr, int8TypePtr, int32Type))
            overrideRuntimeGlobal("Kotlin_getSourceInfo_Function", constValue(getSourceInfoFunction!!))
        }
        if (context.config.target.family == Family.ANDROID && context.config.produce == CompilerOutputKind.PROGRAM) {
            val configuration = context.config.configuration
            val programType = configuration.get(BinaryOptions.androidProgramType) ?: AndroidProgramType.Default
            overrideRuntimeGlobal("Kotlin_printToAndroidLogcat", Int32(if (programType.consolePrintsToLogcat) 1 else 0))
        }
    }

    //-------------------------------------------------------------------------//
    // Create type { i32, void ()*, i8* }

    val kCtorType = structType(int32Type, pointerType(kVoidFuncType), kInt8Ptr)

    //-------------------------------------------------------------------------//
    // Create object { i32, void ()*, i8* } { i32 1, void ()* @ctorFunction, i8* null }

    fun createGlobalCtor(ctorFunction: LLVMValueRef): ConstPointer {
        val priority = if (context.config.target.family == Family.MINGW) {
            // Workaround MinGW bug. Using this value makes the compiler generate
            // '.ctors' section instead of '.ctors.XXXXX', which can't be recognized by ld
            // when string table is too long.
            // More details: https://youtrack.jetbrains.com/issue/KT-39548
            Int32(65535).llvm
            // Note: this difference in priorities doesn't actually make initializers
            // platform-dependent, because handling priorities for initializers
            // from different object files is platform-dependent anyway.
        } else {
            kImmInt32One
        }
        val data     = kNullInt8Ptr
        val argList  = cValuesOf(priority, ctorFunction, data)
        val ctorItem = LLVMConstNamedStruct(kCtorType, argList, 3)!!
        return constPointer(ctorItem)
    }

    //-------------------------------------------------------------------------//
    fun appendStaticInitializers() {
        // Note: the list of libraries is topologically sorted (in order for initializers to be called correctly).
        val libraries = (context.llvm.allBitcodeDependencies + listOf(null)/* Null for "current" non-library module */)

        val libraryToInitializers = libraries.associateWith {
            mutableListOf<LLVMValueRef>()
        }

        context.llvm.irStaticInitializers.forEach {
            val library = it.konanLibrary
            val initializers = libraryToInitializers[library]
                    ?: error("initializer for not included library ${library?.libraryFile}")

            initializers.add(it.initializer)
        }

        val ctorFunctions = libraries.map { library ->
            val ctorName = if (library != null) {
                library.moduleConstructorName
            } else {
                context.config.moduleId.moduleConstructorName
            }

            val ctorFunction = addLlvmFunctionWithDefaultAttributes(
                    context,
                    context.llvmModule!!,
                    ctorName,
                    kVoidFuncType
            )
            LLVMSetLinkage(ctorFunction, LLVMLinkage.LLVMExternalLinkage)

            val initializers = libraryToInitializers.getValue(library)

            if (library == null || context.llvmModuleSpecification.containsLibrary(library)) {
                val otherInitializers =
                        context.llvm.otherStaticInitializers.takeIf { library == null }.orEmpty()

                appendStaticInitializers(ctorFunction, initializers + otherInitializers)
            } else {
                check(initializers.isEmpty()) {
                    "found initializer from ${library.libraryFile}, which is not included into compilation"
                }
            }

            ctorFunction
        }

        appendGlobalCtors(ctorFunctions)
    }

    private fun appendStaticInitializers(ctorFunction: LLVMValueRef, initializers: List<LLVMValueRef>) {
        generateFunctionNoRuntime(codegen, ctorFunction) {
            val initGuardName = ctorFunction.name.orEmpty() + "_guard"
            val initGuard = LLVMAddGlobal(context.llvmModule, int32Type, initGuardName)
            LLVMSetInitializer(initGuard, kImmZero)
            LLVMSetLinkage(initGuard, LLVMLinkage.LLVMPrivateLinkage)
            val bbInited = basicBlock("inited", null)
            val bbNeedInit = basicBlock("need_init", null)


            val value = LLVMBuildLoad(builder, initGuard, "")!!
            condBr(icmpEq(value, kImmZero), bbNeedInit, bbInited)

            appendingTo(bbInited) {
                ret(null)
            }

            appendingTo(bbNeedInit) {
                LLVMBuildStore(builder, kImmOne, initGuard)

                // TODO: shall we put that into the try block?
                initializers.forEach {
                    call(it, emptyList(), Lifetime.IRRELEVANT,
                            exceptionHandler = ExceptionHandler.Caller, verbatim = true)
                }
                ret(null)
            }
        }
    }

    private fun appendGlobalCtors(ctorFunctions: List<LLVMValueRef>) {
        if (context.config.produce.isFinalBinary) {
            // Generate function calling all [ctorFunctions].
            val globalCtorFunction = generateFunctionNoRuntime(codegen, kVoidFuncType, "_Konan_constructors") {
                ctorFunctions.forEach {
                    call(it, emptyList(), Lifetime.IRRELEVANT,
                            exceptionHandler = ExceptionHandler.Caller, verbatim = true)
                }
                ret(null)
            }
            LLVMSetLinkage(globalCtorFunction, LLVMLinkage.LLVMPrivateLinkage)

            // Append initializers of global variables in "llvm.global_ctors" array.
            val globalCtors = context.llvm.staticData.placeGlobalArray("llvm.global_ctors", kCtorType,
                    listOf(createGlobalCtor(globalCtorFunction)))
            LLVMSetLinkage(globalCtors.llvmGlobal, LLVMLinkage.LLVMAppendingLinkage)
            if (context.config.produce == CompilerOutputKind.PROGRAM) {
                // Provide an optional handle for calling .ctors, if standard constructors mechanism
                // is not available on the platform (i.e. WASM, embedded).
                LLVMSetLinkage(globalCtorFunction, LLVMLinkage.LLVMExternalLinkage)
                appendLlvmUsed("llvm.used", listOf(globalCtorFunction))
            }
        }
    }

    //-------------------------------------------------------------------------//

    fun FunctionGenerationContext.basicBlock(name: String, locationInfo: LocationInfo?, code: () -> Unit) = functionGenerationContext.basicBlock(name, locationInfo).apply {
        appendingTo(this) {
            code()
        }
    }
}

private fun IrValueParameter.debugNameConversion() = descriptor.name.debugNameConversion()

private fun IrVariable.debugNameConversion() = descriptor.name.debugNameConversion()

private val thisName = Name.special("<this>")
private val underscoreThisName = Name.identifier("_this")
/**
 * HACK: this is workaround for GH-2316, to let IDE some how operate with this.
 * We're experiencing issue with libclang which is used as compiler of expression in lldb
 * for current state support Kotlin in lldb:
 *   1. <this> isn't accepted by libclang as valid variable name.
 *   2. this is reserved name and compiled in special way.
 */
private fun Name.debugNameConversion(): Name = when(this) {
    thisName -> underscoreThisName
    else -> this
}

internal class LocationInfo(val scope: DIScopeOpaqueRef,
                            val line: Int,
                            val column: Int,
                            val inlinedAt: LocationInfo? = null) {
    init {
        assert(line != 0)
    }
}
