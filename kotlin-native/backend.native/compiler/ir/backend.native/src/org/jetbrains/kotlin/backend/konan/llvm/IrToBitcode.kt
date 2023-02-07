/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.backend.common.lower.coroutines.getOrCreateFunctionWithContinuationStub
import org.jetbrains.kotlin.backend.common.lower.inline.InlinerExpressionLocationHint
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.cexport.CAdapterApiExporter
import org.jetbrains.kotlin.backend.konan.cexport.CAdapterCodegen
import org.jetbrains.kotlin.backend.konan.cexport.CAdapterExportedElements
import org.jetbrains.kotlin.backend.konan.cgen.CBridgeOrigin
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.backend.konan.ir.*
import org.jetbrains.kotlin.backend.konan.llvm.coverage.LLVMCoverageInstrumentation
import org.jetbrains.kotlin.backend.konan.lower.*
import org.jetbrains.kotlin.backend.konan.lower.DECLARATION_ORIGIN_STATIC_GLOBAL_INITIALIZER
import org.jetbrains.kotlin.backend.konan.lower.DECLARATION_ORIGIN_STATIC_STANDALONE_THREAD_LOCAL_INITIALIZER
import org.jetbrains.kotlin.backend.konan.lower.DECLARATION_ORIGIN_STATIC_THREAD_LOCAL_INITIALIZER
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
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
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.classId

internal enum class FieldStorageKind {
    GLOBAL, // In the old memory model these are only accessible from the "main" thread.
    SHARED_FROZEN,
    THREAD_LOCAL
}

// TODO: maybe unannotated singleton objects shall be accessed from main thread only as well?
internal fun IrField.storageKind(context: Context): FieldStorageKind {
    // TODO: Is this correct?
    val annotations = correspondingPropertySymbol?.owner?.annotations ?: annotations
    val isLegacyMM = context.memoryModel != MemoryModel.EXPERIMENTAL
    // TODO: simplify, once IR types are fully there.
    val typeAnnotations = (type.classifierOrNull?.owner as? IrAnnotationContainer)?.annotations
    val typeFrozen = typeAnnotations?.hasAnnotation(KonanFqNames.frozen) == true ||
        (typeAnnotations?.hasAnnotation(KonanFqNames.frozenLegacyMM) == true && isLegacyMM)
    return when {
        annotations.hasAnnotation(KonanFqNames.threadLocal) -> FieldStorageKind.THREAD_LOCAL
        !isLegacyMM && !context.config.freezing.freezeImplicit -> FieldStorageKind.GLOBAL
        !isFinal -> FieldStorageKind.GLOBAL
        annotations.hasAnnotation(KonanFqNames.sharedImmutable) -> FieldStorageKind.SHARED_FROZEN
        typeFrozen -> FieldStorageKind.SHARED_FROZEN
        else -> FieldStorageKind.GLOBAL
    }
}

internal fun IrField.needsGCRegistration(context: Context) =
        context.memoryModel == MemoryModel.EXPERIMENTAL && // only for the new MM
                type.binaryTypeIsReference() && // only for references
                (hasNonConstInitializer || // which are initialized from heap object
                        !isFinal) // or are not final


internal fun IrField.isGlobalNonPrimitive(context: Context) = when  {
        type.computePrimitiveBinaryTypeOrNull() != null -> false
        else -> storageKind(context) == FieldStorageKind.GLOBAL
    }


internal fun IrField.shouldBeFrozen(context: Context): Boolean =
        this.storageKind(context) == FieldStorageKind.SHARED_FROZEN

internal class RTTIGeneratorVisitor(generationState: NativeGenerationState, referencedFunctions: Set<IrFunction>?) : IrElementVisitorVoid {
    val generator = RTTIGenerator(generationState, referencedFunctions)

    val kotlinObjCClassInfoGenerator = KotlinObjCClassInfoGenerator(generationState)

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

    fun getReturnSlot(target: IrSymbolOwner) : LLVMValueRef?

    fun genBreak(destination: IrBreak)

    fun genContinue(destination: IrContinue)

    val exceptionHandler: ExceptionHandler

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
    fun genGetValue(value: IrValueDeclaration, resultSlot: LLVMValueRef?): LLVMValueRef

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

    /**
     * Called, when context is pushed on stack
     */
    fun onEnter() {}

    /**
     * Called, when context is removed from stack
     */
    fun onExit() {}
}

//-------------------------------------------------------------------------//

internal class CodeGeneratorVisitor(
        val generationState: NativeGenerationState,
        val irBuiltins: IrBuiltIns,
        val lifetimes: Map<IrElement, Lifetime>
) : IrElementVisitorVoid {
    private val context = generationState.context
    private val llvm = generationState.llvm
    private val debugInfo: DebugInfo
        get() = generationState.debugInfo

    val codegen = CodeGenerator(generationState)

    // TODO: consider eliminating mutable state
    private var currentCodeContext: CodeContext = TopLevelCodeContext

    private val intrinsicGeneratorEnvironment = object : IntrinsicGeneratorEnvironment {
        override val codegen: CodeGenerator
            get() = this@CodeGeneratorVisitor.codegen

        override val functionGenerationContext: FunctionGenerationContext
            get() = this@CodeGeneratorVisitor.functionGenerationContext

        override fun calculateLifetime(element: IrElement): Lifetime =
                resultLifetime(element)

        override val exceptionHandler: ExceptionHandler
            get() = currentCodeContext.exceptionHandler

        override fun evaluateCall(function: IrFunction, args: List<LLVMValueRef>, resultLifetime: Lifetime, superClass: IrClass?, resultSlot: LLVMValueRef?) =
                evaluateSimpleFunctionCall(function, args, resultLifetime, superClass, resultSlot)

        override fun evaluateExplicitArgs(expression: IrFunctionAccessExpression): List<LLVMValueRef> =
                this@CodeGeneratorVisitor.evaluateExplicitArgs(expression)

        override fun evaluateExpression(value: IrExpression, resultSlot: LLVMValueRef?): LLVMValueRef =
                this@CodeGeneratorVisitor.evaluateExpression(value, resultSlot)

        override fun getObjectFieldPointer(thisRef: LLVMValueRef, field: IrField): LLVMValueRef =
                this@CodeGeneratorVisitor.fieldPtrOfClass(thisRef, field)

        override fun getStaticFieldPointer(field: IrField) =
                this@CodeGeneratorVisitor.staticFieldPtr(field, functionGenerationContext)
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

        override fun getReturnSlot(target: IrSymbolOwner): LLVMValueRef? = unsupported(target)

        override fun genBreak(destination: IrBreak) = unsupported()

        override fun genContinue(destination: IrContinue) = unsupported()

        override val exceptionHandler get() = unsupported()

        override fun genDeclareVariable(variable: IrVariable, value: LLVMValueRef?, variableLocation: VariableDebugLocation?) = unsupported(variable)

        override fun getDeclaredValue(value: IrValueDeclaration) = -1

        override fun genGetValue(value: IrValueDeclaration, resultSlot: LLVMValueRef?) = unsupported(value)

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
            codeContext.onEnter()
        }
        try {
            return block()
        } finally {
            codeContext?.onExit()
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
    private fun appendCAdapters(elements: CAdapterExportedElements) {
        CAdapterCodegen(codegen, generationState).buildAllAdaptersRecursively(elements)
        // TODO: It is not a part of IrToBitcode. Maybe move it somewhere?
        CAdapterApiExporter(generationState, elements).makeGlobalStruct()
    }

    private fun FunctionGenerationContext.initThreadLocalField(irField: IrField) {
        val initializer = irField.initializer ?: return
        val address = staticFieldPtr(irField, this)
        storeAny(evaluateExpression(initializer.expression), address, false)
    }

    private fun FunctionGenerationContext.initGlobalField(irField: IrField) {
        val address = staticFieldPtr(irField, this)
        val initialValue = if (irField.hasNonConstInitializer) {
            val initialization = evaluateExpression(irField.initializer!!.expression)
            if (irField.shouldBeFrozen(context))
                freeze(initialization, currentCodeContext.exceptionHandler)
            initialization
        } else {
            null
        }
        if (irField.needsGCRegistration(context)) {
            call(llvm.initAndRegisterGlobalFunction, listOf(address, initialValue
                    ?: kNullObjHeaderPtr))
        } else if (initialValue != null) {
            storeAny(initialValue, address, false)
        }
    }

    private fun buildInitializerFunctions(scopeState: ScopeInitializersGenerationState) {
        scopeState.globalInitFunction?.let { fileInitFunction ->
            generateFunction(codegen, fileInitFunction, fileInitFunction.location(start = true), fileInitFunction.location(start = false)) {
                using(FunctionScope(fileInitFunction, this)) {
                    val parameterScope = ParameterScope(fileInitFunction, functionGenerationContext)
                    using(parameterScope) usingParameterScope@{
                        using(VariableScope()) usingVariableScope@{
                            scopeState.topLevelFields
                                    .filter { it.storageKind(context) != FieldStorageKind.THREAD_LOCAL }
                                    .filterNot { context.shouldBeInitializedEagerly(it) }
                                    .forEach { initGlobalField(it) }
                            ret(null)
                        }
                    }
                }
            }
        }

        scopeState.threadLocalInitFunction?.let { fileInitFunction ->
            generateFunction(codegen, fileInitFunction, fileInitFunction.location(start = true), fileInitFunction.location(start = false)) {
                using(FunctionScope(fileInitFunction, this)) {
                    val parameterScope = ParameterScope(fileInitFunction, functionGenerationContext)
                    using(parameterScope) usingParameterScope@{
                        using(VariableScope()) usingVariableScope@{
                            scopeState.topLevelFields
                                    .filter { it.storageKind(context) == FieldStorageKind.THREAD_LOCAL }
                                    .filterNot { context.shouldBeInitializedEagerly(it) }
                                    .forEach { initThreadLocalField(it) }
                            ret(null)
                        }
                    }
                }
            }
        }
    }

    private fun runAndProcessInitializers(konanLibrary: KotlinLibrary?, f: () -> Unit) {
        val oldScopeState = llvm.initializersGenerationState.reset(ScopeInitializersGenerationState())
        f()
        val scopeState = llvm.initializersGenerationState.reset(oldScopeState)
        scopeState.takeIf { !it.isEmpty() }?.let {
            buildInitializerFunctions(it)
            val initNode = createInitNode(createInitBody(it))
            llvm.irStaticInitializers.add(IrStaticInitializer(konanLibrary, createInitCtor(initNode)))
        }
    }

    //-------------------------------------------------------------------------//

    override fun visitElement(element: IrElement) {
        TODO(ir2string(element))
    }

    //-------------------------------------------------------------------------//
    override fun visitModuleFragment(declaration: IrModuleFragment) {
        context.log{"visitModule                    : ${ir2string(declaration)}"}

        generationState.coverage.collectRegions(declaration)

        initializeCachedBoxes(generationState)
        declaration.acceptChildrenVoid(this)

        runAndProcessInitializers(null) {
            // Note: it is here because it also generates some bitcode.
            generationState.objCExport.generate(codegen)

            codegen.objCDataGenerator?.finishModule()

            generationState.coverage.writeRegionInfo()
            overrideRuntimeGlobals()
            appendLlvmUsed("llvm.used", llvm.usedFunctions + llvm.usedGlobals)
            appendLlvmUsed("llvm.compiler.used", llvm.compilerUsedGlobals)
            if (context.config.produce.isNativeLibrary) {
                context.cAdapterExportedElements?.let { appendCAdapters(it) }
            }
        }

        appendStaticInitializers()
    }

    //-------------------------------------------------------------------------//

    val kVoidFuncType = functionType(llvm.voidType)
    val kNodeInitType = LLVMGetTypeByName(llvm.module, "struct.InitNode")!!
    val kMemoryStateType = LLVMGetTypeByName(llvm.module, "struct.MemoryState")!!
    val kInitFuncType = functionType(llvm.voidType, false, llvm.int32Type, pointerType(kMemoryStateType))

    //-------------------------------------------------------------------------//

    // Must be synchronized with Runtime.cpp
    val ALLOC_THREAD_LOCAL_GLOBALS = 0
    val INIT_GLOBALS = 1
    val INIT_THREAD_LOCAL_GLOBALS = 2
    val DEINIT_GLOBALS = 3

    val FILE_NOT_INITIALIZED = 0
    val FILE_INITIALIZED = 2

    private fun createInitBody(state: ScopeInitializersGenerationState): LLVMValueRef {
        val initFunction = addLlvmFunctionWithDefaultAttributes(
                context,
                llvm.module,
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
                        listOf(llvm.int32(INIT_GLOBALS) to bbInit,
                                llvm.int32(INIT_THREAD_LOCAL_GLOBALS) to bbLocalInit,
                                llvm.int32(ALLOC_THREAD_LOCAL_GLOBALS) to bbLocalAlloc,
                                llvm.int32(DEINIT_GLOBALS) to bbGlobalDeinit),
                        bbDefault)

                // Globals initializers may contain accesses to objects, so visit them first.
                appendingTo(bbInit) {
                    state.topLevelFields
                            .filter { context.shouldBeInitializedEagerly(it) }
                            .filterNot { it.storageKind(context) == FieldStorageKind.THREAD_LOCAL }
                            .forEach { initGlobalField(it) }
                    ret(null)
                }

                appendingTo(bbLocalInit) {
                    state.topLevelFields
                            .filter { context.shouldBeInitializedEagerly(it) }
                            .filter { it.storageKind(context) == FieldStorageKind.THREAD_LOCAL }
                            .forEach { initThreadLocalField(it) }
                    ret(null)
                }

                appendingTo(bbLocalAlloc) {
                    if (llvm.tlsCount > 0) {
                        val memory = LLVMGetParam(initFunction, 1)!!
                        call(llvm.addTLSRecord, listOf(memory, llvm.tlsKey, llvm.int32(llvm.tlsCount)))
                    }
                    ret(null)
                }

                appendingTo(bbGlobalDeinit) {
                    state.topLevelFields
                            // Only if a subject for memory management.
                            .forEach { irField ->
                                if (irField.type.binaryTypeIsReference() && irField.storageKind(context) != FieldStorageKind.THREAD_LOCAL) {
                                    val address = staticFieldPtr(irField, functionGenerationContext)
                                    storeHeapRef(codegen.kNullObjHeaderPtr, address)
                                }
                            }
                    state.globalSharedObjects.forEach { address ->
                        storeHeapRef(codegen.kNullObjHeaderPtr, address)
                    }
                    state.globalInitState?.let {
                        store(llvm.int32(FILE_NOT_INITIALIZED), it)
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
        return llvm.staticData.placeGlobal("init_node", constPointer(initNode), isExported = false).llvmGlobal
    }

    //-------------------------------------------------------------------------//

    private fun createInitCtor(initNodePtr: LLVMValueRef): LLVMValueRef {
        val ctorFunction = generateFunctionNoRuntime(codegen, kVoidFuncType, "") {
            call(llvm.appendToInitalizersTail, listOf(initNodePtr))
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

    private open inner class StackLocalsScope() : InnerScopeImpl() {
        override fun onEnter() {
            functionGenerationContext.stackLocalsManager.enterScope()
        }
        override fun onExit() {
            functionGenerationContext.stackLocalsManager.exitScope()
        }
    }

    private inner class LoopScope(val loop: IrLoop) : StackLocalsScope() {
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

        override fun genGetValue(value: IrValueDeclaration, resultSlot: LLVMValueRef?): LLVMValueRef {
            val index = functionGenerationContext.vars.indexOf(value)
            if (index < 0) {
                return super.genGetValue(value, resultSlot)
            } else {
                return functionGenerationContext.vars.load(index, resultSlot)
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
                parameters.forEach {
                    val parameter = it.key

                    if (context.shouldContainDebugInfo()) {
                        val local = functionGenerationContext.vars.createParameterOnStack(
                                parameter, debugInfoIfNeeded(function, parameter))
                        functionGenerationContext.mapParameterForDebug(local, it.value)
                    } else {
                        functionGenerationContext.vars.createParameter(parameter, it.value)
                    }
                }
            }
        }

        override fun genGetValue(value: IrValueDeclaration, resultSlot: LLVMValueRef?): LLVMValueRef {
            val index = functionGenerationContext.vars.indexOf(value)
            if (index < 0) {
                return super.genGetValue(value, resultSlot)
            } else {
                return functionGenerationContext.vars.load(index, resultSlot)
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
                generationState.coverage.tryGetInstrumentation(declaration) { function, args -> functionGenerationContext.call(function, args) }

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

        override fun getReturnSlot(target: IrSymbolOwner) : LLVMValueRef? {
            return if (declaration == null || target == declaration) {
                functionGenerationContext.returnSlot
            } else {
                super.getReturnSlot(target)
            }
        }

        override val exceptionHandler: ExceptionHandler
            get() = ExceptionHandler.Caller

        override fun functionScope(): CodeContext = this


        private val scope by lazy {
            if (!context.shouldContainLocationDebugInfo() || declaration == null)
                return@lazy null
            declaration.scope() ?: llvmFunction.scope(0, debugInfo.subroutineType(codegen.llvmTargetData, listOf(context.irBuiltIns.intType)), false)
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
            assert(irParameter.type.toLLVMType(llvm) == parameter.type)
            irParameter to parameter
        }.toMap()
    }

    private val IrDeclarationContainer.initVariableSuffix get() = when (this) {
        is IrFile -> "${fqName}\$${fileEntry.name}"
        else -> fqNameForIrSerialization.asString()
    }

    private fun getGlobalInitStateFor(container: IrDeclarationContainer): LLVMValueRef =
            llvm.initializersGenerationState.fileGlobalInitStates.getOrPut(container) {
                codegen.addGlobal("state_global$${container.initVariableSuffix}", llvm.int32Type, false).also {
                    LLVMSetInitializer(it, llvm.int32(FILE_NOT_INITIALIZED))
                    LLVMSetLinkage(it, LLVMLinkage.LLVMInternalLinkage)
                }
            }

    private fun getThreadLocalInitStateFor(container: IrDeclarationContainer): AddressAccess =
            llvm.initializersGenerationState.fileThreadLocalInitStates.getOrPut(container) {
                codegen.addKotlinThreadLocal("state_thread_local$${container.initVariableSuffix}", llvm.int32Type,
                        LLVMPreferredAlignmentOfType(llvm.runtime.targetData, llvm.int32Type)).also {
                    LLVMSetInitializer((it as GlobalAddressAccess).getAddress(null), llvm.int32(FILE_NOT_INITIALIZED))
                }
            }

    override fun visitFunction(declaration: IrFunction) {
        context.log{"visitFunction                  : ${ir2string(declaration)}"}

        val body = declaration.body

        val scopeState = llvm.initializersGenerationState.scopeState
        if (declaration.origin == DECLARATION_ORIGIN_STATIC_GLOBAL_INITIALIZER) {
            require(scopeState.globalInitFunction == null) { "There can only be at most one global file initializer" }
            require(body == null) { "The body of file initializer should be null" }
            require(declaration.valueParameters.isEmpty()) { "File initializer must be parameterless" }
            require(declaration.returnsUnit()) { "File initializer must return Unit" }
            scopeState.globalInitFunction = declaration
            scopeState.globalInitState = getGlobalInitStateFor(declaration.parent as IrDeclarationContainer)
        }
        if (declaration.origin == DECLARATION_ORIGIN_STATIC_THREAD_LOCAL_INITIALIZER
                || declaration.origin == DECLARATION_ORIGIN_STATIC_STANDALONE_THREAD_LOCAL_INITIALIZER) {
            require(scopeState.threadLocalInitFunction == null) { "There can only be at most one thread local file initializer" }
            require(body == null) { "The body of file initializer should be null" }
            require(declaration.valueParameters.isEmpty()) { "File initializer must be parameterless" }
            require(declaration.returnsUnit()) { "File initializer must return Unit" }
            scopeState.threadLocalInitFunction = declaration
            scopeState.threadLocalInitState = getThreadLocalInitStateFor(declaration.parent as IrDeclarationContainer)
        }


        if ((declaration as? IrSimpleFunction)?.modality == Modality.ABSTRACT
                || declaration.isExternal
                || body == null)
            return
        val file = if (declaration.origin != IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA)
            null
        else ((declaration as? IrSimpleFunction)?.attributeOwnerId as? IrSimpleFunction)?.let { context.irLinker.getFileOf(it) }?.takeIf {
            (currentCodeContext.fileScope() as FileScope).file != it
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
                                        listOf(llvm.staticData.kotlinStringLiteral(
                                                "unsupported call of reified inlined function `${declaration.fqNameForIrSerialization}`").llvm),
                                        Lifetime.IRRELEVANT, null)
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
            llvm.usedFunctions.add(codegen.llvmFunction(declaration).llvmValue)
        }

        if (context.shouldVerifyBitCode())
            verifyModule(llvm.module, "${declaration.descriptor.containingDeclaration}::${ir2string(declaration)}")
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
            runAndProcessInitializers(declaration.konanLibrary) {
                declaration.declarations.forEach {
                    it.acceptVoid(this)
                }
            }
        }
    }

    override fun visitProperty(declaration: IrProperty) {
        declaration.getter?.acceptVoid(this)
        declaration.setter?.acceptVoid(this)
        declaration.backingField?.acceptVoid(this)
    }

    private fun needGlobalInit(field: IrField): Boolean {
        if (field.descriptor.containingDeclaration !is PackageFragmentDescriptor) return field.isStatic
        // TODO: add some smartness here. Maybe if package of the field is in never accessed
        // assume its global init can be actually omitted.
        return true
    }

    override fun visitField(declaration: IrField) {
        context.log{"visitField                     : ${ir2string(declaration)}"}
        debugFieldDeclaration(declaration)
        if (needGlobalInit(declaration)) {
            val type = declaration.type.toLLVMType(llvm)
            val globalPropertyAccess = generationState.llvmDeclarations.forStaticField(declaration).storageAddressAccess
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
            llvm.initializersGenerationState.scopeState.topLevelFields.add(declaration)
        }
    }

    private fun recordCoverage(irElement: IrElement) {
        val scope = currentCodeContext.functionScope()
        if (scope is FunctionScope) {
            scope.coverageInstrumentation?.instrumentIrElement(irElement)
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateExpression(value: IrExpression, resultSlot: LLVMValueRef? = null): LLVMValueRef {
        updateBuilderDebugLocation(value)
        recordCoverage(value)
        when (value) {
            is IrTypeOperatorCall    -> return evaluateTypeOperator           (value, resultSlot)
            is IrCall                -> return evaluateCall                   (value, resultSlot)
            is IrDelegatingConstructorCall ->
                                        return evaluateCall                   (value, resultSlot)
            is IrConstructorCall     -> return evaluateCall                   (value, resultSlot)
            is IrInstanceInitializerCall ->
                                        return evaluateInstanceInitializerCall(value)
            is IrGetValue            -> return evaluateGetValue               (value, resultSlot)
            is IrSetValue            -> return evaluateSetValue               (value)
            is IrGetField            -> return evaluateGetField               (value, resultSlot)
            is IrSetField            -> return evaluateSetField               (value)
            is IrConst<*>            -> return evaluateConst                  (value).llvm
            is IrReturn              -> return evaluateReturn                 (value)
            is IrWhen                -> return evaluateWhen                   (value, resultSlot)
            is IrThrow               -> return evaluateThrow                  (value)
            is IrTry                 -> return evaluateTry                    (value)
            is IrReturnableBlock     -> return evaluateReturnableBlock        (value, resultSlot)
            is IrContainerExpression -> return evaluateContainerExpression    (value, resultSlot)
            is IrWhileLoop           -> return evaluateWhileLoop              (value)
            is IrDoWhileLoop         -> return evaluateDoWhileLoop            (value)
            is IrVararg              -> return evaluateVararg                 (value)
            is IrBreak               -> return evaluateBreak                  (value)
            is IrContinue            -> return evaluateContinue               (value)
            is IrGetObjectValue      -> return evaluateGetObjectValue         (value)
            is IrFunctionReference   -> return evaluateFunctionReference      (value)
            is IrSuspendableExpression ->
                                        return evaluateSuspendableExpression  (value, resultSlot)
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

    private fun evaluateGetObjectValue(value: IrGetObjectValue): LLVMValueRef {
        error("Should be lowered out: ${value.symbol.owner.render()}")
    }


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
                functionGenerationContext.phi(type.toLLVMType(llvm))
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
                        currentCodeContext.genDeclareVariable(catch.catchParameter, exception)
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
    private inner class WhenEmittingContext(val expression: IrWhen, val lastBBOfWhenCases: LLVMBasicBlockRef) {
        val needsPhi = expression.branches.last().isUnconditional() && !expression.type.isUnit()
        val llvmType = expression.type.toLLVMType(llvm)

        val bbExit = lazy {
            // bbExit must be positioned after all blocks of WHEN construct
            functionGenerationContext.appendingTo(lastBBOfWhenCases) {
                functionGenerationContext.basicBlock("when_exit", expression.endLocation)
            }
        }
        val resultPhi = lazy {
            functionGenerationContext.appendingTo(bbExit.value) {
                functionGenerationContext.phi(llvmType)
            }
        }
    }

    /** For WHEN { COND1 -> CASE1, COND2 -> CASE2, ELSE -> UNCONDITIONAL }
     * the following sequence of basic blocks is generated:
     * -- if COND1
     * -- CASE1
     * -- NEXT1(if COND2)
     * -- CASE2
     * -- NEXT2 (UNCONDITIONAL)
     * -- EXIT
     */
    private fun evaluateWhen(expression: IrWhen, resultSlot: LLVMValueRef?): LLVMValueRef {
        context.log{"evaluateWhen                   : ${ir2string(expression)}"}

        generateDebugTrambolineIf("when", expression)

        // First, generate all empty basic blocks for conditions and variants
        val bbOfFirstConditionCheck = functionGenerationContext.currentBlock
        val branchInfos: List<BranchCaseNextInfo> = expression.branches.map {
            // Carefully create empty basic blocks and position them one after another
            val bbCase = if (it.isUnconditional()) null else
                functionGenerationContext.basicBlock("when_case", it.startLocation, it.endLocation).apply { functionGenerationContext.positionAtEnd(this) }
            val bbNext = if (it.isUnconditional() || it == expression.branches.last()) null else
                functionGenerationContext.basicBlock("when_next", it.startLocation, it.endLocation).apply { functionGenerationContext.positionAtEnd(this) }
            BranchCaseNextInfo(it, bbCase, bbNext, resultSlot)
        }
        // Now, exit basic block can be positioned after all blocks of WHEN expression
        val whenEmittingContext = WhenEmittingContext(expression, lastBBOfWhenCases = functionGenerationContext.currentBlock)
        functionGenerationContext.positionAtEnd(bbOfFirstConditionCheck)

        branchInfos.forEach { generateWhenCase(whenEmittingContext, it) }

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

    private data class BranchCaseNextInfo(val branch: IrBranch, val bbCase: LLVMBasicBlockRef?, val bbNext: LLVMBasicBlockRef?,
                                          val resultSlot: LLVMValueRef?)

    private fun generateWhenCase(whenEmittingContext: WhenEmittingContext, branchCaseNextInfo: BranchCaseNextInfo) {
        with(branchCaseNextInfo) {
            if (!branch.isUnconditional()) {
                val condition = evaluateExpression(branch.condition)
                functionGenerationContext.condBr(condition, bbCase, bbNext ?: whenEmittingContext.bbExit.value)
                functionGenerationContext.positionAtEnd(bbCase!!)
            }
            val brResult = evaluateExpression(branch.result, resultSlot)
            if (!functionGenerationContext.isAfterTerminator()) {
                if (whenEmittingContext.needsPhi)
                    functionGenerationContext.assignPhis(whenEmittingContext.resultPhi.value to brResult)
                functionGenerationContext.br(whenEmittingContext.bbExit.value)
            }
            if (bbNext != null)
                functionGenerationContext.positionAtEnd(bbNext)
        }
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
                call(llvm.Kotlin_mm_safePointWhileLoopBody, emptyList())
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
                call(llvm.Kotlin_mm_safePointWhileLoopBody, emptyList())
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

    private fun evaluateGetValue(value: IrGetValue, resultSlot: LLVMValueRef?): LLVMValueRef {
        context.log{"evaluateGetValue               : ${ir2string(value)}"}
        return currentCodeContext.genGetValue(value.symbol.owner, resultSlot)
    }

    //-------------------------------------------------------------------------//

    private fun evaluateSetValue(value: IrSetValue): LLVMValueRef {
        context.log{"evaluateSetValue               : ${ir2string(value)}"}
        /*
         * Probably, here returnSlot optimization can be done, for not creating extra slot and reuse slot for a variable.
         * On the other side, eliminating extra slot is not so profitable, as eliminating all slots in a function,
         * while removing this slot is dangerous, as it needs to be accurate with setting variable inside expression.
         * So optimization was not implemented here for now.
         */
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
                    builder       = debugInfo.builder,
                    functionScope = locationInfo.scope,
                    diType        = with(debugInfo) { element.type.diType(codegen.llvmTargetData) },
                    name          = element.debugNameConversion(),
                    file          = file,
                    line          = locationInfo.line,
                    location      = location)
                    else null
            is IrValueParameter -> debugInfoParameterLocation(
                    builder       = debugInfo.builder,
                    functionScope = locationInfo.scope,
                    diType        = with(debugInfo) { element.type.diType(codegen.llvmTargetData) },
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
        this.currentCodeContext.genDeclareVariable(variable, value)
    }

    private fun CodeContext.genDeclareVariable(
            variable: IrVariable,
            value: LLVMValueRef?
    ) = genDeclareVariable(
            variable, value, debugInfoIfNeeded(
            (functionScope() as FunctionScope).declaration, variable))

    //-------------------------------------------------------------------------//

    private fun evaluateTypeOperator(value: IrTypeOperatorCall, resultSlot: LLVMValueRef?): LLVMValueRef {
        return when (value.operator) {
            IrTypeOperator.CAST                      -> evaluateCast(value, resultSlot)
            IrTypeOperator.IMPLICIT_INTEGER_COERCION -> evaluateIntegerCoercion(value)
            IrTypeOperator.IMPLICIT_CAST             -> evaluateExpression(value.argument, resultSlot)
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

    private fun IrType.isUnsignedInteger(): Boolean = !isNullable() &&
                    UnsignedType.values().any { it.classId == this.getClass()?.descriptor?.classId }

    private fun evaluateIntegerCoercion(value: IrTypeOperatorCall): LLVMValueRef {
        context.log{"evaluateIntegerCoercion        : ${ir2string(value)}"}
        val type = value.typeOperand
        assert(type.isPrimitiveInteger() || type.isUnsignedInteger())
        val result = evaluateExpression(value.argument)
        assert(value.argument.type.isInt())
        val llvmSrcType = value.argument.type.toLLVMType(llvm)
        val llvmDstType = type.toLLVMType(llvm)
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

    private fun evaluateCast(value: IrTypeOperatorCall, resultSlot: LLVMValueRef?): LLVMValueRef {
        context.log{"evaluateCast                   : ${ir2string(value)}"}
        val dstClass = value.typeOperand.getClass()
                ?: error("No class for ${value.typeOperand.render()} from \n${functionGenerationContext.irFunction?.render()}")

        val srcArg = evaluateExpression(value.argument, resultSlot)
        assert(srcArg.type == codegen.kObjHeaderPtr)

        with(functionGenerationContext) {
            ifThen(not(genInstanceOf(srcArg, dstClass))) {
                if (dstClass.defaultType.isObjCObjectType()) {
                    val dstFullClassName = dstClass.fqNameWhenAvailable?.toString() ?: dstClass.name.toString()
                    callDirect(
                            context.ir.symbols.throwTypeCastException.owner,
                            listOf(srcArg, llvm.staticData.kotlinStringLiteral(dstFullClassName).llvm),
                            Lifetime.GLOBAL,
                            null
                    )
                } else {
                    val dstTypeInfo = functionGenerationContext.bitcast(llvm.int8PtrType, codegen.typeInfoValue(dstClass))
                    callDirect(
                            context.ir.symbols.throwClassCastException.owner,
                            listOf(srcArg, dstTypeInfo),
                            Lifetime.GLOBAL,
                            null
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
        val resultNull = if (type.isNullable()) kTrue else kFalse
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
        val result = functionGenerationContext.phi(llvm.int1Type)
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
            call(llvm.isInstanceFunction, listOf(srcObjInfoPtr, codegen.typeInfoValue(dstClass)))
        } else {
            val dstHierarchyInfo = context.getLayoutBuilder(dstClass).hierarchyInfo
            if (!dstClass.isInterface) {
                call(llvm.isInstanceOfClassFastFunction,
                        listOf(srcObjInfoPtr, llvm.int32(dstHierarchyInfo.classIdLo), llvm.int32(dstHierarchyInfo.classIdHi)))
            } else {
                // Essentially: typeInfo.itable[place(interfaceId)].id == interfaceId
                val interfaceId = dstHierarchyInfo.interfaceId
                val typeInfo = functionGenerationContext.loadTypeInfo(srcObjInfoPtr)
                with(functionGenerationContext) {
                    val interfaceTableRecord = lookupInterfaceTableRecord(typeInfo, interfaceId)
                    icmpEq(load(structGep(interfaceTableRecord, 0 /* id */)), llvm.int32(interfaceId))
                }
            }
        }
    }

    private fun genInstanceOfObjC(obj: LLVMValueRef, dstClass: IrClass): LLVMValueRef {
        val objCObject = callDirect(
                context.ir.symbols.interopObjCObjectRawValueGetter.owner,
                listOf(obj),
                Lifetime.IRRELEVANT,
                null
        )

        return if (dstClass.isObjCClass()) {
            if (dstClass.isInterface) {
                val isMeta = if (dstClass.isObjCMetaClass()) kTrue else kFalse
                call(
                        llvm.Kotlin_Interop_DoesObjectConformToProtocol,
                        listOf(
                                objCObject,
                                genGetObjCProtocol(dstClass),
                                isMeta
                        )
                )
            } else {
                call(
                        llvm.Kotlin_Interop_IsObjectKindOfClass,
                        listOf(objCObject, genGetObjCClass(dstClass))
                )
            }.let {
                functionGenerationContext.icmpNe(it, kFalse)
            }


        } else {
            // e.g. ObjCObject, ObjCObjectBase etc.
            if (dstClass.isObjCMetaClass()) {
                val isClass = llvm.externalNativeRuntimeFunction(
                        "object_isClass",
                        LlvmRetType(llvm.int8Type),
                        listOf(LlvmParamType(llvm.int8PtrType))
                )
                call(isClass, listOf(objCObject)).let {
                    functionGenerationContext.icmpNe(it, llvm.int8(0))
                }
            } else if (dstClass.isObjCProtocolClass()) {
                // Note: it is not clear whether this class should be looked up this way.
                // clang does the same, however swiftc uses dynamic lookup.
                val protocolClass = functionGenerationContext.getObjCClassFromNativeRuntime("Protocol")
                call(
                        llvm.Kotlin_Interop_IsObjectKindOfClass,
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

    private fun evaluateGetField(value: IrGetField, resultSlot: LLVMValueRef?): LLVMValueRef {
        context.log { "evaluateGetField               : ${ir2string(value)}" }
        val alignment : Int
        val order = when {
            value.symbol.owner.hasAnnotation(KonanFqNames.volatile) ->
                LLVMAtomicOrdering.LLVMAtomicOrderingSequentiallyConsistent
            else -> null
        }
        val fieldAddress: LLVMValueRef

        when {
            !value.symbol.owner.isStatic -> {
                fieldAddress = fieldPtrOfClass(evaluateExpression(value.receiver!!), value.symbol.owner)
                alignment = generationState.llvmDeclarations.forField(value.symbol.owner).alignment
            }
            value.symbol.owner.correspondingPropertySymbol?.owner?.isConst == true -> {
                // TODO: probably can be removed, as they are inlined.
                return evaluateConst(value.symbol.owner.initializer?.expression as IrConst<*>).llvm
            }
            else -> {
                if (context.config.threadsAreAllowed && value.symbol.owner.isGlobalNonPrimitive(context)) {
                    functionGenerationContext.checkGlobalsAccessible(currentCodeContext.exceptionHandler)
                }
                fieldAddress = staticFieldPtr(value.symbol.owner, functionGenerationContext)
                alignment = generationState.llvmDeclarations.forStaticField(value.symbol.owner).alignment
            }
        }
        return functionGenerationContext.loadSlot(
                fieldAddress, !value.symbol.owner.isFinal, resultSlot,
                memoryOrder = order,
                alignment = alignment
        )
    }

    //-------------------------------------------------------------------------//
    private fun needMutationCheck(irField: IrField): Boolean {
        // For now we omit mutation checks on immutable types, as this allows initialization in constructor
        // and it is assumed that API doesn't allow to change them.
        return context.config.freezing.enableFreezeChecks && !irField.parentAsClass.isFrozen(context) && !irField.hasAnnotation(KonanFqNames.volatile)
    }

    private fun needLifetimeConstraintsCheck(valueToAssign: LLVMValueRef, irClass: IrClass): Boolean {
        // TODO: Likely, we don't need isFrozen check here at all.
        return functionGenerationContext.isObjectType(valueToAssign.type) && !irClass.isFrozen(context)
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

        val thisPtr = value.receiver?.let { evaluateExpression(it) }
        val valueToAssign = evaluateExpression(value.value)
        val address: LLVMValueRef
        val alignment: Int
        if (thisPtr != null) {
            require(!value.symbol.owner.isStatic) { "Unexpected receiver for a static field: ${value.render()}" }
            require(thisPtr.type == codegen.kObjHeaderPtr) {
                LLVMPrintTypeToString(thisPtr.type)?.toKString().toString()
            }
            val parentAsClass = value.symbol.owner.parentAsClass
            if (needMutationCheck(value.symbol.owner)) {
                functionGenerationContext.call(llvm.mutationCheck,
                        listOf(functionGenerationContext.bitcast(codegen.kObjHeaderPtr, thisPtr)),
                        Lifetime.IRRELEVANT, currentCodeContext.exceptionHandler)
            }
            if (needLifetimeConstraintsCheck(valueToAssign, parentAsClass)) {
                functionGenerationContext.call(llvm.checkLifetimesConstraint, listOf(thisPtr, valueToAssign))
            }
            address = fieldPtrOfClass(thisPtr, value.symbol.owner)
            alignment = generationState.llvmDeclarations.forField(value.symbol.owner).alignment
        } else {
            require(value.symbol.owner.isStatic) { "A receiver expected for a non-static field: ${value.render()}" }
            if (context.config.threadsAreAllowed && value.symbol.owner.storageKind(context) == FieldStorageKind.GLOBAL)
                functionGenerationContext.checkGlobalsAccessible(currentCodeContext.exceptionHandler)
            if (value.symbol.owner.shouldBeFrozen(context) && value.origin != ObjectClassLowering.IrStatementOriginFieldPreInit)
                functionGenerationContext.freeze(valueToAssign, currentCodeContext.exceptionHandler)
            address = staticFieldPtr(value.symbol.owner, functionGenerationContext)
            alignment = generationState.llvmDeclarations.forStaticField(value.symbol.owner).alignment
        }
        functionGenerationContext.storeAny(
                valueToAssign, address, false,
                isVolatile = value.symbol.owner.hasAnnotation(KonanFqNames.volatile),
                alignment = alignment,
        )

        assert (value.type.isUnit())
        return codegen.theUnitInstanceRef.llvm
    }

    //-------------------------------------------------------------------------//
    private fun fieldPtrOfClass(thisPtr: LLVMValueRef, value: IrField): LLVMValueRef {
        val fieldInfo = generationState.llvmDeclarations.forField(value)

        val typePtr = pointerType(fieldInfo.classBodyType)

        val typedBodyPtr = functionGenerationContext.bitcast(typePtr, thisPtr)
        val fieldPtr = LLVMBuildStructGEP(functionGenerationContext.builder, typedBodyPtr, fieldInfo.index, "")
        return fieldPtr!!
    }

    private fun staticFieldPtr(value: IrField, context: FunctionGenerationContext) =
            generationState.llvmDeclarations
                    .forStaticField(value.symbol.owner)
                    .storageAddressAccess
                    .getAddress(context)

    //-------------------------------------------------------------------------//
    private fun evaluateStringConst(value: IrConst<String>) =
            llvm.staticData.kotlinStringLiteral(value.value)

    private fun evaluateConst(value: IrConst<*>): ConstValue {
        context.log{"evaluateConst                  : ${ir2string(value)}"}
        /* This suppression against IrConst<String> */
        @Suppress("UNCHECKED_CAST")
        return when (value.kind) {
            IrConstKind.Null -> constPointer(codegen.kNullObjHeaderPtr)
            IrConstKind.Boolean -> llvm.constInt1(value.value as Boolean)
            IrConstKind.Char -> llvm.constChar16(value.value as Char)
            IrConstKind.Byte -> llvm.constInt8(value.value as Byte)
            IrConstKind.Short -> llvm.constInt16(value.value as Short)
            IrConstKind.Int -> llvm.constInt32(value.value as Int)
            IrConstKind.Long -> llvm.constInt64(value.value as Long)
            IrConstKind.String -> evaluateStringConst(value as IrConst<String>)
            IrConstKind.Float -> llvm.constFloat32(value.value as Float)
            IrConstKind.Double -> llvm.constFloat64(value.value as Double)
        }
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
                if (context.getTypeConversion(constructedType, value.type) != null) {
                    if (value.value.kind == IrConstKind.Null) {
                        Zero(value.type.toLLVMType(llvm))
                    } else {
                        require(value.type.toLLVMType(llvm) == codegen.kObjHeaderPtr) {
                            "Can't wrap ${value.value.kind.asString} constant to type ${value.type.render()}"
                        }
                        value.toBoxCacheValue(generationState) ?: llvm.staticData.createConstKotlinObject(
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
                llvm.staticData.createConstKotlinArray(
                        value.type.getClass()!!,
                        value.elements.map { evaluateConstantValue(it) }
                )
            }
            is IrConstantObject -> {
                val constructedType = value.constructor.owner.constructedClassType
                val constructedClass = constructedType.getClass()!!
                val needUnBoxing = constructedType.getInlinedClassNative() != null &&
                        context.getTypeConversion(constructedType, value.type) == null
                if (needUnBoxing) {
                    val unboxed = value.valueArguments.singleOrNull()
                            ?: error("Inlined class should have exactly one constructor argument")
                    return evaluateConstantValue(unboxed)
                }
                val fields = if (value.constructor.owner.isConstantConstructorIntrinsic) {
                    intrinsicGenerator.evaluateConstantConstructorFields(value, value.valueArguments.map { evaluateConstantValue(it) })
                } else {
                    val fields = context.getLayoutBuilder(constructedClass).getFields(llvm)
                    val valueParameters = value.constructor.owner.valueParameters.associateBy { it.name.toString() }
                    fields.map { field ->
                        if (field.isConst) {
                            val init = field.irField!!.initializer?.expression
                            require(field.name !in valueParameters) {
                                "Constant field ${field.name} of class ${constructedClass.name} shouldn't be a constructor parameter"
                            }
                            when (init) {
                                is IrConst<*> -> evaluateConst(init)
                                is IrConstantValue -> evaluateConstantValue(init)
                                null -> error("Constant field ${field.name} of class ${constructedClass.name} should have initializer")
                                else -> error("Unexpected constant initializer type: ${init::class}")
                            }
                        } else {
                            val index = valueParameters[field.name]?.index
                                    ?: error("Bad statically initialized object: field ${field.name} value not set in ${constructedClass.name}")
                            evaluateConstantValue(value.valueArguments[index])
                        }
                    }.also {
                        require(it.size == value.valueArguments.size + fields.count { it.isConst }) {
                            "Bad statically initialized object of class ${constructedClass.name}: too many fields"
                        }
                    }
                }

                require(value.type.toLLVMType(llvm) == codegen.kObjHeaderPtr) { "Constant object is not an object, but ${value.type.render()}" }
                llvm.staticData.createConstKotlinObject(
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
        val target = expression.returnTargetSymbol.owner

        val evaluated = evaluateExpression(value, currentCodeContext.getReturnSlot(target))
        currentCodeContext.genReturn(target, evaluated)
        return codegen.kNothingFakeValue
    }

    //-------------------------------------------------------------------------//
    private inner class ReturnableBlockScope(val returnableBlock: IrReturnableBlock, val resultSlot: LLVMValueRef?) :
            FileScope(returnableBlock.inlineFunctionSymbol?.owner?.let {
                generationState.loweredInlineFunctions[it]?.irFile ?: it.fileOrNull
            }
                    ?: (currentCodeContext.fileScope() as? FileScope)?.file
                    ?: error("returnable block should belong to current file at least")) {

        var bbExit : LLVMBasicBlockRef? = null
        var resultPhi : LLVMValueRef? = null
        private val functionScope by lazy {
            returnableBlock.inlineFunctionSymbol?.owner?.let {
                it.scope(file().fileEntry.line(generationState.loweredInlineFunctions[it]?.startOffset ?: it.startOffset))
            }
        }

        private fun getExit(): LLVMBasicBlockRef {
            val location = returnableBlock.inlineFunctionSymbol?.owner?.let {
                location(generationState.loweredInlineFunctions[it]?.endOffset ?: it.endOffset)
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
                resultPhi = functionGenerationContext.phi(returnableBlock.type.toLLVMType(llvm))
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

        override fun getReturnSlot(target: IrSymbolOwner) : LLVMValueRef? {
            return if (target == returnableBlock) {
                resultSlot
            } else {
                super.getReturnSlot(target)
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
            val lexicalBlockFile = DICreateLexicalBlockFile(debugInfo.builder, functionScope()!!.scope(), super.file.file())
            DICreateLexicalBlock(debugInfo.builder, lexicalBlockFile, super.file.file(), returnableBlock.startLine(), returnableBlock.startColumn())!!
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
            debugInfo.objHeaderPointerType
        else null
        override fun classScope(): CodeContext? = this
    }

    //-------------------------------------------------------------------------//
    private fun evaluateReturnableBlock(value: IrReturnableBlock, resultSlot: LLVMValueRef?): LLVMValueRef {
        context.log{"evaluateReturnableBlock         : ${value.statements.forEach { ir2string(it) }}"}

        val returnableBlockScope = ReturnableBlockScope(value, resultSlot)
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
            LLVMGetUndef(value.type.toLLVMType(llvm))!!
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateContainerExpression(value: IrContainerExpression, resultSlot: LLVMValueRef?): LLVMValueRef {
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
                    return evaluateExpression(it, resultSlot)
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
    private fun evaluateCall(value: IrFunctionAccessExpression, resultSlot: LLVMValueRef?): LLVMValueRef {
        context.log{"evaluateCall                   : ${ir2string(value)}"}

        intrinsicGenerator.tryEvaluateSpecialCall(value, resultSlot)?.let { return it }

        val args = evaluateExplicitArgs(value)

        updateBuilderDebugLocation(value)
        return when (value) {
            is IrDelegatingConstructorCall -> delegatingConstructorCall(value.symbol.owner, args)
            is IrConstructorCall -> evaluateConstructorCall(value, args, resultSlot)
            else -> evaluateFunctionCall(value as IrCall, args, resultLifetime(value), resultSlot)
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
        get() = if (!context.shouldContainLocationDebugInfo()) null
            else currentCodeContext.location(startOffset)

    private val IrElement.endLocation: LocationInfo?
        get() = if (!context.shouldContainLocationDebugInfo()) null
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
        with(debugInfo) {
            val irFile = (currentCodeContext.fileScope() as FileScope).file
            val sizeInBits = expression.type.size
            scope.offsetInBits += sizeInBits
            val alignInBits = expression.type.alignment
            scope.offsetInBits = alignTo(scope.offsetInBits, alignInBits)
            @Suppress("UNCHECKED_CAST")
            scope.members.add(DICreateMemberType(
                    refBuilder = builder,
                    refScope = scope.scope as DIScopeOpaqueRef,
                    name = expression.computeSymbolName(),
                    file = irFile.file(),
                    lineNum = expression.startLine(),
                    sizeInBits = sizeInBits,
                    alignInBits = alignInBits,
                    offsetInBits = scope.offsetInBits,
                    flags = 0,
                    type = expression.type.diType(codegen.llvmTargetData)
            )!!)
        }
    }


    //-------------------------------------------------------------------------//
    private fun IrFile.file(): DIFileRef {
        return debugInfo.files.getOrPut(this.fileEntry.name) {
            val path = this.fileEntry.name.toFileAndFolder(context.config)
            DICreateFile(debugInfo.builder, path.file, path.folder)!!
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

        val functionLlvmValue = when {
            isReifiedInline -> null
            // TODO: May be tie up inline lambdas to their outer function?
            codegen.isExternal(this) && !KonanBinaryInterface.isExported(this) -> null
            this is IrSimpleFunction && isSuspend -> this.getOrCreateFunctionWithContinuationStub(context).let { codegen.llvmFunctionOrNull(it)?.llvmValue }
            else -> codegen.llvmFunctionOrNull(this)?.llvmValue
        }
        return with(debugInfo) {
            val f = this@scope
            val nodebug = f is IrConstructor && f.parentAsClass.isSubclassOf(context.irBuiltIns.throwableClass.owner)
            if (functionLlvmValue != null) {
                subprograms.getOrPut(functionLlvmValue) {
                    memScoped {
                        val subroutineType = subroutineType(codegen.llvmTargetData)
                        diFunctionScope(name.asString(), functionLlvmValue.name!!, startLine, subroutineType, nodebug).also {
                            if (!this@scope.isInline)
                                DIFunctionAddSubprogram(functionLlvmValue, it)
                        }
                    }
                } as DIScopeOpaqueRef
            } else {
                inlinedSubprograms.getOrPut(this@scope) {
                    memScoped {
                        val subroutineType = subroutineType(codegen.llvmTargetData)
                        diFunctionScope(name.asString(), "<inlined-out:$name>", startLine, subroutineType, nodebug)
                    }
                } as DIScopeOpaqueRef
            }
        }

    }

    @Suppress("UNCHECKED_CAST")
    private fun LLVMValueRef.scope(startLine:Int, subroutineType: DISubroutineTypeRef, nodebug: Boolean): DIScopeOpaqueRef? {
        return debugInfo.subprograms.getOrPut(this) {
            diFunctionScope(name!!, name!!, startLine, subroutineType, nodebug).also {
                DIFunctionAddSubprogram(this@scope, it)
            }
        }  as DIScopeOpaqueRef
    }

    @Suppress("UNCHECKED_CAST")
    private fun diFunctionScope(name: String, linkageName: String, startLine: Int, subroutineType: DISubroutineTypeRef, nodebug: Boolean) = DICreateFunction(
                builder = debugInfo.builder,
                scope = debugInfo.compilationUnit,
                name = (if (nodebug) "<NODEBUG>" else "") + name,
                linkageName = linkageName,
                file = file().file(),
                lineNo = startLine,
                type = subroutineType,
                //TODO: need more investigations.
                isLocal = 0,
                isDefinition = 1,
                scopeLine = 0)!!

    //-------------------------------------------------------------------------//


    private fun IrFunction.returnsUnit() = returnType.isUnit().also {
        require(!isSuspend) { "Suspend functions should be lowered out at this point"}
    }

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

    private fun evaluateSuspendableExpression(expression: IrSuspendableExpression, resultSlot: LLVMValueRef?): LLVMValueRef {
        val suspensionPointId = evaluateExpression(expression.suspensionPointId)
        val bbStart = functionGenerationContext.basicBlock("start", expression.result.startLocation)
        val bbDispatch = functionGenerationContext.basicBlock("dispatch", expression.suspensionPointId.startLocation)

        val resumePoints = mutableListOf<LLVMBasicBlockRef>()
        using (SuspendableExpressionScope(resumePoints)) {
            functionGenerationContext.condBr(functionGenerationContext.icmpEq(suspensionPointId, llvm.kNullInt8Ptr), bbStart, bbDispatch)

            functionGenerationContext.positionAtEnd(bbStart)
            val result = evaluateExpression(expression.result, resultSlot)

            functionGenerationContext.appendingTo(bbDispatch) {
                if (context.config.indirectBranchesAreAllowed)
                    functionGenerationContext.indirectBr(suspensionPointId, resumePoints)
                else {
                    val bbElse = functionGenerationContext.basicBlock("else", null) {
                        functionGenerationContext.unreachable()
                    }

                    val cases = resumePoints.withIndex().map { llvm.int32(it.index + 1) to it.value }
                    functionGenerationContext.switch(functionGenerationContext.ptrToInt(suspensionPointId, llvm.int32Type), cases, bbElse)
                }
            }
            return result
        }
    }

    private inner class SuspensionPointScope(val suspensionPointId: IrVariable,
                                             val bbResume: LLVMBasicBlockRef,
                                             val bbResumeId: Int): InnerScopeImpl() {
        override fun genGetValue(value: IrValueDeclaration, resultSlot: LLVMValueRef?): LLVMValueRef {
            if (value == suspensionPointId) {
                return if (context.config.indirectBranchesAreAllowed)
                           functionGenerationContext.blockAddress(bbResume)
                       else
                           functionGenerationContext.intToPtr(llvm.int32(bbResumeId + 1), llvm.int8PtrType)
            }
            return super.genGetValue(value, resultSlot)
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
        return functionGenerationContext.bitcast(llvm.int8PtrType, typeInfoPtr)
    }

    //-------------------------------------------------------------------------//

    private fun evaluateFunctionCall(callee: IrCall, args: List<LLVMValueRef>,
                                     resultLifetime: Lifetime, resultSlot: LLVMValueRef?): LLVMValueRef {
        val function = callee.symbol.owner
        require(!function.isSuspend) { "Suspend functions should be lowered out at this point"}

        return when {
            function.isTypedIntrinsic -> intrinsicGenerator.evaluateCall(callee, args, resultSlot)
            function.isBuiltInOperator -> evaluateOperatorCall(callee, args)
            function.origin == DECLARATION_ORIGIN_STATIC_GLOBAL_INITIALIZER -> evaluateFileGlobalInitializerCall(function)
            function.origin == DECLARATION_ORIGIN_STATIC_THREAD_LOCAL_INITIALIZER -> evaluateFileThreadLocalInitializerCall(function)
            function.origin == DECLARATION_ORIGIN_STATIC_STANDALONE_THREAD_LOCAL_INITIALIZER -> evaluateFileStandaloneThreadLocalInitializerCall(function)
            else -> evaluateSimpleFunctionCall(function, args, resultLifetime, callee.superQualifierSymbol?.owner, resultSlot)
        }
    }

    private fun evaluateFileGlobalInitializerCall(fileInitializer: IrFunction) = with(functionGenerationContext) {
        val statePtr = getGlobalInitStateFor(fileInitializer.parent as IrDeclarationContainer)
        val initializerPtr = with(codegen) { fileInitializer.llvmFunction.llvmValue }

        val bbInit = basicBlock("label_init", null)
        val bbExit = basicBlock("label_continue", null)
        moveBlockAfterEntry(bbExit)
        moveBlockAfterEntry(bbInit)
        val state = load(statePtr, memoryOrder = LLVMAtomicOrdering.LLVMAtomicOrderingAcquire)
        condBr(icmpEq(state, llvm.int32(FILE_INITIALIZED)), bbExit, bbInit)
        positionAtEnd(bbInit)
        call(llvm.callInitGlobalPossiblyLock, listOf(statePtr, initializerPtr),
                exceptionHandler = currentCodeContext.exceptionHandler)
        br(bbExit)
        positionAtEnd(bbExit)
        codegen.theUnitInstanceRef.llvm
    }

    private fun evaluateFileThreadLocalInitializerCall(fileInitializer: IrFunction) = with(functionGenerationContext) {
        val globalStatePtr = getGlobalInitStateFor(fileInitializer.parent as IrDeclarationContainer)
        val localState = getThreadLocalInitStateFor(fileInitializer.parent as IrDeclarationContainer)
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
        condBr(icmpNe(globalState, llvm.int32(FILE_INITIALIZED)), bbExit, bbCheckLocalState)
        positionAtEnd(bbCheckLocalState)
        condBr(icmpNe(load(localStatePtr), llvm.int32(FILE_INITIALIZED)), bbInit, bbExit)
        positionAtEnd(bbInit)
        call(llvm.callInitThreadLocal, listOf(globalStatePtr, localStatePtr, initializerPtr),
                exceptionHandler = currentCodeContext.exceptionHandler)
        br(bbExit)
        positionAtEnd(bbExit)
        codegen.theUnitInstanceRef.llvm
    }

    private fun evaluateFileStandaloneThreadLocalInitializerCall(fileInitializer: IrFunction) = with(functionGenerationContext) {
        val state = getThreadLocalInitStateFor(fileInitializer.parent as IrDeclarationContainer)
        val statePtr = state.getAddress(functionGenerationContext)
        val initializerPtr = with(codegen) { fileInitializer.llvmFunction.llvmValue }

        val bbInit = basicBlock("label_init", null)
        val bbExit = basicBlock("label_continue", null)
        moveBlockAfterEntry(bbExit)
        moveBlockAfterEntry(bbInit)
        condBr(icmpEq(load(statePtr), llvm.int32(FILE_INITIALIZED)), bbExit, bbInit)
        positionAtEnd(bbInit)
        call(llvm.callInitThreadLocal, listOf(llvm.kNullInt32Ptr, statePtr, initializerPtr),
                exceptionHandler = currentCodeContext.exceptionHandler)
        br(bbExit)
        positionAtEnd(bbExit)
        codegen.theUnitInstanceRef.llvm
    }

    //-------------------------------------------------------------------------//

    private fun evaluateSimpleFunctionCall(
            function: IrFunction, args: List<LLVMValueRef>,
            resultLifetime: Lifetime, superClass: IrClass? = null, resultSlot: LLVMValueRef? = null): LLVMValueRef {
        //context.log{"evaluateSimpleFunctionCall : $tmpVariableName = ${ir2string(value)}"}
        if (superClass == null && function is IrSimpleFunction && function.isOverridable)
            return callVirtual(function, args, resultLifetime, resultSlot)
        else
            return callDirect(function, args, resultLifetime, resultSlot)
    }

    //-------------------------------------------------------------------------//
    private fun resultLifetime(callee: IrElement): Lifetime {
        return lifetimes.getOrElse(callee) { /* TODO: make IRRELEVANT */ Lifetime.GLOBAL }
    }

    private fun evaluateConstructorCall(callee: IrConstructorCall, args: List<LLVMValueRef>, resultSlot: LLVMValueRef?): LLVMValueRef {
        context.log{"evaluateConstructorCall        : ${ir2string(callee)}"}
        return memScoped {
            val constructedClass = callee.symbol.owner.constructedClass
            val thisValue = when {
                constructedClass.isArray -> {
                    assert(args.isNotEmpty() && args[0].type == llvm.int32Type)
                    functionGenerationContext.allocArray(constructedClass, args[0],
                            resultLifetime(callee), currentCodeContext.exceptionHandler, resultSlot = resultSlot)
                }
                constructedClass == context.ir.symbols.string.owner -> {
                    // TODO: consider returning the empty string literal instead.
                    assert(args.isEmpty())
                    functionGenerationContext.allocArray(constructedClass, count = llvm.kImmInt32Zero,
                            lifetime = resultLifetime(callee), exceptionHandler = currentCodeContext.exceptionHandler, resultSlot = resultSlot)
                }

                constructedClass.isObjCClass() -> error("Call should've been lowered: ${callee.dump()}")

                else -> functionGenerationContext.allocInstance(constructedClass, resultLifetime(callee), resultSlot = resultSlot)
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
                LlvmRetType(llvm.int8PtrType),
                origin = FunctionOrigin.OwnedBy(irClass),
                independent = true // Protocol is header-only declaration.
        )
        val protocolGetter = llvm.externalFunction(protocolGetterProto)

        return call(protocolGetter, emptyList())
    }

    //-------------------------------------------------------------------------//
    private val kTrue = llvm.int1(true)
    private val kFalse = llvm.int1(false)

    // TODO: Intrinsify?
    private fun evaluateOperatorCall(callee: IrCall, args: List<LLVMValueRef>): LLVMValueRef {
        context.log{"evaluateOperatorCall           : origin:${ir2string(callee)}"}
        val function = callee.symbol.owner
        val ib = context.irBuiltIns

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
                                    Lifetime.GLOBAL,
                                    null
                            )
                        }
                        else -> TODO(function.name.toString())
                    }
                }
            }
        }
    }

    //-------------------------------------------------------------------------//

    fun callDirect(function: IrFunction, args: List<LLVMValueRef>, resultLifetime: Lifetime, resultSlot: LLVMValueRef?): LLVMValueRef {
        val functionDeclarations = codegen.llvmFunction(function.target)
        return call(function, functionDeclarations, args, resultLifetime, resultSlot)
    }

    //-------------------------------------------------------------------------//

    fun callVirtual(function: IrFunction, args: List<LLVMValueRef>, resultLifetime: Lifetime, resultSlot: LLVMValueRef?): LLVMValueRef {
        val functionDeclarations = functionGenerationContext.lookupVirtualImpl(args.first(), function)
        return call(function, functionDeclarations, args, resultLifetime, resultSlot)
    }

    //-------------------------------------------------------------------------//

    private val IrFunction.needsNativeThreadState: Boolean
        get() {
            // We assume that call site thread state switching is required for interop calls only.
            val result = context.memoryModel == MemoryModel.EXPERIMENTAL && origin == CBridgeOrigin.KOTLIN_TO_C_BRIDGE
            if (result) {
                check(isExternal)
                check(!annotations.hasAnnotation(KonanFqNames.gcUnsafeCall))
                check(annotations.hasAnnotation(RuntimeNames.filterExceptions))
            }
            return result
        }

    private fun call(function: IrFunction, llvmCallable: LlvmCallable, args: List<LLVMValueRef>,
                     resultLifetime: Lifetime, resultSlot: LLVMValueRef?): LLVMValueRef {
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

        val result = call(llvmCallable, args, resultLifetime, exceptionHandler, resultSlot)

        when  {
            function.returnType.isNothing() -> functionGenerationContext.unreachable()
            needsNativeThreadState -> functionGenerationContext.switchThreadState(ThreadState.Runnable)
        }

        if (llvmCallable.returnType == llvm.voidType) {
            return codegen.theUnitInstanceRef.llvm
        }

        return result
    }

    private fun call(
            function: LlvmCallable, args: List<LLVMValueRef>,
            resultLifetime: Lifetime = Lifetime.IRRELEVANT,
            exceptionHandler: ExceptionHandler = currentCodeContext.exceptionHandler,
            resultSlot: LLVMValueRef? = null
    ): LLVMValueRef {
        return functionGenerationContext.call(function, args, resultLifetime, exceptionHandler, resultSlot = resultSlot)
    }

    //-------------------------------------------------------------------------//

    private fun delegatingConstructorCall(constructor: IrConstructor, args: List<LLVMValueRef>): LLVMValueRef {

        val constructedClass = functionGenerationContext.constructedClass!!
        val thisPtr = currentCodeContext.genGetValue(constructedClass.thisReceiver!!, null)

        if (constructor.constructedClass.isExternalObjCClass() || constructor.constructedClass.isAny()) {
            assert(args.isEmpty())
            return codegen.theUnitInstanceRef.llvm
        }

        val thisPtrArgType = constructor.allParameters[0].type.toLLVMType(llvm)
        val thisPtrArg = if (thisPtr.type == thisPtrArgType) {
            thisPtr
        } else {
            // e.g. when array constructor calls super (i.e. Any) constructor.
            functionGenerationContext.bitcast(thisPtrArgType, thisPtr)
        }

        return callDirect(constructor, listOf(thisPtrArg) + args,
                Lifetime.IRRELEVANT /* no value returned */, null)
    }

    //-------------------------------------------------------------------------//

    private fun appendLlvmUsed(name: String, args: List<LLVMValueRef>) {
        if (args.isEmpty()) return

        val argsCasted = args.map { constPointer(it).bitcast(llvm.int8PtrType) }
        val llvmUsedGlobal = llvm.staticData.placeGlobalArray(name, llvm.int8PtrType, argsCasted)

        LLVMSetLinkage(llvmUsedGlobal.llvmGlobal, LLVMLinkage.LLVMAppendingLinkage)
        LLVMSetSection(llvmUsedGlobal.llvmGlobal, "llvm.metadata")
    }

    // Globals set this way cannot be const, but are overridable when producing final executable.
    private fun overrideRuntimeGlobal(name: String, value: ConstValue) =
            codegen.replaceExternalWeakOrCommonGlobalFromNativeRuntime(name, value)

    private fun overrideRuntimeGlobals() {
        if (!context.config.isFinalBinary)
            return

        overrideRuntimeGlobal("Kotlin_destroyRuntimeMode", llvm.constInt32(context.config.destroyRuntimeMode.value))
        overrideRuntimeGlobal("Kotlin_gcMarkSingleThreaded", llvm.constInt32(if (context.config.gcMarkSingleThreaded) 1 else 0))
        overrideRuntimeGlobal("Kotlin_workerExceptionHandling", llvm.constInt32(context.config.workerExceptionHandling.value))
        overrideRuntimeGlobal("Kotlin_suspendFunctionsFromAnyThreadFromObjC", llvm.constInt32(if (context.config.suspendFunctionsFromAnyThreadFromObjC) 1 else 0))
        val getSourceInfoFunctionName = when (context.config.sourceInfoType) {
            SourceInfoType.NOOP -> null
            SourceInfoType.LIBBACKTRACE -> "Kotlin_getSourceInfo_libbacktrace"
            SourceInfoType.CORESYMBOLICATION -> "Kotlin_getSourceInfo_core_symbolication"
        }
        if (getSourceInfoFunctionName != null) {
            val getSourceInfoFunction = LLVMGetNamedFunction(llvm.module, getSourceInfoFunctionName)
                    ?: LLVMAddFunction(llvm.module, getSourceInfoFunctionName,
                            functionType(llvm.int32Type, false, llvm.int8PtrType, llvm.int8PtrType, llvm.int32Type))
            overrideRuntimeGlobal("Kotlin_getSourceInfo_Function", constValue(getSourceInfoFunction!!))
        }
        if (context.config.target.family == Family.ANDROID && context.config.produce == CompilerOutputKind.PROGRAM) {
            val configuration = context.config.configuration
            val programType = configuration.get(BinaryOptions.androidProgramType) ?: AndroidProgramType.Default
            overrideRuntimeGlobal("Kotlin_printToAndroidLogcat", llvm.constInt32(if (programType.consolePrintsToLogcat) 1 else 0))
        }
        overrideRuntimeGlobal("Kotlin_appStateTracking", llvm.constInt32(context.config.appStateTracking.value))
        overrideRuntimeGlobal("Kotlin_mimallocUseDefaultOptions", llvm.constInt32(if (context.config.mimallocUseDefaultOptions) 1 else 0))
        overrideRuntimeGlobal("Kotlin_mimallocUseCompaction", llvm.constInt32(if (context.config.mimallocUseCompaction) 1 else 0))
    }

    //-------------------------------------------------------------------------//
    // Create type { i32, void ()*, i8* }

    val kCtorType = llvm.structType(llvm.int32Type, pointerType(kVoidFuncType), llvm.int8PtrType)

    //-------------------------------------------------------------------------//
    // Create object { i32, void ()*, i8* } { i32 1, void ()* @ctorFunction, i8* null }

    fun createGlobalCtor(ctorFunction: LLVMValueRef): ConstPointer {
        val priority = if (context.config.target.family == Family.MINGW) {
            // Workaround MinGW bug. Using this value makes the compiler generate
            // '.ctors' section instead of '.ctors.XXXXX', which can't be recognized by ld
            // when string table is too long.
            // More details: https://youtrack.jetbrains.com/issue/KT-39548
            llvm.int32(65535)
            // Note: this difference in priorities doesn't actually make initializers
            // platform-dependent, because handling priorities for initializers
            // from different object files is platform-dependent anyway.
        } else {
            llvm.kImmInt32One
        }
        val data = llvm.kNullInt8Ptr
        val argList = cValuesOf(priority, ctorFunction, data)
        val ctorItem = LLVMConstNamedStruct(kCtorType, argList, 3)!!
        return constPointer(ctorItem)
    }

    //-------------------------------------------------------------------------//
    fun appendStaticInitializers() {
        // Note: the list of libraries is topologically sorted (in order for initializers to be called correctly).
        val dependencies = (generationState.dependenciesTracker.allBitcodeDependencies + listOf(null)/* Null for "current" non-library module */)

        val libraryToInitializers = dependencies.associate { it?.library to mutableListOf<LLVMValueRef>() }

        llvm.irStaticInitializers.forEach {
            val library = it.konanLibrary
            val initializers = libraryToInitializers[library]
                    ?: error("initializer for not included library ${library?.libraryFile}")

            initializers.add(it.initializer)
        }

        fun fileCtorName(libraryName: String, fileName: String) = "$libraryName:$fileName".moduleConstructorName

        fun addCtorFunction(ctorName: String) =
                addLlvmFunctionWithDefaultAttributes(
                        context,
                        llvm.module,
                        ctorName,
                        kVoidFuncType
                ).also { LLVMSetLinkage(it, LLVMLinkage.LLVMExternalLinkage) }

        val ctorFunctions = dependencies.flatMap { dependency ->
            val library = dependency?.library
            val initializers = libraryToInitializers.getValue(library)

            val ctorName = when {
                // TODO: Try to not use moduleId.
                library == null -> (if (context.config.produce.isCache) generationState.outputFiles.cacheFileName else context.config.moduleId).moduleConstructorName
                library == context.config.libraryToCache?.klib
                        && context.config.producePerFileCache ->
                    fileCtorName(library.uniqueName, generationState.outputFiles.perFileCacheFileName)
                else -> library.moduleConstructorName
            }

            if (library == null || generationState.llvmModuleSpecification.containsLibrary(library)) {
                val otherInitializers = llvm.otherStaticInitializers.takeIf { library == null }.orEmpty()

                val ctorFunction = addCtorFunction(ctorName)
                appendStaticInitializers(ctorFunction, initializers + otherInitializers)

                listOf(ctorFunction)
            } else {
                // A cached library.
                check(initializers.isEmpty()) {
                    "found initializer from ${library.libraryFile}, which is not included into compilation"
                }

                val cache = context.config.cachedLibraries.getLibraryCache(library)
                        ?: error("Library ${library.libraryFile} is expected to be cached")

                when (cache) {
                    is CachedLibraries.Cache.Monolithic -> listOf(addCtorFunction(ctorName))
                    is CachedLibraries.Cache.PerFile -> {
                        val files = when (dependency.kind) {
                            is DependenciesTracker.DependencyKind.WholeModule ->
                                context.irLinker.klibToModuleDeserializerMap[library]!!.sortedFileIds
                            is DependenciesTracker.DependencyKind.CertainFiles ->
                                dependency.kind.files
                        }
                        files.map { addCtorFunction(fileCtorName(library.uniqueName, it)) }
                    }
                }
            }
        }

        appendGlobalCtors(ctorFunctions)
    }

    private fun appendStaticInitializers(ctorFunction: LLVMValueRef, initializers: List<LLVMValueRef>) {
        generateFunctionNoRuntime(codegen, ctorFunction) {
            val initGuardName = ctorFunction.name.orEmpty() + "_guard"
            val initGuard = LLVMAddGlobal(llvm.module, llvm.int32Type, initGuardName)
            LLVMSetInitializer(initGuard, llvm.kImmInt32Zero)
            LLVMSetLinkage(initGuard, LLVMLinkage.LLVMPrivateLinkage)
            val bbInited = basicBlock("inited", null)
            val bbNeedInit = basicBlock("need_init", null)


            val value = LLVMBuildLoad(builder, initGuard, "")!!
            condBr(icmpEq(value, llvm.kImmInt32Zero), bbNeedInit, bbInited)

            appendingTo(bbInited) {
                ret(null)
            }

            appendingTo(bbNeedInit) {
                LLVMBuildStore(builder, llvm.kImmInt32One, initGuard)

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
        if (context.config.isFinalBinary) {
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
            val globalCtors = llvm.staticData.placeGlobalArray("llvm.global_ctors", kCtorType,
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

private val thisName = Name.special("<this>")
private val underscoreThisName = Name.identifier("_this")
private val doubleUnderscoreThisName = Name.identifier("__this")

/**
 * HACK: this is workaround for GH-2316, to let IDE some how operate with this.
 * We're experiencing issue with libclang which is used as compiler of expression in lldb
 * for current state support Kotlin in lldb:
 *   1. <this> isn't accepted by libclang as valid variable name.
 *   2. this is reserved name and compiled in special way.
 */
private fun IrValueDeclaration.debugNameConversion(): Name {
    val name = descriptor.name
    if (name == thisName) {
        return when (origin) {
            IrDeclarationOrigin.IR_TEMPORARY_VARIABLE_FOR_INLINED_EXTENSION_RECEIVER -> doubleUnderscoreThisName
            else -> underscoreThisName
        }
    }
    return name
}

internal class LocationInfo(val scope: DIScopeOpaqueRef,
                            val line: Int,
                            val column: Int,
                            val inlinedAt: LocationInfo? = null)

internal fun NativeGenerationState.generateRuntimeConstantsModule() : LLVMModuleRef {
    val llvmModule = LLVMModuleCreateWithNameInContext("constants", llvmContext)!!
    LLVMSetDataLayout(llvmModule, runtime.dataLayout)
    val static = StaticData(llvmModule, llvm)

    fun setRuntimeConstGlobal(name: String, value: ConstValue) {
        val global = static.placeGlobal(name, value)
        global.setConstant(true)
        global.setLinkage(LLVMLinkage.LLVMExternalLinkage)
    }

    setRuntimeConstGlobal("Kotlin_needDebugInfo", llvm.constInt32(if (shouldContainDebugInfo()) 1 else 0))
    setRuntimeConstGlobal("Kotlin_runtimeAssertsMode", llvm.constInt32(config.runtimeAssertsMode.value))
    val runtimeLogs = config.runtimeLogs?.let {
        static.cStringLiteral(it)
    } ?: NullPointer(llvm.int8Type)
    setRuntimeConstGlobal("Kotlin_runtimeLogs", runtimeLogs)
    setRuntimeConstGlobal("Kotlin_freezingEnabled", llvm.constInt32(if (config.freezing.enableFreezeAtRuntime) 1 else 0))
    setRuntimeConstGlobal("Kotlin_freezingChecksEnabled", llvm.constInt32(if (config.freezing.enableFreezeChecks) 1 else 0))
    setRuntimeConstGlobal("Kotlin_gcSchedulerType", llvm.constInt32(config.gcSchedulerType.value))

    return llvmModule
}
