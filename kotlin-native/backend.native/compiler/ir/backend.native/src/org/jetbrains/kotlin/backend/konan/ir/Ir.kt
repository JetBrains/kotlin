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
import org.jetbrains.kotlin.backend.konan.llvm.findMainEntryPoint
import org.jetbrains.kotlin.backend.konan.lower.TestProcessor
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.ReferenceSymbolTable
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.findDeclaration
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object KonanNameConventions {
    val setWithoutBoundCheck = Name.special("<setWithoutBoundCheck>")
    val getWithoutBoundCheck = Name.special("<getWithoutBoundCheck>")
}

// This is what Context collects about IR.
internal class KonanIr(context: Context, override val symbols: KonanSymbols): Ir<Context>(context)

internal abstract class KonanSymbols(
        context: PhaseContext,
        descriptorsLookup: DescriptorsLookup,
        irBuiltIns: IrBuiltIns,
        internal val symbolTable: SymbolTable,
        lazySymbolTable: ReferenceSymbolTable
): Symbols(irBuiltIns, symbolTable) {
    internal abstract fun IrClassSymbol.findMemberSimpleFunction(name: Name): IrSimpleFunctionSymbol?
    internal abstract fun IrClassSymbol.findMemberPropertyGetter(name: Name): IrSimpleFunctionSymbol?

    val entryPoint = findMainEntryPoint(context, descriptorsLookup.builtIns)?.let { symbolTable.referenceSimpleFunction(it) }

    override val externalSymbolTable = lazySymbolTable

    val nothing get() = irBuiltIns.nothingClass
    val throwable get() = irBuiltIns.throwableClass
    val enum get() = irBuiltIns.enumClass
    val nativePtr = symbolTable.referenceClass(descriptorsLookup.nativePtr)
    val nativePointed = symbolTable.referenceClass(descriptorsLookup.interopBuiltIns.nativePointed)
    val nativePtrType = nativePtr.typeWith(arguments = emptyList())

    val immutableBlobOf = symbolTable.referenceSimpleFunction(descriptorsLookup.immutableBlobOf)

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
                irBuiltIns.getNonBuiltInFunctionsByExtensionReceiver(name, "kotlin")[fromClass]!!
            } else {
                fromClass.findMemberSimpleFunction(name)!!
            }

            (fromClass to toClass) to symbol
        }
    }.toMap()

    val symbolName = topLevelClass(RuntimeNames.symbolNameAnnotation)
    val filterExceptions = topLevelClass(RuntimeNames.filterExceptions)
    val exportForCppRuntime = topLevelClass(RuntimeNames.exportForCppRuntime)
    val typedIntrinsic = topLevelClass(RuntimeNames.typedIntrinsicAnnotation)

    val objCMethodImp = symbolTable.referenceClass(descriptorsLookup.interopBuiltIns.objCMethodImp)

    val processUnhandledException = irBuiltIns.findFunctions(Name.identifier("processUnhandledException"), "kotlin", "native").single()
    val terminateWithUnhandledException = irBuiltIns.findFunctions(Name.identifier("terminateWithUnhandledException"), "kotlin", "native").single()

    val interopNativePointedGetRawPointer =
            symbolTable.referenceSimpleFunction(descriptorsLookup.interopBuiltIns.nativePointedGetRawPointer)

    val interopCPointer = symbolTable.referenceClass(descriptorsLookup.interopBuiltIns.cPointer)
    val interopCstr = symbolTable.referenceSimpleFunction(descriptorsLookup.interopBuiltIns.cstr.getter!!)
    val interopWcstr = symbolTable.referenceSimpleFunction(descriptorsLookup.interopBuiltIns.wcstr.getter!!)
    val interopMemScope = symbolTable.referenceClass(descriptorsLookup.interopBuiltIns.memScope)
    val interopCValue = symbolTable.referenceClass(descriptorsLookup.interopBuiltIns.cValue)
    val interopCValuesRef = symbolTable.referenceClass(descriptorsLookup.interopBuiltIns.cValuesRef)
    val interopCValueWrite = symbolTable.referenceSimpleFunction(descriptorsLookup.interopBuiltIns.cValueWrite)
    val interopCValueRead = symbolTable.referenceSimpleFunction(descriptorsLookup.interopBuiltIns.cValueRead)
    val interopAllocType = symbolTable.referenceSimpleFunction(descriptorsLookup.interopBuiltIns.allocType)

    val interopTypeOf = symbolTable.referenceSimpleFunction(descriptorsLookup.interopBuiltIns.typeOf)

    val interopCPointerGetRawValue = symbolTable.referenceSimpleFunction(descriptorsLookup.interopBuiltIns.cPointerGetRawValue)

    val interopAllocObjCObject = symbolTable.referenceSimpleFunction(descriptorsLookup.interopBuiltIns.allocObjCObject)

    val interopForeignObjCObject = interopClass("ForeignObjCObject")

    // These are possible supertypes of forward declarations - we need to reference them explicitly to force their deserialization.
    // TODO: Do it lazily.
    val interopCOpaque = symbolTable.referenceClass(descriptorsLookup.interopBuiltIns.cOpaque)
    val interopObjCObject = symbolTable.referenceClass(descriptorsLookup.interopBuiltIns.objCObject)
    val interopObjCObjectBase = symbolTable.referenceClass(descriptorsLookup.interopBuiltIns.objCObjectBase)
    val interopObjCObjectBaseMeta = symbolTable.referenceClass(descriptorsLookup.interopBuiltIns.objCObjectBaseMeta)
    val interopObjCClass = symbolTable.referenceClass(descriptorsLookup.interopBuiltIns.objCClass)
    val interopObjCClassOf = symbolTable.referenceClass(descriptorsLookup.interopBuiltIns.objCClassOf)
    val interopObjCProtocol = symbolTable.referenceClass(descriptorsLookup.interopBuiltIns.objCProtocol)

    val interopObjCRelease = interopFunction("objc_release")

    val interopObjCRetain = interopFunction("objc_retain")

    val interopObjcRetainAutoreleaseReturnValue = interopFunction("objc_retainAutoreleaseReturnValue")

    val interopCreateObjCObjectHolder = interopFunction("createObjCObjectHolder")

    val interopCreateKotlinObjectHolder = interopFunction("createKotlinObjectHolder")
    val interopUnwrapKotlinObjectHolderImpl = interopFunction("unwrapKotlinObjectHolderImpl")

    val interopCreateObjCSuperStruct = interopFunction("createObjCSuperStruct")

    val interopGetMessenger = interopFunction("getMessenger")
    val interopGetMessengerStret = interopFunction("getMessengerStret")

    val interopGetObjCClass = symbolTable.referenceSimpleFunction(descriptorsLookup.interopBuiltIns.getObjCClass)

    val interopObjCObjectSuperInitCheck =
            symbolTable.referenceSimpleFunction(descriptorsLookup.interopBuiltIns.objCObjectSuperInitCheck)

    val interopObjCObjectInitBy = symbolTable.referenceSimpleFunction(descriptorsLookup.interopBuiltIns.objCObjectInitBy)

    val interopObjCObjectRawValueGetter =
            symbolTable.referenceSimpleFunction(descriptorsLookup.interopBuiltIns.objCObjectRawPtr)

    val interopNativePointedRawPtrGetter =
            symbolTable.referenceSimpleFunction(descriptorsLookup.interopBuiltIns.nativePointedRawPtrGetter)

    val interopCPointerRawValue =
            symbolTable.referenceProperty(descriptorsLookup.interopBuiltIns.cPointerRawValue)

    val interopInterpretObjCPointer =
            symbolTable.referenceSimpleFunction(descriptorsLookup.interopBuiltIns.interpretObjCPointer)

    val interopInterpretObjCPointerOrNull =
            symbolTable.referenceSimpleFunction(descriptorsLookup.interopBuiltIns.interpretObjCPointerOrNull)

    val interopInterpretNullablePointed =
            symbolTable.referenceSimpleFunction(descriptorsLookup.interopBuiltIns.interpretNullablePointed)

    val interopInterpretCPointer =
            symbolTable.referenceSimpleFunction(descriptorsLookup.interopBuiltIns.interpretCPointer)

    val createForeignException = interopFunction("CreateForeignException")

    val interopCEnumVar = interopClass("CEnumVar")

    val nativeMemUtils = symbolTable.referenceClass(descriptorsLookup.interopBuiltIns.nativeMemUtils)

    val nativeHeap = symbolTable.referenceClass(descriptorsLookup.interopBuiltIns.nativeHeap)

    val interopGetPtr = symbolTable.referenceSimpleFunction(descriptorsLookup.interopBuiltIns.interopGetPtr)

    val interopManagedGetPtr = symbolTable.referenceSimpleFunction(descriptorsLookup.interopBuiltIns.interopManagedGetPtr)

    val interopManagedType = symbolTable.referenceClass(descriptorsLookup.interopBuiltIns.managedType)
    val interopCPlusPlusClass = symbolTable.referenceClass(descriptorsLookup.interopBuiltIns.cPlusPlusClass)
    val interopSkiaRefCnt = symbolTable.referenceClass(descriptorsLookup.interopBuiltIns.skiaRefCnt)

    val readBits = interopFunction("readBits")
    val writeBits = interopFunction("writeBits")

    val objCExportTrapOnUndeclaredException = internalFunction("trapOnUndeclaredException")
    val objCExportResumeContinuation = internalFunction("resumeContinuation")
    val objCExportResumeContinuationWithException = internalFunction("resumeContinuationWithException")
    val objCExportGetCoroutineSuspended = internalFunction("getCoroutineSuspended")
    val objCExportInterceptedContinuation = internalFunction("interceptedContinuation")

    val getNativeNullPtr = symbolTable.referenceSimpleFunction(descriptorsLookup.getNativeNullPtr)

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

    val areEqualByValue = internalFunctions("areEqualByValue").associateBy {
        it.descriptor.valueParameters[0].type.computePrimitiveBinaryTypeOrNull()!!
    }

    val reinterpret = internalFunction("reinterpret")

    val theUnitInstance = internalFunction("theUnitInstance")

    val ieee754Equals = internalFunctions("ieee754Equals")

    val equals = any.findMemberSimpleFunction(Name.identifier("equals"))!!

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

    private fun arrayToExtensionSymbolMap(name: String, filter: (FunctionDescriptor) -> Boolean = { true }) =
            arrays.associateWith { classSymbol ->
                irBuiltIns.findFunctions(Name.identifier(name), "kotlin", "collections")
                        .singleOrNull { function ->
                            function.descriptor.let {
                                it.extensionReceiverParameter?.type?.constructor?.declarationDescriptor == classSymbol.descriptor
                                        && !it.isExpect
                                        && filter(it)
                            }
                        } ?: error(classSymbol.toString())
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

    val copyInto = arrayToExtensionSymbolMap("copyInto")
    val copyOf = arrayToExtensionSymbolMap("copyOf") { it.valueParameters.isEmpty() }

    val arrayGet = arrays.associateWith { it.findMemberSimpleFunction(Name.identifier("get"))!! }

    val arraySet = arrays.associateWith { it.findMemberSimpleFunction(Name.identifier("set"))!! }

    val arraySize = arrays.associateWith { it.findMemberPropertyGetter(Name.identifier("size"))!! }

    val valuesForEnum = internalFunction("valuesForEnum")

    val valueOfForEnum = internalFunction("valueOfForEnum")

    val createEnumEntries = irBuiltIns.findFunctions(Name.identifier("enumEntries"), "kotlin", "enums")
            .single { it.descriptor.valueParameters.singleOrNull()?.type?.constructor?.declarationDescriptor == array.descriptor }

    val enumEntriesInterface = irBuiltIns.findClass(Name.identifier("EnumEntries"), "kotlin", "enums")!!

    val createUninitializedInstance = internalFunction("createUninitializedInstance")

    val initInstance = internalFunction("initInstance")

    val println = irBuiltIns.findFunctions(Name.identifier("println"), "kotlin", "io")
            .single { it.descriptor.valueParameters.singleOrNull()?.type?.constructor?.declarationDescriptor == string.descriptor }

    override val getContinuation = internalFunction("getContinuation")

    override val continuationClass = irBuiltIns.findClass(Name.identifier("Continuation"), StandardNames.COROUTINES_PACKAGE_FQ_NAME)!!

    override val returnIfSuspended = internalFunction("returnIfSuspended")

    override val suspendCoroutineUninterceptedOrReturn = internalFunction("suspendCoroutineUninterceptedOrReturn")

    private val coroutinesIntrinsicsPackage =
            descriptorsLookup.builtIns.builtInsModule.getPackage(StandardNames.COROUTINES_INTRINSICS_PACKAGE_FQ_NAME).memberScope

    private val coroutinesPackage =
            descriptorsLookup.builtIns.builtInsModule.getPackage(StandardNames.COROUTINES_PACKAGE_FQ_NAME).memberScope

    override val coroutineContextGetter = symbolTable.referenceSimpleFunction(
            coroutinesPackage
                    .getContributedVariables(Name.identifier("coroutineContext"), NoLookupLocation.FROM_BACKEND)
                    .single()
                    .getter!!)

    override val coroutineGetContext = internalFunction("getCoroutineContext")

    override val coroutineImpl get() = TODO()

    val baseContinuationImpl = internalCoroutinesClass("BaseContinuationImpl")

    val restrictedContinuationImpl = internalCoroutinesClass("RestrictedContinuationImpl")

    val continuationImpl = internalCoroutinesClass("ContinuationImpl")

    val invokeSuspendFunction =
            baseContinuationImpl.findMemberSimpleFunction(Name.identifier("invokeSuspend"))!!

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
    val kClassImplConstructor by lazy { kClassImpl.constructors.single { it.descriptor.isPrimary } }
    val kClassImplIntrinsicConstructor by lazy { kClassImpl.constructors.single { it.descriptor.valueParameters.isEmpty() } }
    val kClassUnsupportedImpl = internalClass("KClassUnsupportedImpl")
    val kTypeParameterImpl = internalClass("KTypeParameterImpl")
    val kTypeImpl = internalClass("KTypeImpl")
    val kTypeImplIntrinsicConstructor by lazy { kTypeImpl.constructors.single { it.descriptor.valueParameters.isEmpty() } }
    val kTypeImplForTypeParametersWithRecursiveBounds = internalClass("KTypeImplForTypeParametersWithRecursiveBounds")
    val kTypeProjectionList = internalClass("KTypeProjectionList")

    val threadLocal = topLevelClass(KonanFqNames.threadLocal)

    val sharedImmutable = topLevelClass(KonanFqNames.sharedImmutable)

    val volatile = topLevelClass(KonanFqNames.volatile)

    val eagerInitialization = topLevelClass(KonanFqNames.eagerInitialization)

    val cStructVarConstructorSymbol = symbolTable.referenceConstructor(
            descriptorsLookup.interopBuiltIns.cStructVar.unsubstitutedPrimaryConstructor!!
    )
    val managedTypeConstructor = symbolTable.referenceConstructor(
            descriptorsLookup.interopBuiltIns.managedType.unsubstitutedPrimaryConstructor!!
    )
    val enumVarConstructorSymbol = symbolTable.referenceConstructor(
            descriptorsLookup.interopBuiltIns.cEnumVar.unsubstitutedPrimaryConstructor!!
    )
    val primitiveVarPrimaryConstructor = symbolTable.referenceConstructor(
            descriptorsLookup.interopBuiltIns.cPrimitiveVarType.unsubstitutedPrimaryConstructor!!)
    val structVarPrimaryConstructor = symbolTable.referenceConstructor(
            descriptorsLookup.interopBuiltIns.cStructVarType.unsubstitutedPrimaryConstructor!!)

    private fun topLevelClass(fqName: FqName): IrClassSymbol = irBuiltIns.findClass(fqName.shortName(), fqName.parent())!!

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

    private fun interopFunction(name: String) =
            irBuiltIns.findFunctions(Name.identifier(name), InteropFqNames.packageName).single()

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

internal class KonanSymbolsOverDescriptors(
        context: PhaseContext,
        descriptorsLookup: DescriptorsLookup,
        irBuiltIns: IrBuiltIns,
        symbolTable: SymbolTable,
        lazySymbolTable: ReferenceSymbolTable
) : KonanSymbols(context, descriptorsLookup, irBuiltIns, symbolTable, lazySymbolTable) {
    override fun IrClassSymbol.findMemberSimpleFunction(name: Name): IrSimpleFunctionSymbol? =
            // inspired by: irBuiltIns.findBuiltInClassMemberFunctions(this, name).singleOrNull()
            descriptor.unsubstitutedMemberScope.getContributedFunctions(name, NoLookupLocation.FROM_BACKEND)
                    .singleOrNull()
                    ?.let { symbolTable.referenceSimpleFunction(it) }

    override fun IrClassSymbol.findMemberPropertyGetter(name: Name): IrSimpleFunctionSymbol? =
            descriptor.unsubstitutedMemberScope.getContributedVariables(name, NoLookupLocation.FROM_BACKEND)
                    .singleOrNull()
                    ?.getter
                    ?.let { symbolTable.referenceSimpleFunction(it) }
}

internal class KonanSymbolsOverFir(
        context: PhaseContext,
        descriptorsLookup: DescriptorsLookup,
        irBuiltIns: IrBuiltIns,
        symbolTable: SymbolTable,
        lazySymbolTable: ReferenceSymbolTable
) : KonanSymbols(context, descriptorsLookup, irBuiltIns, symbolTable, lazySymbolTable) {
    override fun IrClassSymbol.findMemberSimpleFunction(name: Name): IrSimpleFunctionSymbol? =
            owner.findDeclaration<IrSimpleFunction> { it.name == name }?.symbol

    override fun IrClassSymbol.findMemberPropertyGetter(name: Name): IrSimpleFunctionSymbol? =
            owner.findDeclaration<IrProperty> { it.name == name }?.getter?.symbol
}
