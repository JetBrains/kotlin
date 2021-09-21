/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.backend.common.COROUTINE_SUSPENDED_NAME
import org.jetbrains.kotlin.backend.common.ir.Ir
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.enumEntries
import org.jetbrains.kotlin.backend.konan.descriptors.kotlinNativeInternal
import org.jetbrains.kotlin.backend.konan.llvm.findMainEntryPoint
import org.jetbrains.kotlin.backend.konan.lower.TestProcessor
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrBuiltInsOverDescriptors
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrEnumEntrySymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.ReferenceSymbolTable
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import kotlin.properties.Delegates

object KonanNameConventions {
    val setWithoutBoundCheck = Name.special("<setWithoutBoundCheck>")
    val getWithoutBoundCheck = Name.special("<getWithoutBoundCheck>")
}

// This is what Context collects about IR.
internal class KonanIr(context: Context, irModule: IrModuleFragment): Ir<Context>(context, irModule) {
    override var symbols: KonanSymbols by Delegates.notNull()
}

internal class KonanSymbols(
        context: Context,
        irBuiltIns: IrBuiltIns,
        private val symbolTable: SymbolTable,
        lazySymbolTable: ReferenceSymbolTable
): Symbols<Context>(context, irBuiltIns, symbolTable) {

    val entryPoint = findMainEntryPoint(context)?.let { symbolTable.referenceSimpleFunction(it) }

    override val externalSymbolTable = lazySymbolTable

    val nothing get() = irBuiltIns.nothingClass
    val throwable get() = irBuiltIns.throwableClass
    val enum get() = irBuiltIns.enumClass
    val nativePtr = symbolTable.referenceClass(context.nativePtr)
    val nativePointed = symbolTable.referenceClass(context.interopBuiltIns.nativePointed)
    val nativePtrType = nativePtr.typeWith(arguments = emptyList())
    val nonNullNativePtr = symbolTable.referenceClass(context.nonNullNativePtr)
    val nonNullNativePtrType = nonNullNativePtr.typeWith(arguments = emptyList())

    val immutableBlobOf = symbolTable.referenceSimpleFunction(context.immutableBlobOf)

    val signedIntegerClasses = setOf(byte, short, int, long)
    val unsignedIntegerClasses = setOf(uByte!!, uShort!!, uInt!!, uLong!!)

    val allIntegerClasses = signedIntegerClasses + unsignedIntegerClasses

    val unsignedToSignedOfSameBitWidth = unsignedIntegerClasses.associate {
        it to when (it) {
            uByte -> byte
            uShort -> short
            uInt -> int
            uLong -> long
            else -> error(it.toString())
        }
    }

    val integerConversions = allIntegerClasses.flatMap { fromClass ->
        allIntegerClasses.map { toClass ->
            val name = Name.identifier("to${toClass.descriptor.name.asString().replaceFirstChar(Char::uppercaseChar)}")
            val symbol = if (fromClass in signedIntegerClasses && toClass in unsignedIntegerClasses) {
                irBuiltIns.findFunctions(name, "kotlin")
                        .single {
                            it.descriptor.dispatchReceiverParameter == null &&
                                    it.descriptor.extensionReceiverParameter?.type == fromClass.descriptor.defaultType &&
                                    it.descriptor.valueParameters.isEmpty()
                        }
            } else {
                irBuiltIns.findBuiltInClassMemberFunctions(fromClass, name)
                        .single {
                            it.descriptor.extensionReceiverParameter == null && it.descriptor.valueParameters.isEmpty()
                        }
            }

            (fromClass to toClass) to symbol
        }
    }.toMap()

    val arrayList = symbolTable.referenceClass(getArrayListClassDescriptor(context))

    val symbolName = topLevelClass(RuntimeNames.symbolNameAnnotation)
    val gcUnsafeCall = topLevelClass(KonanFqNames.gcUnsafeCall)
    val filterExceptions = topLevelClass(RuntimeNames.filterExceptions)
    val exportForCppRuntime = topLevelClass(RuntimeNames.exportForCppRuntime)

    val objCMethodImp = symbolTable.referenceClass(context.interopBuiltIns.objCMethodImp)

    val processUnhandledException = irBuiltIns.findFunctions(Name.identifier("processUnhandledException"), "kotlin", "native").single()
    val terminateWithUnhandledException = irBuiltIns.findFunctions(Name.identifier("terminateWithUnhandledException"), "kotlin", "native").single()

    val interopNativePointedGetRawPointer =
            symbolTable.referenceSimpleFunction(context.interopBuiltIns.nativePointedGetRawPointer)

    val interopCPointer = symbolTable.referenceClass(context.interopBuiltIns.cPointer)
    val interopCstr = symbolTable.referenceSimpleFunction(context.interopBuiltIns.cstr.getter!!)
    val interopWcstr = symbolTable.referenceSimpleFunction(context.interopBuiltIns.wcstr.getter!!)
    val interopMemScope = symbolTable.referenceClass(context.interopBuiltIns.memScope)
    val interopCValue = symbolTable.referenceClass(context.interopBuiltIns.cValue)
    val interopCValues = symbolTable.referenceClass(context.interopBuiltIns.cValues)
    val interopCValuesRef = symbolTable.referenceClass(context.interopBuiltIns.cValuesRef)
    val interopCValueWrite = symbolTable.referenceSimpleFunction(context.interopBuiltIns.cValueWrite)
    val interopCValueRead = symbolTable.referenceSimpleFunction(context.interopBuiltIns.cValueRead)
    val interopAllocType = symbolTable.referenceSimpleFunction(context.interopBuiltIns.allocType)

    val interopTypeOf = symbolTable.referenceSimpleFunction(context.interopBuiltIns.typeOf)

    val interopCPointerGetRawValue = symbolTable.referenceSimpleFunction(context.interopBuiltIns.cPointerGetRawValue)

    val interopAllocObjCObject = symbolTable.referenceSimpleFunction(context.interopBuiltIns.allocObjCObject)

    val interopForeignObjCObject = interopClass("ForeignObjCObject")

    // These are possible supertypes of forward declarations - we need to reference them explicitly to force their deserialization.
    // TODO: Do it lazily.
    val interopCOpaque = symbolTable.referenceClass(context.interopBuiltIns.cOpaque)
    val interopObjCObject = symbolTable.referenceClass(context.interopBuiltIns.objCObject)
    val interopObjCObjectBase = symbolTable.referenceClass(context.interopBuiltIns.objCObjectBase)

    val interopObjCRelease = interopFunction("objc_release")

    val interopObjCRetain = interopFunction("objc_retain")

    val interopObjcRetainAutoreleaseReturnValue = interopFunction("objc_retainAutoreleaseReturnValue")

    val interopCreateObjCObjectHolder = interopFunction("createObjCObjectHolder")

    val interopCreateKotlinObjectHolder = interopFunction("createKotlinObjectHolder")
    val interopUnwrapKotlinObjectHolderImpl = interopFunction("unwrapKotlinObjectHolderImpl")

    val interopCreateObjCSuperStruct = interopFunction("createObjCSuperStruct")

    val interopGetMessenger = interopFunction("getMessenger")
    val interopGetMessengerStret = interopFunction("getMessengerStret")

    val interopGetObjCClass = symbolTable.referenceSimpleFunction(context.interopBuiltIns.getObjCClass)

    val interopObjCObjectSuperInitCheck =
            symbolTable.referenceSimpleFunction(context.interopBuiltIns.objCObjectSuperInitCheck)

    val interopObjCObjectInitBy = symbolTable.referenceSimpleFunction(context.interopBuiltIns.objCObjectInitBy)

    val interopObjCObjectRawValueGetter =
            symbolTable.referenceSimpleFunction(context.interopBuiltIns.objCObjectRawPtr)

    val interopNativePointedRawPtrGetter =
            symbolTable.referenceSimpleFunction(context.interopBuiltIns.nativePointedRawPtrGetter)

    val interopCPointerRawValue =
            symbolTable.referenceProperty(context.interopBuiltIns.cPointerRawValue)

    val interopInterpretObjCPointer =
            symbolTable.referenceSimpleFunction(context.interopBuiltIns.interpretObjCPointer)

    val interopInterpretObjCPointerOrNull =
            symbolTable.referenceSimpleFunction(context.interopBuiltIns.interpretObjCPointerOrNull)

    val interopInterpretNullablePointed =
            symbolTable.referenceSimpleFunction(context.interopBuiltIns.interpretNullablePointed)

    val interopInterpretCPointer =
            symbolTable.referenceSimpleFunction(context.interopBuiltIns.interpretCPointer)

    val interopCreateNSStringFromKString =
            symbolTable.referenceSimpleFunction(context.interopBuiltIns.CreateNSStringFromKString)

    val createForeignException = interopFunction("CreateForeignException")

    val interopObjCGetSelector = interopFunction("objCGetSelector")

    val interopCEnumVar = interopClass("CEnumVar")

    val nativeMemUtils = symbolTable.referenceClass(context.interopBuiltIns.nativeMemUtils)

    val nativeHeap = symbolTable.referenceClass(context.interopBuiltIns.nativeHeap)

    val interopGetPtr = symbolTable.referenceSimpleFunction(context.interopBuiltIns.interopGetPtr)

    val interopManagedGetPtr = symbolTable.referenceSimpleFunction(context.interopBuiltIns.interopManagedGetPtr)

    val interopManagedType = symbolTable.referenceClass(context.interopBuiltIns.managedType)
    val interopCPlusPlusClass = symbolTable.referenceClass(context.interopBuiltIns.cPlusPlusClass)
    val interopSkiaRefCnt = symbolTable.referenceClass(context.interopBuiltIns.skiaRefCnt)

    val readBits = interopFunction("readBits")
    val writeBits = interopFunction("writeBits")

    val objCExportTrapOnUndeclaredException =
            symbolTable.referenceSimpleFunction(context.builtIns.kotlinNativeInternal.getContributedFunctions(
                    Name.identifier("trapOnUndeclaredException"),
                    NoLookupLocation.FROM_BACKEND
            ).single())

    val objCExportResumeContinuation = internalFunction("resumeContinuation")
    val objCExportResumeContinuationWithException = internalFunction("resumeContinuationWithException")
    val objCExportGetCoroutineSuspended = internalFunction("getCoroutineSuspended")
    val objCExportInterceptedContinuation = internalFunction("interceptedContinuation")

    val getNativeNullPtr = symbolTable.referenceSimpleFunction(context.getNativeNullPtr)

    val boxCachePredicates = BoxCache.values().associate {
        it to internalFunction("in${it.name.lowercase().replaceFirstChar(Char::uppercaseChar)}BoxCache")
    }

    val boxCacheGetters = BoxCache.values().associate {
        it to internalFunction("getCached${it.name.lowercase().replaceFirstChar(Char::uppercaseChar)}Box")
    }

    val immutableBlob = irBuiltIns.findClass(Name.identifier("ImmutableBlob"), "kotlin", "native")!!

    val executeImpl =
            irBuiltIns.findFunctions(Name.identifier("executeImpl"),"kotlin", "native", "concurrent").single()

    val createCleaner =
            irBuiltIns.findFunctions(Name.identifier("createCleaner"),"kotlin", "native", "internal").single()

    val areEqualByValue = context.getKonanInternalFunctions("areEqualByValue").map {
        symbolTable.referenceSimpleFunction(it)
    }.associateBy { it.descriptor.valueParameters[0].type.computePrimitiveBinaryTypeOrNull()!! }

    val reinterpret = internalFunction("reinterpret")

    val ieee754Equals = context.getKonanInternalFunctions("ieee754Equals").map {
        symbolTable.referenceSimpleFunction(it)
    }

    val equals = context.builtIns.any.unsubstitutedMemberScope
            .getContributedFunctions(Name.identifier("equals"), NoLookupLocation.FROM_BACKEND)
            .single().let { symbolTable.referenceSimpleFunction(it) }

    val throwArithmeticException = internalFunction("ThrowArithmeticException")

    val throwIndexOutOfBoundsException = internalFunction("ThrowIndexOutOfBoundsException")

    override val throwNullPointerException = internalFunction("ThrowNullPointerException")

    val throwNoWhenBranchMatchedException = internalFunction("ThrowNoWhenBranchMatchedException")

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

    override val defaultConstructorMarker = symbolTable.referenceClass(
            context.getKonanInternalClass("DefaultConstructorMarker")
    )

    val checkProgressionStep = context.getKonanInternalFunctions("checkProgressionStep")
            .map { Pair(it.returnType, symbolTable.referenceSimpleFunction(it)) }.toMap()
    val getProgressionLast = context.getKonanInternalFunctions("getProgressionLast")
            .map { Pair(it.returnType, symbolTable.referenceSimpleFunction(it)) }.toMap()

    private fun arrayToExtensionSymbolMap(name: String, filter: (FunctionDescriptor) -> Boolean = { true }) = arrays.associateWith {
        findArrayExtension(it, name, filter)
    }

    val arrayContentToString = arrayToExtensionSymbolMap("contentToString") {
        it.extensionReceiverParameter?.type?.isMarkedNullable == false
    }
    val arrayContentHashCode = arrayToExtensionSymbolMap("contentHashCode") {
        it.extensionReceiverParameter?.type?.isMarkedNullable == false
    }
    val arrayContentEquals = arrayToExtensionSymbolMap("contentEquals") {
        it.extensionReceiverParameter?.type?.isMarkedNullable == false
    }

    override val arraysContentEquals by lazy { arrayContentEquals.mapKeys { it.key.defaultType } }

    private fun findArrayExtension(classSymbol: IrClassSymbol, name: String, filter: (FunctionDescriptor) -> Boolean): IrSimpleFunctionSymbol =
            irBuiltIns.findFunctions(Name.identifier(name), "kotlin", "collections")
                    .singleOrNull {
                        it.descriptor.let {
                            it.extensionReceiverParameter?.type?.constructor?.declarationDescriptor == classSymbol.descriptor
                                    && !it.isExpect
                                    && filter(it)
                        }
                    } ?: error(classSymbol.toString())


    fun getNoParamFunction(name: Name, receiverType: KotlinType): IrFunctionSymbol {
        val descriptor = receiverType.memberScope.getContributedFunctions(name, NoLookupLocation.FROM_BACKEND)
                .first { it.valueParameters.isEmpty() }
        return symbolTable.referenceFunction(descriptor)
    }
    
    val copyInto = arrayToExtensionSymbolMap("copyInto")

    val arrayGet = arrays.associateWith { it.descriptor.unsubstitutedMemberScope
            .getContributedFunctions(Name.identifier("get"), NoLookupLocation.FROM_BACKEND)
            .single().let { symbolTable.referenceSimpleFunction(it) } }

    val arraySet = arrays.associateWith { it.descriptor.unsubstitutedMemberScope
                    .getContributedFunctions(Name.identifier("set"), NoLookupLocation.FROM_BACKEND)
                    .single().let { symbolTable.referenceSimpleFunction(it) } }


    val arraySize = arrays.associateWith { it.descriptor.unsubstitutedMemberScope
                    .getContributedVariables(Name.identifier("size"), NoLookupLocation.FROM_BACKEND)
                    .single().let { symbolTable.referenceSimpleFunction(it.getter!!) } }


    val valuesForEnum = internalFunction("valuesForEnum")

    val valueOfForEnum = internalFunction("valueOfForEnum")

    val createUninitializedInstance = internalFunction("createUninitializedInstance")

    val initInstance = internalFunction("initInstance")

    val freeze = irBuiltIns.findFunctions(Name.identifier("freeze"), "kotlin", "native", "concurrent").single()

    val println = irBuiltIns.findFunctions(Name.identifier("println"), "kotlin", "io")
            .single { it.descriptor.valueParameters.singleOrNull()?.type == (irBuiltIns as IrBuiltInsOverDescriptors).builtIns.stringType }

    override val getContinuation = internalFunction("getContinuation")

    override val returnIfSuspended = internalFunction("returnIfSuspended")

    val coroutineLaunchpad = internalFunction("coroutineLaunchpad")

    override val suspendCoroutineUninterceptedOrReturn = internalFunction("suspendCoroutineUninterceptedOrReturn")

    private val coroutinesIntrinsicsPackage =
            context.builtIns.builtInsModule.getPackage(StandardNames.COROUTINES_INTRINSICS_PACKAGE_FQ_NAME).memberScope

    private val coroutinesPackage =
            context.builtIns.builtInsModule.getPackage(StandardNames.COROUTINES_PACKAGE_FQ_NAME).memberScope

    override val coroutineContextGetter = symbolTable.referenceSimpleFunction(
            coroutinesPackage
                    .getContributedVariables(Name.identifier("coroutineContext"), NoLookupLocation.FROM_BACKEND)
                    .single()
                    .getter!!)

    override val coroutineGetContext = internalFunction("getCoroutineContext")

    override val coroutineImpl get() = TODO()

    val baseContinuationImpl =
            irBuiltIns.findClass(Name.identifier("BaseContinuationImpl"), "kotlin", "coroutines", "native", "internal")!!

    val restrictedContinuationImpl =
            irBuiltIns.findClass(Name.identifier("RestrictedContinuationImpl"), "kotlin", "coroutines", "native", "internal")!!

    val continuationImpl = irBuiltIns.findClass(Name.identifier("ContinuationImpl"), "kotlin", "coroutines", "native", "internal")!!

    val invokeSuspendFunction =
            symbolTable.referenceSimpleFunction(
                    baseContinuationImpl.descriptor.unsubstitutedMemberScope
                            .getContributedFunctions(Name.identifier("invokeSuspend"), NoLookupLocation.FROM_BACKEND)
                            .single()
            )

    override val coroutineSuspendedGetter = symbolTable.referenceSimpleFunction(
            coroutinesIntrinsicsPackage
                    .getContributedVariables(COROUTINE_SUSPENDED_NAME, NoLookupLocation.FROM_BACKEND)
                    .filterNot { it.isExpect }.single().getter!!
    )

    val cancellationException = topLevelClass(KonanFqNames.cancellationException)

    val kotlinResult = irBuiltIns.findClass(Name.identifier("Result"))!!

    val kotlinResultGetOrThrow = irBuiltIns.findFunctions(Name.identifier("getOrThrow"))
            .single {
                it.descriptor.extensionReceiverParameter?.type?.constructor?.declarationDescriptor == kotlinResult.descriptor
            }

    override val functionAdapter = symbolTable.referenceClass(context.getKonanInternalClass("FunctionAdapter"))

    val refClass = symbolTable.referenceClass(context.getKonanInternalClass("Ref"))

    val kFunctionImpl =  symbolTable.referenceClass(context.reflectionTypes.kFunctionImpl)
    val kSuspendFunctionImpl =  symbolTable.referenceClass(context.reflectionTypes.kSuspendFunctionImpl)

    val kMutableProperty0 = symbolTable.referenceClass(context.reflectionTypes.kMutableProperty0)
    val kMutableProperty1 = symbolTable.referenceClass(context.reflectionTypes.kMutableProperty1)
    val kMutableProperty2 = symbolTable.referenceClass(context.reflectionTypes.kMutableProperty2)

    val kProperty0Impl = symbolTable.referenceClass(context.reflectionTypes.kProperty0Impl)
    val kProperty1Impl = symbolTable.referenceClass(context.reflectionTypes.kProperty1Impl)
    val kProperty2Impl = symbolTable.referenceClass(context.reflectionTypes.kProperty2Impl)
    val kMutableProperty0Impl = symbolTable.referenceClass(context.reflectionTypes.kMutableProperty0Impl)
    val kMutableProperty1Impl = symbolTable.referenceClass(context.reflectionTypes.kMutableProperty1Impl)
    val kMutableProperty2Impl = symbolTable.referenceClass(context.reflectionTypes.kMutableProperty2Impl)

    val kLocalDelegatedPropertyImpl = symbolTable.referenceClass(context.reflectionTypes.kLocalDelegatedPropertyImpl)
    val kLocalDelegatedMutablePropertyImpl = symbolTable.referenceClass(context.reflectionTypes.kLocalDelegatedMutablePropertyImpl)

    val typeOf = symbolTable.referenceSimpleFunction(context.reflectionTypes.typeOf)

    val kType = symbolTable.referenceClass(context.reflectionTypes.kType)
    val kVariance = symbolTable.referenceClass(context.reflectionTypes.kVariance)
    val kVarianceIn = getKVarianceEnumEntry("IN")
    val kVarianceOut = getKVarianceEnumEntry("OUT")
    val kVarianceInvariant = getKVarianceEnumEntry("INVARIANT")

    private fun getKVarianceEnumEntry(name: String): IrEnumEntrySymbol {
        val descriptor = context.reflectionTypes.kVariance.enumEntries.single { it.name.asString() == name }
        return symbolTable.referenceEnumEntry(descriptor)
    }

    val getClassTypeInfo = internalFunction("getClassTypeInfo")
    val getObjectTypeInfo = internalFunction("getObjectTypeInfo")
    val kClassImpl = internalClass("KClassImpl")
    val kClassImplConstructor by lazy { kClassImpl.constructors.single() }
    val kClassUnsupportedImpl = internalClass("KClassUnsupportedImpl")
    val kClassUnsupportedImplConstructor by lazy { kClassUnsupportedImpl.constructors.single() }
    val kTypeParameterImpl = internalClass("KTypeParameterImpl")
    val kTypeImpl = internalClass("KTypeImpl")
    val kTypeImplForTypeParametersWithRecursiveBounds = internalClass("KTypeImplForTypeParametersWithRecursiveBounds")

    val kTypeProjection = symbolTable.referenceClass(context.reflectionTypes.kTypeProjection)

    private val kTypeProjectionCompanionDescriptor = context.reflectionTypes.kTypeProjection.companionObjectDescriptor!!

    val kTypeProjectionCompanion = symbolTable.referenceClass(kTypeProjectionCompanionDescriptor)

    val kTypeProjectionStar = symbolTable.referenceProperty(
            kTypeProjectionCompanionDescriptor.unsubstitutedMemberScope
                    .getContributedVariables(Name.identifier("STAR"), NoLookupLocation.FROM_BACKEND).single()
    )

    val kTypeProjectionFactories: Map<Variance, IrSimpleFunctionSymbol> = Variance.values().toList().associateWith {
        val factoryName = when (it) {
            Variance.INVARIANT -> "invariant"
            Variance.IN_VARIANCE -> "contravariant"
            Variance.OUT_VARIANCE -> "covariant"
        }

        symbolTable.referenceSimpleFunction(
                kTypeProjectionCompanionDescriptor.unsubstitutedMemberScope
                        .getContributedFunctions(Name.identifier(factoryName), NoLookupLocation.FROM_BACKEND).single()
        )
    }

    val emptyList = irBuiltIns.findFunctions(Name.identifier("emptyList"), "kotlin", "collections")
            .single { it.descriptor.valueParameters.isEmpty() }

    val listOf = irBuiltIns.findFunctions(Name.identifier("listOf"), "kotlin", "collections")
            .single { it.descriptor.valueParameters.size == 1 && it.descriptor.valueParameters[0].isVararg }

    val listOfInternal = internalFunction("listOfInternal")

    val threadLocal = symbolTable.referenceClass(
            context.builtIns.builtInsModule.findClassAcrossModuleDependencies(
                    ClassId.topLevel(KonanFqNames.threadLocal))!!)

    val sharedImmutable = symbolTable.referenceClass(
            context.builtIns.builtInsModule.findClassAcrossModuleDependencies(
                    ClassId.topLevel(KonanFqNames.sharedImmutable))!!)

    val eagerInitialization = symbolTable.referenceClass(
            context.builtIns.builtInsModule.findClassAcrossModuleDependencies(
                    ClassId.topLevel(KonanFqNames.eagerInitialization))!!)

    private fun topLevelClass(fqName: FqName): IrClassSymbol = irBuiltIns.findClass(fqName.shortName(), fqName.parent())!!

    private fun internalFunction(name: String): IrSimpleFunctionSymbol =
            symbolTable.referenceSimpleFunction(context.getKonanInternalFunctions(name).single())

    private fun internalClass(name: String): IrClassSymbol =
            symbolTable.referenceClass(context.getKonanInternalClass(name))

    private fun getKonanTestClass(className: String) =
            irBuiltIns.findClass(Name.identifier(className), "kotlin", "native", "internal", "test")!!

    private fun interopFunction(name: String) = symbolTable.referenceSimpleFunction(
            context.interopBuiltIns.packageScope
                    .getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BACKEND)
                    .single()
    )

    private fun interopClass(name: String) = symbolTable.referenceClass(
            context.interopBuiltIns.packageScope
                    .getContributedClassifier(Name.identifier(name), NoLookupLocation.FROM_BACKEND) as ClassDescriptor
    )

    fun kFunctionN(n: Int) = irBuiltIns.kFunctionN(n).symbol

    fun kSuspendFunctionN(n: Int) = irBuiltIns.kSuspendFunctionN(n).symbol

    fun getKFunctionType(returnType: IrType, parameterTypes: List<IrType>) =
            kFunctionN(parameterTypes.size).typeWith(parameterTypes + returnType)

    val baseClassSuite   = getKonanTestClass("BaseClassSuite")
    val topLevelSuite    = getKonanTestClass("TopLevelSuite")
    val testFunctionKind = getKonanTestClass("TestFunctionKind")

    override val getWithoutBoundCheckName: Name? = KonanNameConventions.getWithoutBoundCheck

    override val setWithoutBoundCheckName: Name? = KonanNameConventions.setWithoutBoundCheck

    private val testFunctionKindCache = TestProcessor.FunctionKind.values().associate {
        val symbol = if (it.runtimeKindString.isEmpty())
            null
        else
            symbolTable.referenceEnumEntry(testFunctionKind.descriptor.unsubstitutedMemberScope.getContributedClassifier(
                    Name.identifier(it.runtimeKindString), NoLookupLocation.FROM_BACKEND
            ) as ClassDescriptor)
        it to symbol
    }

    fun getTestFunctionKind(kind: TestProcessor.FunctionKind) = testFunctionKindCache[kind]!!
}

private fun getArrayListClassDescriptor(context: Context): ClassDescriptor {
    val module = context.builtIns.builtInsModule
    val pkg = module.getPackage(FqName.fromSegments(listOf("kotlin", "collections")))
    val classifier = pkg.memberScope.getContributedClassifier(Name.identifier("ArrayList"),
            NoLookupLocation.FROM_BACKEND)

    return classifier as ClassDescriptor
}
