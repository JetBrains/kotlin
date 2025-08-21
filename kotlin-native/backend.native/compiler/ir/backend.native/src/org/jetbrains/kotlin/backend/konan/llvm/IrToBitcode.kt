/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.common.ir.isUnconditional
import org.jetbrains.kotlin.backend.common.lower.coroutines.getOrCreateFunctionWithContinuationStub
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.cexport.CAdapterCodegen
import org.jetbrains.kotlin.backend.konan.cexport.CAdapterExportedElements
import org.jetbrains.kotlin.backend.konan.cgen.CBridgeOrigin
import org.jetbrains.kotlin.backend.konan.ir.*
import org.jetbrains.kotlin.backend.konan.llvm.objc.processBindClassToObjCNameAnnotations
import org.jetbrains.kotlin.backend.konan.lower.*
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.config.nativeBinaryOptions.AndroidProgramType
import org.jetbrains.kotlin.config.nativeBinaryOptions.BinaryOptions
import org.jetbrains.kotlin.config.nativeBinaryOptions.SourceInfoType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.objcinterop.*
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.isSubtypeOfClass
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.konan.ForeignExceptionMode
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.name.Name

internal class NativeCodeGeneratorException(val declarations: List<IrElement>, cause: Throwable?): IllegalStateException(cause) {
    override val message: String
        get() = try {
            buildString {
                appendLine("Exception during generating code for following declaration:")
                declarations.dropLast(1).forEach {
                    append("Inside: ")
                    appendLine(it.render())
                }
                declarations.lastOrNull()?.let {
                    appendLine(it.dumpKotlinLike())
                }
            }
        } catch (e: Exception) { // shouldn't happen, but if it somehow does, it would be better to have at least a cause printed
            "Exception during code generation"
        }

    companion object {
        fun wrap(e: Exception, element: IrElement?) = if (e is NativeCodeGeneratorException) {
            if (element == null || e.declarations.firstOrNull() === element)
                e
            else
                NativeCodeGeneratorException(listOfNotNull(element) + e.declarations, e.cause)
        } else {
            NativeCodeGeneratorException(listOfNotNull(element), e)
        }
    }
}

internal enum class FieldStorageKind {
    GLOBAL,
    THREAD_LOCAL
}

internal val IrField.storageKind: FieldStorageKind
    get() {
        // TODO: Is this correct?
        val annotations = correspondingPropertySymbol?.owner?.annotations ?: annotations
        return when {
            annotations.hasAnnotation(KonanFqNames.threadLocal) -> FieldStorageKind.THREAD_LOCAL
            else -> FieldStorageKind.GLOBAL
        }
    }

internal val IrField.needsGCRegistration
    get() = type.binaryTypeIsReference() && // only for references
                (hasNonConstInitializer || // which are initialized from heap object
                        !isFinal) // or are not final


internal fun IrSimpleFunction.shouldGenerateBody(): Boolean = modality != Modality.ABSTRACT && !isExternal

internal class RTTIGeneratorVisitor(generationState: NativeGenerationState, referencedFunctions: Set<IrSimpleFunction>?) : IrVisitorVoid() {
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

    /**
     * Called, when exception is caught in this block. Result expception would be rethrown instead.
     */
    fun wrapException(e: Exception) : Exception
}

//-------------------------------------------------------------------------//

internal class CodeGeneratorVisitor(
        val generationState: NativeGenerationState,
        val irBuiltins: IrBuiltIns,
        val lifetimes: Map<IrElement, Lifetime>
) : IrVisitorVoid() {
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

        override fun wrapException(e: Exception) = e
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
        } catch (e: Exception) {
            throw (codeContext?.wrapException(e) ?: e)
        } finally {
            codeContext?.onExit()
            currentCodeContext = oldCodeContext
        }
    }

    private fun appendCAdapters(elements: CAdapterExportedElements) {
        CAdapterCodegen(codegen, generationState).buildAllAdaptersRecursively(elements)
    }

    private fun FunctionGenerationContext.initThreadLocalField(irField: IrField) {
        val initializer = irField.initializer ?: return
        val address = staticFieldPtr(irField, this)
        storeAny(evaluateExpression(initializer.expression), address, irField.type.binaryTypeIsReference(), false)
    }

    private fun FunctionGenerationContext.initGlobalField(irField: IrField) {
        val address = staticFieldPtr(irField, this)
        val initialValue = if (irField.hasNonConstInitializer) {
            evaluateExpression(irField.initializer!!.expression)
        } else {
            null
        }
        if (irField.needsGCRegistration) {
            call(llvm.initAndRegisterGlobalFunction, listOf(address, initialValue
                    ?: kNullObjHeaderPtr))
        } else if (initialValue != null) {
            storeAny(initialValue, address, irField.type.binaryTypeIsReference(), false)
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
                                    .filter { it.storageKind != FieldStorageKind.THREAD_LOCAL }
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
                                    .filter { it.storageKind == FieldStorageKind.THREAD_LOCAL }
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
            val runtimeInitializer = createInitBody(it)
            llvm.irStaticInitializers.add(IrStaticInitializer(konanLibrary, runtimeInitializer))
        }
    }

    //-------------------------------------------------------------------------//

    override fun visitElement(element: IrElement) {
        TODO(ir2string(element))
    }

    //-------------------------------------------------------------------------//
    override fun visitModuleFragment(declaration: IrModuleFragment) {
        context.log{"visitModule                    : ${ir2string(declaration)}"}

        initializeCachedBoxes(generationState)
        declaration.acceptChildrenVoid(this)

        runAndProcessInitializers(null) {
            // Note: it is here because it also generates some bitcode.
            generationState.objCExport.generate(codegen)

            codegen.objCDataGenerator?.finishModule()

            overrideRuntimeGlobals()
            appendLlvmUsed("llvm.used", llvm.usedFunctions.map { it.toConstPointer().llvm } + llvm.usedGlobals)
            appendLlvmUsed("llvm.compiler.used", llvm.compilerUsedGlobals)
            if (context.config.produceCInterface) {
                context.cAdapterExportedElements?.let { appendCAdapters(it) }
            }
        }

        appendStaticInitializers()
    }

    //-------------------------------------------------------------------------//

    val ctorFunctionSignature = LlvmFunctionSignature(LlvmRetType(llvm.voidType, isObjectType = false))
    val kNodeInitType = llvm.runtime.initNodeType
    val kMemoryStateType = llvm.runtime.memoryStateType
    val kInitFuncType = LlvmFunctionSignature(LlvmRetType(llvm.voidType, isObjectType = false), listOf(LlvmParamType(llvm.int32Type), LlvmParamType(pointerType(kMemoryStateType))))

    //-------------------------------------------------------------------------//

    // Must be synchronized with Runtime.cpp
    val ALLOC_THREAD_LOCAL_GLOBALS = 0
    val INIT_GLOBALS = 1
    val INIT_THREAD_LOCAL_GLOBALS = 2
    val DEINIT_GLOBALS = 3

    val FILE_NOT_INITIALIZED = 0
    val FILE_INITIALIZED = 2

    private fun createInitBody(state: ScopeInitializersGenerationState): RuntimeInitializer {
        return generateRuntimeInitializer {
            using(FunctionScope(function, this)) {
                val bbInit = basicBlock("init", null)
                val bbLocalInit = basicBlock("local_init", null)
                val bbLocalAlloc = basicBlock("local_alloc", null)
                val bbGlobalDeinit = basicBlock("global_deinit", null)
                val bbDefault = basicBlock("default", null) {
                    unreachable()
                }

                switch(function.param(0),
                        listOf(llvm.int32(INIT_GLOBALS) to bbInit,
                                llvm.int32(INIT_THREAD_LOCAL_GLOBALS) to bbLocalInit,
                                llvm.int32(ALLOC_THREAD_LOCAL_GLOBALS) to bbLocalAlloc,
                                llvm.int32(DEINIT_GLOBALS) to bbGlobalDeinit),
                        bbDefault)

                // Globals initializers may contain accesses to objects, so visit them first.
                appendingTo(bbInit) {
                    state.topLevelFields
                            .filter { context.shouldBeInitializedEagerly(it) }
                            .filterNot { it.storageKind == FieldStorageKind.THREAD_LOCAL }
                            .forEach { initGlobalField(it) }
                    ret(null)
                }

                appendingTo(bbLocalInit) {
                    state.topLevelFields
                            .filter { context.shouldBeInitializedEagerly(it) }
                            .filter { it.storageKind == FieldStorageKind.THREAD_LOCAL }
                            .forEach { initThreadLocalField(it) }
                    ret(null)
                }

                appendingTo(bbLocalAlloc) {
                    if (llvm.tlsCount > 0) {
                        val memory = function.param(1)
                        call(llvm.addTLSRecord, listOf(memory, llvm.tlsKey, llvm.int32(llvm.tlsCount)))
                    }
                    ret(null)
                }

                appendingTo(bbGlobalDeinit) {
                    state.topLevelFields
                            // Only if a subject for memory management.
                            .forEach { irField ->
                                if (irField.type.binaryTypeIsReference() && irField.storageKind != FieldStorageKind.THREAD_LOCAL) {
                                    val address = staticFieldPtr(irField, functionGenerationContext)
                                    storeHeapRef(codegen.kNullObjHeaderPtr, address)
                                }
                            }
                    state.globalSharedObjects.forEach { address ->
                        storeHeapRef(codegen.kNullObjHeaderPtr, address)
                    }
                    state.globalInitState?.let {
                        store(llvm.intptr(FILE_NOT_INITIALIZED), it)
                    }
                    ret(null)
                }
            }
        }
    }

    private fun mergeRuntimeInitializers(runtimeInitializers: List<RuntimeInitializer>): RuntimeInitializer? {
        if (runtimeInitializers.size <= 1) return runtimeInitializers.singleOrNull()

        // It would be natural to generate a single runtime initializer function
        // and call all the initializers from it.
        // However, right now we can have quite many initializers (see e.g. KT-74774).
        // So, this natural solution can lead to generating huge LLVM functions triggering slow compilation.
        // Apply a cheap trick -- merge them by chunks recursively.

        val chunkInitializers = runtimeInitializers.chunked(100) { chunk ->
            generateRuntimeInitializer {
                chunk.forEach {
                    this.call(it.llvmCallable, listOf(param(0), param(1)), exceptionHandler = ExceptionHandler.Caller)
                }
                ret(null)
            }
        }

        return mergeRuntimeInitializers(chunkInitializers)
    }

    private fun generateRuntimeInitializer(block: FunctionGenerationContext.() -> Unit): RuntimeInitializer {
        val initFunctionProto = kInitFuncType.toProto("", null, LLVMLinkage.LLVMPrivateLinkage)
        return RuntimeInitializer(generateFunction(codegen, initFunctionProto, code = block))
    }

    //-------------------------------------------------------------------------//
    // Creates static struct InitNode $nodeName = {$initName, NULL};

    private fun createInitNode(runtimeInitializer: RuntimeInitializer): LLVMValueRef {
        val initFunction = runtimeInitializer.llvmCallable
        val nextInitNode = LLVMConstNull(pointerType(kNodeInitType))
        val argList = cValuesOf(initFunction.toConstPointer().llvm, nextInitNode)
        // Create static object of class InitNode.
        val initNode = LLVMConstNamedStruct(kNodeInitType, argList, 2)!!
        // Create global variable with init record data.
        return codegen.staticData.placeGlobal("init_node", constPointer(initNode), isExported = false).llvmGlobal
    }

    //-------------------------------------------------------------------------//

    private fun createInitCtor(initNodePtr: LLVMValueRef): LlvmCallable {
        val ctorProto = ctorFunctionSignature.toProto("", null, LLVMLinkage.LLVMPrivateLinkage)
        val ctor = generateFunctionNoRuntime(codegen, ctorProto) {
            call(llvm.appendToInitalizersTail, listOf(initNodePtr))
            ret(null)
        }
        return ctor
    }

    //-------------------------------------------------------------------------//

    override fun visitFile(declaration: IrFile) {
        @Suppress("UNCHECKED_CAST")
        using(FileScope(declaration, declaration.fileEntry)) {
            runAndProcessInitializers(declaration.konanLibrary) {
                declaration.acceptChildrenVoid(this)
                codegen.processBindClassToObjCNameAnnotations(declaration)
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
            function: IrSimpleFunction?,
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
            val declaration: IrSimpleFunction?,
            val llvmFunction: LlvmCallable) : InnerScopeImpl() {

        constructor(declaration: IrSimpleFunction, functionGenerationContext: FunctionGenerationContext) :
                this(functionGenerationContext, declaration, codegen.llvmFunction(declaration))

        constructor(llvmFunction: LlvmCallable, functionGenerationContext: FunctionGenerationContext) :
                this(functionGenerationContext, null, llvmFunction)

        override fun genReturn(target: IrSymbolOwner, value: LLVMValueRef?) {
            if (declaration == null || target == declaration) {
                if ((target as IrSimpleFunction).returnsUnit()) {
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

        private val fileEntry = fileEntry()
        override fun location(offset: Int) = scope?.let { scope ->
            val (line, column) = fileEntry.lineAndColumn(offset)
            LocationInfo(scope, line, column)
        }

        override fun scope() = scope

        override fun wrapException(e: Exception) = NativeCodeGeneratorException.wrap(e, declaration)
    }

    private val functionGenerationContext
            get() = (currentCodeContext.functionScope() as FunctionScope).functionGenerationContext
    /**
     * Binds LLVM function parameters to IR parameter descriptors.
     */
    private fun bindParameters(function: IrSimpleFunction?): Map<IrValueParameter, LLVMValueRef> {
        if (function == null) return emptyMap()
        return function.parameters.mapIndexed { i, irParameter ->
            val parameter = codegen.param(function, i)
            assert(irParameter.type.toLLVMType(llvm) == parameter.type)
            irParameter to parameter
        }.toMap()
    }

    private val IrDeclarationContainer.initVariableSuffix get() = when (this) {
        is IrFile -> "${packageFqName}\$${fileEntry.name}"
        else -> fqNameForIrSerialization.asString()
    }

    private fun getGlobalInitStateFor(container: IrDeclarationContainer): LLVMValueRef =
            llvm.initializersGenerationState.fileGlobalInitStates.getOrPut(container) {
                codegen.addGlobal("state_global$${container.initVariableSuffix}", llvm.intptrType, false).also {
                    LLVMSetInitializer(it, llvm.intptr(FILE_NOT_INITIALIZED))
                    LLVMSetLinkage(it, LLVMLinkage.LLVMInternalLinkage)
                }
            }

    private fun getThreadLocalInitStateFor(container: IrDeclarationContainer): AddressAccess =
            llvm.initializersGenerationState.fileThreadLocalInitStates.getOrPut(container) {
                codegen.addKotlinThreadLocal("state_thread_local$${container.initVariableSuffix}", llvm.intptrType,
                        LLVMPreferredAlignmentOfType(llvm.runtime.targetData, llvm.intptrType), false).also {
                    LLVMSetInitializer((it as GlobalAddressAccess).getAddress(null), llvm.intptr(FILE_NOT_INITIALIZED))
                }
            }

    private fun buildVirtualFunctionTrampoline(irFunction: IrSimpleFunction) {
        codegen.getVirtualFunctionTrampoline(irFunction)
    }

    override fun visitConstructor(declaration: IrConstructor) {
        // All constructors are lowered by this point, but for some cases original constructors are left as is; just skip them here.
        return
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        context.log{"visitFunction                  : ${ir2string(declaration)}"}

        if (declaration.isOverridable && declaration.origin !is DECLARATION_ORIGIN_BRIDGE_METHOD)
            buildVirtualFunctionTrampoline(declaration)

        val scopeState = llvm.initializersGenerationState.scopeState
        if (declaration.origin == DECLARATION_ORIGIN_STATIC_GLOBAL_INITIALIZER) {
            require(scopeState.globalInitFunction == null) { "There can only be at most one global file initializer" }
            require(declaration.body == null) { "The body of file initializer should be null" }
            require(declaration.hasShape()) { "File initializer must be parameterless" }
            require(declaration.returnsUnit()) { "File initializer must return Unit" }
            scopeState.globalInitFunction = declaration
            scopeState.globalInitState = getGlobalInitStateFor(declaration.parent as IrDeclarationContainer)
        }
        if (declaration.origin == DECLARATION_ORIGIN_STATIC_THREAD_LOCAL_INITIALIZER
                || declaration.origin == DECLARATION_ORIGIN_STATIC_STANDALONE_THREAD_LOCAL_INITIALIZER) {
            require(scopeState.threadLocalInitFunction == null) { "There can only be at most one thread local file initializer" }
            require(declaration.body == null) { "The body of file initializer should be null" }
            require(declaration.hasShape()) { "File initializer must be parameterless" }
            require(declaration.returnsUnit()) { "File initializer must return Unit" }
            scopeState.threadLocalInitFunction = declaration
            scopeState.threadLocalInitState = getThreadLocalInitStateFor(declaration.parent as IrDeclarationContainer)
        }

        if (!declaration.shouldGenerateBody())
            return

        // Some special functions may have empty body, they are handled separately.
        val body = declaration.body ?: return

        usingFileScope(declaration.sourceFileWhenInlined) {
            generateFunction(codegen, declaration,
                    declaration.location(start = true),
                    declaration.location(start = false)) {
                using(FunctionScope(declaration, this)) {
                    val parameterScope = ParameterScope(declaration, functionGenerationContext)
                    using(parameterScope) usingParameterScope@{
                        using(VariableScope()) usingVariableScope@{
                            if (declaration.isReifiedInline) {
                                callDirect(context.symbols.throwIllegalStateExceptionWithMessage.owner,
                                        listOf(codegen.staticData.kotlinStringLiteral(
                                                "unsupported call of reified inlined function `${declaration.fqNameForIrSerialization}`").llvm),
                                        Lifetime.IRRELEVANT, null)
                                return@usingVariableScope
                            }
                            when (body) {
                                is IrBlockBody -> body.statements.forEach { generateStatement(it) }
                                is IrExpressionBody -> compilationException("IrExpressionBody should've been lowered", declaration)
                                is IrSyntheticBody -> compilationException("Synthetic body ${body.kind} has not been lowered", declaration)
                            }
                        }
                    }
                }
            }
        }


        if (declaration.retainAnnotation(context.config.target)) {
            llvm.usedFunctions.add(codegen.llvmFunction(declaration))
        }

        if (context.shouldVerifyBitCode())
            verifyModule(llvm.module, "${ir2string(declaration.parent)}::${ir2string(declaration)}")
    }

    private fun IrSimpleFunction.location(start: Boolean): LocationInfo? {
        if (!context.shouldContainLocationDebugInfo() || startOffset == UNDEFINED_OFFSET) return null

        val (line, column) = if (start) startLineAndColumn() else endLineAndColumn()
        return LocationInfo(scope = scope()!!, line = line, column = column)
    }

    //-------------------------------------------------------------------------//

    override fun visitClass(declaration: IrClass) {
        context.log{"visitClass                     : ${ir2string(declaration)}"}

        usingFileScope(declaration.sourceFileWhenInlined) {
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
    }

    override fun visitProperty(declaration: IrProperty) {
        declaration.getter?.acceptVoid(this)
        declaration.setter?.acceptVoid(this)
        declaration.backingField?.acceptVoid(this)
    }

    private fun needGlobalInit(field: IrField): Boolean {
        if (field.parent !is IrPackageFragment) return field.isStatic
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
                LLVMSetInitializer(globalProperty, when {
                    initializer == null || declaration.hasNonConstInitializer -> LLVMConstNull(type)
                    else -> evaluateExpression(initializer)
                })
                // (Cannot do this before the global is initialized).
                LLVMSetLinkage(globalProperty, LLVMLinkage.LLVMInternalLinkage)
            }
            llvm.initializersGenerationState.scopeState.topLevelFields.add(declaration)
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateExpression(value: IrExpression, resultSlot: LLVMValueRef? = null): LLVMValueRef {
        updateBuilderDebugLocation(value)
        return when (value) {
            is IrTypeOperatorCall -> evaluateTypeOperator(value, resultSlot)
            is IrCall -> evaluateCall(value, resultSlot)
            is IrInstanceInitializerCall -> evaluateInstanceInitializerCall(value)
            is IrGetValue -> evaluateGetValue(value, resultSlot)
            is IrSetValue -> evaluateSetValue(value)
            is IrGetField -> evaluateGetField(value, resultSlot)
            is IrSetField -> evaluateSetField(value)
            is IrConst -> evaluateConst(value).llvm
            is IrReturn -> evaluateReturn(value)
            is IrWhen -> evaluateWhen(value, resultSlot)
            is IrThrow -> evaluateThrow(value)
            is IrTry -> evaluateTry(value)
            is IrReturnableBlock -> evaluateReturnableBlock(value, resultSlot)
            is IrInlinedFunctionBlock -> evaluateInlinedBlock(value, resultSlot)
            is IrContainerExpression -> evaluateContainerExpression(value, resultSlot)
            is IrWhileLoop -> evaluateWhileLoop(value)
            is IrDoWhileLoop -> evaluateDoWhileLoop(value)
            is IrVararg -> evaluateVararg(value)
            is IrBreak -> evaluateBreak(value)
            is IrContinue -> evaluateContinue(value)
            is IrRawFunctionReference -> evaluateRawFunctionReference(value)
            is IrSuspendableExpression -> evaluateSuspendableExpression(value, resultSlot)
            is IrSuspensionPoint -> evaluateSuspensionPoint(value)
            is IrClassReference -> evaluateClassReference(value)
            is IrConstantValue -> evaluateConstantValue(value).llvm
            else -> error("The node must be lowered before code generation: ${value.render()}")
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
                continuationBlock(context.symbols.throwable.owner.defaultType, endLocationInfoFromScope()) {
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

                if (catch.catchParameter.type == context.irBuiltIns.throwableType) {
                    genCatchBlock()
                    return      // Remaining catch clauses are unreachable.
                } else {
                    val isInstance = genInstanceOfImpl(exception, catch.catchParameter.type.getClass()!!)
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
            expression.type.isUnit() -> codegen.theUnitInstanceRef.llvm
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
            call(llvm.Kotlin_mm_safePointWhileLoopBody, emptyList())
            loop.body?.generate()

            functionGenerationContext.br(loopScope.loopCheck)
            functionGenerationContext.positionAtEnd(loopScope.loopExit)
        }

        assert(loop.type.isUnit())
        return codegen.theUnitInstanceRef.llvm
    }

    //-------------------------------------------------------------------------//

    private fun evaluateDoWhileLoop(loop: IrDoWhileLoop): LLVMValueRef {
        val loopScope = LoopScope(loop)
        using(loopScope) {
            val loopBody = functionGenerationContext.basicBlock("do_while_loop", loop.body?.startLocation ?: loop.startLocation)
            functionGenerationContext.br(loopBody)

            functionGenerationContext.positionAtEnd(loopBody)
            call(llvm.Kotlin_mm_safePointWhileLoopBody, emptyList())
            loop.body?.generate()
            functionGenerationContext.br(loopScope.loopCheck)

            functionGenerationContext.positionAtEnd(loopScope.loopCheck)
            val condition = evaluateExpression(loop.condition)
            functionGenerationContext.condBr(condition, loopBody, loopScope.loopExit)

            functionGenerationContext.positionAtEnd(loopScope.loopExit)
        }

        assert(loop.type.isUnit())
        return codegen.theUnitInstanceRef.llvm
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
        return codegen.theUnitInstanceRef.llvm
    }

    //-------------------------------------------------------------------------//
    private fun debugInfoIfNeeded(function: IrSimpleFunction?, element: IrElement): VariableDebugLocation? {
        if (function == null || !element.needDebugInfo(context) || currentCodeContext.scope() == null) return null
        val locationInfo = element.startLocation ?: return null
        val location = codegen.generateLocationInfo(locationInfo)
        val file = fileEntry().diFileScope()
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
                    argNo         = function.parameters.indexOf(element) + 1,
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
        context.log { "generateVariable               : ${ir2string(variable)}" }
        val value = variable.initializer?.let { initializer ->
            evaluateExpression(initializer)
        }
        currentCodeContext.genDeclareVariable(variable, value)
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
                codegen.theUnitInstanceRef.llvm
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
                    UnsignedType.values().any { it.classId == this.getClass()?.classId }

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

        return genInstanceOf(
                value,
                dstClass,
                resultSlot,
                onSuperClassCast = {
                    it.takeIf { value.typeOperand.isNullable() }
                },
                onNull = {
                    if (value.typeOperand.isNullable()) {
                        codegen.kNullObjHeaderPtr
                    } else {
                        callDirect(
                                context.symbols.throwNullPointerException.owner,
                                listOf(),
                                Lifetime.GLOBAL,
                                null
                        )
                    }
                },
                onCheck = { argument, checkResult ->
                    with(functionGenerationContext) {
                        if (checkResult != kTrue) {
                            ifThen(not(checkResult)) {
                                if (dstClass.defaultType.isObjCObjectType()) {
                                    val dstFullClassName = dstClass.fqNameWhenAvailable?.toString() ?: dstClass.name.toString()
                                    callDirect(
                                            context.symbols.throwTypeCastException.owner,
                                            listOf(argument, codegen.staticData.kotlinStringLiteral(dstFullClassName).llvm),
                                            Lifetime.GLOBAL,
                                            null
                                    )
                                } else {
                                    val dstTypeInfo = functionGenerationContext.bitcast(llvm.int8PtrType, codegen.typeInfoValue(dstClass))
                                    callDirect(
                                            context.symbols.throwClassCastException.owner,
                                            listOf(argument, dstTypeInfo),
                                            Lifetime.GLOBAL,
                                            null
                                    )
                                }
                            }
                        }
                        argument
                    }
                }
        )
    }

    //-------------------------------------------------------------------------//

    private fun evaluateInstanceOf(value: IrTypeOperatorCall): LLVMValueRef {
        context.log{"evaluateInstanceOf             : ${ir2string(value)}"}
        val type     = value.typeOperand
        return genInstanceOf(
                value,
                type.getClass() ?: context.symbols.any.owner,
                resultSlot = null,
                onSuperClassCast = { arg ->
                    if (type.isNullable())
                        kTrue
                    else
                        functionGenerationContext.icmpNe(arg, codegen.kNullObjHeaderPtr)
                },
                onNull = { if (type.isNullable()) kTrue else kFalse },
                onCheck = { _, checkResult -> checkResult }
        )
    }

    //-------------------------------------------------------------------------//

    private inline fun genInstanceOf(
            value: IrTypeOperatorCall,
            dstClass: IrClass,
            resultSlot: LLVMValueRef?,
            onSuperClassCast: (LLVMValueRef) -> LLVMValueRef?,
            onNull: () -> LLVMValueRef,
            onCheck: (argument: LLVMValueRef, checkResult: LLVMValueRef) -> LLVMValueRef,
    ) : LLVMValueRef {
        val srcArg = evaluateExpression(value.argument, resultSlot)
        require(srcArg.type == codegen.kObjHeaderPtr) { "Expected ObjHeader but was ${llvmtype2string(srcArg.type)} for ${value.argument.dump()}" }
        val srcType = value.argument.type
        val isSuperClassCast = srcType.classifierOrNull !is IrTypeParameterSymbol // Due to unsafe casts, see unchecked_cast8.kt as an example.
                && srcType.isSubtypeOfClass(dstClass.symbol)

        if (isSuperClassCast) {
            onSuperClassCast(srcArg)?.let { return it }
        }
        return with(functionGenerationContext) {
            val bbInstanceOf = basicBlock("instance_of_notnull", value.startLocation)
            val bbNull = basicBlock("instance_of_null", value.startLocation)


            val condition = icmpEq(srcArg, codegen.kNullObjHeaderPtr)
            condBr(condition, bbNull, bbInstanceOf)

            positionAtEnd(bbNull)
            val resultNull = onNull()
            val resultNullBB = currentBlock.takeIf { !isAfterTerminator() }

            positionAtEnd(bbInstanceOf)
            val resultInstanceOf = onCheck(srcArg, if (isSuperClassCast) kTrue else genInstanceOfImpl(srcArg, dstClass))
            val resultInstanceOfBB = currentBlock.also { require(!isAfterTerminator()) }


            if (resultNullBB == null) {
                resultInstanceOf
            } else {
                val bbExit = basicBlock("instance_of_exit", value.startLocation)
                positionAtEnd(bbExit)
                appendingTo(resultInstanceOfBB) { br(bbExit) }
                appendingTo(resultNullBB) { br(bbExit) }
                require(resultNull.type == resultInstanceOf.type)
                val result = phi(resultNull.type)
                addPhiIncoming(result, resultNullBB to resultNull, resultInstanceOfBB to resultInstanceOf)
                result
            }
        }
    }

    private fun genInstanceOfImpl(obj: LLVMValueRef, dstClass: IrClass) = with(functionGenerationContext) {
        if (dstClass.defaultType.isObjCObjectType()) {
            genInstanceOfObjC(obj, dstClass)
        } else with(VirtualTablesLookup) {
            checkIsSubtype(
                    objTypeInfo = loadTypeInfo(bitcast(codegen.kObjHeaderPtr, obj)),
                    dstClass
            )
        }
    }

    private fun genInstanceOfObjC(obj: LLVMValueRef, dstClass: IrClass): LLVMValueRef {
        val objCObject = callDirect(
                context.symbols.interopObjCObjectRawValueGetter.owner,
                listOf(obj),
                Lifetime.IRRELEVANT,
                null
        )

        return if (dstClass.isCompanion) {
            functionGenerationContext.icmpEq(objCObject, genGetObjCClass(dstClass.parentAsClass))
        } else if (dstClass.isObjCClass()) {
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
                        LlvmRetType(llvm.int8Type, isObjectType = false),
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
                return evaluateConst(value.symbol.owner.initializer?.expression as IrConst).llvm
            }
            else -> {
                fieldAddress = staticFieldPtr(value.symbol.owner, functionGenerationContext)
                alignment = generationState.llvmDeclarations.forStaticField(value.symbol.owner).alignment
            }
        }
        return functionGenerationContext.loadSlot(
                value.type.toLLVMType(llvm),
                value.type.binaryTypeIsReference(),
                fieldAddress,
                !value.symbol.owner.isFinal,
                resultSlot,
                memoryOrder = order,
                alignment = alignment
        )
    }

    //-------------------------------------------------------------------------//

    private fun isZeroConstValue(value: IrExpression): Boolean {
        if (value !is IrConst) return false
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
            var receiver = value.receiver
            while (receiver is IrTypeOperatorCall && receiver.operator == IrTypeOperator.IMPLICIT_CAST)
                receiver = receiver.argument
            check(receiver is IrGetValue) { "Only IrGetValue expected for receiver of a field initializer" }
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
            address = fieldPtrOfClass(thisPtr, value.symbol.owner)
            alignment = generationState.llvmDeclarations.forField(value.symbol.owner).alignment
        } else {
            require(value.symbol.owner.isStatic) { "A receiver expected for a non-static field: ${value.render()}" }
            address = staticFieldPtr(value.symbol.owner, functionGenerationContext)
            alignment = generationState.llvmDeclarations.forStaticField(value.symbol.owner).alignment
        }
        functionGenerationContext.storeAny(
                valueToAssign, address, value.symbol.owner.type.binaryTypeIsReference(), false,
                isVolatile = value.symbol.owner.hasAnnotation(KonanFqNames.volatile),
                alignment = alignment,
        )

        assert (value.type.isUnit())
        return codegen.theUnitInstanceRef.llvm
    }

    //-------------------------------------------------------------------------//
    private fun fieldPtrOfClass(thisPtr: LLVMValueRef, value: IrField): LLVMValueRef {
        val fieldInfo = generationState.llvmDeclarations.forField(value)
        val classBodyType = fieldInfo.classBodyType
        val typedBodyPtr = functionGenerationContext.bitcast(pointerType(classBodyType), thisPtr)
        val fieldPtr = LLVMBuildStructGEP2(functionGenerationContext.builder, classBodyType, typedBodyPtr, fieldInfo.index, "")
        return fieldPtr!!
    }

    private fun staticFieldPtr(value: IrField, context: FunctionGenerationContext) =
            generationState.llvmDeclarations
                    .forStaticField(value.symbol.owner)
                    .storageAddressAccess
                    .getAddress(context)

    //-------------------------------------------------------------------------//
    private fun evaluateStringConst(value: String) =
            codegen.staticData.kotlinStringLiteral(value)

    /**
     * Normalizing nans to single value is useful for build reproducibility.
     *
     * It's possible that it can lead to some bad consequences for interop libraries,
     * for which exact nan value is important. We are not aware of the existence of
     * any such useful library, at least on priority targets.
     *
     * On the other side, the semantics of exact cases, NaN values should be not normalized, is unclear.
     * E.g., in previous implementation, storing constant to another constant could change the exact bit pattern.
     *
     * So for now, we would just normalize all NaN constants. At least this leads to predictable result
     * useful in almost all cases.
     *
     * Also, java.lang classes are used here to avoid unexpected NaN values if a compiler and stdlib
     * are built in an arm64 architecture environment.
     */
    private fun Float.normalizeNan() = if (isNaN()) java.lang.Float.NaN else this
    private fun Double.normalizeNan() = if (isNaN()) java.lang.Double.NaN else this

    private fun evaluateConst(value: IrConst): ConstValue {
        context.log{"evaluateConst                  : ${ir2string(value)}"}
        return when (value.kind) {
            IrConstKind.Null -> constPointer(codegen.kNullObjHeaderPtr)
            IrConstKind.Boolean -> llvm.constInt1(value.value as Boolean)
            IrConstKind.Char -> llvm.constChar16(value.value as Char)
            IrConstKind.Byte -> llvm.constInt8(value.value as Byte)
            IrConstKind.Short -> llvm.constInt16(value.value as Short)
            IrConstKind.Int -> llvm.constInt32(value.value as Int)
            IrConstKind.Long -> llvm.constInt64(value.value as Long)
            IrConstKind.String -> evaluateStringConst(value.value as String)
            IrConstKind.Float -> llvm.constFloat32((value.value as Float).normalizeNan())
            IrConstKind.Double -> llvm.constFloat64((value.value as Double).normalizeNan())
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
        val symbols = context.symbols
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
                        value.toBoxCacheValue(generationState) ?: codegen.staticData.createConstKotlinObject(
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
                codegen.staticData.createConstKotlinArray(
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
                    val constructor = value.constructor.owner
                    val parameters = constructor.parameters.associateBy { it.name.toString() }
                    // support of initilaization of object in following case:
                    // open class Base(val field: ...)
                    // Child(val otherField: ...) : Base(constantValue)
                    //
                    //  Child(constantValue) could be initialized constantly. This is required for function references.
                    val delegatedCallConstants = constructor.loweredConstructorFunction?.body?.statements
                            ?.filterIsInstance<IrCall>()
                            ?.singleOrNull { it.origin == LOWERED_DELEGATING_CONSTRUCTOR_CALL }
                            ?.getArgumentsWithIr()
                            ?.filter { it.second is IrConstantValue }
                            ?.associate { it.first.name.toString() to it.second }
                            .orEmpty()
                    fields.map { field ->
                        val init = if (field.isConst) {
                            field.irField!!.initializer?.expression.also {
                                require(field.name !in parameters) {
                                    "Constant field ${field.name} of class ${constructedClass.name} shouldn't be a constructor parameter"
                                }
                            }
                        } else {
                            val index = parameters[field.name]?.indexInParameters
                            if (index != null)
                                value.valueArguments[index]
                            else
                                delegatedCallConstants[field.name]
                        }
                        when (init) {
                            is IrConst -> evaluateConst(init)
                            is IrConstantValue -> evaluateConstantValue(init)
                            null -> error("Bad statically initialized object: field ${field.name} value not set in ${constructedClass.name}")
                            else -> error("Unexpected constant initializer type: ${init::class}")
                        }
                    }.also {
                        require(it.size == value.valueArguments.size + fields.count { it.isConst } + delegatedCallConstants.size) {
                            "Bad statically initialized object of class ${constructedClass.name}: not all arguments are used"
                        }
                    }
                }

                require(value.type.toLLVMType(llvm) == codegen.kObjHeaderPtr) { "Constant object is not an object, but ${value.type.render()}" }
                codegen.staticData.createConstKotlinObject(
                        constructedClass,
                        *fields.toTypedArray()
                )
            }
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

    private inner class InlinedBlockScope(val inlinedBlock: IrInlinedFunctionBlock) : FileScope(file = null, inlinedBlock.inlinedFunctionFileEntry) {

        private val inlineFunctionScope: DIScopeOpaqueRef? by lazy {
            val owner = inlinedBlock.inlinedFunctionSymbol?.owner
            if (owner == null) {
                @Suppress("UNCHECKED_CAST")
                return@lazy debugInfo.diFunctionScope(
                        inlinedBlock.inlinedFunctionFileEntry,
                        name = "<inlined-lambda>",
                        linkageName = "<inlined-lambda>",
                        inlinedBlock.inlinedFunctionFileEntry.line(inlinedBlock.inlinedFunctionStartOffset),
                        debugInfo.subroutineType(debugInfo.llvmTargetData, listOf(inlinedBlock.type)),
                        nodebug = false,
                        isTransparentStepping = false
                ) as DIScopeOpaqueRef
            }

            require(owner is IrSimpleFunction) { "Inline constructors should've been lowered: ${owner.render()}" }
            owner.scope(fileEntry().line(inlinedBlock.inlinedFunctionStartOffset))
        }

        override fun location(offset: Int): LocationInfo? {
            val diScope = inlineFunctionScope ?: return null
            val inlinedAt = outerContext.location(inlinedBlock.startOffset) ?: return null
            val (line, column) = fileEntry.lineAndColumn(offset)
            return LocationInfo(diScope, line, column, inlinedAt)
        }

        override fun scope(): DIScopeOpaqueRef? {
            return inlineFunctionScope.takeIf { context.shouldContainLocationDebugInfo() && inlinedBlock.startOffset != UNDEFINED_OFFSET }
        }

        override fun wrapException(e: Exception): NativeCodeGeneratorException {
            return NativeCodeGeneratorException.wrap(e, inlinedBlock.inlinedFunctionSymbol?.owner)
        }
    }

    //-------------------------------------------------------------------------//
    private inner class ReturnableBlockScope(val returnableBlock: IrReturnableBlock, val resultSlot: LLVMValueRef?) : InnerScopeImpl() {

        var bbExit : LLVMBasicBlockRef? = null
        var resultPhi : LLVMValueRef? = null

        private fun getExit(): LLVMBasicBlockRef {
            val location = returnableBlock.statements.lastOrNull()?.let {
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
    }

    //-------------------------------------------------------------------------//

    private inline fun <R> usingFileScope(fileEntry: IrFileEntry?, block: () -> R): R {
        val fileScope = if (fileEntry != null && fileEntry != fileEntry()) {
            FileScope(null, fileEntry)
        } else {
            null
        }
        return using(fileScope, block)
    }

    private open inner class FileScope(private val file: IrFile?, val fileEntry: IrFileEntry) : InnerScopeImpl() {
        override fun fileScope(): CodeContext? = this

        override fun location(offset: Int) = scope()?.let {
            val (line, column) = fileEntry.lineAndColumn(offset)
            LocationInfo(it, line, column)
        }

        @Suppress("UNCHECKED_CAST")
        private val scope by lazy {
            if (!context.shouldContainLocationDebugInfo())
                return@lazy null
            fileEntry.diFileScope() as DIScopeOpaqueRef?
        }

        override fun scope() = scope

        override fun wrapException(e: Exception) = NativeCodeGeneratorException.wrap(e, file)
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
        override fun wrapException(e: Exception) = NativeCodeGeneratorException.wrap(e, clazz)
    }

    //-------------------------------------------------------------------------//
    private fun evaluateReturnableBlock(value: IrReturnableBlock, resultSlot: LLVMValueRef?): LLVMValueRef {
        context.log{"evaluateReturnableBlock         : ${value.statements.forEach { ir2string(it) }}"}

        val returnableBlockScope = ReturnableBlockScope(value, resultSlot)
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

    private fun evaluateInlinedBlock(value: IrInlinedFunctionBlock, resultSlot: LLVMValueRef?): LLVMValueRef {
        context.log{"evaluateInlinedBlock         : ${value.statements.forEach { ir2string(it) }}"}

        val inlinedBlockScope = InlinedBlockScope(value)
        generateDebugTrambolineIf("inline", value)

        return using(inlinedBlockScope) {
            evaluateContainerExpression(value, resultSlot)
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
    private fun evaluateCall(value: IrCall, resultSlot: LLVMValueRef?): LLVMValueRef {
        context.log{"evaluateCall                   : ${ir2string(value)}"}

        intrinsicGenerator.tryEvaluateSpecialCall(value, resultSlot)?.let { return it }

        val args = evaluateExplicitArgs(value)

        updateBuilderDebugLocation(value)
        return evaluateFunctionCall(value, args, resultLifetime(value), resultSlot)
    }

    //-------------------------------------------------------------------------//
    private fun fileEntry(): IrFileEntry = (currentCodeContext.fileScope() as FileScope).fileEntry

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
    private fun IrElement.startLine() = fileEntry().line(this.startOffset)

    //-------------------------------------------------------------------------//
    private fun IrElement.startLineAndColumn() = fileEntry().lineAndColumn(this.startOffset)

    //-------------------------------------------------------------------------//
    private fun IrElement.endLineAndColumn() = fileEntry().lineAndColumn(this.endOffset)

    //-------------------------------------------------------------------------//
    private fun debugFieldDeclaration(expression: IrField) {
        val scope = currentCodeContext.classScope() as? ClassScope ?: return
        if (!scope.isExported || !context.shouldContainDebugInfo()) return
        with(debugInfo) {
            val fileEntry = fileEntry()
            val sizeInBits = expression.type.size
            scope.offsetInBits += sizeInBits
            val alignInBits = expression.type.alignment
            scope.offsetInBits = alignTo(scope.offsetInBits, alignInBits)
            @Suppress("UNCHECKED_CAST")
            scope.members.add(DICreateMemberType(
                    refBuilder = builder,
                    refScope = scope.scope as DIScopeOpaqueRef,
                    name = expression.computeSymbolName(),
                    file = fileEntry.diFileScope(),
                    lineNum = expression.startLine(),
                    sizeInBits = sizeInBits,
                    alignInBits = alignInBits,
                    offsetInBits = scope.offsetInBits,
                    flags = 0,
                    type = expression.type.diType(codegen.llvmTargetData)
            )!!)
        }
    }

    private fun IrFileEntry.diFileScope() = with(debugInfo) { diFileScope() }

    // Saved calculated IrFunction scope which is used several time for getting locations and generating debug info.
    private var irFunctionSavedScope: Pair<IrSimpleFunction, DIScopeOpaqueRef?>? = null

    private fun IrSimpleFunction.scope(): DIScopeOpaqueRef? = if (startOffset != UNDEFINED_OFFSET) (
            if (irFunctionSavedScope != null && this == irFunctionSavedScope!!.first)
                irFunctionSavedScope!!.second
            else
                this.scope(startLine()).also { irFunctionSavedScope = Pair(this, it) }
            ) else null

    private val IrSimpleFunction.isReifiedInline:Boolean
        get() = isInline && typeParameters.any { it.isReified }

    @Suppress("UNCHECKED_CAST")
    private fun IrSimpleFunction.scope(startLine:Int): DIScopeOpaqueRef? {
        if (!context.shouldContainLocationDebugInfo())
            return null

        val functionLlvmValue = when {
            isReifiedInline -> null
            // TODO: May be tie up inline lambdas to their outer function?
            codegen.isExternal(this) && !KonanBinaryInterface.isExported(this) -> null
            isSuspend -> this.getOrCreateFunctionWithContinuationStub(context).let { codegen.llvmFunctionOrNull(it) }
            else -> codegen.llvmFunctionOrNull(this)
        }
        return with(debugInfo) {
            val f = this@scope
            val nodebug = f.originalConstructor != null && f.parentAsClass.isSubclassOf(context.irBuiltIns.throwableClass.owner)
            if (functionLlvmValue != null) {
                subprograms.getOrPut(functionLlvmValue) {
                    // Also enable transparent stepping if this function is a bridge:
                    val isTransparentStepping = generationState.config.enableDebugTransparentStepping && f.bridgeTarget != null

                    diFunctionScope(fileEntry(), functionLlvmValue.name!!, startLine, nodebug, isTransparentStepping).also {
                        if (!this@scope.isInline)
                            functionLlvmValue.addDebugInfoSubprogram(it)
                    }
                } as DIScopeOpaqueRef
            } else {
                inlinedSubprograms.getOrPut(this@scope) {
                    diFunctionScope(fileEntry(), "<inlined-out:$name>", startLine, nodebug)
                } as DIScopeOpaqueRef
            }
        }

    }

    @Suppress("UNCHECKED_CAST")
    private fun LlvmCallable.scope(startLine: Int, subroutineType: DISubroutineTypeRef, nodebug: Boolean) =
            with(debugInfo) {
                subprograms.getOrPut(this@scope) {
                    diFunctionScope(fileEntry(), name!!, name!!, startLine, subroutineType, nodebug).also {
                        this@scope.addDebugInfoSubprogram(it)
                    }
                } as DIScopeOpaqueRef
            }

    private fun IrSimpleFunction.returnsUnit() = returnType.isUnit().also {
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
        val explicitParametersCount = expression.symbol.owner.parameters.size
        if (result.size != explicitParametersCount) {
            error("Number of arguments explicitly represented in the IR ${result.size} differs from expected " +
                    "$explicitParametersCount in ${ir2string(expression)}")
        }
        return result
    }

    //-------------------------------------------------------------------------//

    private fun evaluateRawFunctionReference(expression: IrRawFunctionReference): LLVMValueRef {
        require(expression.type.getClass()?.symbol?.hasEqualFqName(InteropFqNames.cPointer.toSafe()) == true) {
            "Raw reference should be ${InteropFqNames.cPointer}, ${expression.type.render()} found"
        }
        val function = expression.symbol.owner
        require(function is IrSimpleFunction) {
            "Raw reference can't be for constructor: ${expression.render()}"
        }
        require(function.dispatchReceiverParameter == null) {
            "Raw reference can't be for member function: ${expression.render()}"
        }
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
                functionGenerationContext.indirectBr(suspensionPointId, resumePoints)
            }
            return result
        }
    }

    private inner class SuspensionPointScope(val suspensionPointId: IrVariable,
                                             val bbResume: LLVMBasicBlockRef,
                                             val bbResumeId: Int): InnerScopeImpl() {
        override fun genGetValue(value: IrValueDeclaration, resultSlot: LLVMValueRef?): LLVMValueRef {
            if (value == suspensionPointId) {
                return functionGenerationContext.blockAddress(bbResume)
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

    private fun evaluateFileGlobalInitializerCall(fileInitializer: IrSimpleFunction) = with(functionGenerationContext) {
        val statePtr = getGlobalInitStateFor(fileInitializer.parent as IrDeclarationContainer)
        val initializerPtr = with(codegen) { fileInitializer.llvmFunction.asCallback() }

        val bbInit = basicBlock("label_init", null)
        val bbExit = basicBlock("label_continue", null)
        moveBlockAfterEntry(bbExit)
        moveBlockAfterEntry(bbInit)
        val state = load(llvm.intptrType, statePtr, memoryOrder = LLVMAtomicOrdering.LLVMAtomicOrderingAcquire)
        condBr(icmpEq(state, llvm.intptr(FILE_INITIALIZED)), bbExit, bbInit)
        positionAtEnd(bbInit)
        call(llvm.callInitGlobalPossiblyLock, listOf(statePtr, initializerPtr),
                exceptionHandler = currentCodeContext.exceptionHandler)
        br(bbExit)
        positionAtEnd(bbExit)
        codegen.theUnitInstanceRef.llvm
    }

    private fun evaluateFileThreadLocalInitializerCall(fileInitializer: IrSimpleFunction) = with(functionGenerationContext) {
        val globalStatePtr = getGlobalInitStateFor(fileInitializer.parent as IrDeclarationContainer)
        val localState = getThreadLocalInitStateFor(fileInitializer.parent as IrDeclarationContainer)
        val localStatePtr = localState.getAddress(functionGenerationContext)
        val initializerPtr = with(codegen) { fileInitializer.llvmFunction.asCallback() }

        val bbInit = basicBlock("label_init", null)
        val bbCheckLocalState = basicBlock("label_check_local", null)
        val bbExit = basicBlock("label_continue", null)
        moveBlockAfterEntry(bbExit)
        moveBlockAfterEntry(bbCheckLocalState)
        moveBlockAfterEntry(bbInit)
        val globalState = load(llvm.intptrType, globalStatePtr)
        LLVMSetVolatile(globalState, 1)
        // Make sure we're not in the middle of global initializer invocation -
        // thread locals can be initialized only after all shared globals have been initialized.
        condBr(icmpNe(globalState, llvm.intptr(FILE_INITIALIZED)), bbExit, bbCheckLocalState)
        positionAtEnd(bbCheckLocalState)
        condBr(icmpNe(load(llvm.intptrType, localStatePtr), llvm.intptr(FILE_INITIALIZED)), bbInit, bbExit)
        positionAtEnd(bbInit)
        call(llvm.callInitThreadLocal, listOf(globalStatePtr, localStatePtr, initializerPtr),
                exceptionHandler = currentCodeContext.exceptionHandler)
        br(bbExit)
        positionAtEnd(bbExit)
        codegen.theUnitInstanceRef.llvm
    }

    private fun evaluateFileStandaloneThreadLocalInitializerCall(fileInitializer: IrSimpleFunction) = with(functionGenerationContext) {
        val state = getThreadLocalInitStateFor(fileInitializer.parent as IrDeclarationContainer)
        val statePtr = state.getAddress(functionGenerationContext)
        val initializerPtr = with(codegen) { fileInitializer.llvmFunction.asCallback() }

        val bbInit = basicBlock("label_init", null)
        val bbExit = basicBlock("label_continue", null)
        moveBlockAfterEntry(bbExit)
        moveBlockAfterEntry(bbInit)
        condBr(icmpEq(load(llvm.intptrType, statePtr), llvm.intptr(FILE_INITIALIZED)), bbExit, bbInit)
        positionAtEnd(bbInit)
        call(llvm.callInitThreadLocal, listOf(llvm.kNullIntptrPtr, statePtr, initializerPtr),
                exceptionHandler = currentCodeContext.exceptionHandler)
        br(bbExit)
        positionAtEnd(bbExit)
        codegen.theUnitInstanceRef.llvm
    }

    //-------------------------------------------------------------------------//

    private fun evaluateSimpleFunctionCall(
            function: IrSimpleFunction, args: List<LLVMValueRef>,
            resultLifetime: Lifetime, superClass: IrClass? = null, resultSlot: LLVMValueRef? = null): LLVMValueRef {
        //context.log{"evaluateSimpleFunctionCall : $tmpVariableName = ${ir2string(value)}"}
        if (superClass == null && function.isOverridable)
            return callVirtual(function, args, resultLifetime, resultSlot)
        else
            return callDirect(function, args, resultLifetime, resultSlot)
    }

    //-------------------------------------------------------------------------//
    private fun resultLifetime(callee: IrElement): Lifetime {
        return lifetimes.getOrElse(callee) { /* TODO: make IRRELEVANT */ Lifetime.GLOBAL }
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
                LlvmFunctionSignature(LlvmRetType(llvm.int8PtrType, isObjectType = false)),
                origin = FunctionOrigin.OwnedBy(irClass),
                linkage = LLVMLinkage.LLVMExternalLinkage,
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
                    val shouldUseUnsignedComparison = function.parameters[0].type.isChar()
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
                                    context.symbols.throwIllegalArgumentExceptionWithMessage.owner,
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

    fun callDirect(function: IrSimpleFunction, args: List<LLVMValueRef>, resultLifetime: Lifetime, resultSlot: LLVMValueRef?): LLVMValueRef {
        val functionDeclarations = codegen.llvmFunction(function.target)
        return call(function, functionDeclarations, args, resultLifetime, resultSlot)
    }

    //-------------------------------------------------------------------------//

    fun callVirtual(function: IrSimpleFunction, args: List<LLVMValueRef>, resultLifetime: Lifetime, resultSlot: LLVMValueRef?): LLVMValueRef {
        val functionDeclarations = codegen.getVirtualFunctionTrampoline(function)
        return call(function, functionDeclarations, args, resultLifetime, resultSlot)
    }

    //-------------------------------------------------------------------------//

    private val IrSimpleFunction.needsNativeThreadState: Boolean
        get() {
            // We assume that call site thread state switching is required for interop calls only.
            val result = origin == CBridgeOrigin.KOTLIN_TO_C_BRIDGE
            if (result) {
                check(isExternal)
                check(!annotations.hasAnnotation(KonanFqNames.gcUnsafeCall))
                check(annotations.hasAnnotation(RuntimeNames.filterExceptions))
            }
            return result
        }

    private fun call(function: IrSimpleFunction, llvmCallable: LlvmCallable, args: List<LLVMValueRef>,
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

    private fun appendLlvmUsed(name: String, args: List<LLVMValueRef>) {
        if (args.isEmpty()) return

        val argsCasted = args.map { constPointer(it).bitcast(llvm.int8PtrType) }
        val llvmUsedGlobal = codegen.staticData.placeGlobalArray(name, llvm.int8PtrType, argsCasted)

        LLVMSetLinkage(llvmUsedGlobal.llvmGlobal, LLVMLinkage.LLVMAppendingLinkage)
        LLVMSetSection(llvmUsedGlobal.llvmGlobal, "llvm.metadata")
    }

    // Globals set this way cannot be const, but are overridable when producing final executable.
    private fun overrideRuntimeGlobal(name: String, value: ConstValue) =
            codegen.replaceExternalWeakOrCommonGlobalFromNativeRuntime(name, value)

    private fun overrideRuntimeGlobals() {
        if (!context.config.isFinalBinary)
            return

        overrideRuntimeGlobal("Kotlin_gcMutatorsCooperate", llvm.constInt32(if (context.config.gcMutatorsCooperate) 1 else 0))
        overrideRuntimeGlobal("Kotlin_auxGCThreads", llvm.constInt32(context.config.auxGCThreads.toInt()))
        overrideRuntimeGlobal("Kotlin_concurrentMarkMaxIterations", llvm.constInt32(context.config.concurrentMarkMaxIterations.toInt()))
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
        overrideRuntimeGlobal("Kotlin_CoreSymbolication_useOnlyKotlinImage",
                llvm.constInt32(if (context.config.coreSymbolicationUseOnlyKotlinImage) 1 else 0))
        if (context.config.target.family == Family.ANDROID && context.config.produce == CompilerOutputKind.PROGRAM) {
            val configuration = context.config.configuration
            val programType = configuration.get(BinaryOptions.androidProgramType) ?: AndroidProgramType.Default
            overrideRuntimeGlobal("Kotlin_printToAndroidLogcat", llvm.constInt32(if (programType.consolePrintsToLogcat) 1 else 0))
        }
        overrideRuntimeGlobal("Kotlin_appStateTracking", llvm.constInt32(context.config.appStateTracking.value))
        overrideRuntimeGlobal("Kotlin_objcDisposeOnMain", llvm.constInt32(if (context.config.objcDisposeOnMain) 1 else 0))
        overrideRuntimeGlobal("Kotlin_objcDisposeWithRunLoop", llvm.constInt32(if (context.config.objcDisposeWithRunLoop) 1 else 0))
        overrideRuntimeGlobal("Kotlin_enableSafepointSignposts", llvm.constInt32(if (context.config.enableSafepointSignposts) 1 else 0))
        overrideRuntimeGlobal("Kotlin_globalDataLazyInit", llvm.constInt32(if (context.config.globalDataLazyInit) 1 else 0))
        overrideRuntimeGlobal("Kotlin_swiftExport", llvm.constInt32(if (context.config.swiftExport) 1 else 0))
        overrideRuntimeGlobal("Kotlin_latin1Strings", llvm.constInt32(if (context.config.latin1Strings) 1 else 0))
        overrideRuntimeGlobal("Kotlin_mmapTag", llvm.constUInt8(context.config.mmapTag))
        val minidumpLocation = context.config.minidumpLocation?.let {
            llvm.staticData.cStringLiteral(it)
        } ?: constValue(llvm.kNullInt8Ptr)
        overrideRuntimeGlobal("Kotlin_minidumpLocation", minidumpLocation)
    }

    //-------------------------------------------------------------------------//
    // Create type { i32, void ()*, i8* }

    val kCtorType = llvm.structType(llvm.int32Type, pointerType(ctorFunctionSignature.llvmFunctionType), llvm.int8PtrType)

    //-------------------------------------------------------------------------//
    // Create object { i32, void ()*, i8* } { i32 1, void ()* @ctorFunction, i8* null }

    fun createGlobalCtor(ctorFunction: LlvmCallable): ConstPointer {
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
        val argList = cValuesOf(priority, ctorFunction.toConstPointer().llvm, data)
        val ctorItem = LLVMConstNamedStruct(kCtorType, argList, 3)!!
        return constPointer(ctorItem)
    }

    //-------------------------------------------------------------------------//
    fun appendStaticInitializers() {
        // Note: the list of libraries is topologically sorted (in order for initializers to be called correctly).
        val dependencies = (generationState.dependenciesTracker.allBitcodeDependencies + listOf(null)/* Null for "current" non-library module */)

        val libraryToInitializers = dependencies.associate { it?.library to mutableListOf<RuntimeInitializer>() }

        llvm.irStaticInitializers.forEach {
            val library = it.konanLibrary
            val initializers = libraryToInitializers[library]
                    ?: error("initializer for not included library ${library?.libraryFile}")

            initializers.add(it.runtimeInitializer)
        }

        fun fileCtorName(libraryName: String, fileName: String) = "$libraryName:$fileName".moduleConstructorName

        fun ctorProto(ctorName: String): LlvmFunctionProto {
            return ctorFunctionSignature.toProto(ctorName, null, LLVMLinkage.LLVMExternalLinkage)
        }

        val ctorFunctions = dependencies.flatMap { dependency ->
            val library = dependency?.library
            val initializer = mergeRuntimeInitializers(libraryToInitializers.getValue(library))
                    ?.let { createInitCtor(createInitNode(it)) }

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

                listOf(
                    appendStaticInitializers(ctorProto(ctorName), listOfNotNull(initializer) + otherInitializers)
                )
            } else {
                // A cached library.
                check(initializer == null) {
                    "found initializer from ${library.libraryFile}, which is not included into compilation"
                }

                val cache = context.config.cachedLibraries.getLibraryCache(library)
                        ?: error("Library ${library.libraryFile} is expected to be cached")

                when (cache) {
                    is CachedLibraries.Cache.Monolithic -> listOf(ctorProto(ctorName))
                    is CachedLibraries.Cache.PerFile -> {
                        val files = when (dependency.kind) {
                            is DependenciesTracker.DependencyKind.WholeModule -> {
                                val fileIdProvider: FileIdProvider = context.moduleDeserializerProvider.getDeserializerOrNull(library)
                                        ?.let { FileIdProvider(it) }
                                        ?: error("Can't find deserializer for ${library.libraryFile}")
                                fileIdProvider.sortedFileIds
                            }
                            is DependenciesTracker.DependencyKind.CertainFiles ->
                                dependency.kind.files
                        }
                        files.map { ctorProto(fileCtorName(library.uniqueName, it)) }
                    }
                }.map {
                    codegen.addFunction(it)
                }
            }
        }

        appendGlobalCtors(ctorFunctions)
    }

    private fun appendStaticInitializers(ctorCallableProto: LlvmFunctionProto, initializers: List<LlvmCallable>) : LlvmCallable {
        return generateFunctionNoRuntime(codegen, ctorCallableProto) {
            val initGuardName = function.name.orEmpty() + "_guard"
            val initGuard = LLVMAddGlobal(llvm.module, llvm.int32Type, initGuardName)
            LLVMSetInitializer(initGuard, llvm.kImmInt32Zero)
            LLVMSetLinkage(initGuard, LLVMLinkage.LLVMPrivateLinkage)
            val bbInited = basicBlock("inited", null)
            val bbNeedInit = basicBlock("need_init", null)


            val value = LLVMBuildLoad2(builder, llvm.int32Type, initGuard, "")!!
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

    private fun appendGlobalCtors(ctorFunctions: List<LlvmCallable>) {
        if (context.config.isFinalBinary) {
            // Generate function calling all [ctorFunctions].
            val ctorProto = ctorFunctionSignature.toProto(
                    name = "_Konan_constructors",
                    origin = null,
                    linkage = if (context.config.produce == CompilerOutputKind.PROGRAM) LLVMLinkage.LLVMExternalLinkage else LLVMLinkage.LLVMPrivateLinkage
            )
            val globalCtorCallable = generateFunctionNoRuntime(codegen, ctorProto) {
                ctorFunctions.forEach {
                    call(it, emptyList(), Lifetime.IRRELEVANT,
                            exceptionHandler = ExceptionHandler.Caller, verbatim = true)
                }
                ret(null)
            }

            // Append initializers of global variables in "llvm.global_ctors" array.
            val globalCtors = codegen.staticData.placeGlobalArray("llvm.global_ctors", kCtorType,
                    listOf(createGlobalCtor(globalCtorCallable)))
            LLVMSetLinkage(globalCtors.llvmGlobal, LLVMLinkage.LLVMAppendingLinkage)
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
    setRuntimeConstGlobal("Kotlin_disableMmap", llvm.constInt32(if (config.disableMmap) 1 else 0))

    val runtimeLogs = ConstArray(llvm.int32Type, LoggingTag.entries.sortedBy { it.ord }.map {
        config.runtimeLogs[it]!!.ord.let { llvm.constInt32(it) }
    })
    setRuntimeConstGlobal("Kotlin_runtimeLogs", runtimeLogs)
    setRuntimeConstGlobal("Kotlin_concurrentWeakSweep", llvm.constInt32(if (context.config.concurrentWeakSweep) 1 else 0))
    setRuntimeConstGlobal("Kotlin_gcMarkSingleThreaded", llvm.constInt32(if (config.gcMarkSingleThreaded) 1 else 0))
    setRuntimeConstGlobal("Kotlin_fixedBlockPageSize", llvm.constInt32(config.fixedBlockPageSize.toInt()))
    setRuntimeConstGlobal("Kotlin_pagedAllocator", llvm.constInt32(if (config.pagedAllocator) 1 else 0))

    return llvmModule
}
