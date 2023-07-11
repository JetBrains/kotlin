/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.backend.common.COROUTINE_SUSPENDED_NAME
import org.jetbrains.kotlin.backend.common.ir.Ir
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.lower.TestProcessor
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils

object KonanNameConventions {
    val setWithoutBoundCheck = Name.special("<setWithoutBoundCheck>")
    val getWithoutBoundCheck = Name.special("<getWithoutBoundCheck>")
}

internal interface SymbolLookupUtils {
    fun findMemberFunction(clazz: IrClassSymbol, name: Name): IrSimpleFunctionSymbol?
    fun findMemberProperty(clazz: IrClassSymbol, name: Name): IrPropertySymbol?
    fun findMemberPropertyGetter(clazz: IrClassSymbol, name: Name): IrSimpleFunctionSymbol?
    fun findPrimaryConstructor(clazz: IrClassSymbol): IrConstructorSymbol?
    fun findNoParametersConstructor(clazz: IrClassSymbol): IrConstructorSymbol?
    fun findNestedClass(clazz: IrClassSymbol, name: Name): IrClassSymbol?
    fun findGetter(property: IrPropertySymbol): IrSimpleFunctionSymbol?

    fun getName(clazz: IrClassSymbol): Name
    fun isExtensionReceiverClass(property: IrPropertySymbol, expected: IrClassSymbol?): Boolean
    fun isExtensionReceiverClass(function: IrFunctionSymbol, expected: IrClassSymbol?): Boolean
    fun isExtensionReceiverNullable(function: IrFunctionSymbol): Boolean?
    fun getValueParametersCount(function: IrFunctionSymbol): Int
    fun getTypeParametersCount(function: IrFunctionSymbol): Int
    fun isTypeParameterUpperBoundClass(property: IrPropertySymbol, index: Int, expected: IrClassSymbol): Boolean
    fun isValueParameterClass(function: IrFunctionSymbol, index: Int, expected: IrClassSymbol?): Boolean
    fun isReturnClass(function: IrFunctionSymbol, expected: IrClassSymbol): Boolean
    fun isValueParameterTypeArgumentClass(function: IrFunctionSymbol, index: Int, argumentIndex: Int, expected: IrClassSymbol?): Boolean
    fun isValueParameterNullable(function: IrFunctionSymbol, index: Int): Boolean?
    fun isExpect(function: IrFunctionSymbol): Boolean
    fun isSuspend(functionSymbol: IrFunctionSymbol): Boolean
    fun getVisibility(function: IrFunctionSymbol): DescriptorVisibility
    fun getValueParameterPrimitiveBinaryType(function: IrFunctionSymbol, index: Int): PrimitiveBinaryType?
}

// This is what Context collects about IR.
internal class KonanIr(context: Context, override val symbols: KonanSymbols): Ir<Context>(context)

internal class KonanSymbols(
        context: PhaseContext,
        val lookup: SymbolLookupUtils,
        irBuiltIns: IrBuiltIns,
        symbolTable: ReferenceSymbolTable,
): Symbols(irBuiltIns, symbolTable) {
    val entryPoint = run {
        val config = context.config.configuration
        if (config.get(KonanConfigKeys.PRODUCE) != CompilerOutputKind.PROGRAM) return@run null

        val entryPoint = FqName(config.get(KonanConfigKeys.ENTRY) ?: when (config.get(KonanConfigKeys.GENERATE_TEST_RUNNER)) {
            TestRunnerKind.MAIN_THREAD -> "kotlin.native.internal.test.main"
            TestRunnerKind.WORKER -> "kotlin.native.internal.test.worker"
            TestRunnerKind.MAIN_THREAD_NO_EXIT -> "kotlin.native.internal.test.mainNoExit"
            else -> "main"
        })

        val entryName = entryPoint.shortName()
        val packageName = entryPoint.parent()

        fun IrSimpleFunctionSymbol.isArrayStringMain() =
                lookup.getValueParametersCount(this) == 1 &&
                        lookup.isValueParameterClass(this, 0, array) &&
                        lookup.isValueParameterTypeArgumentClass(this, 0, 0, string)

        fun IrSimpleFunctionSymbol.isNoArgsMain() = lookup.getValueParametersCount(this) == 0

        val candidates = irBuiltIns.findFunctions(entryName, packageName)
                .filter {
                    lookup.isReturnClass(it, unit) &&
                            lookup.getTypeParametersCount(it) == 0 &&
                            lookup.getVisibility(it).isPublicAPI
                }

        val main = candidates.singleOrNull { it.isArrayStringMain() } ?: candidates.singleOrNull { it.isNoArgsMain() }
        if (main == null) context.reportCompilationError("Could not find '$entryName' in '$packageName' package.")
        if (lookup.isSuspend(main)) context.reportCompilationError("Entry point can not be a suspend function.")
        main
    }

    val nothing get() = irBuiltIns.nothingClass
    val throwable get() = irBuiltIns.throwableClass
    val enum get() = irBuiltIns.enumClass
    private val nativePtr = internalClass(NATIVE_PTR_NAME)
    val nativePointed = interopClass(InteropFqNames.nativePointedName)
    val nativePtrType = nativePtr.typeWith(arguments = emptyList())

    val immutableBlobOf = nativeFunction(IMMUTABLE_BLOB_OF)

    val signedIntegerClasses = setOf(byte, short, int, long)
    val unsignedIntegerClasses = setOf(uByte!!, uShort!!, uInt!!, uLong!!)

    val allIntegerClasses = signedIntegerClasses + unsignedIntegerClasses

    val unsignedToSignedOfSameBitWidth = unsignedIntegerClasses.associateWith {
        when (it) {
            uByte -> byte
            uShort -> short
            uInt -> int
            uLong -> long
            else -> error(it.toString())
        }
    }

    val integerConversions = allIntegerClasses.flatMap { fromClass ->
        allIntegerClasses.map { toClass ->
            val name = Name.identifier("to${lookup.getName(toClass).asString().replaceFirstChar(Char::uppercaseChar)}")
            val symbol = if (fromClass in signedIntegerClasses && toClass in unsignedIntegerClasses) {
                irBuiltIns.getNonBuiltInFunctionsByExtensionReceiver(name, "kotlin")[fromClass]!!
            } else {
                lookup.findMemberFunction(fromClass, name)!!
            }

            (fromClass to toClass) to symbol
        }
    }.toMap()

    val symbolName = topLevelClass(RuntimeNames.symbolNameAnnotation)
    val filterExceptions = topLevelClass(RuntimeNames.filterExceptions)
    val exportForCppRuntime = topLevelClass(RuntimeNames.exportForCppRuntime)
    val typedIntrinsic = topLevelClass(RuntimeNames.typedIntrinsicAnnotation)

    val objCMethodImp = interopClass(InteropFqNames.objCMethodImpName)

    val processUnhandledException = irBuiltIns.findFunctions(Name.identifier("processUnhandledException"), "kotlin", "native").single()
    val terminateWithUnhandledException = irBuiltIns.findFunctions(Name.identifier("terminateWithUnhandledException"), "kotlin", "native").single()

    val interopNativePointedGetRawPointer = interopFunctions(InteropFqNames.nativePointedGetRawPointerFunName).single {
        lookup.isExtensionReceiverClass(it, nativePointed)
    }

    val interopCPointer = interopClass(InteropFqNames.cPointerName)
    val interopCPointed = interopClass(InteropFqNames.cPointedName)
    val interopCstr = findTopLevelPropertyGetter(InteropFqNames.packageName, Name.identifier(InteropFqNames.cstrPropertyName), string)
    val interopWcstr = findTopLevelPropertyGetter(InteropFqNames.packageName, Name.identifier(InteropFqNames.wcstrPropertyName), string)
    val interopMemScope = interopClass(InteropFqNames.memScopeName)
    val interopCValue = interopClass(InteropFqNames.cValueName)
    val interopCValuesRef = interopClass(InteropFqNames.cValuesRefName)
    val interopCValueWrite = interopFunctions(InteropFqNames.cValueWriteFunName).single {
        lookup.isExtensionReceiverClass(it, interopCValue)
    }
    val interopCValueRead = interopFunctions(InteropFqNames.cValueReadFunName).single {
        lookup.getValueParametersCount(it) == 1
    }
    val interopAllocType = interopFunctions(InteropFqNames.allocTypeFunName).single {
        lookup.getTypeParametersCount(it) == 0
    }

    val interopTypeOf = interopFunction(InteropFqNames.typeOfFunName)

    val interopCPointerGetRawValue = interopFunctions(InteropFqNames.cPointerGetRawValueFunName).single {
        lookup.isExtensionReceiverClass(it, interopCPointer)
    }

    val interopAllocObjCObject = interopFunction(InteropFqNames.allocObjCObjectFunName)

    val interopForeignObjCObject = interopClass(InteropFqNames.foreignObjCObjectName)

    // These are possible supertypes of forward declarations - we need to reference them explicitly to force their deserialization.
    // TODO: Do it lazily.
    val interopCOpaque = interopClass(InteropFqNames.cOpaqueName)
    val interopObjCObject = interopClass(InteropFqNames.objCObjectName)
    val interopObjCObjectBase = interopClass(InteropFqNames.objCObjectBaseName)
    val interopObjCObjectBaseMeta = interopClass(InteropFqNames.objCObjectBaseMetaName)
    val interopObjCClass = interopClass(InteropFqNames.objCClassName)
    val interopObjCClassOf = interopClass(InteropFqNames.objCClassOfName)
    val interopObjCProtocol = interopClass(InteropFqNames.objCProtocolName)

    val interopObjCRelease = interopFunction("objc_release")

    val interopObjCRetain = interopFunction("objc_retain")

    val interopObjcRetainAutoreleaseReturnValue = interopFunction("objc_retainAutoreleaseReturnValue")

    val interopCreateObjCObjectHolder = interopFunction("createObjCObjectHolder")

    val interopCreateKotlinObjectHolder = interopFunction("createKotlinObjectHolder")
    val interopUnwrapKotlinObjectHolderImpl = interopFunction("unwrapKotlinObjectHolderImpl")

    val interopCreateObjCSuperStruct = interopFunction("createObjCSuperStruct")

    val interopGetMessenger = interopFunction("getMessenger")
    val interopGetMessengerStret = interopFunction("getMessengerStret")

    val interopGetObjCClass = interopFunction(InteropFqNames.getObjCClassFunName)
    val interopObjCObjectSuperInitCheck = interopFunction(InteropFqNames.objCObjectSuperInitCheckFunName)
    val interopObjCObjectInitBy = interopFunction(InteropFqNames.objCObjectInitByFunName)
    val interopObjCObjectRawValueGetter = interopFunction(InteropFqNames.objCObjectRawPtrFunName)

    val interopNativePointedRawPtrGetter = lookup.findMemberPropertyGetter(interopClass(InteropFqNames.nativePointedName), Name.identifier(InteropFqNames.nativePointedRawPtrPropertyName))!!

    val interopCPointerRawValue: IrPropertySymbol = lookup.findMemberProperty(interopClass(InteropFqNames.cPointerName), Name.identifier(InteropFqNames.cPointerRawValuePropertyName))!!

    val interopInterpretObjCPointer = interopFunction(InteropFqNames.interpretObjCPointerFunName)
    val interopInterpretObjCPointerOrNull = interopFunction(InteropFqNames.interpretObjCPointerOrNullFunName)
    val interopInterpretNullablePointed = interopFunction(InteropFqNames.interpretNullablePointedFunName)
    val interopInterpretCPointer = interopFunction(InteropFqNames.interpretCPointerFunName)

    val createForeignException = interopFunction("CreateForeignException")

    val interopCEnumVar = interopClass("CEnumVar")

    val nativeMemUtils = interopClass(InteropFqNames.nativeMemUtilsName)
    val nativeHeap = interopClass(InteropFqNames.nativeHeapName)

    val cStuctVar = interopClass(InteropFqNames.cStructVarName)
    val cStructVarConstructorSymbol = lookup.findPrimaryConstructor(cStuctVar)!!
    val managedTypeConstructor = lookup.findPrimaryConstructor(interopClass(InteropFqNames.managedTypeName))!!
    val structVarPrimaryConstructor = lookup.findPrimaryConstructor(lookup.findNestedClass(cStuctVar, Name.identifier(InteropFqNames.TypeName))!!)!!

    val interopGetPtr = findTopLevelPropertyGetter(InteropFqNames.packageName, Name.identifier("ptr")) {
        lookup.isTypeParameterUpperBoundClass(it, 0, interopCPointed)
    }

    val interopManagedType = interopClass(InteropFqNames.managedTypeName)

    val interopManagedGetPtr = findTopLevelPropertyGetter(InteropFqNames.packageName, Name.identifier("ptr")) {
        lookup.isTypeParameterUpperBoundClass(it, 0, cStuctVar) && lookup.isExtensionReceiverClass(it, interopManagedType)
    }

    val interopCPlusPlusClass = interopClass(InteropFqNames.cPlusPlusClassName)
    val interopSkiaRefCnt = interopClass(InteropFqNames.skiaRefCntName)

    val readBits = interopFunction("readBits")
    val writeBits = interopFunction("writeBits")

    val objCExportTrapOnUndeclaredException = internalFunction("trapOnUndeclaredException")
    val objCExportResumeContinuation = internalFunction("resumeContinuation")
    val objCExportResumeContinuationWithException = internalFunction("resumeContinuationWithException")
    val objCExportGetCoroutineSuspended = internalFunction("getCoroutineSuspended")
    val objCExportInterceptedContinuation = internalFunction("interceptedContinuation")

    val getNativeNullPtr = internalFunction("getNativeNullPtr")

    val boxCachePredicates = BoxCache.values().associateWith {
        internalFunction("in${it.name.lowercase().replaceFirstChar(Char::uppercaseChar)}BoxCache")
    }

    val boxCacheGetters = BoxCache.values().associateWith {
        internalFunction("getCached${it.name.lowercase().replaceFirstChar(Char::uppercaseChar)}Box")
    }

    val immutableBlob = irBuiltIns.findClass(Name.identifier("ImmutableBlob"), "kotlin", "native")!!

    val executeImpl =
            irBuiltIns.findFunctions(Name.identifier("executeImpl"),"kotlin", "native", "concurrent").single()

    val createCleaner =
            irBuiltIns.findFunctions(Name.identifier("createCleaner"),"kotlin", "native", "ref").single()

    // TODO: this is strange. It should be a map from IrClassSymbol
    val areEqualByValue = internalFunctions("areEqualByValue").associateBy {
        lookup.getValueParameterPrimitiveBinaryType(it, 0)!!
    }

    val reinterpret = internalFunction("reinterpret")

    val theUnitInstance = internalFunction("theUnitInstance")

    val ieee754Equals = internalFunctions("ieee754Equals")

    val equals = lookup.findMemberFunction(any, Name.identifier("equals"))!!

    val throwArithmeticException = internalFunction("ThrowArithmeticException")

    val throwIndexOutOfBoundsException = internalFunction("ThrowIndexOutOfBoundsException")

    override val throwNullPointerException = internalFunction("ThrowNullPointerException")

    val throwNoWhenBranchMatchedException = internalFunction("ThrowNoWhenBranchMatchedException")
    val throwIrLinkageError = internalFunction("ThrowIrLinkageError")

    override val throwTypeCastException = internalFunction("ThrowTypeCastException")

    override val throwKotlinNothingValueException  = internalFunction("ThrowKotlinNothingValueException")

    val throwClassCastException = internalFunction("ThrowClassCastException")

    val throwInvalidReceiverTypeException = internalFunction("ThrowInvalidReceiverTypeException")
    val throwIllegalStateException = internalFunction("ThrowIllegalStateException")
    val throwIllegalStateExceptionWithMessage = internalFunction("ThrowIllegalStateExceptionWithMessage")
    val throwIllegalArgumentException = internalFunction("ThrowIllegalArgumentException")
    val throwIllegalArgumentExceptionWithMessage = internalFunction("ThrowIllegalArgumentExceptionWithMessage")


    override val throwUninitializedPropertyAccessException = internalFunction("ThrowUninitializedPropertyAccessException")

    override val stringBuilder = irBuiltIns.findClass(Name.identifier("StringBuilder"),"kotlin", "text")!!

    override val defaultConstructorMarker = internalClass("DefaultConstructorMarker")

    private fun arrayToExtensionSymbolMap(name: String, filter: (IrFunctionSymbol) -> Boolean = { true }) =
            arrays.associateWith { classSymbol ->
                irBuiltIns.findFunctions(Name.identifier(name), "kotlin", "collections")
                        .singleOrNull { function ->
                            lookup.isExtensionReceiverClass(function, classSymbol) && !lookup.isExpect(function) && filter(function)
                        } ?: error("No function $name for $classSymbol")
            }

    val arrayContentToString = arrayToExtensionSymbolMap("contentToString") {
        lookup.isExtensionReceiverNullable(it) == true
    }
    val arrayContentHashCode = arrayToExtensionSymbolMap("contentHashCode") {
        lookup.isExtensionReceiverNullable(it) == true
    }
    val arrayContentEquals = arrayToExtensionSymbolMap("contentEquals") {
        lookup.isExtensionReceiverNullable(it) == true
    }

    override val arraysContentEquals by lazy { arrayContentEquals.mapKeys { it.key.defaultType } }

    val copyInto = arrayToExtensionSymbolMap("copyInto")
    val copyOf = arrayToExtensionSymbolMap("copyOf") { lookup.getValueParametersCount(it) == 0 }

    val arrayGet = arrays.associateWith { lookup.findMemberFunction(it, Name.identifier("get"))!! }

    val arraySet = arrays.associateWith { lookup.findMemberFunction(it, Name.identifier("set"))!! }

    val arraySize = arrays.associateWith { lookup.findMemberPropertyGetter(it, Name.identifier("size"))!! }

    val valuesForEnum = internalFunction("valuesForEnum")

    val valueOfForEnum = internalFunction("valueOfForEnum")

    val createEnumEntries = irBuiltIns.findFunctions(Name.identifier("enumEntries"), "kotlin", "enums")
            .single { lookup.getValueParametersCount(it) == 1 && lookup.isValueParameterClass(it, 0, array) }

    val enumEntriesInterface = irBuiltIns.findClass(Name.identifier("EnumEntries"), "kotlin", "enums")!!

    val createUninitializedInstance = internalFunction("createUninitializedInstance")

    val initInstance = internalFunction("initInstance")

    val isSubtype = internalFunction("isSubtype")

    val println = irBuiltIns.findFunctions(Name.identifier("println"), "kotlin", "io")
            .single { lookup.getValueParametersCount(it) == 1 && lookup.isValueParameterClass(it, 0, string) }

    override val getContinuation = internalFunction("getContinuation")

    override val continuationClass = irBuiltIns.findClass(Name.identifier("Continuation"), StandardNames.COROUTINES_PACKAGE_FQ_NAME)!!

    override val returnIfSuspended = internalFunction("returnIfSuspended")

    override val suspendCoroutineUninterceptedOrReturn = internalFunction("suspendCoroutineUninterceptedOrReturn")

    override val coroutineContextGetter =
            findTopLevelPropertyGetter(StandardNames.COROUTINES_PACKAGE_FQ_NAME, Name.identifier("coroutineContext"), null)

    override val coroutineGetContext = internalFunction("getCoroutineContext")

    override val coroutineImpl get() = TODO()

    val baseContinuationImpl = internalCoroutinesClass("BaseContinuationImpl")

    val restrictedContinuationImpl = internalCoroutinesClass("RestrictedContinuationImpl")

    val continuationImpl = internalCoroutinesClass("ContinuationImpl")

    val invokeSuspendFunction = lookup.findMemberFunction(baseContinuationImpl, Name.identifier("invokeSuspend"))!!

    override val coroutineSuspendedGetter =
            findTopLevelPropertyGetter(StandardNames.COROUTINES_INTRINSICS_PACKAGE_FQ_NAME, COROUTINE_SUSPENDED_NAME, null)

    val cancellationException = topLevelClass(KonanFqNames.cancellationException)

    val kotlinResult = irBuiltIns.findClass(Name.identifier("Result"))!!

    val kotlinResultGetOrThrow = irBuiltIns.findFunctions(Name.identifier("getOrThrow"))
            .single { lookup.isExtensionReceiverClass(it, kotlinResult) }

    override val functionAdapter = internalClass("FunctionAdapter")

    val refClass = internalClass("Ref")

    private fun reflectionClass(name: String) = irBuiltIns.findClass(Name.identifier(name), StandardNames.KOTLIN_REFLECT_FQ_NAME)!!

    val kFunctionImpl = internalClass("KFunctionImpl")
    val kFunctionDescription = internalClass("KFunctionDescription")
    val kSuspendFunctionImpl = internalClass("KSuspendFunctionImpl")

    val kMutableProperty0 = reflectionClass("KMutableProperty0")
    val kMutableProperty1 = reflectionClass("KMutableProperty1")
    val kMutableProperty2 = reflectionClass("KMutableProperty2")

    val kProperty0Impl = internalClass("KProperty0Impl")
    val kProperty1Impl = internalClass("KProperty1Impl")
    val kProperty2Impl = internalClass("KProperty2Impl")
    val kMutableProperty0Impl = internalClass("KMutableProperty0Impl")
    val kMutableProperty1Impl = internalClass("KMutableProperty1Impl")
    val kMutableProperty2Impl = internalClass("KMutableProperty2Impl")

    val kLocalDelegatedPropertyImpl = internalClass("KLocalDelegatedPropertyImpl")
    val kLocalDelegatedMutablePropertyImpl = internalClass("KLocalDelegatedMutablePropertyImpl")

    val kType = reflectionClass("KType")
    val getObjectTypeInfo = internalFunction("getObjectTypeInfo")
    val kClassImpl = internalClass("KClassImpl")
    val kClassImplConstructor = lookup.findPrimaryConstructor(kClassImpl)!!
    val kClassImplIntrinsicConstructor = lookup.findNoParametersConstructor(kClassImpl)!!
    val kClassUnsupportedImpl = internalClass("KClassUnsupportedImpl")
    val kTypeParameterImpl = internalClass("KTypeParameterImpl")
    val kTypeImpl = internalClass("KTypeImpl")
    val kTypeImplIntrinsicConstructor = lookup.findNoParametersConstructor(kTypeImpl)!!
    val kTypeImplForTypeParametersWithRecursiveBounds = internalClass("KTypeImplForTypeParametersWithRecursiveBounds")
    val kTypeProjectionList = internalClass("KTypeProjectionList")

    val threadLocal = topLevelClass(KonanFqNames.threadLocal)

    val sharedImmutable = topLevelClass(KonanFqNames.sharedImmutable)

    val eagerInitialization = topLevelClass(KonanFqNames.eagerInitialization)

    val enumVarConstructorSymbol = lookup.findPrimaryConstructor(interopClass(InteropFqNames.cEnumVarName))!!
    val primitiveVarPrimaryConstructor = lookup.findPrimaryConstructor(lookup.findNestedClass(interopClass(InteropFqNames.cPrimitiveVarName), Name.identifier(InteropFqNames.TypeName))!!)!!

    private fun topLevelClass(fqName: FqName): IrClassSymbol = irBuiltIns.findClass(fqName.shortName(), fqName.parent())!!

    private fun findTopLevelPropertyGetter(packageName: FqName, name: Name, extensionReceiverClass: IrClassSymbol?) =
            findTopLevelPropertyGetter(packageName, name) { lookup.isExtensionReceiverClass(it, extensionReceiverClass) }

    private fun findTopLevelPropertyGetter(packageName: FqName, name: Name, predicate: (IrPropertySymbol) -> Boolean) =
            lookup.findGetter(irBuiltIns.findProperties(name, packageName).single(predicate))!!

    private fun nativeFunction(name: String) =
            irBuiltIns.findFunctions(Name.identifier(name), KonanFqNames.packageName).single()

    private fun internalFunction(name: String) =
            irBuiltIns.findFunctions(Name.identifier(name), RuntimeNames.kotlinNativeInternalPackageName).single()

    private fun internalFunctions(name: String) =
            irBuiltIns.findFunctions(Name.identifier(name), RuntimeNames.kotlinNativeInternalPackageName).toList()

    private fun internalClass(name: String) =
            irBuiltIns.findClass(Name.identifier(name), RuntimeNames.kotlinNativeInternalPackageName)!!

    private fun internalCoroutinesClass(name: String) =
            irBuiltIns.findClass(Name.identifier(name), RuntimeNames.kotlinNativeCoroutinesInternalPackageName)!!

    private fun getKonanTestClass(className: String) =
            irBuiltIns.findClass(Name.identifier(className), "kotlin", "native", "internal", "test")!!

    private fun interopFunctions(name: String) =
            irBuiltIns.findFunctions(Name.identifier(name), InteropFqNames.packageName)

    private fun interopFunction(name: String) = interopFunctions(name).single()

    private fun interopClass(name: String) =
            irBuiltIns.findClass(Name.identifier(name), InteropFqNames.packageName)!!

    fun kFunctionN(n: Int) = irBuiltIns.kFunctionN(n).symbol

    fun kSuspendFunctionN(n: Int) = irBuiltIns.kSuspendFunctionN(n).symbol

    fun getKFunctionType(returnType: IrType, parameterTypes: List<IrType>) =
            kFunctionN(parameterTypes.size).typeWith(parameterTypes + returnType)

    val baseClassSuite   = getKonanTestClass("BaseClassSuite")
    val topLevelSuite    = getKonanTestClass("TopLevelSuite")
    val testFunctionKind = getKonanTestClass("TestFunctionKind")

    override val getWithoutBoundCheckName: Name? = KonanNameConventions.getWithoutBoundCheck

    override val setWithoutBoundCheckName: Name? = KonanNameConventions.setWithoutBoundCheck

    private val testFunctionKindCache by lazy {
        TestProcessor.FunctionKind.values().associateWith { kind ->
            if (kind.runtimeKindString.isEmpty())
                null
            else
                testFunctionKind.owner.declarations
                        .filterIsInstance<IrEnumEntry>()
                        .single { it.name == Name.identifier(kind.runtimeKindString) }
                        .symbol
        }
    }

    fun getTestFunctionKind(kind: TestProcessor.FunctionKind) = testFunctionKindCache[kind]!!
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
internal class SymbolOverDescriptorsLookupUtils(val symbolTable: SymbolTable) : SymbolLookupUtils {
    override fun findMemberFunction(clazz: IrClassSymbol, name: Name): IrSimpleFunctionSymbol? =
            // inspired by: irBuiltIns.findBuiltInClassMemberFunctions(this, name).singleOrNull()
            clazz.descriptor.unsubstitutedMemberScope.getContributedFunctions(name, NoLookupLocation.FROM_BACKEND)
                    .singleOrNull()
                    ?.let { symbolTable.descriptorExtension.referenceSimpleFunction(it) }

    override fun findMemberProperty(clazz: IrClassSymbol, name: Name): IrPropertySymbol? =
            clazz.descriptor.unsubstitutedMemberScope.getContributedVariables(name, NoLookupLocation.FROM_BACKEND)
                    .singleOrNull()
                    ?.let { symbolTable.descriptorExtension.referenceProperty(it) }

    override fun findMemberPropertyGetter(clazz: IrClassSymbol, name: Name): IrSimpleFunctionSymbol? =
            clazz.descriptor.unsubstitutedMemberScope.getContributedVariables(name, NoLookupLocation.FROM_BACKEND)
                    .singleOrNull()
                    ?.getter
                    ?.let { symbolTable.descriptorExtension.referenceSimpleFunction(it) }

    override fun getName(clazz: IrClassSymbol) = clazz.descriptor.name
    override fun isExtensionReceiverClass(property: IrPropertySymbol, expected: IrClassSymbol?): Boolean {
        return property.descriptor.extensionReceiverParameter?.type?.let { TypeUtils.getClassDescriptor(it) } == expected?.descriptor
    }

    override fun isExtensionReceiverClass(function: IrFunctionSymbol, expected: IrClassSymbol?): Boolean {
        return function.descriptor.extensionReceiverParameter?.type?.let { TypeUtils.getClassDescriptor(it) } == expected?.descriptor
    }

    override fun findGetter(property: IrPropertySymbol): IrSimpleFunctionSymbol = symbolTable.descriptorExtension.referenceSimpleFunction(property.descriptor.getter!!)

    override fun isExtensionReceiverNullable(function: IrFunctionSymbol): Boolean? {
        return function.descriptor.extensionReceiverParameter?.type?.isMarkedNullable
    }

    override fun getValueParametersCount(function: IrFunctionSymbol): Int = function.descriptor.valueParameters.size

    override fun getTypeParametersCount(function: IrFunctionSymbol): Int = function.descriptor.typeParameters.size

    private fun match(type: KotlinType?, symbol: IrClassSymbol?) =
            if (type == null)
                symbol == null
            else
                TypeUtils.getClassDescriptor(type) == symbol?.descriptor

    override fun isTypeParameterUpperBoundClass(property: IrPropertySymbol, index: Int, expected: IrClassSymbol): Boolean {
        return property.descriptor.typeParameters.getOrNull(index)?.upperBounds?.any { match(it, expected) } ?: false
    }

    override fun isValueParameterClass(function: IrFunctionSymbol, index: Int, expected: IrClassSymbol?): Boolean {
        return match(function.descriptor.valueParameters.getOrNull(index)?.type, expected)
    }

    override fun isReturnClass(function: IrFunctionSymbol, expected: IrClassSymbol): Boolean {
        return match(function.descriptor.returnType, expected)
    }

    override fun isValueParameterTypeArgumentClass(function: IrFunctionSymbol, index: Int, argumentIndex: Int, expected: IrClassSymbol?): Boolean {
        return match(function.descriptor.valueParameters.getOrNull(index)?.type?.arguments?.getOrNull(argumentIndex)?.type, expected)
    }

    override fun isValueParameterNullable(function: IrFunctionSymbol, index: Int): Boolean? {
        return function.descriptor.valueParameters.getOrNull(index)?.type?.isMarkedNullable
    }

    override fun isExpect(function: IrFunctionSymbol): Boolean = function.descriptor.isExpect

    override fun isSuspend(functionSymbol: IrFunctionSymbol): Boolean = functionSymbol.descriptor.isSuspend
    override fun getVisibility(function: IrFunctionSymbol): DescriptorVisibility = function.descriptor.visibility

    override fun findPrimaryConstructor(clazz: IrClassSymbol) = clazz.descriptor.unsubstitutedPrimaryConstructor?.let { symbolTable.descriptorExtension.referenceConstructor(it) }
    override fun findNoParametersConstructor(clazz: IrClassSymbol) = clazz.descriptor.constructors.singleOrNull { it.valueParameters.size == 0 }?.let { symbolTable.descriptorExtension.referenceConstructor(it) }

    override fun findNestedClass(clazz: IrClassSymbol, name: Name): IrClassSymbol? {
        val classDescriptor = clazz.descriptor.defaultType.memberScope.getContributedClassifier(name, NoLookupLocation.FROM_BUILTINS) as? ClassDescriptor
        return classDescriptor?.let {
            symbolTable.descriptorExtension.referenceClass(it)
        }
    }

    override fun getValueParameterPrimitiveBinaryType(function: IrFunctionSymbol, index: Int): PrimitiveBinaryType? {
        return function.descriptor.valueParameters[0].type.computePrimitiveBinaryTypeOrNull()
    }
}

internal class SymbolOverIrLookupUtils() : SymbolLookupUtils {
    override fun findMemberFunction(clazz: IrClassSymbol, name: Name): IrSimpleFunctionSymbol? =
            clazz.owner.findDeclaration<IrSimpleFunction> { it.name == name }?.symbol

    override fun findMemberProperty(clazz: IrClassSymbol, name: Name): IrPropertySymbol? =
            clazz.owner.findDeclaration<IrProperty> { it.name == name }?.symbol

    override fun findMemberPropertyGetter(clazz: IrClassSymbol, name: Name): IrSimpleFunctionSymbol? =
            clazz.owner.findDeclaration<IrProperty> { it.name == name }?.getter?.symbol

    override fun findPrimaryConstructor(clazz: IrClassSymbol): IrConstructorSymbol? = clazz.owner.primaryConstructor?.symbol
    override fun findNoParametersConstructor(clazz: IrClassSymbol): IrConstructorSymbol? = clazz.owner.constructors.singleOrNull { it.valueParameters.isEmpty() }?.symbol

    override fun findNestedClass(clazz: IrClassSymbol, name: Name): IrClassSymbol? {
        return clazz.owner.declarations.filterIsInstance<IrClass>().singleOrNull { it.name == name }?.symbol
    }

    override fun getName(clazz: IrClassSymbol): Name = clazz.owner.name

    override fun isExtensionReceiverClass(property: IrPropertySymbol, expected: IrClassSymbol?): Boolean {
        return property.owner.getter?.extensionReceiverParameter?.type?.classOrNull == expected
    }

    override fun isExtensionReceiverClass(function: IrFunctionSymbol, expected: IrClassSymbol?): Boolean {
        return function.owner.extensionReceiverParameter?.type?.classOrNull == expected
    }

    override fun findGetter(property: IrPropertySymbol): IrSimpleFunctionSymbol? = property.owner.getter?.symbol

    override fun isExtensionReceiverNullable(function: IrFunctionSymbol): Boolean? {
        return function.owner.extensionReceiverParameter?.type?.isMarkedNullable()
    }

    override fun getValueParametersCount(function: IrFunctionSymbol): Int = function.owner.valueParameters.size

    override fun getTypeParametersCount(function: IrFunctionSymbol): Int = function.owner.typeParameters.size

    override fun isTypeParameterUpperBoundClass(property: IrPropertySymbol, index: Int, expected: IrClassSymbol): Boolean {
        return property.owner.getter?.typeParameters?.getOrNull(index)?.superTypes?.any { it.classOrNull == expected } ?: false
    }

    override fun isValueParameterClass(function: IrFunctionSymbol, index: Int, expected: IrClassSymbol?): Boolean {
        return function.owner.valueParameters.getOrNull(index)?.type?.classOrNull == expected
    }

    override fun isReturnClass(function: IrFunctionSymbol, expected: IrClassSymbol): Boolean {
        return function.owner.returnType.classOrNull == expected
    }

    override fun isValueParameterTypeArgumentClass(function: IrFunctionSymbol, index: Int, argumentIndex: Int, expected: IrClassSymbol?): Boolean {
        val type = function.owner.valueParameters.getOrNull(index)?.type as? IrSimpleType ?: return false
        val argumentType = type.arguments.getOrNull(argumentIndex) as? IrSimpleType ?: return false
        return argumentType.classOrNull == expected
    }

    override fun isValueParameterNullable(function: IrFunctionSymbol, index: Int): Boolean? {
        return function.owner.valueParameters.getOrNull(index)?.type?.isMarkedNullable()
    }

    override fun isExpect(function: IrFunctionSymbol): Boolean = function.owner.isExpect

    override fun isSuspend(functionSymbol: IrFunctionSymbol): Boolean = functionSymbol.owner.isSuspend
    override fun getVisibility(function: IrFunctionSymbol): DescriptorVisibility = function.owner.visibility

    override fun getValueParameterPrimitiveBinaryType(function: IrFunctionSymbol, index: Int): PrimitiveBinaryType? {
        return function.owner.valueParameters[0].type.computePrimitiveBinaryTypeOrNull()
    }
}
