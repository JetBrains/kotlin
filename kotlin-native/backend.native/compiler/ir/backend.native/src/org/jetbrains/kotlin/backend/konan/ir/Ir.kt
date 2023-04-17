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
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyProperty
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.ReferenceSymbolTable
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.findDeclaration
import org.jetbrains.kotlin.js.descriptorUtils.getKotlinTypeFqName
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.types.TypeUtils

object KonanNameConventions {
    val setWithoutBoundCheck = Name.special("<setWithoutBoundCheck>")
    val getWithoutBoundCheck = Name.special("<getWithoutBoundCheck>")
}

// This is what Context collects about IR.
internal class KonanIr(context: Context, override val symbols: KonanSymbols): Ir<Context>(context)

internal abstract class KonanSymbols(
        context: PhaseContext,
        val descriptorsLookup: DescriptorsLookup, // Please don't add usages of `descriptorsLookup` and `descriptorsLookup.builtIns` to this class anymore.
        irBuiltIns: IrBuiltIns,
        internal val symbolTable: SymbolTable, // Please don't add usages of `symbolTable` to this class anymore.
        lazySymbolTable: ReferenceSymbolTable
): Symbols(irBuiltIns, symbolTable) {
    protected abstract fun IrClassSymbol.findMemberSimpleFunction(name: Name): IrSimpleFunctionSymbol?
    protected abstract fun IrClassSymbol.findMemberProperty(name: Name): IrPropertySymbol?
    protected abstract fun IrClassSymbol.findMemberPropertyGetter(name: Name): IrSimpleFunctionSymbol?
    protected abstract fun findDefaultConstructor(className: String): IrConstructorSymbol
    protected abstract fun findDefaultConstructor(className: String, nestedClassName: String): IrConstructorSymbol
    protected abstract fun findTopLevelExtensionPropertyGetter(packageName: FqName, name: Name, extensionParameterClassID: ClassId): IrSimpleFunctionSymbol?
    private fun findInteropExtensionPropertyGetter(name: Name, extensionClassID: ClassId): IrSimpleFunctionSymbol? =
            findTopLevelExtensionPropertyGetter(InteropFqNames.packageName, name, extensionClassID)

    val entryPoint = findMainEntryPoint(context, descriptorsLookup.builtIns)?.let { symbolTable.referenceSimpleFunction(it) }

    override val externalSymbolTable = lazySymbolTable

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

    val objCMethodImp = interopClass(InteropFqNames.objCMethodImpName)

    val processUnhandledException = irBuiltIns.findFunctions(Name.identifier("processUnhandledException"), "kotlin", "native").single()
    val terminateWithUnhandledException = irBuiltIns.findFunctions(Name.identifier("terminateWithUnhandledException"), "kotlin", "native").single()

    abstract val interopNativePointedGetRawPointer: IrSimpleFunctionSymbol

    val interopCPointer = interopClass(InteropFqNames.cPointerName)
    val interopCstr = findInteropExtensionPropertyGetter(Name.identifier(InteropFqNames.cstrPropertyName), StandardClassIds.String)!!
    val interopWcstr = findInteropExtensionPropertyGetter(Name.identifier(InteropFqNames.wcstrPropertyName), StandardClassIds.String)!!
    val interopMemScope = interopClass(InteropFqNames.memScopeName)
    val interopCValue = interopClass(InteropFqNames.cValueName)
    val interopCValuesRef = interopClass(InteropFqNames.cValuesRefName)
    abstract val interopCValueWrite: IrSimpleFunctionSymbol
    abstract val interopCValueRead: IrSimpleFunctionSymbol
    abstract val interopAllocType: IrSimpleFunctionSymbol

    val interopTypeOf = interopFunction(InteropFqNames.typeOfFunName)

    abstract val interopCPointerGetRawValue: IrSimpleFunctionSymbol

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

    val interopNativePointedRawPtrGetter = interopClass(InteropFqNames.nativePointedName)
            .findMemberPropertyGetter(Name.identifier(InteropFqNames.nativePointedRawPtrPropertyName))!!

    val interopCPointerRawValue: IrPropertySymbol = interopClass(InteropFqNames.cPointerName)
            .findMemberProperty(Name.identifier(InteropFqNames.cPointerRawValuePropertyName))!!

    val interopInterpretObjCPointer = interopFunction(InteropFqNames.interpretObjCPointerFunName)
    val interopInterpretObjCPointerOrNull = interopFunction(InteropFqNames.interpretObjCPointerOrNullFunName)
    val interopInterpretNullablePointed = interopFunction(InteropFqNames.interpretNullablePointedFunName)
    val interopInterpretCPointer = interopFunction(InteropFqNames.interpretCPointerFunName)

    val createForeignException = interopFunction("CreateForeignException")

    val interopCEnumVar = interopClass("CEnumVar")

    val nativeMemUtils = interopClass(InteropFqNames.nativeMemUtilsName)
    val nativeHeap = interopClass(InteropFqNames.nativeHeapName)

    abstract val interopGetPtr: IrSimpleFunctionSymbol
    abstract val interopManagedGetPtr: IrSimpleFunctionSymbol

    val interopManagedType = interopClass(InteropFqNames.managedTypeName)
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

    val eagerInitialization = topLevelClass(KonanFqNames.eagerInitialization)

    val cStructVarConstructorSymbol = findDefaultConstructor(InteropFqNames.cStructVarName)
    val managedTypeConstructor = findDefaultConstructor(InteropFqNames.managedTypeName)
    val enumVarConstructorSymbol = findDefaultConstructor(InteropFqNames.cEnumVarName)
    val primitiveVarPrimaryConstructor = findDefaultConstructor(InteropFqNames.cPrimitiveVarName, InteropFqNames.TypeName)
    val structVarPrimaryConstructor = findDefaultConstructor(InteropFqNames.cStructVarName, InteropFqNames.TypeName)

    private fun topLevelClass(fqName: FqName): IrClassSymbol = irBuiltIns.findClass(fqName.shortName(), fqName.parent())!!

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

    private fun interopFunction(name: String) =
            irBuiltIns.findFunctions(Name.identifier(name), InteropFqNames.packageName).single()

    protected fun interopClass(name: String) =
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

// WARNING: override protected functions are called from base class(`KonanSymbols`) constructor,
// so its functionality must be independent of the derived object(`KonanSymbolsOverDescriptors`)'s state, which is uninitialized at the invocation moment
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

    override fun IrClassSymbol.findMemberProperty(name: Name): IrPropertySymbol? =
            descriptor.unsubstitutedMemberScope.getContributedVariables(name, NoLookupLocation.FROM_BACKEND)
                    .singleOrNull()
                    ?.let { symbolTable.referenceProperty(it) }

    override fun IrClassSymbol.findMemberPropertyGetter(name: Name): IrSimpleFunctionSymbol? =
            descriptor.unsubstitutedMemberScope.getContributedVariables(name, NoLookupLocation.FROM_BACKEND)
                    .singleOrNull()
                    ?.getter
                    ?.let { symbolTable.referenceSimpleFunction(it) }

    override fun findTopLevelExtensionPropertyGetter(packageName: FqName, name: Name, extensionParameterClassID: ClassId): IrSimpleFunctionSymbol? {
        val extensionParameterFqNameString = extensionParameterClassID.asFqNameString()
        return descriptorsLookup.builtIns.builtInsModule.getPackage(InteropFqNames.packageName).memberScope
                .getContributedVariables(name, NoLookupLocation.FROM_BACKEND)
                .singleOrNull {
                    it.extensionReceiverParameter?.type?.getKotlinTypeFqName(true) == extensionParameterFqNameString
                }?.getter
                ?.let { symbolTable.referenceSimpleFunction(it) }
    }

    override val interopNativePointedGetRawPointer = symbolTable.referenceSimpleFunction(
            descriptorsLookup.interopBuiltIns.getContributedFunctions(InteropFqNames.nativePointedGetRawPointerFunName).single {
                val extensionReceiverParameter = it.extensionReceiverParameter
                extensionReceiverParameter != null &&
                        TypeUtils.getClassDescriptor(extensionReceiverParameter.type)?.fqNameUnsafe == InteropFqNames.nativePointed
            })
    override val interopCValueWrite = symbolTable.referenceSimpleFunction(
            descriptorsLookup.interopBuiltIns.getContributedFunctions(InteropFqNames.cValueWriteFunName).single {
                it.extensionReceiverParameter?.type?.constructor?.declarationDescriptor?.fqNameSafe == InteropFqNames.cValue
            })
    override val interopCValueRead = symbolTable.referenceSimpleFunction(
            descriptorsLookup.interopBuiltIns.getContributedFunctions(InteropFqNames.cValueReadFunName).single {
                it.valueParameters.size == 1
            })
    override val interopAllocType = symbolTable.referenceSimpleFunction(
            descriptorsLookup.interopBuiltIns.getContributedFunctions(InteropFqNames.allocTypeFunName).single {
                it.extensionReceiverParameter != null && it.valueParameters.singleOrNull()?.name?.toString() == "type"
            })
    override val interopCPointerGetRawValue = symbolTable.referenceSimpleFunction(
            descriptorsLookup.interopBuiltIns.getContributedFunctions(InteropFqNames.cPointerGetRawValueFunName).single {
                val extensionReceiverParameter = it.extensionReceiverParameter
                extensionReceiverParameter != null &&
                        TypeUtils.getClassDescriptor(extensionReceiverParameter.type)?.fqNameUnsafe == InteropFqNames.cPointer
            })

    override fun findDefaultConstructor(className: String): IrConstructorSymbol = symbolTable.referenceConstructor(
            descriptorsLookup.interopBuiltIns.getContributedClass(className)
                    .unsubstitutedPrimaryConstructor!!
    )

    override fun findDefaultConstructor(className: String, nestedClassName: String): IrConstructorSymbol = symbolTable.referenceConstructor(
            descriptorsLookup.interopBuiltIns.getContributedClass(className)
                    .defaultType.memberScope.getContributedClass(nestedClassName)
                    .unsubstitutedPrimaryConstructor!!
    )

    override val interopGetPtr = descriptorsLookup.interopBuiltIns.getContributedVariables("ptr").single {
        val singleTypeParameter = it.typeParameters.singleOrNull()
        val singleTypeParameterUpperBound = singleTypeParameter?.upperBounds?.singleOrNull()
        val extensionReceiverParameter = it.extensionReceiverParameter

        singleTypeParameterUpperBound != null &&
                extensionReceiverParameter != null &&
                TypeUtils.getClassDescriptor(singleTypeParameterUpperBound)?.fqNameSafe == InteropFqNames.cPointed &&
                extensionReceiverParameter.type == singleTypeParameter.defaultType
    }.getter!!.let { symbolTable.referenceSimpleFunction(it) }

    override val interopManagedGetPtr = descriptorsLookup.interopBuiltIns.getContributedVariables("ptr").single {
        val singleTypeParameter = it.typeParameters.singleOrNull()
        val singleTypeParameterUpperBound = singleTypeParameter?.upperBounds?.singleOrNull()
        val extensionReceiverParameter = it.extensionReceiverParameter

        singleTypeParameterUpperBound != null &&
                extensionReceiverParameter != null &&
                TypeUtils.getClassDescriptor(singleTypeParameterUpperBound)?.fqNameSafe == InteropFqNames.cStructVar &&
                TypeUtils.getClassDescriptor(extensionReceiverParameter.type)?.fqNameSafe == InteropFqNames.managedType
    }.getter!!.let { symbolTable.referenceSimpleFunction(it) }
}

// WARNING: override protected functions are called from base class(`KonanSymbols`) constructor,
// so its functionality must be independent of the derived object(`KonanSymbolsOverFir`)'s state, which is uninitialized at the invocation moment
internal class KonanSymbolsOverFir(
        context: PhaseContext,
        descriptorsLookup: DescriptorsLookup,
        irBuiltIns: IrBuiltIns,
        symbolTable: SymbolTable,
        lazySymbolTable: ReferenceSymbolTable
) : KonanSymbols(context, descriptorsLookup, irBuiltIns, symbolTable, lazySymbolTable) {
    override fun IrClassSymbol.findMemberSimpleFunction(name: Name): IrSimpleFunctionSymbol? =
            owner.findDeclaration<IrSimpleFunction> { it.name == name }?.symbol

    override fun IrClassSymbol.findMemberProperty(name: Name): IrPropertySymbol? =
            owner.findDeclaration<IrProperty> { it.name == name }?.symbol

    override fun IrClassSymbol.findMemberPropertyGetter(name: Name): IrSimpleFunctionSymbol? =
            owner.findDeclaration<IrProperty> { it.name == name }?.getter?.symbol

    override fun findTopLevelExtensionPropertyGetter(packageName: FqName, name: Name, extensionParameterClassID: ClassId): IrSimpleFunctionSymbol? {
        val extensionParameterConeKotlinType = extensionParameterClassID.constructClassLikeType(arrayOf(), false).type
        return irBuiltIns.findProperties(name, packageName).singleOrNull {
            ((it.owner as Fir2IrLazyProperty).fir.receiverParameter?.typeRef as FirResolvedTypeRef).type == extensionParameterConeKotlinType
        }?.owner?.getter?.symbol
    }

    override val interopNativePointedGetRawPointer = findSingleInteropFunction(InteropFqNames.nativePointedGetRawPointerFunName) {
        val extensionReceiverParameter = it.owner.extensionReceiverParameter
        extensionReceiverParameter != null &&
                extensionReceiverParameter.type.classFqName?.toUnsafe() == InteropFqNames.nativePointed
    }
    override val interopCValueWrite = findSingleInteropFunction(InteropFqNames.cValueWriteFunName) {
        it.owner.extensionReceiverParameter?.type?.classFqName == InteropFqNames.cValue
    }
    override val interopCValueRead = findSingleInteropFunction(InteropFqNames.cValueReadFunName) {
        it.owner.valueParameters.size == 1
    }
    override val interopAllocType = findSingleInteropFunction(InteropFqNames.allocTypeFunName) {
        it.owner.extensionReceiverParameter != null && it.owner.valueParameters.singleOrNull()?.name?.toString() == "type"
    }
    override val interopCPointerGetRawValue = findSingleInteropFunction(InteropFqNames.cPointerGetRawValueFunName) {
        it.owner.extensionReceiverParameter?.type?.classFqName?.toUnsafe() == InteropFqNames.cPointer
    }

    override val interopGetPtr = irBuiltIns.findProperties(Name.identifier("ptr"), InteropFqNames.packageName).single {
        val singleTypeParameter = it.owner.getter?.typeParameters?.singleOrNull()
        val singleTypeParameterUpperBound = singleTypeParameter?.symbol?.superTypes()?.singleOrNull()
        val extensionReceiverParameter = it.owner.getter?.extensionReceiverParameter

        singleTypeParameterUpperBound != null &&
                extensionReceiverParameter != null &&
                singleTypeParameterUpperBound.classFqName == InteropFqNames.cPointed &&
                extensionReceiverParameter.type.classFqName == singleTypeParameter.defaultType.classFqName
    }.owner.getter!!.symbol

    override val interopManagedGetPtr = irBuiltIns.findProperties(Name.identifier("ptr"), InteropFqNames.packageName).single {
        val singleTypeParameter = it.owner.getter?.typeParameters?.singleOrNull()
        val singleTypeParameterUpperBound = singleTypeParameter?.symbol?.superTypes()?.singleOrNull()
        val extensionReceiverParameter = it.owner.getter?.extensionReceiverParameter

        singleTypeParameterUpperBound != null &&
                extensionReceiverParameter != null &&
                singleTypeParameterUpperBound.classFqName == InteropFqNames.cStructVar &&
                extensionReceiverParameter.type.classFqName == InteropFqNames.managedType
    }.owner.getter!!.symbol

    private fun findSingleInteropFunction(name: String, predicate: (IrSimpleFunctionSymbol) -> Boolean) =
            irBuiltIns.findFunctions(Name.identifier(name), InteropFqNames.packageName).single(predicate)

    override fun findDefaultConstructor(className: String): IrConstructorSymbol =
            interopClass(className).owner.declarations.single {
                it is IrConstructor && it.isPrimary
            }.symbol as IrConstructorSymbol

    override fun findDefaultConstructor(className: String, nestedClassName: String): IrConstructorSymbol {
        val nested = interopClass(className).owner.declarations.single {
            it is IrClass && it.name == Name.identifier(nestedClassName)
        }.symbol as IrClassSymbol
        return nested.owner.declarations.single {
            it is IrConstructor && it.isPrimary
        }.symbol as IrConstructorSymbol
    }
}
