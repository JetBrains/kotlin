@file:JvmName("llvm")
@file:Suppress("UNUSED_VARIABLE", "UNUSED_EXPRESSION", "DEPRECATION")
@file:OptIn(ExperimentalForeignApi::class)
package llvm

import kotlinx.cinterop.*

// NOTE THIS FILE IS AUTO-GENERATED

@ExperimentalForeignApi
class LLVMOpaqueMemoryBuffer(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class LLVMOpaqueContext(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class LLVMOpaqueModule(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class LLVMOpaqueType(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class LLVMOpaqueValue(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class LLVMOpaqueBasicBlock(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class LLVMOpaqueMetadata(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class LLVMOpaqueNamedMDNode(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class LLVMOpaqueValueMetadataEntry(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class LLVMOpaqueBuilder(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class LLVMOpaqueDIBuilder(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class LLVMOpaqueModuleProvider(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class LLVMOpaquePassManager(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class LLVMOpaquePassRegistry(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class LLVMOpaqueUse(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class LLVMOpaqueAttributeRef(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class LLVMOpaqueDiagnosticInfo(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class LLVMComdat(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class LLVMOpaqueModuleFlagEntry(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class LLVMOpaqueJITEventListener(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class LLVMOpaqueBinary(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class LLVMOpaqueTargetData(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class LLVMOpaqueTargetLibraryInfotData(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class LLVMOpaqueError(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class LLVMOpaqueTargetMachine(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class LLVMTarget(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class LLVMOpaquePassBuilderOptions(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class DICompileUnit(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class DIFile(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class DIBasicType(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class DICompositeType(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class DIDerivedType(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class DIType(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class DISubprogram(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class DIModule(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class DIScope(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class DISubroutineType(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class DILocation(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class DILocalVariable(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
class DIExpression(rawPtr: NativePtr) : COpaque(rawPtr) {
}

@ExperimentalForeignApi
enum class LLVMOpcode(value: Int) : CEnum {
    LLVMRet(1),
    LLVMBr(2),
    LLVMSwitch(3),
    LLVMIndirectBr(4),
    LLVMInvoke(5),
    LLVMUnreachable(7),
    LLVMAdd(8),
    LLVMFAdd(9),
    LLVMSub(10),
    LLVMFSub(11),
    LLVMMul(12),
    LLVMFMul(13),
    LLVMUDiv(14),
    LLVMSDiv(15),
    LLVMFDiv(16),
    LLVMURem(17),
    LLVMSRem(18),
    LLVMFRem(19),
    LLVMShl(20),
    LLVMLShr(21),
    LLVMAShr(22),
    LLVMAnd(23),
    LLVMOr(24),
    LLVMXor(25),
    LLVMAlloca(26),
    LLVMLoad(27),
    LLVMStore(28),
    LLVMGetElementPtr(29),
    LLVMTrunc(30),
    LLVMZExt(31),
    LLVMSExt(32),
    LLVMFPToUI(33),
    LLVMFPToSI(34),
    LLVMUIToFP(35),
    LLVMSIToFP(36),
    LLVMFPTrunc(37),
    LLVMFPExt(38),
    LLVMPtrToInt(39),
    LLVMIntToPtr(40),
    LLVMBitCast(41),
    LLVMICmp(42),
    LLVMFCmp(43),
    LLVMPHI(44),
    LLVMCall(45),
    LLVMSelect(46),
    LLVMUserOp1(47),
    LLVMUserOp2(48),
    LLVMVAArg(49),
    LLVMExtractElement(50),
    LLVMInsertElement(51),
    LLVMShuffleVector(52),
    LLVMExtractValue(53),
    LLVMInsertValue(54),
    LLVMFence(55),
    LLVMAtomicCmpXchg(56),
    LLVMAtomicRMW(57),
    LLVMResume(58),
    LLVMLandingPad(59),
    LLVMAddrSpaceCast(60),
    LLVMCleanupRet(61),
    LLVMCatchRet(62),
    LLVMCatchPad(63),
    LLVMCleanupPad(64),
    LLVMCatchSwitch(65),
    LLVMFNeg(66),
    LLVMCallBr(67),
    LLVMFreeze(68),
    ;
    
    companion object {
        
        @Deprecated("Will be removed.", ReplaceWith(""), DeprecationLevel.WARNING)
        fun byValue(value: Int): LLVMOpcode = values().find { it.value == value }!!
    }
    
    override open val value: Int = value
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
        companion object : Type(sizeOf<IntVar>().toInt())
        var value: LLVMOpcode
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

@ExperimentalForeignApi
enum class LLVMTypeKind(value: Int) : CEnum {
    LLVMVoidTypeKind(0),
    LLVMHalfTypeKind(1),
    LLVMFloatTypeKind(2),
    LLVMDoubleTypeKind(3),
    LLVMX86_FP80TypeKind(4),
    LLVMFP128TypeKind(5),
    LLVMPPC_FP128TypeKind(6),
    LLVMLabelTypeKind(7),
    LLVMIntegerTypeKind(8),
    LLVMFunctionTypeKind(9),
    LLVMStructTypeKind(10),
    LLVMArrayTypeKind(11),
    LLVMPointerTypeKind(12),
    LLVMVectorTypeKind(13),
    LLVMMetadataTypeKind(14),
    LLVMX86_MMXTypeKind(15),
    LLVMTokenTypeKind(16),
    LLVMScalableVectorTypeKind(17),
    LLVMBFloatTypeKind(18),
    LLVMX86_AMXTypeKind(19),
    LLVMTargetExtTypeKind(20),
    ;
    
    companion object {
        
        @Deprecated("Will be removed.", ReplaceWith(""), DeprecationLevel.WARNING)
        fun byValue(value: Int): LLVMTypeKind = values().find { it.value == value }!!
    }
    
    override open val value: Int = value
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
        companion object : Type(sizeOf<IntVar>().toInt())
        var value: LLVMTypeKind
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

@ExperimentalForeignApi
enum class LLVMLinkage(value: Int) : CEnum {
    LLVMExternalLinkage(0),
    LLVMAvailableExternallyLinkage(1),
    LLVMLinkOnceAnyLinkage(2),
    LLVMLinkOnceODRLinkage(3),
    LLVMLinkOnceODRAutoHideLinkage(4),
    LLVMWeakAnyLinkage(5),
    LLVMWeakODRLinkage(6),
    LLVMAppendingLinkage(7),
    LLVMInternalLinkage(8),
    LLVMPrivateLinkage(9),
    LLVMDLLImportLinkage(10),
    LLVMDLLExportLinkage(11),
    LLVMExternalWeakLinkage(12),
    LLVMGhostLinkage(13),
    LLVMCommonLinkage(14),
    LLVMLinkerPrivateLinkage(15),
    LLVMLinkerPrivateWeakLinkage(16),
    ;
    
    companion object {
        
        @Deprecated("Will be removed.", ReplaceWith(""), DeprecationLevel.WARNING)
        fun byValue(value: Int): LLVMLinkage = values().find { it.value == value }!!
    }
    
    override open val value: Int = value
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
        companion object : Type(sizeOf<IntVar>().toInt())
        var value: LLVMLinkage
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

@ExperimentalForeignApi
enum class LLVMVisibility(value: Int) : CEnum {
    LLVMDefaultVisibility(0),
    LLVMHiddenVisibility(1),
    LLVMProtectedVisibility(2),
    ;
    
    companion object {
        
        @Deprecated("Will be removed.", ReplaceWith(""), DeprecationLevel.WARNING)
        fun byValue(value: Int): LLVMVisibility = values().find { it.value == value }!!
    }
    
    override open val value: Int = value
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
        companion object : Type(sizeOf<IntVar>().toInt())
        var value: LLVMVisibility
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

@ExperimentalForeignApi
enum class LLVMUnnamedAddr(value: Int) : CEnum {
    LLVMNoUnnamedAddr(0),
    LLVMLocalUnnamedAddr(1),
    LLVMGlobalUnnamedAddr(2),
    ;
    
    companion object {
        
        @Deprecated("Will be removed.", ReplaceWith(""), DeprecationLevel.WARNING)
        fun byValue(value: Int): LLVMUnnamedAddr = values().find { it.value == value }!!
    }
    
    override open val value: Int = value
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
        companion object : Type(sizeOf<IntVar>().toInt())
        var value: LLVMUnnamedAddr
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

@ExperimentalForeignApi
enum class LLVMDLLStorageClass(value: Int) : CEnum {
    LLVMDefaultStorageClass(0),
    LLVMDLLImportStorageClass(1),
    LLVMDLLExportStorageClass(2),
    ;
    
    companion object {
        
        @Deprecated("Will be removed.", ReplaceWith(""), DeprecationLevel.WARNING)
        fun byValue(value: Int): LLVMDLLStorageClass = values().find { it.value == value }!!
    }
    
    override open val value: Int = value
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
        companion object : Type(sizeOf<IntVar>().toInt())
        var value: LLVMDLLStorageClass
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

@ExperimentalForeignApi
enum class LLVMCallConv(value: Int) : CEnum {
    LLVMCCallConv(0),
    LLVMFastCallConv(8),
    LLVMColdCallConv(9),
    LLVMGHCCallConv(10),
    LLVMHiPECallConv(11),
    LLVMWebKitJSCallConv(12),
    LLVMAnyRegCallConv(13),
    LLVMPreserveMostCallConv(14),
    LLVMPreserveAllCallConv(15),
    LLVMSwiftCallConv(16),
    LLVMCXXFASTTLSCallConv(17),
    LLVMX86StdcallCallConv(64),
    LLVMX86FastcallCallConv(65),
    LLVMARMAPCSCallConv(66),
    LLVMARMAAPCSCallConv(67),
    LLVMARMAAPCSVFPCallConv(68),
    LLVMMSP430INTRCallConv(69),
    LLVMX86ThisCallCallConv(70),
    LLVMPTXKernelCallConv(71),
    LLVMPTXDeviceCallConv(72),
    LLVMSPIRFUNCCallConv(75),
    LLVMSPIRKERNELCallConv(76),
    LLVMIntelOCLBICallConv(77),
    LLVMX8664SysVCallConv(78),
    LLVMWin64CallConv(79),
    LLVMX86VectorCallCallConv(80),
    LLVMHHVMCallConv(81),
    LLVMHHVMCCallConv(82),
    LLVMX86INTRCallConv(83),
    LLVMAVRINTRCallConv(84),
    LLVMAVRSIGNALCallConv(85),
    LLVMAVRBUILTINCallConv(86),
    LLVMAMDGPUVSCallConv(87),
    LLVMAMDGPUGSCallConv(88),
    LLVMAMDGPUPSCallConv(89),
    LLVMAMDGPUCSCallConv(90),
    LLVMAMDGPUKERNELCallConv(91),
    LLVMX86RegCallCallConv(92),
    LLVMAMDGPUHSCallConv(93),
    LLVMMSP430BUILTINCallConv(94),
    LLVMAMDGPULSCallConv(95),
    LLVMAMDGPUESCallConv(96),
    ;
    
    companion object {
        
        @Deprecated("Will be removed.", ReplaceWith(""), DeprecationLevel.WARNING)
        fun byValue(value: Int): LLVMCallConv = values().find { it.value == value }!!
    }
    
    override open val value: Int = value
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
        companion object : Type(sizeOf<IntVar>().toInt())
        var value: LLVMCallConv
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

@ExperimentalForeignApi
enum class LLVMValueKind(value: Int) : CEnum {
    LLVMArgumentValueKind(0),
    LLVMBasicBlockValueKind(1),
    LLVMMemoryUseValueKind(2),
    LLVMMemoryDefValueKind(3),
    LLVMMemoryPhiValueKind(4),
    LLVMFunctionValueKind(5),
    LLVMGlobalAliasValueKind(6),
    LLVMGlobalIFuncValueKind(7),
    LLVMGlobalVariableValueKind(8),
    LLVMBlockAddressValueKind(9),
    LLVMConstantExprValueKind(10),
    LLVMConstantArrayValueKind(11),
    LLVMConstantStructValueKind(12),
    LLVMConstantVectorValueKind(13),
    LLVMUndefValueValueKind(14),
    LLVMConstantAggregateZeroValueKind(15),
    LLVMConstantDataArrayValueKind(16),
    LLVMConstantDataVectorValueKind(17),
    LLVMConstantIntValueKind(18),
    LLVMConstantFPValueKind(19),
    LLVMConstantPointerNullValueKind(20),
    LLVMConstantTokenNoneValueKind(21),
    LLVMMetadataAsValueValueKind(22),
    LLVMInlineAsmValueKind(23),
    LLVMInstructionValueKind(24),
    LLVMPoisonValueValueKind(25),
    LLVMConstantTargetNoneValueKind(26),
    ;
    
    companion object {
        
        @Deprecated("Will be removed.", ReplaceWith(""), DeprecationLevel.WARNING)
        fun byValue(value: Int): LLVMValueKind = values().find { it.value == value }!!
    }
    
    override open val value: Int = value
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
        companion object : Type(sizeOf<IntVar>().toInt())
        var value: LLVMValueKind
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

@ExperimentalForeignApi
enum class LLVMIntPredicate(value: Int) : CEnum {
    LLVMIntEQ(32),
    LLVMIntNE(33),
    LLVMIntUGT(34),
    LLVMIntUGE(35),
    LLVMIntULT(36),
    LLVMIntULE(37),
    LLVMIntSGT(38),
    LLVMIntSGE(39),
    LLVMIntSLT(40),
    LLVMIntSLE(41),
    ;
    
    companion object {
        
        @Deprecated("Will be removed.", ReplaceWith(""), DeprecationLevel.WARNING)
        fun byValue(value: Int): LLVMIntPredicate = values().find { it.value == value }!!
    }
    
    override open val value: Int = value
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
        companion object : Type(sizeOf<IntVar>().toInt())
        var value: LLVMIntPredicate
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

@ExperimentalForeignApi
enum class LLVMRealPredicate(value: Int) : CEnum {
    LLVMRealPredicateFalse(0),
    LLVMRealOEQ(1),
    LLVMRealOGT(2),
    LLVMRealOGE(3),
    LLVMRealOLT(4),
    LLVMRealOLE(5),
    LLVMRealONE(6),
    LLVMRealORD(7),
    LLVMRealUNO(8),
    LLVMRealUEQ(9),
    LLVMRealUGT(10),
    LLVMRealUGE(11),
    LLVMRealULT(12),
    LLVMRealULE(13),
    LLVMRealUNE(14),
    LLVMRealPredicateTrue(15),
    ;
    
    companion object {
        
        @Deprecated("Will be removed.", ReplaceWith(""), DeprecationLevel.WARNING)
        fun byValue(value: Int): LLVMRealPredicate = values().find { it.value == value }!!
    }
    
    override open val value: Int = value
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
        companion object : Type(sizeOf<IntVar>().toInt())
        var value: LLVMRealPredicate
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

@ExperimentalForeignApi
enum class LLVMLandingPadClauseTy(value: Int) : CEnum {
    LLVMLandingPadCatch(0),
    LLVMLandingPadFilter(1),
    ;
    
    companion object {
        
        @Deprecated("Will be removed.", ReplaceWith(""), DeprecationLevel.WARNING)
        fun byValue(value: Int): LLVMLandingPadClauseTy = values().find { it.value == value }!!
    }
    
    override open val value: Int = value
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
        companion object : Type(sizeOf<IntVar>().toInt())
        var value: LLVMLandingPadClauseTy
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

@ExperimentalForeignApi
enum class LLVMThreadLocalMode(value: Int) : CEnum {
    LLVMNotThreadLocal(0),
    LLVMGeneralDynamicTLSModel(1),
    LLVMLocalDynamicTLSModel(2),
    LLVMInitialExecTLSModel(3),
    LLVMLocalExecTLSModel(4),
    ;
    
    companion object {
        
        @Deprecated("Will be removed.", ReplaceWith(""), DeprecationLevel.WARNING)
        fun byValue(value: Int): LLVMThreadLocalMode = values().find { it.value == value }!!
    }
    
    override open val value: Int = value
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
        companion object : Type(sizeOf<IntVar>().toInt())
        var value: LLVMThreadLocalMode
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

@ExperimentalForeignApi
enum class LLVMAtomicOrdering(value: Int) : CEnum {
    LLVMAtomicOrderingNotAtomic(0),
    LLVMAtomicOrderingUnordered(1),
    LLVMAtomicOrderingMonotonic(2),
    LLVMAtomicOrderingAcquire(4),
    LLVMAtomicOrderingRelease(5),
    LLVMAtomicOrderingAcquireRelease(6),
    LLVMAtomicOrderingSequentiallyConsistent(7),
    ;
    
    companion object {
        
        @Deprecated("Will be removed.", ReplaceWith(""), DeprecationLevel.WARNING)
        fun byValue(value: Int): LLVMAtomicOrdering = values().find { it.value == value }!!
    }
    
    override open val value: Int = value
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
        companion object : Type(sizeOf<IntVar>().toInt())
        var value: LLVMAtomicOrdering
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

@ExperimentalForeignApi
enum class LLVMAtomicRMWBinOp(value: Int) : CEnum {
    LLVMAtomicRMWBinOpXchg(0),
    LLVMAtomicRMWBinOpAdd(1),
    LLVMAtomicRMWBinOpSub(2),
    LLVMAtomicRMWBinOpAnd(3),
    LLVMAtomicRMWBinOpNand(4),
    LLVMAtomicRMWBinOpOr(5),
    LLVMAtomicRMWBinOpXor(6),
    LLVMAtomicRMWBinOpMax(7),
    LLVMAtomicRMWBinOpMin(8),
    LLVMAtomicRMWBinOpUMax(9),
    LLVMAtomicRMWBinOpUMin(10),
    LLVMAtomicRMWBinOpFAdd(11),
    LLVMAtomicRMWBinOpFSub(12),
    LLVMAtomicRMWBinOpFMax(13),
    LLVMAtomicRMWBinOpFMin(14),
    ;
    
    companion object {
        
        @Deprecated("Will be removed.", ReplaceWith(""), DeprecationLevel.WARNING)
        fun byValue(value: Int): LLVMAtomicRMWBinOp = values().find { it.value == value }!!
    }
    
    override open val value: Int = value
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
        companion object : Type(sizeOf<IntVar>().toInt())
        var value: LLVMAtomicRMWBinOp
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

@ExperimentalForeignApi
enum class LLVMDiagnosticSeverity(value: Int) : CEnum {
    LLVMDSError(0),
    LLVMDSWarning(1),
    LLVMDSRemark(2),
    LLVMDSNote(3),
    ;
    
    companion object {
        
        @Deprecated("Will be removed.", ReplaceWith(""), DeprecationLevel.WARNING)
        fun byValue(value: Int): LLVMDiagnosticSeverity = values().find { it.value == value }!!
    }
    
    override open val value: Int = value
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
        companion object : Type(sizeOf<IntVar>().toInt())
        var value: LLVMDiagnosticSeverity
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

@ExperimentalForeignApi
enum class LLVMInlineAsmDialect(value: Int) : CEnum {
    LLVMInlineAsmDialectATT(0),
    LLVMInlineAsmDialectIntel(1),
    ;
    
    companion object {
        
        @Deprecated("Will be removed.", ReplaceWith(""), DeprecationLevel.WARNING)
        fun byValue(value: Int): LLVMInlineAsmDialect = values().find { it.value == value }!!
    }
    
    override open val value: Int = value
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
        companion object : Type(sizeOf<IntVar>().toInt())
        var value: LLVMInlineAsmDialect
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

@ExperimentalForeignApi
enum class LLVMModuleFlagBehavior(value: Int) : CEnum {
    LLVMModuleFlagBehaviorError(0),
    LLVMModuleFlagBehaviorWarning(1),
    LLVMModuleFlagBehaviorRequire(2),
    LLVMModuleFlagBehaviorOverride(3),
    LLVMModuleFlagBehaviorAppend(4),
    LLVMModuleFlagBehaviorAppendUnique(5),
    ;
    
    companion object {
        
        @Deprecated("Will be removed.", ReplaceWith(""), DeprecationLevel.WARNING)
        fun byValue(value: Int): LLVMModuleFlagBehavior = values().find { it.value == value }!!
    }
    
    override open val value: Int = value
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
        companion object : Type(sizeOf<IntVar>().toInt())
        var value: LLVMModuleFlagBehavior
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

@ExperimentalForeignApi
enum class LLVMByteOrdering(value: Int) : CEnum {
    LLVMBigEndian(0),
    LLVMLittleEndian(1),
    ;
    
    companion object {
        
        @Deprecated("Will be removed.", ReplaceWith(""), DeprecationLevel.WARNING)
        fun byValue(value: Int): LLVMByteOrdering = values().find { it.value == value }!!
    }
    
    override open val value: Int = value
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
        companion object : Type(sizeOf<IntVar>().toInt())
        var value: LLVMByteOrdering
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

@ExperimentalForeignApi
enum class LLVMVerifierFailureAction(value: Int) : CEnum {
    LLVMAbortProcessAction(0),
    LLVMPrintMessageAction(1),
    LLVMReturnStatusAction(2),
    ;
    
    companion object {
        
        @Deprecated("Will be removed.", ReplaceWith(""), DeprecationLevel.WARNING)
        fun byValue(value: Int): LLVMVerifierFailureAction = values().find { it.value == value }!!
    }
    
    override open val value: Int = value
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
        companion object : Type(sizeOf<IntVar>().toInt())
        var value: LLVMVerifierFailureAction
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

@ExperimentalForeignApi
enum class LLVMCodeGenOptLevel(value: Int) : CEnum {
    LLVMCodeGenLevelNone(0),
    LLVMCodeGenLevelLess(1),
    LLVMCodeGenLevelDefault(2),
    LLVMCodeGenLevelAggressive(3),
    ;
    
    companion object {
        
        @Deprecated("Will be removed.", ReplaceWith(""), DeprecationLevel.WARNING)
        fun byValue(value: Int): LLVMCodeGenOptLevel = values().find { it.value == value }!!
    }
    
    override open val value: Int = value
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
        companion object : Type(sizeOf<IntVar>().toInt())
        var value: LLVMCodeGenOptLevel
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

@ExperimentalForeignApi
enum class LLVMRelocMode(value: Int) : CEnum {
    LLVMRelocDefault(0),
    LLVMRelocStatic(1),
    LLVMRelocPIC(2),
    LLVMRelocDynamicNoPic(3),
    LLVMRelocROPI(4),
    LLVMRelocRWPI(5),
    LLVMRelocROPI_RWPI(6),
    ;
    
    companion object {
        
        @Deprecated("Will be removed.", ReplaceWith(""), DeprecationLevel.WARNING)
        fun byValue(value: Int): LLVMRelocMode = values().find { it.value == value }!!
    }
    
    override open val value: Int = value
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
        companion object : Type(sizeOf<IntVar>().toInt())
        var value: LLVMRelocMode
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

@ExperimentalForeignApi
enum class LLVMCodeModel(value: Int) : CEnum {
    LLVMCodeModelDefault(0),
    LLVMCodeModelJITDefault(1),
    LLVMCodeModelTiny(2),
    LLVMCodeModelSmall(3),
    LLVMCodeModelKernel(4),
    LLVMCodeModelMedium(5),
    LLVMCodeModelLarge(6),
    ;
    
    companion object {
        
        @Deprecated("Will be removed.", ReplaceWith(""), DeprecationLevel.WARNING)
        fun byValue(value: Int): LLVMCodeModel = values().find { it.value == value }!!
    }
    
    override open val value: Int = value
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
        companion object : Type(sizeOf<IntVar>().toInt())
        var value: LLVMCodeModel
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

@ExperimentalForeignApi
enum class LLVMCodeGenFileType(value: Int) : CEnum {
    LLVMAssemblyFile(0),
    LLVMObjectFile(1),
    ;
    
    companion object {
        
        @Deprecated("Will be removed.", ReplaceWith(""), DeprecationLevel.WARNING)
        fun byValue(value: Int): LLVMCodeGenFileType = values().find { it.value == value }!!
    }
    
    override open val value: Int = value
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
        companion object : Type(sizeOf<IntVar>().toInt())
        var value: LLVMCodeGenFileType
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

@ExperimentalForeignApi
enum class LLVMDWARFSourceLanguage(value: Int) : CEnum {
    LLVMDWARFSourceLanguageC89(0),
    LLVMDWARFSourceLanguageC(1),
    LLVMDWARFSourceLanguageAda83(2),
    LLVMDWARFSourceLanguageC_plus_plus(3),
    LLVMDWARFSourceLanguageCobol74(4),
    LLVMDWARFSourceLanguageCobol85(5),
    LLVMDWARFSourceLanguageFortran77(6),
    LLVMDWARFSourceLanguageFortran90(7),
    LLVMDWARFSourceLanguagePascal83(8),
    LLVMDWARFSourceLanguageModula2(9),
    LLVMDWARFSourceLanguageJava(10),
    LLVMDWARFSourceLanguageC99(11),
    LLVMDWARFSourceLanguageAda95(12),
    LLVMDWARFSourceLanguageFortran95(13),
    LLVMDWARFSourceLanguagePLI(14),
    LLVMDWARFSourceLanguageObjC(15),
    LLVMDWARFSourceLanguageObjC_plus_plus(16),
    LLVMDWARFSourceLanguageUPC(17),
    LLVMDWARFSourceLanguageD(18),
    LLVMDWARFSourceLanguagePython(19),
    LLVMDWARFSourceLanguageOpenCL(20),
    LLVMDWARFSourceLanguageGo(21),
    LLVMDWARFSourceLanguageModula3(22),
    LLVMDWARFSourceLanguageHaskell(23),
    LLVMDWARFSourceLanguageC_plus_plus_03(24),
    LLVMDWARFSourceLanguageC_plus_plus_11(25),
    LLVMDWARFSourceLanguageOCaml(26),
    LLVMDWARFSourceLanguageRust(27),
    LLVMDWARFSourceLanguageC11(28),
    LLVMDWARFSourceLanguageSwift(29),
    LLVMDWARFSourceLanguageJulia(30),
    LLVMDWARFSourceLanguageDylan(31),
    LLVMDWARFSourceLanguageC_plus_plus_14(32),
    LLVMDWARFSourceLanguageFortran03(33),
    LLVMDWARFSourceLanguageFortran08(34),
    LLVMDWARFSourceLanguageRenderScript(35),
    LLVMDWARFSourceLanguageBLISS(36),
    LLVMDWARFSourceLanguageKotlin(37),
    LLVMDWARFSourceLanguageZig(38),
    LLVMDWARFSourceLanguageCrystal(39),
    LLVMDWARFSourceLanguageC_plus_plus_17(40),
    LLVMDWARFSourceLanguageC_plus_plus_20(41),
    LLVMDWARFSourceLanguageC17(42),
    LLVMDWARFSourceLanguageFortran18(43),
    LLVMDWARFSourceLanguageAda2005(44),
    LLVMDWARFSourceLanguageAda2012(45),
    LLVMDWARFSourceLanguageMips_Assembler(46),
    LLVMDWARFSourceLanguageGOOGLE_RenderScript(47),
    LLVMDWARFSourceLanguageBORLAND_Delphi(48),
    ;
    
    companion object {
        
        @Deprecated("Will be removed.", ReplaceWith(""), DeprecationLevel.WARNING)
        fun byValue(value: Int): LLVMDWARFSourceLanguage = values().find { it.value == value }!!
    }
    
    override open val value: Int = value
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
        companion object : Type(sizeOf<IntVar>().toInt())
        var value: LLVMDWARFSourceLanguage
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

@ExperimentalForeignApi
fun LLVMInstallFatalErrorHandler(Handler: LLVMFatalErrorHandler?): Unit {
    return kniBridge0(Handler.rawValue)
}

@ExperimentalForeignApi
fun LLVMResetFatalErrorHandler(): Unit {
    return kniBridge1()
}

@ExperimentalForeignApi
fun LLVMEnablePrettyStackTrace(): Unit {
    return kniBridge2()
}

@ExperimentalForeignApi
fun LLVMInitializeCore(R: LLVMPassRegistryRef?): Unit {
    return kniBridge3(R.rawValue)
}

@ExperimentalForeignApi
fun LLVMShutdown(): Unit {
    return kniBridge4()
}

@ExperimentalForeignApi
fun LLVMGetVersion(Major: CValuesRef<IntVar>?, Minor: CValuesRef<IntVar>?, Patch: CValuesRef<IntVar>?): Unit {
    memScoped {
        return kniBridge5(Major?.getPointer(memScope).rawValue, Minor?.getPointer(memScope).rawValue, Patch?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMCreateMessage(Message: String?): CPointer<ByteVar>? {
    memScoped {
        return interpretCPointer<ByteVar>(kniBridge6(Message?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMDisposeMessage(Message: CValuesRef<ByteVar>?): Unit {
    memScoped {
        return kniBridge7(Message?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMContextCreate(): LLVMContextRef? {
    return interpretCPointer<LLVMOpaqueContext>(kniBridge8())
}

@ExperimentalForeignApi
fun LLVMContextSetDiagnosticHandler(C: LLVMContextRef?, Handler: LLVMDiagnosticHandler?, DiagnosticContext: CValuesRef<*>?): Unit {
    memScoped {
        return kniBridge9(C.rawValue, Handler.rawValue, DiagnosticContext?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMContextGetDiagnosticHandler(C: LLVMContextRef?): LLVMDiagnosticHandler? {
    return interpretCPointer<CFunction<(LLVMDiagnosticInfoRef?, COpaquePointer?) -> Unit>>(kniBridge10(C.rawValue))
}

@ExperimentalForeignApi
fun LLVMContextGetDiagnosticContext(C: LLVMContextRef?): COpaquePointer? {
    return interpretCPointer<COpaque>(kniBridge11(C.rawValue))
}

@ExperimentalForeignApi
fun LLVMContextSetYieldCallback(C: LLVMContextRef?, Callback: LLVMYieldCallback?, OpaqueHandle: CValuesRef<*>?): Unit {
    memScoped {
        return kniBridge12(C.rawValue, Callback.rawValue, OpaqueHandle?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMContextShouldDiscardValueNames(C: LLVMContextRef?): LLVMBool {
    return kniBridge13(C.rawValue)
}

@ExperimentalForeignApi
fun LLVMContextSetDiscardValueNames(C: LLVMContextRef?, Discard: LLVMBool): Unit {
    return kniBridge14(C.rawValue, Discard)
}

@ExperimentalForeignApi
fun LLVMContextSetOpaquePointers(C: LLVMContextRef?, OpaquePointers: LLVMBool): Unit {
    return kniBridge15(C.rawValue, OpaquePointers)
}

@ExperimentalForeignApi
fun LLVMContextDispose(C: LLVMContextRef?): Unit {
    return kniBridge16(C.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetDiagInfoDescription(DI: LLVMDiagnosticInfoRef?): CPointer<ByteVar>? {
    return interpretCPointer<ByteVar>(kniBridge17(DI.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetDiagInfoSeverity(DI: LLVMDiagnosticInfoRef?): LLVMDiagnosticSeverity {
    return LLVMDiagnosticSeverity.byValue(kniBridge18(DI.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetMDKindIDInContext(C: LLVMContextRef?, Name: String?, SLen: Int): Int {
    memScoped {
        return kniBridge19(C.rawValue, Name?.cstr?.getPointer(memScope).rawValue, SLen)
    }
}

@ExperimentalForeignApi
fun LLVMGetEnumAttributeKindForName(Name: String?, SLen: size_t): Int {
    memScoped {
        return kniBridge20(Name?.cstr?.getPointer(memScope).rawValue, SLen)
    }
}

@ExperimentalForeignApi
fun LLVMGetLastEnumAttributeKind(): Int {
    return kniBridge21()
}

@ExperimentalForeignApi
fun LLVMCreateEnumAttribute(C: LLVMContextRef?, KindID: Int, Val: uint64_t): LLVMAttributeRef? {
    return interpretCPointer<LLVMOpaqueAttributeRef>(kniBridge22(C.rawValue, KindID, Val))
}

@ExperimentalForeignApi
fun LLVMGetEnumAttributeKind(A: LLVMAttributeRef?): Int {
    return kniBridge23(A.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetEnumAttributeValue(A: LLVMAttributeRef?): uint64_t {
    return kniBridge24(A.rawValue)
}

@ExperimentalForeignApi
fun LLVMCreateTypeAttribute(C: LLVMContextRef?, KindID: Int, type_ref: LLVMTypeRef?): LLVMAttributeRef? {
    return interpretCPointer<LLVMOpaqueAttributeRef>(kniBridge25(C.rawValue, KindID, type_ref.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetTypeAttributeValue(A: LLVMAttributeRef?): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge26(A.rawValue))
}

@ExperimentalForeignApi
fun LLVMCreateStringAttribute(C: LLVMContextRef?, K: String?, KLength: Int, V: String?, VLength: Int): LLVMAttributeRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueAttributeRef>(kniBridge27(C.rawValue, K?.cstr?.getPointer(memScope).rawValue, KLength, V?.cstr?.getPointer(memScope).rawValue, VLength))
    }
}

@ExperimentalForeignApi
fun LLVMGetStringAttributeKind(A: LLVMAttributeRef?, Length: CValuesRef<IntVar>?): CPointer<ByteVar>? {
    memScoped {
        return interpretCPointer<ByteVar>(kniBridge28(A.rawValue, Length?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMGetStringAttributeValue(A: LLVMAttributeRef?, Length: CValuesRef<IntVar>?): CPointer<ByteVar>? {
    memScoped {
        return interpretCPointer<ByteVar>(kniBridge29(A.rawValue, Length?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMIsEnumAttribute(A: LLVMAttributeRef?): LLVMBool {
    return kniBridge30(A.rawValue)
}

@ExperimentalForeignApi
fun LLVMIsStringAttribute(A: LLVMAttributeRef?): LLVMBool {
    return kniBridge31(A.rawValue)
}

@ExperimentalForeignApi
fun LLVMIsTypeAttribute(A: LLVMAttributeRef?): LLVMBool {
    return kniBridge32(A.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetTypeByName2(C: LLVMContextRef?, Name: String?): LLVMTypeRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueType>(kniBridge33(C.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMModuleCreateWithNameInContext(ModuleID: String?, C: LLVMContextRef?): LLVMModuleRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueModule>(kniBridge34(ModuleID?.cstr?.getPointer(memScope).rawValue, C.rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMCloneModule(M: LLVMModuleRef?): LLVMModuleRef? {
    return interpretCPointer<LLVMOpaqueModule>(kniBridge35(M.rawValue))
}

@ExperimentalForeignApi
fun LLVMDisposeModule(M: LLVMModuleRef?): Unit {
    return kniBridge36(M.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetModuleIdentifier(M: LLVMModuleRef?, Len: CValuesRef<size_tVar>?): CPointer<ByteVar>? {
    memScoped {
        return interpretCPointer<ByteVar>(kniBridge37(M.rawValue, Len?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMSetModuleIdentifier(M: LLVMModuleRef?, Ident: String?, Len: size_t): Unit {
    memScoped {
        return kniBridge38(M.rawValue, Ident?.cstr?.getPointer(memScope).rawValue, Len)
    }
}

@ExperimentalForeignApi
fun LLVMGetSourceFileName(M: LLVMModuleRef?, Len: CValuesRef<size_tVar>?): CPointer<ByteVar>? {
    memScoped {
        return interpretCPointer<ByteVar>(kniBridge39(M.rawValue, Len?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMSetSourceFileName(M: LLVMModuleRef?, Name: String?, Len: size_t): Unit {
    memScoped {
        return kniBridge40(M.rawValue, Name?.cstr?.getPointer(memScope).rawValue, Len)
    }
}

@ExperimentalForeignApi
fun LLVMGetDataLayoutStr(M: LLVMModuleRef?): CPointer<ByteVar>? {
    return interpretCPointer<ByteVar>(kniBridge41(M.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetDataLayout(M: LLVMModuleRef?): CPointer<ByteVar>? {
    return interpretCPointer<ByteVar>(kniBridge42(M.rawValue))
}

@ExperimentalForeignApi
fun LLVMSetDataLayout(M: LLVMModuleRef?, DataLayoutStr: String?): Unit {
    memScoped {
        return kniBridge43(M.rawValue, DataLayoutStr?.cstr?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMGetTarget(M: LLVMModuleRef?): CPointer<ByteVar>? {
    return interpretCPointer<ByteVar>(kniBridge44(M.rawValue))
}

@ExperimentalForeignApi
fun LLVMSetTarget(M: LLVMModuleRef?, Triple: String?): Unit {
    memScoped {
        return kniBridge45(M.rawValue, Triple?.cstr?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMCopyModuleFlagsMetadata(M: LLVMModuleRef?, Len: CValuesRef<size_tVar>?): CPointer<LLVMModuleFlagEntry>? {
    memScoped {
        return interpretCPointer<LLVMModuleFlagEntry>(kniBridge46(M.rawValue, Len?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMDisposeModuleFlagsMetadata(Entries: CValuesRef<LLVMModuleFlagEntry>?): Unit {
    memScoped {
        return kniBridge47(Entries?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMModuleFlagEntriesGetFlagBehavior(Entries: CValuesRef<LLVMModuleFlagEntry>?, Index: Int): LLVMModuleFlagBehavior {
    memScoped {
        return LLVMModuleFlagBehavior.byValue(kniBridge48(Entries?.getPointer(memScope).rawValue, Index))
    }
}

@ExperimentalForeignApi
fun LLVMModuleFlagEntriesGetKey(Entries: CValuesRef<LLVMModuleFlagEntry>?, Index: Int, Len: CValuesRef<size_tVar>?): CPointer<ByteVar>? {
    memScoped {
        return interpretCPointer<ByteVar>(kniBridge49(Entries?.getPointer(memScope).rawValue, Index, Len?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMModuleFlagEntriesGetMetadata(Entries: CValuesRef<LLVMModuleFlagEntry>?, Index: Int): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge50(Entries?.getPointer(memScope).rawValue, Index))
    }
}

@ExperimentalForeignApi
fun LLVMGetModuleFlag(M: LLVMModuleRef?, Key: String?, KeyLen: size_t): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge51(M.rawValue, Key?.cstr?.getPointer(memScope).rawValue, KeyLen))
    }
}

@ExperimentalForeignApi
fun LLVMAddModuleFlag(M: LLVMModuleRef?, Behavior: LLVMModuleFlagBehavior, Key: String?, KeyLen: size_t, Val: LLVMMetadataRef?): Unit {
    memScoped {
        return kniBridge52(M.rawValue, Behavior.value, Key?.cstr?.getPointer(memScope).rawValue, KeyLen, Val.rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMDumpModule(M: LLVMModuleRef?): Unit {
    return kniBridge53(M.rawValue)
}

@ExperimentalForeignApi
fun LLVMPrintModuleToFile(M: LLVMModuleRef?, Filename: String?, ErrorMessage: CValuesRef<CPointerVar<ByteVar>>?): LLVMBool {
    memScoped {
        return kniBridge54(M.rawValue, Filename?.cstr?.getPointer(memScope).rawValue, ErrorMessage?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMPrintModuleToString(M: LLVMModuleRef?): CPointer<ByteVar>? {
    return interpretCPointer<ByteVar>(kniBridge55(M.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetModuleInlineAsm(M: LLVMModuleRef?, Len: CValuesRef<size_tVar>?): CPointer<ByteVar>? {
    memScoped {
        return interpretCPointer<ByteVar>(kniBridge56(M.rawValue, Len?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMSetModuleInlineAsm2(M: LLVMModuleRef?, Asm: String?, Len: size_t): Unit {
    memScoped {
        return kniBridge57(M.rawValue, Asm?.cstr?.getPointer(memScope).rawValue, Len)
    }
}

@ExperimentalForeignApi
fun LLVMAppendModuleInlineAsm(M: LLVMModuleRef?, Asm: String?, Len: size_t): Unit {
    memScoped {
        return kniBridge58(M.rawValue, Asm?.cstr?.getPointer(memScope).rawValue, Len)
    }
}

@ExperimentalForeignApi
fun LLVMGetInlineAsm(Ty: LLVMTypeRef?, AsmString: CValuesRef<ByteVar>?, AsmStringSize: size_t, Constraints: CValuesRef<ByteVar>?, ConstraintsSize: size_t, HasSideEffects: LLVMBool, IsAlignStack: LLVMBool, Dialect: LLVMInlineAsmDialect, CanThrow: LLVMBool): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge59(Ty.rawValue, AsmString?.getPointer(memScope).rawValue, AsmStringSize, Constraints?.getPointer(memScope).rawValue, ConstraintsSize, HasSideEffects, IsAlignStack, Dialect.value, CanThrow))
    }
}

@ExperimentalForeignApi
fun LLVMGetModuleContext(M: LLVMModuleRef?): LLVMContextRef? {
    return interpretCPointer<LLVMOpaqueContext>(kniBridge60(M.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetTypeByName(M: LLVMModuleRef?, Name: String?): LLVMTypeRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueType>(kniBridge61(M.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMGetFirstNamedMetadata(M: LLVMModuleRef?): LLVMNamedMDNodeRef? {
    return interpretCPointer<LLVMOpaqueNamedMDNode>(kniBridge62(M.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetLastNamedMetadata(M: LLVMModuleRef?): LLVMNamedMDNodeRef? {
    return interpretCPointer<LLVMOpaqueNamedMDNode>(kniBridge63(M.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetNextNamedMetadata(NamedMDNode: LLVMNamedMDNodeRef?): LLVMNamedMDNodeRef? {
    return interpretCPointer<LLVMOpaqueNamedMDNode>(kniBridge64(NamedMDNode.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetPreviousNamedMetadata(NamedMDNode: LLVMNamedMDNodeRef?): LLVMNamedMDNodeRef? {
    return interpretCPointer<LLVMOpaqueNamedMDNode>(kniBridge65(NamedMDNode.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetNamedMetadata(M: LLVMModuleRef?, Name: String?, NameLen: size_t): LLVMNamedMDNodeRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueNamedMDNode>(kniBridge66(M.rawValue, Name?.cstr?.getPointer(memScope).rawValue, NameLen))
    }
}

@ExperimentalForeignApi
fun LLVMGetOrInsertNamedMetadata(M: LLVMModuleRef?, Name: String?, NameLen: size_t): LLVMNamedMDNodeRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueNamedMDNode>(kniBridge67(M.rawValue, Name?.cstr?.getPointer(memScope).rawValue, NameLen))
    }
}

@ExperimentalForeignApi
fun LLVMGetNamedMetadataName(NamedMD: LLVMNamedMDNodeRef?, NameLen: CValuesRef<size_tVar>?): CPointer<ByteVar>? {
    memScoped {
        return interpretCPointer<ByteVar>(kniBridge68(NamedMD.rawValue, NameLen?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMGetNamedMetadataNumOperands(M: LLVMModuleRef?, Name: String?): Int {
    memScoped {
        return kniBridge69(M.rawValue, Name?.cstr?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMGetNamedMetadataOperands(M: LLVMModuleRef?, Name: String?, Dest: CValuesRef<LLVMValueRefVar>?): Unit {
    memScoped {
        return kniBridge70(M.rawValue, Name?.cstr?.getPointer(memScope).rawValue, Dest?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMAddNamedMetadataOperand(M: LLVMModuleRef?, Name: String?, Val: LLVMValueRef?): Unit {
    memScoped {
        return kniBridge71(M.rawValue, Name?.cstr?.getPointer(memScope).rawValue, Val.rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMGetDebugLocDirectory(Val: LLVMValueRef?, Length: CValuesRef<IntVar>?): CPointer<ByteVar>? {
    memScoped {
        return interpretCPointer<ByteVar>(kniBridge72(Val.rawValue, Length?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMGetDebugLocFilename(Val: LLVMValueRef?, Length: CValuesRef<IntVar>?): CPointer<ByteVar>? {
    memScoped {
        return interpretCPointer<ByteVar>(kniBridge73(Val.rawValue, Length?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMGetDebugLocLine(Val: LLVMValueRef?): Int {
    return kniBridge74(Val.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetDebugLocColumn(Val: LLVMValueRef?): Int {
    return kniBridge75(Val.rawValue)
}

@ExperimentalForeignApi
fun LLVMAddFunction(M: LLVMModuleRef?, Name: String?, FunctionTy: LLVMTypeRef?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge76(M.rawValue, Name?.cstr?.getPointer(memScope).rawValue, FunctionTy.rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMGetNamedFunction(M: LLVMModuleRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge77(M.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMGetFirstFunction(M: LLVMModuleRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge78(M.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetLastFunction(M: LLVMModuleRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge79(M.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetNextFunction(Fn: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge80(Fn.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetPreviousFunction(Fn: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge81(Fn.rawValue))
}

@ExperimentalForeignApi
fun LLVMSetModuleInlineAsm(M: LLVMModuleRef?, Asm: String?): Unit {
    memScoped {
        return kniBridge82(M.rawValue, Asm?.cstr?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMGetTypeKind(Ty: LLVMTypeRef?): LLVMTypeKind {
    return LLVMTypeKind.byValue(kniBridge83(Ty.rawValue))
}

@ExperimentalForeignApi
fun LLVMTypeIsSized(Ty: LLVMTypeRef?): LLVMBool {
    return kniBridge84(Ty.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetTypeContext(Ty: LLVMTypeRef?): LLVMContextRef? {
    return interpretCPointer<LLVMOpaqueContext>(kniBridge85(Ty.rawValue))
}

@ExperimentalForeignApi
fun LLVMDumpType(Val: LLVMTypeRef?): Unit {
    return kniBridge86(Val.rawValue)
}

@ExperimentalForeignApi
fun LLVMPrintTypeToString(Val: LLVMTypeRef?): CPointer<ByteVar>? {
    return interpretCPointer<ByteVar>(kniBridge87(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMInt1TypeInContext(C: LLVMContextRef?): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge88(C.rawValue))
}

@ExperimentalForeignApi
fun LLVMInt8TypeInContext(C: LLVMContextRef?): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge89(C.rawValue))
}

@ExperimentalForeignApi
fun LLVMInt16TypeInContext(C: LLVMContextRef?): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge90(C.rawValue))
}

@ExperimentalForeignApi
fun LLVMInt32TypeInContext(C: LLVMContextRef?): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge91(C.rawValue))
}

@ExperimentalForeignApi
fun LLVMInt64TypeInContext(C: LLVMContextRef?): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge92(C.rawValue))
}

@ExperimentalForeignApi
fun LLVMInt128TypeInContext(C: LLVMContextRef?): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge93(C.rawValue))
}

@ExperimentalForeignApi
fun LLVMIntTypeInContext(C: LLVMContextRef?, NumBits: Int): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge94(C.rawValue, NumBits))
}

@ExperimentalForeignApi
fun LLVMGetIntTypeWidth(IntegerTy: LLVMTypeRef?): Int {
    return kniBridge95(IntegerTy.rawValue)
}

@ExperimentalForeignApi
fun LLVMHalfTypeInContext(C: LLVMContextRef?): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge96(C.rawValue))
}

@ExperimentalForeignApi
fun LLVMBFloatTypeInContext(C: LLVMContextRef?): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge97(C.rawValue))
}

@ExperimentalForeignApi
fun LLVMFloatTypeInContext(C: LLVMContextRef?): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge98(C.rawValue))
}

@ExperimentalForeignApi
fun LLVMDoubleTypeInContext(C: LLVMContextRef?): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge99(C.rawValue))
}

@ExperimentalForeignApi
fun LLVMX86FP80TypeInContext(C: LLVMContextRef?): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge100(C.rawValue))
}

@ExperimentalForeignApi
fun LLVMFP128TypeInContext(C: LLVMContextRef?): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge101(C.rawValue))
}

@ExperimentalForeignApi
fun LLVMPPCFP128TypeInContext(C: LLVMContextRef?): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge102(C.rawValue))
}

@ExperimentalForeignApi
fun LLVMBFloatType(): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge103())
}

@ExperimentalForeignApi
fun LLVMFunctionType(ReturnType: LLVMTypeRef?, ParamTypes: CValuesRef<LLVMTypeRefVar>?, ParamCount: Int, IsVarArg: LLVMBool): LLVMTypeRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueType>(kniBridge104(ReturnType.rawValue, ParamTypes?.getPointer(memScope).rawValue, ParamCount, IsVarArg))
    }
}

@ExperimentalForeignApi
fun LLVMIsFunctionVarArg(FunctionTy: LLVMTypeRef?): LLVMBool {
    return kniBridge105(FunctionTy.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetReturnType(FunctionTy: LLVMTypeRef?): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge106(FunctionTy.rawValue))
}

@ExperimentalForeignApi
fun LLVMCountParamTypes(FunctionTy: LLVMTypeRef?): Int {
    return kniBridge107(FunctionTy.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetParamTypes(FunctionTy: LLVMTypeRef?, Dest: CValuesRef<LLVMTypeRefVar>?): Unit {
    memScoped {
        return kniBridge108(FunctionTy.rawValue, Dest?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMStructTypeInContext(C: LLVMContextRef?, ElementTypes: CValuesRef<LLVMTypeRefVar>?, ElementCount: Int, Packed: LLVMBool): LLVMTypeRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueType>(kniBridge109(C.rawValue, ElementTypes?.getPointer(memScope).rawValue, ElementCount, Packed))
    }
}

@ExperimentalForeignApi
fun LLVMStructCreateNamed(C: LLVMContextRef?, Name: String?): LLVMTypeRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueType>(kniBridge110(C.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMGetStructName(Ty: LLVMTypeRef?): CPointer<ByteVar>? {
    return interpretCPointer<ByteVar>(kniBridge111(Ty.rawValue))
}

@ExperimentalForeignApi
fun LLVMStructSetBody(StructTy: LLVMTypeRef?, ElementTypes: CValuesRef<LLVMTypeRefVar>?, ElementCount: Int, Packed: LLVMBool): Unit {
    memScoped {
        return kniBridge112(StructTy.rawValue, ElementTypes?.getPointer(memScope).rawValue, ElementCount, Packed)
    }
}

@ExperimentalForeignApi
fun LLVMCountStructElementTypes(StructTy: LLVMTypeRef?): Int {
    return kniBridge113(StructTy.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetStructElementTypes(StructTy: LLVMTypeRef?, Dest: CValuesRef<LLVMTypeRefVar>?): Unit {
    memScoped {
        return kniBridge114(StructTy.rawValue, Dest?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMStructGetTypeAtIndex(StructTy: LLVMTypeRef?, i: Int): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge115(StructTy.rawValue, i))
}

@ExperimentalForeignApi
fun LLVMIsPackedStruct(StructTy: LLVMTypeRef?): LLVMBool {
    return kniBridge116(StructTy.rawValue)
}

@ExperimentalForeignApi
fun LLVMIsOpaqueStruct(StructTy: LLVMTypeRef?): LLVMBool {
    return kniBridge117(StructTy.rawValue)
}

@ExperimentalForeignApi
fun LLVMIsLiteralStruct(StructTy: LLVMTypeRef?): LLVMBool {
    return kniBridge118(StructTy.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetElementType(Ty: LLVMTypeRef?): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge119(Ty.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetSubtypes(Tp: LLVMTypeRef?, Arr: CValuesRef<LLVMTypeRefVar>?): Unit {
    memScoped {
        return kniBridge120(Tp.rawValue, Arr?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMGetNumContainedTypes(Tp: LLVMTypeRef?): Int {
    return kniBridge121(Tp.rawValue)
}

@ExperimentalForeignApi
fun LLVMArrayType(ElementType: LLVMTypeRef?, ElementCount: Int): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge122(ElementType.rawValue, ElementCount))
}

@ExperimentalForeignApi
fun LLVMGetArrayLength(ArrayTy: LLVMTypeRef?): Int {
    return kniBridge123(ArrayTy.rawValue)
}

@ExperimentalForeignApi
fun LLVMPointerType(ElementType: LLVMTypeRef?, AddressSpace: Int): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge124(ElementType.rawValue, AddressSpace))
}

@ExperimentalForeignApi
fun LLVMPointerTypeIsOpaque(Ty: LLVMTypeRef?): LLVMBool {
    return kniBridge125(Ty.rawValue)
}

@ExperimentalForeignApi
fun LLVMPointerTypeInContext(C: LLVMContextRef?, AddressSpace: Int): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge126(C.rawValue, AddressSpace))
}

@ExperimentalForeignApi
fun LLVMGetPointerAddressSpace(PointerTy: LLVMTypeRef?): Int {
    return kniBridge127(PointerTy.rawValue)
}

@ExperimentalForeignApi
fun LLVMVectorType(ElementType: LLVMTypeRef?, ElementCount: Int): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge128(ElementType.rawValue, ElementCount))
}

@ExperimentalForeignApi
fun LLVMScalableVectorType(ElementType: LLVMTypeRef?, ElementCount: Int): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge129(ElementType.rawValue, ElementCount))
}

@ExperimentalForeignApi
fun LLVMGetVectorSize(VectorTy: LLVMTypeRef?): Int {
    return kniBridge130(VectorTy.rawValue)
}

@ExperimentalForeignApi
fun LLVMVoidTypeInContext(C: LLVMContextRef?): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge131(C.rawValue))
}

@ExperimentalForeignApi
fun LLVMLabelTypeInContext(C: LLVMContextRef?): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge132(C.rawValue))
}

@ExperimentalForeignApi
fun LLVMX86MMXTypeInContext(C: LLVMContextRef?): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge133(C.rawValue))
}

@ExperimentalForeignApi
fun LLVMX86AMXTypeInContext(C: LLVMContextRef?): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge134(C.rawValue))
}

@ExperimentalForeignApi
fun LLVMTokenTypeInContext(C: LLVMContextRef?): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge135(C.rawValue))
}

@ExperimentalForeignApi
fun LLVMMetadataTypeInContext(C: LLVMContextRef?): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge136(C.rawValue))
}

@ExperimentalForeignApi
fun LLVMX86AMXType(): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge137())
}

@ExperimentalForeignApi
fun LLVMTargetExtTypeInContext(C: LLVMContextRef?, Name: String?, TypeParams: CValuesRef<LLVMTypeRefVar>?, TypeParamCount: Int, IntParams: CValuesRef<IntVar>?, IntParamCount: Int): LLVMTypeRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueType>(kniBridge138(C.rawValue, Name?.cstr?.getPointer(memScope).rawValue, TypeParams?.getPointer(memScope).rawValue, TypeParamCount, IntParams?.getPointer(memScope).rawValue, IntParamCount))
    }
}

@ExperimentalForeignApi
fun LLVMTypeOf(Val: LLVMValueRef?): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge139(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetValueKind(Val: LLVMValueRef?): LLVMValueKind {
    return LLVMValueKind.byValue(kniBridge140(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetValueName2(Val: LLVMValueRef?, Length: CValuesRef<size_tVar>?): CPointer<ByteVar>? {
    memScoped {
        return interpretCPointer<ByteVar>(kniBridge141(Val.rawValue, Length?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMSetValueName2(Val: LLVMValueRef?, Name: String?, NameLen: size_t): Unit {
    memScoped {
        return kniBridge142(Val.rawValue, Name?.cstr?.getPointer(memScope).rawValue, NameLen)
    }
}

@ExperimentalForeignApi
fun LLVMDumpValue(Val: LLVMValueRef?): Unit {
    return kniBridge143(Val.rawValue)
}

@ExperimentalForeignApi
fun LLVMPrintValueToString(Val: LLVMValueRef?): CPointer<ByteVar>? {
    return interpretCPointer<ByteVar>(kniBridge144(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMReplaceAllUsesWith(OldVal: LLVMValueRef?, NewVal: LLVMValueRef?): Unit {
    return kniBridge145(OldVal.rawValue, NewVal.rawValue)
}

@ExperimentalForeignApi
fun LLVMIsConstant(Val: LLVMValueRef?): LLVMBool {
    return kniBridge146(Val.rawValue)
}

@ExperimentalForeignApi
fun LLVMIsUndef(Val: LLVMValueRef?): LLVMBool {
    return kniBridge147(Val.rawValue)
}

@ExperimentalForeignApi
fun LLVMIsPoison(Val: LLVMValueRef?): LLVMBool {
    return kniBridge148(Val.rawValue)
}

@ExperimentalForeignApi
fun LLVMIsAArgument(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge149(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsABasicBlock(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge150(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAInlineAsm(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge151(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAUser(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge152(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAConstant(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge153(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsABlockAddress(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge154(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAConstantAggregateZero(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge155(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAConstantArray(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge156(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAConstantDataSequential(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge157(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAConstantDataArray(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge158(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAConstantDataVector(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge159(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAConstantExpr(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge160(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAConstantFP(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge161(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAConstantInt(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge162(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAConstantPointerNull(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge163(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAConstantStruct(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge164(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAConstantTokenNone(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge165(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAConstantVector(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge166(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAGlobalValue(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge167(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAGlobalAlias(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge168(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAGlobalObject(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge169(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAFunction(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge170(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAGlobalVariable(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge171(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAGlobalIFunc(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge172(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAUndefValue(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge173(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAPoisonValue(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge174(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAInstruction(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge175(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAUnaryOperator(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge176(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsABinaryOperator(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge177(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsACallInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge178(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAIntrinsicInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge179(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsADbgInfoIntrinsic(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge180(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsADbgVariableIntrinsic(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge181(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsADbgDeclareInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge182(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsADbgLabelInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge183(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAMemIntrinsic(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge184(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAMemCpyInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge185(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAMemMoveInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge186(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAMemSetInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge187(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsACmpInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge188(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAFCmpInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge189(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAICmpInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge190(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAExtractElementInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge191(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAGetElementPtrInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge192(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAInsertElementInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge193(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAInsertValueInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge194(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsALandingPadInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge195(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAPHINode(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge196(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsASelectInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge197(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAShuffleVectorInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge198(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAStoreInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge199(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsABranchInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge200(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAIndirectBrInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge201(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAInvokeInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge202(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAReturnInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge203(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsASwitchInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge204(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAUnreachableInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge205(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAResumeInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge206(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsACleanupReturnInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge207(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsACatchReturnInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge208(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsACatchSwitchInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge209(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsACallBrInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge210(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAFuncletPadInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge211(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsACatchPadInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge212(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsACleanupPadInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge213(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAUnaryInstruction(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge214(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAAllocaInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge215(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsACastInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge216(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAAddrSpaceCastInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge217(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsABitCastInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge218(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAFPExtInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge219(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAFPToSIInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge220(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAFPToUIInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge221(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAFPTruncInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge222(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAIntToPtrInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge223(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAPtrToIntInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge224(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsASExtInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge225(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsASIToFPInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge226(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsATruncInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge227(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAUIToFPInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge228(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAZExtInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge229(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAExtractValueInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge230(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsALoadInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge231(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAVAArgInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge232(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAFreezeInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge233(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAAtomicCmpXchgInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge234(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAAtomicRMWInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge235(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAFenceInst(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge236(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAMDNode(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge237(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsAMDString(Val: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge238(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetValueName(Val: LLVMValueRef?): CPointer<ByteVar>? {
    return interpretCPointer<ByteVar>(kniBridge239(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMSetValueName(Val: LLVMValueRef?, Name: String?): Unit {
    memScoped {
        return kniBridge240(Val.rawValue, Name?.cstr?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMGetFirstUse(Val: LLVMValueRef?): LLVMUseRef? {
    return interpretCPointer<LLVMOpaqueUse>(kniBridge241(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetNextUse(U: LLVMUseRef?): LLVMUseRef? {
    return interpretCPointer<LLVMOpaqueUse>(kniBridge242(U.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetUser(U: LLVMUseRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge243(U.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetUsedValue(U: LLVMUseRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge244(U.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetOperand(Val: LLVMValueRef?, Index: Int): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge245(Val.rawValue, Index))
}

@ExperimentalForeignApi
fun LLVMGetOperandUse(Val: LLVMValueRef?, Index: Int): LLVMUseRef? {
    return interpretCPointer<LLVMOpaqueUse>(kniBridge246(Val.rawValue, Index))
}

@ExperimentalForeignApi
fun LLVMSetOperand(User: LLVMValueRef?, Index: Int, Val: LLVMValueRef?): Unit {
    return kniBridge247(User.rawValue, Index, Val.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetNumOperands(Val: LLVMValueRef?): Int {
    return kniBridge248(Val.rawValue)
}

@ExperimentalForeignApi
fun LLVMConstNull(Ty: LLVMTypeRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge249(Ty.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstAllOnes(Ty: LLVMTypeRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge250(Ty.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetUndef(Ty: LLVMTypeRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge251(Ty.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetPoison(Ty: LLVMTypeRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge252(Ty.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsNull(Val: LLVMValueRef?): LLVMBool {
    return kniBridge253(Val.rawValue)
}

@ExperimentalForeignApi
fun LLVMConstPointerNull(Ty: LLVMTypeRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge254(Ty.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstInt(IntTy: LLVMTypeRef?, N: Long, SignExtend: LLVMBool): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge255(IntTy.rawValue, N, SignExtend))
}

@ExperimentalForeignApi
fun LLVMConstIntOfArbitraryPrecision(IntTy: LLVMTypeRef?, NumWords: Int, Words: CValuesRef<uint64_tVar>?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge256(IntTy.rawValue, NumWords, Words?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMConstIntOfString(IntTy: LLVMTypeRef?, Text: String?, Radix: uint8_t): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge257(IntTy.rawValue, Text?.cstr?.getPointer(memScope).rawValue, Radix))
    }
}

@ExperimentalForeignApi
fun LLVMConstIntOfStringAndSize(IntTy: LLVMTypeRef?, Text: String?, SLen: Int, Radix: uint8_t): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge258(IntTy.rawValue, Text?.cstr?.getPointer(memScope).rawValue, SLen, Radix))
    }
}

@ExperimentalForeignApi
fun LLVMConstReal(RealTy: LLVMTypeRef?, N: Double): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge259(RealTy.rawValue, N))
}

@ExperimentalForeignApi
fun LLVMConstRealOfString(RealTy: LLVMTypeRef?, Text: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge260(RealTy.rawValue, Text?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMConstRealOfStringAndSize(RealTy: LLVMTypeRef?, Text: String?, SLen: Int): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge261(RealTy.rawValue, Text?.cstr?.getPointer(memScope).rawValue, SLen))
    }
}

@ExperimentalForeignApi
fun LLVMConstIntGetZExtValue(ConstantVal: LLVMValueRef?): Long {
    return kniBridge262(ConstantVal.rawValue)
}

@ExperimentalForeignApi
fun LLVMConstIntGetSExtValue(ConstantVal: LLVMValueRef?): Long {
    return kniBridge263(ConstantVal.rawValue)
}

@ExperimentalForeignApi
fun LLVMConstRealGetDouble(ConstantVal: LLVMValueRef?, losesInfo: CValuesRef<LLVMBoolVar>?): Double {
    memScoped {
        return kniBridge264(ConstantVal.rawValue, losesInfo?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMConstStringInContext(C: LLVMContextRef?, Str: String?, Length: Int, DontNullTerminate: LLVMBool): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge265(C.rawValue, Str?.cstr?.getPointer(memScope).rawValue, Length, DontNullTerminate))
    }
}

@ExperimentalForeignApi
fun LLVMIsConstantString(c: LLVMValueRef?): LLVMBool {
    return kniBridge266(c.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetAsString(c: LLVMValueRef?, Length: CValuesRef<size_tVar>?): CPointer<ByteVar>? {
    memScoped {
        return interpretCPointer<ByteVar>(kniBridge267(c.rawValue, Length?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMConstStructInContext(C: LLVMContextRef?, ConstantVals: CValuesRef<LLVMValueRefVar>?, Count: Int, Packed: LLVMBool): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge268(C.rawValue, ConstantVals?.getPointer(memScope).rawValue, Count, Packed))
    }
}

@ExperimentalForeignApi
fun LLVMConstArray(ElementTy: LLVMTypeRef?, ConstantVals: CValuesRef<LLVMValueRefVar>?, Length: Int): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge269(ElementTy.rawValue, ConstantVals?.getPointer(memScope).rawValue, Length))
    }
}

@ExperimentalForeignApi
fun LLVMConstNamedStruct(StructTy: LLVMTypeRef?, ConstantVals: CValuesRef<LLVMValueRefVar>?, Count: Int): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge270(StructTy.rawValue, ConstantVals?.getPointer(memScope).rawValue, Count))
    }
}

@ExperimentalForeignApi
fun LLVMGetAggregateElement(C: LLVMValueRef?, Idx: Int): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge271(C.rawValue, Idx))
}

@ExperimentalForeignApi
fun LLVMGetElementAsConstant(C: LLVMValueRef?, idx: Int): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge272(C.rawValue, idx))
}

@ExperimentalForeignApi
fun LLVMConstVector(ScalarConstantVals: CValuesRef<LLVMValueRefVar>?, Size: Int): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge273(ScalarConstantVals?.getPointer(memScope).rawValue, Size))
    }
}

@ExperimentalForeignApi
fun LLVMGetConstOpcode(ConstantVal: LLVMValueRef?): LLVMOpcode {
    return LLVMOpcode.byValue(kniBridge274(ConstantVal.rawValue))
}

@ExperimentalForeignApi
fun LLVMAlignOf(Ty: LLVMTypeRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge275(Ty.rawValue))
}

@ExperimentalForeignApi
fun LLVMSizeOf(Ty: LLVMTypeRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge276(Ty.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstNeg(ConstantVal: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge277(ConstantVal.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstNSWNeg(ConstantVal: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge278(ConstantVal.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstNUWNeg(ConstantVal: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge279(ConstantVal.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstNot(ConstantVal: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge280(ConstantVal.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstAdd(LHSConstant: LLVMValueRef?, RHSConstant: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge281(LHSConstant.rawValue, RHSConstant.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstNSWAdd(LHSConstant: LLVMValueRef?, RHSConstant: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge282(LHSConstant.rawValue, RHSConstant.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstNUWAdd(LHSConstant: LLVMValueRef?, RHSConstant: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge283(LHSConstant.rawValue, RHSConstant.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstSub(LHSConstant: LLVMValueRef?, RHSConstant: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge284(LHSConstant.rawValue, RHSConstant.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstNSWSub(LHSConstant: LLVMValueRef?, RHSConstant: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge285(LHSConstant.rawValue, RHSConstant.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstNUWSub(LHSConstant: LLVMValueRef?, RHSConstant: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge286(LHSConstant.rawValue, RHSConstant.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstMul(LHSConstant: LLVMValueRef?, RHSConstant: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge287(LHSConstant.rawValue, RHSConstant.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstNSWMul(LHSConstant: LLVMValueRef?, RHSConstant: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge288(LHSConstant.rawValue, RHSConstant.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstNUWMul(LHSConstant: LLVMValueRef?, RHSConstant: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge289(LHSConstant.rawValue, RHSConstant.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstAnd(LHSConstant: LLVMValueRef?, RHSConstant: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge290(LHSConstant.rawValue, RHSConstant.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstOr(LHSConstant: LLVMValueRef?, RHSConstant: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge291(LHSConstant.rawValue, RHSConstant.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstXor(LHSConstant: LLVMValueRef?, RHSConstant: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge292(LHSConstant.rawValue, RHSConstant.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstICmp(Predicate: LLVMIntPredicate, LHSConstant: LLVMValueRef?, RHSConstant: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge293(Predicate.value, LHSConstant.rawValue, RHSConstant.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstFCmp(Predicate: LLVMRealPredicate, LHSConstant: LLVMValueRef?, RHSConstant: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge294(Predicate.value, LHSConstant.rawValue, RHSConstant.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstShl(LHSConstant: LLVMValueRef?, RHSConstant: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge295(LHSConstant.rawValue, RHSConstant.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstLShr(LHSConstant: LLVMValueRef?, RHSConstant: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge296(LHSConstant.rawValue, RHSConstant.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstAShr(LHSConstant: LLVMValueRef?, RHSConstant: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge297(LHSConstant.rawValue, RHSConstant.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstGEP2(Ty: LLVMTypeRef?, ConstantVal: LLVMValueRef?, ConstantIndices: CValuesRef<LLVMValueRefVar>?, NumIndices: Int): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge298(Ty.rawValue, ConstantVal.rawValue, ConstantIndices?.getPointer(memScope).rawValue, NumIndices))
    }
}

@ExperimentalForeignApi
fun LLVMConstInBoundsGEP2(Ty: LLVMTypeRef?, ConstantVal: LLVMValueRef?, ConstantIndices: CValuesRef<LLVMValueRefVar>?, NumIndices: Int): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge299(Ty.rawValue, ConstantVal.rawValue, ConstantIndices?.getPointer(memScope).rawValue, NumIndices))
    }
}

@ExperimentalForeignApi
fun LLVMConstTrunc(ConstantVal: LLVMValueRef?, ToType: LLVMTypeRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge300(ConstantVal.rawValue, ToType.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstSExt(ConstantVal: LLVMValueRef?, ToType: LLVMTypeRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge301(ConstantVal.rawValue, ToType.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstZExt(ConstantVal: LLVMValueRef?, ToType: LLVMTypeRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge302(ConstantVal.rawValue, ToType.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstFPTrunc(ConstantVal: LLVMValueRef?, ToType: LLVMTypeRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge303(ConstantVal.rawValue, ToType.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstFPExt(ConstantVal: LLVMValueRef?, ToType: LLVMTypeRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge304(ConstantVal.rawValue, ToType.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstUIToFP(ConstantVal: LLVMValueRef?, ToType: LLVMTypeRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge305(ConstantVal.rawValue, ToType.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstSIToFP(ConstantVal: LLVMValueRef?, ToType: LLVMTypeRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge306(ConstantVal.rawValue, ToType.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstFPToUI(ConstantVal: LLVMValueRef?, ToType: LLVMTypeRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge307(ConstantVal.rawValue, ToType.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstFPToSI(ConstantVal: LLVMValueRef?, ToType: LLVMTypeRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge308(ConstantVal.rawValue, ToType.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstPtrToInt(ConstantVal: LLVMValueRef?, ToType: LLVMTypeRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge309(ConstantVal.rawValue, ToType.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstIntToPtr(ConstantVal: LLVMValueRef?, ToType: LLVMTypeRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge310(ConstantVal.rawValue, ToType.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstBitCast(ConstantVal: LLVMValueRef?, ToType: LLVMTypeRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge311(ConstantVal.rawValue, ToType.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstAddrSpaceCast(ConstantVal: LLVMValueRef?, ToType: LLVMTypeRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge312(ConstantVal.rawValue, ToType.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstZExtOrBitCast(ConstantVal: LLVMValueRef?, ToType: LLVMTypeRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge313(ConstantVal.rawValue, ToType.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstSExtOrBitCast(ConstantVal: LLVMValueRef?, ToType: LLVMTypeRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge314(ConstantVal.rawValue, ToType.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstTruncOrBitCast(ConstantVal: LLVMValueRef?, ToType: LLVMTypeRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge315(ConstantVal.rawValue, ToType.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstPointerCast(ConstantVal: LLVMValueRef?, ToType: LLVMTypeRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge316(ConstantVal.rawValue, ToType.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstIntCast(ConstantVal: LLVMValueRef?, ToType: LLVMTypeRef?, isSigned: LLVMBool): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge317(ConstantVal.rawValue, ToType.rawValue, isSigned))
}

@ExperimentalForeignApi
fun LLVMConstFPCast(ConstantVal: LLVMValueRef?, ToType: LLVMTypeRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge318(ConstantVal.rawValue, ToType.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstSelect(ConstantCondition: LLVMValueRef?, ConstantIfTrue: LLVMValueRef?, ConstantIfFalse: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge319(ConstantCondition.rawValue, ConstantIfTrue.rawValue, ConstantIfFalse.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstExtractElement(VectorConstant: LLVMValueRef?, IndexConstant: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge320(VectorConstant.rawValue, IndexConstant.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstInsertElement(VectorConstant: LLVMValueRef?, ElementValueConstant: LLVMValueRef?, IndexConstant: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge321(VectorConstant.rawValue, ElementValueConstant.rawValue, IndexConstant.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstShuffleVector(VectorAConstant: LLVMValueRef?, VectorBConstant: LLVMValueRef?, MaskConstant: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge322(VectorAConstant.rawValue, VectorBConstant.rawValue, MaskConstant.rawValue))
}

@ExperimentalForeignApi
fun LLVMBlockAddress(F: LLVMValueRef?, BB: LLVMBasicBlockRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge323(F.rawValue, BB.rawValue))
}

@ExperimentalForeignApi
fun LLVMConstInlineAsm(Ty: LLVMTypeRef?, AsmString: String?, Constraints: String?, HasSideEffects: LLVMBool, IsAlignStack: LLVMBool): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge324(Ty.rawValue, AsmString?.cstr?.getPointer(memScope).rawValue, Constraints?.cstr?.getPointer(memScope).rawValue, HasSideEffects, IsAlignStack))
    }
}

@ExperimentalForeignApi
fun LLVMGetGlobalParent(Global: LLVMValueRef?): LLVMModuleRef? {
    return interpretCPointer<LLVMOpaqueModule>(kniBridge325(Global.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsDeclaration(Global: LLVMValueRef?): LLVMBool {
    return kniBridge326(Global.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetLinkage(Global: LLVMValueRef?): LLVMLinkage {
    return LLVMLinkage.byValue(kniBridge327(Global.rawValue))
}

@ExperimentalForeignApi
fun LLVMSetLinkage(Global: LLVMValueRef?, Linkage: LLVMLinkage): Unit {
    return kniBridge328(Global.rawValue, Linkage.value)
}

@ExperimentalForeignApi
fun LLVMGetSection(Global: LLVMValueRef?): CPointer<ByteVar>? {
    return interpretCPointer<ByteVar>(kniBridge329(Global.rawValue))
}

@ExperimentalForeignApi
fun LLVMSetSection(Global: LLVMValueRef?, Section: String?): Unit {
    memScoped {
        return kniBridge330(Global.rawValue, Section?.cstr?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMGetVisibility(Global: LLVMValueRef?): LLVMVisibility {
    return LLVMVisibility.byValue(kniBridge331(Global.rawValue))
}

@ExperimentalForeignApi
fun LLVMSetVisibility(Global: LLVMValueRef?, Viz: LLVMVisibility): Unit {
    return kniBridge332(Global.rawValue, Viz.value)
}

@ExperimentalForeignApi
fun LLVMGetDLLStorageClass(Global: LLVMValueRef?): LLVMDLLStorageClass {
    return LLVMDLLStorageClass.byValue(kniBridge333(Global.rawValue))
}

@ExperimentalForeignApi
fun LLVMSetDLLStorageClass(Global: LLVMValueRef?, Class: LLVMDLLStorageClass): Unit {
    return kniBridge334(Global.rawValue, Class.value)
}

@ExperimentalForeignApi
fun LLVMGetUnnamedAddress(Global: LLVMValueRef?): LLVMUnnamedAddr {
    return LLVMUnnamedAddr.byValue(kniBridge335(Global.rawValue))
}

@ExperimentalForeignApi
fun LLVMSetUnnamedAddress(Global: LLVMValueRef?, UnnamedAddr: LLVMUnnamedAddr): Unit {
    return kniBridge336(Global.rawValue, UnnamedAddr.value)
}

@ExperimentalForeignApi
fun LLVMGlobalGetValueType(Global: LLVMValueRef?): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge337(Global.rawValue))
}

@ExperimentalForeignApi
fun LLVMHasUnnamedAddr(Global: LLVMValueRef?): LLVMBool {
    return kniBridge338(Global.rawValue)
}

@ExperimentalForeignApi
fun LLVMSetUnnamedAddr(Global: LLVMValueRef?, HasUnnamedAddr: LLVMBool): Unit {
    return kniBridge339(Global.rawValue, HasUnnamedAddr)
}

@ExperimentalForeignApi
fun LLVMGetAlignment(V: LLVMValueRef?): Int {
    return kniBridge340(V.rawValue)
}

@ExperimentalForeignApi
fun LLVMSetAlignment(V: LLVMValueRef?, Bytes: Int): Unit {
    return kniBridge341(V.rawValue, Bytes)
}

@ExperimentalForeignApi
fun LLVMGlobalSetMetadata(Global: LLVMValueRef?, Kind: Int, MD: LLVMMetadataRef?): Unit {
    return kniBridge342(Global.rawValue, Kind, MD.rawValue)
}

@ExperimentalForeignApi
fun LLVMGlobalEraseMetadata(Global: LLVMValueRef?, Kind: Int): Unit {
    return kniBridge343(Global.rawValue, Kind)
}

@ExperimentalForeignApi
fun LLVMGlobalClearMetadata(Global: LLVMValueRef?): Unit {
    return kniBridge344(Global.rawValue)
}

@ExperimentalForeignApi
fun LLVMGlobalCopyAllMetadata(Value: LLVMValueRef?, NumEntries: CValuesRef<size_tVar>?): CPointer<LLVMValueMetadataEntry>? {
    memScoped {
        return interpretCPointer<LLVMValueMetadataEntry>(kniBridge345(Value.rawValue, NumEntries?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMDisposeValueMetadataEntries(Entries: CValuesRef<LLVMValueMetadataEntry>?): Unit {
    memScoped {
        return kniBridge346(Entries?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMValueMetadataEntriesGetKind(Entries: CValuesRef<LLVMValueMetadataEntry>?, Index: Int): Int {
    memScoped {
        return kniBridge347(Entries?.getPointer(memScope).rawValue, Index)
    }
}

@ExperimentalForeignApi
fun LLVMValueMetadataEntriesGetMetadata(Entries: CValuesRef<LLVMValueMetadataEntry>?, Index: Int): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge348(Entries?.getPointer(memScope).rawValue, Index))
    }
}

@ExperimentalForeignApi
fun LLVMAddGlobal(M: LLVMModuleRef?, Ty: LLVMTypeRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge349(M.rawValue, Ty.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMAddGlobalInAddressSpace(M: LLVMModuleRef?, Ty: LLVMTypeRef?, Name: String?, AddressSpace: Int): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge350(M.rawValue, Ty.rawValue, Name?.cstr?.getPointer(memScope).rawValue, AddressSpace))
    }
}

@ExperimentalForeignApi
fun LLVMGetNamedGlobal(M: LLVMModuleRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge351(M.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMGetFirstGlobal(M: LLVMModuleRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge352(M.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetLastGlobal(M: LLVMModuleRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge353(M.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetNextGlobal(GlobalVar: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge354(GlobalVar.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetPreviousGlobal(GlobalVar: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge355(GlobalVar.rawValue))
}

@ExperimentalForeignApi
fun LLVMDeleteGlobal(GlobalVar: LLVMValueRef?): Unit {
    return kniBridge356(GlobalVar.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetInitializer(GlobalVar: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge357(GlobalVar.rawValue))
}

@ExperimentalForeignApi
fun LLVMSetInitializer(GlobalVar: LLVMValueRef?, ConstantVal: LLVMValueRef?): Unit {
    return kniBridge358(GlobalVar.rawValue, ConstantVal.rawValue)
}

@ExperimentalForeignApi
fun LLVMIsThreadLocal(GlobalVar: LLVMValueRef?): LLVMBool {
    return kniBridge359(GlobalVar.rawValue)
}

@ExperimentalForeignApi
fun LLVMSetThreadLocal(GlobalVar: LLVMValueRef?, IsThreadLocal: LLVMBool): Unit {
    return kniBridge360(GlobalVar.rawValue, IsThreadLocal)
}

@ExperimentalForeignApi
fun LLVMIsGlobalConstant(GlobalVar: LLVMValueRef?): LLVMBool {
    return kniBridge361(GlobalVar.rawValue)
}

@ExperimentalForeignApi
fun LLVMSetGlobalConstant(GlobalVar: LLVMValueRef?, IsConstant: LLVMBool): Unit {
    return kniBridge362(GlobalVar.rawValue, IsConstant)
}

@ExperimentalForeignApi
fun LLVMGetThreadLocalMode(GlobalVar: LLVMValueRef?): LLVMThreadLocalMode {
    return LLVMThreadLocalMode.byValue(kniBridge363(GlobalVar.rawValue))
}

@ExperimentalForeignApi
fun LLVMSetThreadLocalMode(GlobalVar: LLVMValueRef?, Mode: LLVMThreadLocalMode): Unit {
    return kniBridge364(GlobalVar.rawValue, Mode.value)
}

@ExperimentalForeignApi
fun LLVMIsExternallyInitialized(GlobalVar: LLVMValueRef?): LLVMBool {
    return kniBridge365(GlobalVar.rawValue)
}

@ExperimentalForeignApi
fun LLVMSetExternallyInitialized(GlobalVar: LLVMValueRef?, IsExtInit: LLVMBool): Unit {
    return kniBridge366(GlobalVar.rawValue, IsExtInit)
}

@ExperimentalForeignApi
fun LLVMAddAlias2(M: LLVMModuleRef?, ValueTy: LLVMTypeRef?, AddrSpace: Int, Aliasee: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge367(M.rawValue, ValueTy.rawValue, AddrSpace, Aliasee.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMGetNamedGlobalAlias(M: LLVMModuleRef?, Name: String?, NameLen: size_t): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge368(M.rawValue, Name?.cstr?.getPointer(memScope).rawValue, NameLen))
    }
}

@ExperimentalForeignApi
fun LLVMGetFirstGlobalAlias(M: LLVMModuleRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge369(M.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetLastGlobalAlias(M: LLVMModuleRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge370(M.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetNextGlobalAlias(GA: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge371(GA.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetPreviousGlobalAlias(GA: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge372(GA.rawValue))
}

@ExperimentalForeignApi
fun LLVMAliasGetAliasee(Alias: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge373(Alias.rawValue))
}

@ExperimentalForeignApi
fun LLVMAliasSetAliasee(Alias: LLVMValueRef?, Aliasee: LLVMValueRef?): Unit {
    return kniBridge374(Alias.rawValue, Aliasee.rawValue)
}

@ExperimentalForeignApi
fun LLVMDeleteFunction(Fn: LLVMValueRef?): Unit {
    return kniBridge375(Fn.rawValue)
}

@ExperimentalForeignApi
fun LLVMHasPersonalityFn(Fn: LLVMValueRef?): LLVMBool {
    return kniBridge376(Fn.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetPersonalityFn(Fn: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge377(Fn.rawValue))
}

@ExperimentalForeignApi
fun LLVMSetPersonalityFn(Fn: LLVMValueRef?, PersonalityFn: LLVMValueRef?): Unit {
    return kniBridge378(Fn.rawValue, PersonalityFn.rawValue)
}

@ExperimentalForeignApi
fun LLVMLookupIntrinsicID(Name: String?, NameLen: size_t): Int {
    memScoped {
        return kniBridge379(Name?.cstr?.getPointer(memScope).rawValue, NameLen)
    }
}

@ExperimentalForeignApi
fun LLVMGetIntrinsicID(Fn: LLVMValueRef?): Int {
    return kniBridge380(Fn.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetIntrinsicDeclaration(Mod: LLVMModuleRef?, ID: Int, ParamTypes: CValuesRef<LLVMTypeRefVar>?, ParamCount: size_t): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge381(Mod.rawValue, ID, ParamTypes?.getPointer(memScope).rawValue, ParamCount))
    }
}

@ExperimentalForeignApi
fun LLVMIntrinsicGetType(Ctx: LLVMContextRef?, ID: Int, ParamTypes: CValuesRef<LLVMTypeRefVar>?, ParamCount: size_t): LLVMTypeRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueType>(kniBridge382(Ctx.rawValue, ID, ParamTypes?.getPointer(memScope).rawValue, ParamCount))
    }
}

@ExperimentalForeignApi
fun LLVMIntrinsicGetName(ID: Int, NameLength: CValuesRef<size_tVar>?): CPointer<ByteVar>? {
    memScoped {
        return interpretCPointer<ByteVar>(kniBridge383(ID, NameLength?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMIntrinsicCopyOverloadedName(ID: Int, ParamTypes: CValuesRef<LLVMTypeRefVar>?, ParamCount: size_t, NameLength: CValuesRef<size_tVar>?): CPointer<ByteVar>? {
    memScoped {
        return interpretCPointer<ByteVar>(kniBridge384(ID, ParamTypes?.getPointer(memScope).rawValue, ParamCount, NameLength?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMIntrinsicCopyOverloadedName2(Mod: LLVMModuleRef?, ID: Int, ParamTypes: CValuesRef<LLVMTypeRefVar>?, ParamCount: size_t, NameLength: CValuesRef<size_tVar>?): CPointer<ByteVar>? {
    memScoped {
        return interpretCPointer<ByteVar>(kniBridge385(Mod.rawValue, ID, ParamTypes?.getPointer(memScope).rawValue, ParamCount, NameLength?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMIntrinsicIsOverloaded(ID: Int): LLVMBool {
    return kniBridge386(ID)
}

@ExperimentalForeignApi
fun LLVMGetFunctionCallConv(Fn: LLVMValueRef?): Int {
    return kniBridge387(Fn.rawValue)
}

@ExperimentalForeignApi
fun LLVMSetFunctionCallConv(Fn: LLVMValueRef?, CC: Int): Unit {
    return kniBridge388(Fn.rawValue, CC)
}

@ExperimentalForeignApi
fun LLVMGetGC(Fn: LLVMValueRef?): CPointer<ByteVar>? {
    return interpretCPointer<ByteVar>(kniBridge389(Fn.rawValue))
}

@ExperimentalForeignApi
fun LLVMSetGC(Fn: LLVMValueRef?, Name: String?): Unit {
    memScoped {
        return kniBridge390(Fn.rawValue, Name?.cstr?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMAddAttributeAtIndex(F: LLVMValueRef?, Idx: LLVMAttributeIndex, A: LLVMAttributeRef?): Unit {
    return kniBridge391(F.rawValue, Idx, A.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetAttributeCountAtIndex(F: LLVMValueRef?, Idx: LLVMAttributeIndex): Int {
    return kniBridge392(F.rawValue, Idx)
}

@ExperimentalForeignApi
fun LLVMGetAttributesAtIndex(F: LLVMValueRef?, Idx: LLVMAttributeIndex, Attrs: CValuesRef<LLVMAttributeRefVar>?): Unit {
    memScoped {
        return kniBridge393(F.rawValue, Idx, Attrs?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMGetEnumAttributeAtIndex(F: LLVMValueRef?, Idx: LLVMAttributeIndex, KindID: Int): LLVMAttributeRef? {
    return interpretCPointer<LLVMOpaqueAttributeRef>(kniBridge394(F.rawValue, Idx, KindID))
}

@ExperimentalForeignApi
fun LLVMGetStringAttributeAtIndex(F: LLVMValueRef?, Idx: LLVMAttributeIndex, K: String?, KLen: Int): LLVMAttributeRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueAttributeRef>(kniBridge395(F.rawValue, Idx, K?.cstr?.getPointer(memScope).rawValue, KLen))
    }
}

@ExperimentalForeignApi
fun LLVMRemoveEnumAttributeAtIndex(F: LLVMValueRef?, Idx: LLVMAttributeIndex, KindID: Int): Unit {
    return kniBridge396(F.rawValue, Idx, KindID)
}

@ExperimentalForeignApi
fun LLVMRemoveStringAttributeAtIndex(F: LLVMValueRef?, Idx: LLVMAttributeIndex, K: String?, KLen: Int): Unit {
    memScoped {
        return kniBridge397(F.rawValue, Idx, K?.cstr?.getPointer(memScope).rawValue, KLen)
    }
}

@ExperimentalForeignApi
fun LLVMAddTargetDependentFunctionAttr(Fn: LLVMValueRef?, A: String?, V: String?): Unit {
    memScoped {
        return kniBridge398(Fn.rawValue, A?.cstr?.getPointer(memScope).rawValue, V?.cstr?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMCountParams(Fn: LLVMValueRef?): Int {
    return kniBridge399(Fn.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetParams(Fn: LLVMValueRef?, Params: CValuesRef<LLVMValueRefVar>?): Unit {
    memScoped {
        return kniBridge400(Fn.rawValue, Params?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMGetParam(Fn: LLVMValueRef?, Index: Int): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge401(Fn.rawValue, Index))
}

@ExperimentalForeignApi
fun LLVMGetParamParent(Inst: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge402(Inst.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetFirstParam(Fn: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge403(Fn.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetLastParam(Fn: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge404(Fn.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetNextParam(Arg: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge405(Arg.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetPreviousParam(Arg: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge406(Arg.rawValue))
}

@ExperimentalForeignApi
fun LLVMSetParamAlignment(Arg: LLVMValueRef?, Align: Int): Unit {
    return kniBridge407(Arg.rawValue, Align)
}

@ExperimentalForeignApi
fun LLVMAddGlobalIFunc(M: LLVMModuleRef?, Name: String?, NameLen: size_t, Ty: LLVMTypeRef?, AddrSpace: Int, Resolver: LLVMValueRef?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge408(M.rawValue, Name?.cstr?.getPointer(memScope).rawValue, NameLen, Ty.rawValue, AddrSpace, Resolver.rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMGetNamedGlobalIFunc(M: LLVMModuleRef?, Name: String?, NameLen: size_t): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge409(M.rawValue, Name?.cstr?.getPointer(memScope).rawValue, NameLen))
    }
}

@ExperimentalForeignApi
fun LLVMGetFirstGlobalIFunc(M: LLVMModuleRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge410(M.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetLastGlobalIFunc(M: LLVMModuleRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge411(M.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetNextGlobalIFunc(IFunc: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge412(IFunc.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetPreviousGlobalIFunc(IFunc: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge413(IFunc.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetGlobalIFuncResolver(IFunc: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge414(IFunc.rawValue))
}

@ExperimentalForeignApi
fun LLVMSetGlobalIFuncResolver(IFunc: LLVMValueRef?, Resolver: LLVMValueRef?): Unit {
    return kniBridge415(IFunc.rawValue, Resolver.rawValue)
}

@ExperimentalForeignApi
fun LLVMEraseGlobalIFunc(IFunc: LLVMValueRef?): Unit {
    return kniBridge416(IFunc.rawValue)
}

@ExperimentalForeignApi
fun LLVMRemoveGlobalIFunc(IFunc: LLVMValueRef?): Unit {
    return kniBridge417(IFunc.rawValue)
}

@ExperimentalForeignApi
fun LLVMMDStringInContext2(C: LLVMContextRef?, Str: String?, SLen: size_t): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge418(C.rawValue, Str?.cstr?.getPointer(memScope).rawValue, SLen))
    }
}

@ExperimentalForeignApi
fun LLVMMDNodeInContext2(C: LLVMContextRef?, MDs: CValuesRef<LLVMMetadataRefVar>?, Count: size_t): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge419(C.rawValue, MDs?.getPointer(memScope).rawValue, Count))
    }
}

@ExperimentalForeignApi
fun LLVMMetadataAsValue(C: LLVMContextRef?, MD: LLVMMetadataRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge420(C.rawValue, MD.rawValue))
}

@ExperimentalForeignApi
fun LLVMValueAsMetadata(Val: LLVMValueRef?): LLVMMetadataRef? {
    return interpretCPointer<LLVMOpaqueMetadata>(kniBridge421(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetMDString(V: LLVMValueRef?, Length: CValuesRef<IntVar>?): CPointer<ByteVar>? {
    memScoped {
        return interpretCPointer<ByteVar>(kniBridge422(V.rawValue, Length?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMGetMDNodeNumOperands(V: LLVMValueRef?): Int {
    return kniBridge423(V.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetMDNodeOperands(V: LLVMValueRef?, Dest: CValuesRef<LLVMValueRefVar>?): Unit {
    memScoped {
        return kniBridge424(V.rawValue, Dest?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMMDStringInContext(C: LLVMContextRef?, Str: String?, SLen: Int): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge425(C.rawValue, Str?.cstr?.getPointer(memScope).rawValue, SLen))
    }
}

@ExperimentalForeignApi
fun LLVMMDNodeInContext(C: LLVMContextRef?, Vals: CValuesRef<LLVMValueRefVar>?, Count: Int): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge426(C.rawValue, Vals?.getPointer(memScope).rawValue, Count))
    }
}

@ExperimentalForeignApi
fun LLVMBasicBlockAsValue(BB: LLVMBasicBlockRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge427(BB.rawValue))
}

@ExperimentalForeignApi
fun LLVMValueIsBasicBlock(Val: LLVMValueRef?): LLVMBool {
    return kniBridge428(Val.rawValue)
}

@ExperimentalForeignApi
fun LLVMValueAsBasicBlock(Val: LLVMValueRef?): LLVMBasicBlockRef? {
    return interpretCPointer<LLVMOpaqueBasicBlock>(kniBridge429(Val.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetBasicBlockName(BB: LLVMBasicBlockRef?): CPointer<ByteVar>? {
    return interpretCPointer<ByteVar>(kniBridge430(BB.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetBasicBlockParent(BB: LLVMBasicBlockRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge431(BB.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetBasicBlockTerminator(BB: LLVMBasicBlockRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge432(BB.rawValue))
}

@ExperimentalForeignApi
fun LLVMCountBasicBlocks(Fn: LLVMValueRef?): Int {
    return kniBridge433(Fn.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetBasicBlocks(Fn: LLVMValueRef?, BasicBlocks: CValuesRef<LLVMBasicBlockRefVar>?): Unit {
    memScoped {
        return kniBridge434(Fn.rawValue, BasicBlocks?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMGetFirstBasicBlock(Fn: LLVMValueRef?): LLVMBasicBlockRef? {
    return interpretCPointer<LLVMOpaqueBasicBlock>(kniBridge435(Fn.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetLastBasicBlock(Fn: LLVMValueRef?): LLVMBasicBlockRef? {
    return interpretCPointer<LLVMOpaqueBasicBlock>(kniBridge436(Fn.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetNextBasicBlock(BB: LLVMBasicBlockRef?): LLVMBasicBlockRef? {
    return interpretCPointer<LLVMOpaqueBasicBlock>(kniBridge437(BB.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetPreviousBasicBlock(BB: LLVMBasicBlockRef?): LLVMBasicBlockRef? {
    return interpretCPointer<LLVMOpaqueBasicBlock>(kniBridge438(BB.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetEntryBasicBlock(Fn: LLVMValueRef?): LLVMBasicBlockRef? {
    return interpretCPointer<LLVMOpaqueBasicBlock>(kniBridge439(Fn.rawValue))
}

@ExperimentalForeignApi
fun LLVMInsertExistingBasicBlockAfterInsertBlock(Builder: LLVMBuilderRef?, BB: LLVMBasicBlockRef?): Unit {
    return kniBridge440(Builder.rawValue, BB.rawValue)
}

@ExperimentalForeignApi
fun LLVMAppendExistingBasicBlock(Fn: LLVMValueRef?, BB: LLVMBasicBlockRef?): Unit {
    return kniBridge441(Fn.rawValue, BB.rawValue)
}

@ExperimentalForeignApi
fun LLVMCreateBasicBlockInContext(C: LLVMContextRef?, Name: String?): LLVMBasicBlockRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueBasicBlock>(kniBridge442(C.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMAppendBasicBlockInContext(C: LLVMContextRef?, Fn: LLVMValueRef?, Name: String?): LLVMBasicBlockRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueBasicBlock>(kniBridge443(C.rawValue, Fn.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMInsertBasicBlockInContext(C: LLVMContextRef?, BB: LLVMBasicBlockRef?, Name: String?): LLVMBasicBlockRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueBasicBlock>(kniBridge444(C.rawValue, BB.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMDeleteBasicBlock(BB: LLVMBasicBlockRef?): Unit {
    return kniBridge445(BB.rawValue)
}

@ExperimentalForeignApi
fun LLVMRemoveBasicBlockFromParent(BB: LLVMBasicBlockRef?): Unit {
    return kniBridge446(BB.rawValue)
}

@ExperimentalForeignApi
fun LLVMMoveBasicBlockBefore(BB: LLVMBasicBlockRef?, MovePos: LLVMBasicBlockRef?): Unit {
    return kniBridge447(BB.rawValue, MovePos.rawValue)
}

@ExperimentalForeignApi
fun LLVMMoveBasicBlockAfter(BB: LLVMBasicBlockRef?, MovePos: LLVMBasicBlockRef?): Unit {
    return kniBridge448(BB.rawValue, MovePos.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetFirstInstruction(BB: LLVMBasicBlockRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge449(BB.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetLastInstruction(BB: LLVMBasicBlockRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge450(BB.rawValue))
}

@ExperimentalForeignApi
fun LLVMHasMetadata(Val: LLVMValueRef?): Int {
    return kniBridge451(Val.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetMetadata(Val: LLVMValueRef?, KindID: Int): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge452(Val.rawValue, KindID))
}

@ExperimentalForeignApi
fun LLVMSetMetadata(Val: LLVMValueRef?, KindID: Int, Node: LLVMValueRef?): Unit {
    return kniBridge453(Val.rawValue, KindID, Node.rawValue)
}

@ExperimentalForeignApi
fun LLVMInstructionGetAllMetadataOtherThanDebugLoc(Instr: LLVMValueRef?, NumEntries: CValuesRef<size_tVar>?): CPointer<LLVMValueMetadataEntry>? {
    memScoped {
        return interpretCPointer<LLVMValueMetadataEntry>(kniBridge454(Instr.rawValue, NumEntries?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMGetInstructionParent(Inst: LLVMValueRef?): LLVMBasicBlockRef? {
    return interpretCPointer<LLVMOpaqueBasicBlock>(kniBridge455(Inst.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetNextInstruction(Inst: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge456(Inst.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetPreviousInstruction(Inst: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge457(Inst.rawValue))
}

@ExperimentalForeignApi
fun LLVMInstructionRemoveFromParent(Inst: LLVMValueRef?): Unit {
    return kniBridge458(Inst.rawValue)
}

@ExperimentalForeignApi
fun LLVMInstructionEraseFromParent(Inst: LLVMValueRef?): Unit {
    return kniBridge459(Inst.rawValue)
}

@ExperimentalForeignApi
fun LLVMDeleteInstruction(Inst: LLVMValueRef?): Unit {
    return kniBridge460(Inst.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetInstructionOpcode(Inst: LLVMValueRef?): LLVMOpcode {
    return LLVMOpcode.byValue(kniBridge461(Inst.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetICmpPredicate(Inst: LLVMValueRef?): LLVMIntPredicate {
    return LLVMIntPredicate.byValue(kniBridge462(Inst.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetFCmpPredicate(Inst: LLVMValueRef?): LLVMRealPredicate {
    return LLVMRealPredicate.byValue(kniBridge463(Inst.rawValue))
}

@ExperimentalForeignApi
fun LLVMInstructionClone(Inst: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge464(Inst.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsATerminatorInst(Inst: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge465(Inst.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetNumArgOperands(Instr: LLVMValueRef?): Int {
    return kniBridge466(Instr.rawValue)
}

@ExperimentalForeignApi
fun LLVMSetInstructionCallConv(Instr: LLVMValueRef?, CC: Int): Unit {
    return kniBridge467(Instr.rawValue, CC)
}

@ExperimentalForeignApi
fun LLVMGetInstructionCallConv(Instr: LLVMValueRef?): Int {
    return kniBridge468(Instr.rawValue)
}

@ExperimentalForeignApi
fun LLVMSetInstrParamAlignment(Instr: LLVMValueRef?, Idx: LLVMAttributeIndex, Align: Int): Unit {
    return kniBridge469(Instr.rawValue, Idx, Align)
}

@ExperimentalForeignApi
fun LLVMAddCallSiteAttribute(C: LLVMValueRef?, Idx: LLVMAttributeIndex, A: LLVMAttributeRef?): Unit {
    return kniBridge470(C.rawValue, Idx, A.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetCallSiteAttributeCount(C: LLVMValueRef?, Idx: LLVMAttributeIndex): Int {
    return kniBridge471(C.rawValue, Idx)
}

@ExperimentalForeignApi
fun LLVMGetCallSiteAttributes(C: LLVMValueRef?, Idx: LLVMAttributeIndex, Attrs: CValuesRef<LLVMAttributeRefVar>?): Unit {
    memScoped {
        return kniBridge472(C.rawValue, Idx, Attrs?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMGetCallSiteEnumAttribute(C: LLVMValueRef?, Idx: LLVMAttributeIndex, KindID: Int): LLVMAttributeRef? {
    return interpretCPointer<LLVMOpaqueAttributeRef>(kniBridge473(C.rawValue, Idx, KindID))
}

@ExperimentalForeignApi
fun LLVMGetCallSiteStringAttribute(C: LLVMValueRef?, Idx: LLVMAttributeIndex, K: String?, KLen: Int): LLVMAttributeRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueAttributeRef>(kniBridge474(C.rawValue, Idx, K?.cstr?.getPointer(memScope).rawValue, KLen))
    }
}

@ExperimentalForeignApi
fun LLVMRemoveCallSiteEnumAttribute(C: LLVMValueRef?, Idx: LLVMAttributeIndex, KindID: Int): Unit {
    return kniBridge475(C.rawValue, Idx, KindID)
}

@ExperimentalForeignApi
fun LLVMRemoveCallSiteStringAttribute(C: LLVMValueRef?, Idx: LLVMAttributeIndex, K: String?, KLen: Int): Unit {
    memScoped {
        return kniBridge476(C.rawValue, Idx, K?.cstr?.getPointer(memScope).rawValue, KLen)
    }
}

@ExperimentalForeignApi
fun LLVMGetCalledFunctionType(C: LLVMValueRef?): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge477(C.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetCalledValue(Instr: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge478(Instr.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsTailCall(CallInst: LLVMValueRef?): LLVMBool {
    return kniBridge479(CallInst.rawValue)
}

@ExperimentalForeignApi
fun LLVMSetTailCall(CallInst: LLVMValueRef?, IsTailCall: LLVMBool): Unit {
    return kniBridge480(CallInst.rawValue, IsTailCall)
}

@ExperimentalForeignApi
fun LLVMGetNormalDest(InvokeInst: LLVMValueRef?): LLVMBasicBlockRef? {
    return interpretCPointer<LLVMOpaqueBasicBlock>(kniBridge481(InvokeInst.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetUnwindDest(InvokeInst: LLVMValueRef?): LLVMBasicBlockRef? {
    return interpretCPointer<LLVMOpaqueBasicBlock>(kniBridge482(InvokeInst.rawValue))
}

@ExperimentalForeignApi
fun LLVMSetNormalDest(InvokeInst: LLVMValueRef?, B: LLVMBasicBlockRef?): Unit {
    return kniBridge483(InvokeInst.rawValue, B.rawValue)
}

@ExperimentalForeignApi
fun LLVMSetUnwindDest(InvokeInst: LLVMValueRef?, B: LLVMBasicBlockRef?): Unit {
    return kniBridge484(InvokeInst.rawValue, B.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetNumSuccessors(Term: LLVMValueRef?): Int {
    return kniBridge485(Term.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetSuccessor(Term: LLVMValueRef?, i: Int): LLVMBasicBlockRef? {
    return interpretCPointer<LLVMOpaqueBasicBlock>(kniBridge486(Term.rawValue, i))
}

@ExperimentalForeignApi
fun LLVMSetSuccessor(Term: LLVMValueRef?, i: Int, block: LLVMBasicBlockRef?): Unit {
    return kniBridge487(Term.rawValue, i, block.rawValue)
}

@ExperimentalForeignApi
fun LLVMIsConditional(Branch: LLVMValueRef?): LLVMBool {
    return kniBridge488(Branch.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetCondition(Branch: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge489(Branch.rawValue))
}

@ExperimentalForeignApi
fun LLVMSetCondition(Branch: LLVMValueRef?, Cond: LLVMValueRef?): Unit {
    return kniBridge490(Branch.rawValue, Cond.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetSwitchDefaultDest(SwitchInstr: LLVMValueRef?): LLVMBasicBlockRef? {
    return interpretCPointer<LLVMOpaqueBasicBlock>(kniBridge491(SwitchInstr.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetAllocatedType(Alloca: LLVMValueRef?): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge492(Alloca.rawValue))
}

@ExperimentalForeignApi
fun LLVMIsInBounds(GEP: LLVMValueRef?): LLVMBool {
    return kniBridge493(GEP.rawValue)
}

@ExperimentalForeignApi
fun LLVMSetIsInBounds(GEP: LLVMValueRef?, InBounds: LLVMBool): Unit {
    return kniBridge494(GEP.rawValue, InBounds)
}

@ExperimentalForeignApi
fun LLVMGetGEPSourceElementType(GEP: LLVMValueRef?): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge495(GEP.rawValue))
}

@ExperimentalForeignApi
fun LLVMAddIncoming(PhiNode: LLVMValueRef?, IncomingValues: CValuesRef<LLVMValueRefVar>?, IncomingBlocks: CValuesRef<LLVMBasicBlockRefVar>?, Count: Int): Unit {
    memScoped {
        return kniBridge496(PhiNode.rawValue, IncomingValues?.getPointer(memScope).rawValue, IncomingBlocks?.getPointer(memScope).rawValue, Count)
    }
}

@ExperimentalForeignApi
fun LLVMCountIncoming(PhiNode: LLVMValueRef?): Int {
    return kniBridge497(PhiNode.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetIncomingValue(PhiNode: LLVMValueRef?, Index: Int): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge498(PhiNode.rawValue, Index))
}

@ExperimentalForeignApi
fun LLVMGetIncomingBlock(PhiNode: LLVMValueRef?, Index: Int): LLVMBasicBlockRef? {
    return interpretCPointer<LLVMOpaqueBasicBlock>(kniBridge499(PhiNode.rawValue, Index))
}

@ExperimentalForeignApi
fun LLVMGetNumIndices(Inst: LLVMValueRef?): Int {
    return kniBridge500(Inst.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetIndices(Inst: LLVMValueRef?): CPointer<IntVar>? {
    return interpretCPointer<IntVar>(kniBridge501(Inst.rawValue))
}

@ExperimentalForeignApi
fun LLVMCreateBuilderInContext(C: LLVMContextRef?): LLVMBuilderRef? {
    return interpretCPointer<LLVMOpaqueBuilder>(kniBridge502(C.rawValue))
}

@ExperimentalForeignApi
fun LLVMPositionBuilder(Builder: LLVMBuilderRef?, Block: LLVMBasicBlockRef?, Instr: LLVMValueRef?): Unit {
    return kniBridge503(Builder.rawValue, Block.rawValue, Instr.rawValue)
}

@ExperimentalForeignApi
fun LLVMPositionBuilderBefore(Builder: LLVMBuilderRef?, Instr: LLVMValueRef?): Unit {
    return kniBridge504(Builder.rawValue, Instr.rawValue)
}

@ExperimentalForeignApi
fun LLVMPositionBuilderAtEnd(Builder: LLVMBuilderRef?, Block: LLVMBasicBlockRef?): Unit {
    return kniBridge505(Builder.rawValue, Block.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetInsertBlock(Builder: LLVMBuilderRef?): LLVMBasicBlockRef? {
    return interpretCPointer<LLVMOpaqueBasicBlock>(kniBridge506(Builder.rawValue))
}

@ExperimentalForeignApi
fun LLVMClearInsertionPosition(Builder: LLVMBuilderRef?): Unit {
    return kniBridge507(Builder.rawValue)
}

@ExperimentalForeignApi
fun LLVMInsertIntoBuilder(Builder: LLVMBuilderRef?, Instr: LLVMValueRef?): Unit {
    return kniBridge508(Builder.rawValue, Instr.rawValue)
}

@ExperimentalForeignApi
fun LLVMInsertIntoBuilderWithName(Builder: LLVMBuilderRef?, Instr: LLVMValueRef?, Name: String?): Unit {
    memScoped {
        return kniBridge509(Builder.rawValue, Instr.rawValue, Name?.cstr?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMDisposeBuilder(Builder: LLVMBuilderRef?): Unit {
    return kniBridge510(Builder.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetCurrentDebugLocation2(Builder: LLVMBuilderRef?): LLVMMetadataRef? {
    return interpretCPointer<LLVMOpaqueMetadata>(kniBridge511(Builder.rawValue))
}

@ExperimentalForeignApi
fun LLVMSetCurrentDebugLocation2(Builder: LLVMBuilderRef?, Loc: LLVMMetadataRef?): Unit {
    return kniBridge512(Builder.rawValue, Loc.rawValue)
}

@ExperimentalForeignApi
fun LLVMSetInstDebugLocation(Builder: LLVMBuilderRef?, Inst: LLVMValueRef?): Unit {
    return kniBridge513(Builder.rawValue, Inst.rawValue)
}

@ExperimentalForeignApi
fun LLVMAddMetadataToInst(Builder: LLVMBuilderRef?, Inst: LLVMValueRef?): Unit {
    return kniBridge514(Builder.rawValue, Inst.rawValue)
}

@ExperimentalForeignApi
fun LLVMBuilderGetDefaultFPMathTag(Builder: LLVMBuilderRef?): LLVMMetadataRef? {
    return interpretCPointer<LLVMOpaqueMetadata>(kniBridge515(Builder.rawValue))
}

@ExperimentalForeignApi
fun LLVMBuilderSetDefaultFPMathTag(Builder: LLVMBuilderRef?, FPMathTag: LLVMMetadataRef?): Unit {
    return kniBridge516(Builder.rawValue, FPMathTag.rawValue)
}

@ExperimentalForeignApi
fun LLVMSetCurrentDebugLocation(Builder: LLVMBuilderRef?, L: LLVMValueRef?): Unit {
    return kniBridge517(Builder.rawValue, L.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetCurrentDebugLocation(Builder: LLVMBuilderRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge518(Builder.rawValue))
}

@ExperimentalForeignApi
fun LLVMBuildRetVoid(arg0: LLVMBuilderRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge519(arg0.rawValue))
}

@ExperimentalForeignApi
fun LLVMBuildRet(arg0: LLVMBuilderRef?, V: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge520(arg0.rawValue, V.rawValue))
}

@ExperimentalForeignApi
fun LLVMBuildAggregateRet(arg0: LLVMBuilderRef?, RetVals: CValuesRef<LLVMValueRefVar>?, N: Int): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge521(arg0.rawValue, RetVals?.getPointer(memScope).rawValue, N))
    }
}

@ExperimentalForeignApi
fun LLVMBuildBr(arg0: LLVMBuilderRef?, Dest: LLVMBasicBlockRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge522(arg0.rawValue, Dest.rawValue))
}

@ExperimentalForeignApi
fun LLVMBuildCondBr(arg0: LLVMBuilderRef?, If: LLVMValueRef?, Then: LLVMBasicBlockRef?, Else: LLVMBasicBlockRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge523(arg0.rawValue, If.rawValue, Then.rawValue, Else.rawValue))
}

@ExperimentalForeignApi
fun LLVMBuildSwitch(arg0: LLVMBuilderRef?, V: LLVMValueRef?, Else: LLVMBasicBlockRef?, NumCases: Int): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge524(arg0.rawValue, V.rawValue, Else.rawValue, NumCases))
}

@ExperimentalForeignApi
fun LLVMBuildIndirectBr(B: LLVMBuilderRef?, Addr: LLVMValueRef?, NumDests: Int): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge525(B.rawValue, Addr.rawValue, NumDests))
}

@ExperimentalForeignApi
fun LLVMBuildInvoke2(arg0: LLVMBuilderRef?, Ty: LLVMTypeRef?, Fn: LLVMValueRef?, Args: CValuesRef<LLVMValueRefVar>?, NumArgs: Int, Then: LLVMBasicBlockRef?, Catch: LLVMBasicBlockRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge526(arg0.rawValue, Ty.rawValue, Fn.rawValue, Args?.getPointer(memScope).rawValue, NumArgs, Then.rawValue, Catch.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildUnreachable(arg0: LLVMBuilderRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge527(arg0.rawValue))
}

@ExperimentalForeignApi
fun LLVMBuildResume(B: LLVMBuilderRef?, Exn: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge528(B.rawValue, Exn.rawValue))
}

@ExperimentalForeignApi
fun LLVMBuildLandingPad(B: LLVMBuilderRef?, Ty: LLVMTypeRef?, PersFn: LLVMValueRef?, NumClauses: Int, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge529(B.rawValue, Ty.rawValue, PersFn.rawValue, NumClauses, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildCleanupRet(B: LLVMBuilderRef?, CatchPad: LLVMValueRef?, BB: LLVMBasicBlockRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge530(B.rawValue, CatchPad.rawValue, BB.rawValue))
}

@ExperimentalForeignApi
fun LLVMBuildCatchRet(B: LLVMBuilderRef?, CatchPad: LLVMValueRef?, BB: LLVMBasicBlockRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge531(B.rawValue, CatchPad.rawValue, BB.rawValue))
}

@ExperimentalForeignApi
fun LLVMBuildCatchPad(B: LLVMBuilderRef?, ParentPad: LLVMValueRef?, Args: CValuesRef<LLVMValueRefVar>?, NumArgs: Int, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge532(B.rawValue, ParentPad.rawValue, Args?.getPointer(memScope).rawValue, NumArgs, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildCleanupPad(B: LLVMBuilderRef?, ParentPad: LLVMValueRef?, Args: CValuesRef<LLVMValueRefVar>?, NumArgs: Int, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge533(B.rawValue, ParentPad.rawValue, Args?.getPointer(memScope).rawValue, NumArgs, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildCatchSwitch(B: LLVMBuilderRef?, ParentPad: LLVMValueRef?, UnwindBB: LLVMBasicBlockRef?, NumHandlers: Int, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge534(B.rawValue, ParentPad.rawValue, UnwindBB.rawValue, NumHandlers, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMAddCase(Switch: LLVMValueRef?, OnVal: LLVMValueRef?, Dest: LLVMBasicBlockRef?): Unit {
    return kniBridge535(Switch.rawValue, OnVal.rawValue, Dest.rawValue)
}

@ExperimentalForeignApi
fun LLVMAddDestination(IndirectBr: LLVMValueRef?, Dest: LLVMBasicBlockRef?): Unit {
    return kniBridge536(IndirectBr.rawValue, Dest.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetNumClauses(LandingPad: LLVMValueRef?): Int {
    return kniBridge537(LandingPad.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetClause(LandingPad: LLVMValueRef?, Idx: Int): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge538(LandingPad.rawValue, Idx))
}

@ExperimentalForeignApi
fun LLVMAddClause(LandingPad: LLVMValueRef?, ClauseVal: LLVMValueRef?): Unit {
    return kniBridge539(LandingPad.rawValue, ClauseVal.rawValue)
}

@ExperimentalForeignApi
fun LLVMIsCleanup(LandingPad: LLVMValueRef?): LLVMBool {
    return kniBridge540(LandingPad.rawValue)
}

@ExperimentalForeignApi
fun LLVMSetCleanup(LandingPad: LLVMValueRef?, Val: LLVMBool): Unit {
    return kniBridge541(LandingPad.rawValue, Val)
}

@ExperimentalForeignApi
fun LLVMAddHandler(CatchSwitch: LLVMValueRef?, Dest: LLVMBasicBlockRef?): Unit {
    return kniBridge542(CatchSwitch.rawValue, Dest.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetNumHandlers(CatchSwitch: LLVMValueRef?): Int {
    return kniBridge543(CatchSwitch.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetHandlers(CatchSwitch: LLVMValueRef?, Handlers: CValuesRef<LLVMBasicBlockRefVar>?): Unit {
    memScoped {
        return kniBridge544(CatchSwitch.rawValue, Handlers?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMGetArgOperand(Funclet: LLVMValueRef?, i: Int): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge545(Funclet.rawValue, i))
}

@ExperimentalForeignApi
fun LLVMSetArgOperand(Funclet: LLVMValueRef?, i: Int, value: LLVMValueRef?): Unit {
    return kniBridge546(Funclet.rawValue, i, value.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetParentCatchSwitch(CatchPad: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge547(CatchPad.rawValue))
}

@ExperimentalForeignApi
fun LLVMSetParentCatchSwitch(CatchPad: LLVMValueRef?, CatchSwitch: LLVMValueRef?): Unit {
    return kniBridge548(CatchPad.rawValue, CatchSwitch.rawValue)
}

@ExperimentalForeignApi
fun LLVMBuildAdd(arg0: LLVMBuilderRef?, LHS: LLVMValueRef?, RHS: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge549(arg0.rawValue, LHS.rawValue, RHS.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildNSWAdd(arg0: LLVMBuilderRef?, LHS: LLVMValueRef?, RHS: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge550(arg0.rawValue, LHS.rawValue, RHS.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildNUWAdd(arg0: LLVMBuilderRef?, LHS: LLVMValueRef?, RHS: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge551(arg0.rawValue, LHS.rawValue, RHS.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildFAdd(arg0: LLVMBuilderRef?, LHS: LLVMValueRef?, RHS: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge552(arg0.rawValue, LHS.rawValue, RHS.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildSub(arg0: LLVMBuilderRef?, LHS: LLVMValueRef?, RHS: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge553(arg0.rawValue, LHS.rawValue, RHS.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildNSWSub(arg0: LLVMBuilderRef?, LHS: LLVMValueRef?, RHS: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge554(arg0.rawValue, LHS.rawValue, RHS.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildNUWSub(arg0: LLVMBuilderRef?, LHS: LLVMValueRef?, RHS: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge555(arg0.rawValue, LHS.rawValue, RHS.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildFSub(arg0: LLVMBuilderRef?, LHS: LLVMValueRef?, RHS: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge556(arg0.rawValue, LHS.rawValue, RHS.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildMul(arg0: LLVMBuilderRef?, LHS: LLVMValueRef?, RHS: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge557(arg0.rawValue, LHS.rawValue, RHS.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildNSWMul(arg0: LLVMBuilderRef?, LHS: LLVMValueRef?, RHS: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge558(arg0.rawValue, LHS.rawValue, RHS.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildNUWMul(arg0: LLVMBuilderRef?, LHS: LLVMValueRef?, RHS: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge559(arg0.rawValue, LHS.rawValue, RHS.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildFMul(arg0: LLVMBuilderRef?, LHS: LLVMValueRef?, RHS: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge560(arg0.rawValue, LHS.rawValue, RHS.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildUDiv(arg0: LLVMBuilderRef?, LHS: LLVMValueRef?, RHS: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge561(arg0.rawValue, LHS.rawValue, RHS.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildExactUDiv(arg0: LLVMBuilderRef?, LHS: LLVMValueRef?, RHS: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge562(arg0.rawValue, LHS.rawValue, RHS.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildSDiv(arg0: LLVMBuilderRef?, LHS: LLVMValueRef?, RHS: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge563(arg0.rawValue, LHS.rawValue, RHS.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildExactSDiv(arg0: LLVMBuilderRef?, LHS: LLVMValueRef?, RHS: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge564(arg0.rawValue, LHS.rawValue, RHS.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildFDiv(arg0: LLVMBuilderRef?, LHS: LLVMValueRef?, RHS: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge565(arg0.rawValue, LHS.rawValue, RHS.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildURem(arg0: LLVMBuilderRef?, LHS: LLVMValueRef?, RHS: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge566(arg0.rawValue, LHS.rawValue, RHS.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildSRem(arg0: LLVMBuilderRef?, LHS: LLVMValueRef?, RHS: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge567(arg0.rawValue, LHS.rawValue, RHS.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildFRem(arg0: LLVMBuilderRef?, LHS: LLVMValueRef?, RHS: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge568(arg0.rawValue, LHS.rawValue, RHS.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildShl(arg0: LLVMBuilderRef?, LHS: LLVMValueRef?, RHS: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge569(arg0.rawValue, LHS.rawValue, RHS.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildLShr(arg0: LLVMBuilderRef?, LHS: LLVMValueRef?, RHS: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge570(arg0.rawValue, LHS.rawValue, RHS.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildAShr(arg0: LLVMBuilderRef?, LHS: LLVMValueRef?, RHS: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge571(arg0.rawValue, LHS.rawValue, RHS.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildAnd(arg0: LLVMBuilderRef?, LHS: LLVMValueRef?, RHS: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge572(arg0.rawValue, LHS.rawValue, RHS.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildOr(arg0: LLVMBuilderRef?, LHS: LLVMValueRef?, RHS: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge573(arg0.rawValue, LHS.rawValue, RHS.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildXor(arg0: LLVMBuilderRef?, LHS: LLVMValueRef?, RHS: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge574(arg0.rawValue, LHS.rawValue, RHS.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildBinOp(B: LLVMBuilderRef?, Op: LLVMOpcode, LHS: LLVMValueRef?, RHS: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge575(B.rawValue, Op.value, LHS.rawValue, RHS.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildNeg(arg0: LLVMBuilderRef?, V: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge576(arg0.rawValue, V.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildNSWNeg(B: LLVMBuilderRef?, V: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge577(B.rawValue, V.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildNUWNeg(B: LLVMBuilderRef?, V: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge578(B.rawValue, V.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildFNeg(arg0: LLVMBuilderRef?, V: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge579(arg0.rawValue, V.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildNot(arg0: LLVMBuilderRef?, V: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge580(arg0.rawValue, V.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildMalloc(arg0: LLVMBuilderRef?, Ty: LLVMTypeRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge581(arg0.rawValue, Ty.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildArrayMalloc(arg0: LLVMBuilderRef?, Ty: LLVMTypeRef?, Val: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge582(arg0.rawValue, Ty.rawValue, Val.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildMemSet(B: LLVMBuilderRef?, Ptr: LLVMValueRef?, Val: LLVMValueRef?, Len: LLVMValueRef?, Align: Int): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge583(B.rawValue, Ptr.rawValue, Val.rawValue, Len.rawValue, Align))
}

@ExperimentalForeignApi
fun LLVMBuildMemCpy(B: LLVMBuilderRef?, Dst: LLVMValueRef?, DstAlign: Int, Src: LLVMValueRef?, SrcAlign: Int, Size: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge584(B.rawValue, Dst.rawValue, DstAlign, Src.rawValue, SrcAlign, Size.rawValue))
}

@ExperimentalForeignApi
fun LLVMBuildMemMove(B: LLVMBuilderRef?, Dst: LLVMValueRef?, DstAlign: Int, Src: LLVMValueRef?, SrcAlign: Int, Size: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge585(B.rawValue, Dst.rawValue, DstAlign, Src.rawValue, SrcAlign, Size.rawValue))
}

@ExperimentalForeignApi
fun LLVMBuildAlloca(arg0: LLVMBuilderRef?, Ty: LLVMTypeRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge586(arg0.rawValue, Ty.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildArrayAlloca(arg0: LLVMBuilderRef?, Ty: LLVMTypeRef?, Val: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge587(arg0.rawValue, Ty.rawValue, Val.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildFree(arg0: LLVMBuilderRef?, PointerVal: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge588(arg0.rawValue, PointerVal.rawValue))
}

@ExperimentalForeignApi
fun LLVMBuildLoad2(arg0: LLVMBuilderRef?, Ty: LLVMTypeRef?, PointerVal: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge589(arg0.rawValue, Ty.rawValue, PointerVal.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildStore(arg0: LLVMBuilderRef?, Val: LLVMValueRef?, Ptr: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge590(arg0.rawValue, Val.rawValue, Ptr.rawValue))
}

@ExperimentalForeignApi
fun LLVMBuildGEP2(B: LLVMBuilderRef?, Ty: LLVMTypeRef?, Pointer: LLVMValueRef?, Indices: CValuesRef<LLVMValueRefVar>?, NumIndices: Int, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge591(B.rawValue, Ty.rawValue, Pointer.rawValue, Indices?.getPointer(memScope).rawValue, NumIndices, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildInBoundsGEP2(B: LLVMBuilderRef?, Ty: LLVMTypeRef?, Pointer: LLVMValueRef?, Indices: CValuesRef<LLVMValueRefVar>?, NumIndices: Int, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge592(B.rawValue, Ty.rawValue, Pointer.rawValue, Indices?.getPointer(memScope).rawValue, NumIndices, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildStructGEP2(B: LLVMBuilderRef?, Ty: LLVMTypeRef?, Pointer: LLVMValueRef?, Idx: Int, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge593(B.rawValue, Ty.rawValue, Pointer.rawValue, Idx, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildGlobalString(B: LLVMBuilderRef?, Str: String?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge594(B.rawValue, Str?.cstr?.getPointer(memScope).rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildGlobalStringPtr(B: LLVMBuilderRef?, Str: String?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge595(B.rawValue, Str?.cstr?.getPointer(memScope).rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMGetVolatile(MemoryAccessInst: LLVMValueRef?): LLVMBool {
    return kniBridge596(MemoryAccessInst.rawValue)
}

@ExperimentalForeignApi
fun LLVMSetVolatile(MemoryAccessInst: LLVMValueRef?, IsVolatile: LLVMBool): Unit {
    return kniBridge597(MemoryAccessInst.rawValue, IsVolatile)
}

@ExperimentalForeignApi
fun LLVMGetWeak(CmpXchgInst: LLVMValueRef?): LLVMBool {
    return kniBridge598(CmpXchgInst.rawValue)
}

@ExperimentalForeignApi
fun LLVMSetWeak(CmpXchgInst: LLVMValueRef?, IsWeak: LLVMBool): Unit {
    return kniBridge599(CmpXchgInst.rawValue, IsWeak)
}

@ExperimentalForeignApi
fun LLVMGetOrdering(MemoryAccessInst: LLVMValueRef?): LLVMAtomicOrdering {
    return LLVMAtomicOrdering.byValue(kniBridge600(MemoryAccessInst.rawValue))
}

@ExperimentalForeignApi
fun LLVMSetOrdering(MemoryAccessInst: LLVMValueRef?, Ordering: LLVMAtomicOrdering): Unit {
    return kniBridge601(MemoryAccessInst.rawValue, Ordering.value)
}

@ExperimentalForeignApi
fun LLVMGetAtomicRMWBinOp(AtomicRMWInst: LLVMValueRef?): LLVMAtomicRMWBinOp {
    return LLVMAtomicRMWBinOp.byValue(kniBridge602(AtomicRMWInst.rawValue))
}

@ExperimentalForeignApi
fun LLVMSetAtomicRMWBinOp(AtomicRMWInst: LLVMValueRef?, BinOp: LLVMAtomicRMWBinOp): Unit {
    return kniBridge603(AtomicRMWInst.rawValue, BinOp.value)
}

@ExperimentalForeignApi
fun LLVMBuildTrunc(arg0: LLVMBuilderRef?, Val: LLVMValueRef?, DestTy: LLVMTypeRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge604(arg0.rawValue, Val.rawValue, DestTy.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildZExt(arg0: LLVMBuilderRef?, Val: LLVMValueRef?, DestTy: LLVMTypeRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge605(arg0.rawValue, Val.rawValue, DestTy.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildSExt(arg0: LLVMBuilderRef?, Val: LLVMValueRef?, DestTy: LLVMTypeRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge606(arg0.rawValue, Val.rawValue, DestTy.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildFPToUI(arg0: LLVMBuilderRef?, Val: LLVMValueRef?, DestTy: LLVMTypeRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge607(arg0.rawValue, Val.rawValue, DestTy.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildFPToSI(arg0: LLVMBuilderRef?, Val: LLVMValueRef?, DestTy: LLVMTypeRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge608(arg0.rawValue, Val.rawValue, DestTy.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildUIToFP(arg0: LLVMBuilderRef?, Val: LLVMValueRef?, DestTy: LLVMTypeRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge609(arg0.rawValue, Val.rawValue, DestTy.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildSIToFP(arg0: LLVMBuilderRef?, Val: LLVMValueRef?, DestTy: LLVMTypeRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge610(arg0.rawValue, Val.rawValue, DestTy.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildFPTrunc(arg0: LLVMBuilderRef?, Val: LLVMValueRef?, DestTy: LLVMTypeRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge611(arg0.rawValue, Val.rawValue, DestTy.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildFPExt(arg0: LLVMBuilderRef?, Val: LLVMValueRef?, DestTy: LLVMTypeRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge612(arg0.rawValue, Val.rawValue, DestTy.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildPtrToInt(arg0: LLVMBuilderRef?, Val: LLVMValueRef?, DestTy: LLVMTypeRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge613(arg0.rawValue, Val.rawValue, DestTy.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildIntToPtr(arg0: LLVMBuilderRef?, Val: LLVMValueRef?, DestTy: LLVMTypeRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge614(arg0.rawValue, Val.rawValue, DestTy.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildBitCast(arg0: LLVMBuilderRef?, Val: LLVMValueRef?, DestTy: LLVMTypeRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge615(arg0.rawValue, Val.rawValue, DestTy.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildAddrSpaceCast(arg0: LLVMBuilderRef?, Val: LLVMValueRef?, DestTy: LLVMTypeRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge616(arg0.rawValue, Val.rawValue, DestTy.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildZExtOrBitCast(arg0: LLVMBuilderRef?, Val: LLVMValueRef?, DestTy: LLVMTypeRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge617(arg0.rawValue, Val.rawValue, DestTy.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildSExtOrBitCast(arg0: LLVMBuilderRef?, Val: LLVMValueRef?, DestTy: LLVMTypeRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge618(arg0.rawValue, Val.rawValue, DestTy.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildTruncOrBitCast(arg0: LLVMBuilderRef?, Val: LLVMValueRef?, DestTy: LLVMTypeRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge619(arg0.rawValue, Val.rawValue, DestTy.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildCast(B: LLVMBuilderRef?, Op: LLVMOpcode, Val: LLVMValueRef?, DestTy: LLVMTypeRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge620(B.rawValue, Op.value, Val.rawValue, DestTy.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildPointerCast(arg0: LLVMBuilderRef?, Val: LLVMValueRef?, DestTy: LLVMTypeRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge621(arg0.rawValue, Val.rawValue, DestTy.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildIntCast2(arg0: LLVMBuilderRef?, Val: LLVMValueRef?, DestTy: LLVMTypeRef?, IsSigned: LLVMBool, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge622(arg0.rawValue, Val.rawValue, DestTy.rawValue, IsSigned, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildFPCast(arg0: LLVMBuilderRef?, Val: LLVMValueRef?, DestTy: LLVMTypeRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge623(arg0.rawValue, Val.rawValue, DestTy.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildIntCast(arg0: LLVMBuilderRef?, Val: LLVMValueRef?, DestTy: LLVMTypeRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge624(arg0.rawValue, Val.rawValue, DestTy.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMGetCastOpcode(Src: LLVMValueRef?, SrcIsSigned: LLVMBool, DestTy: LLVMTypeRef?, DestIsSigned: LLVMBool): LLVMOpcode {
    return LLVMOpcode.byValue(kniBridge625(Src.rawValue, SrcIsSigned, DestTy.rawValue, DestIsSigned))
}

@ExperimentalForeignApi
fun LLVMBuildICmp(arg0: LLVMBuilderRef?, Op: LLVMIntPredicate, LHS: LLVMValueRef?, RHS: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge626(arg0.rawValue, Op.value, LHS.rawValue, RHS.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildFCmp(arg0: LLVMBuilderRef?, Op: LLVMRealPredicate, LHS: LLVMValueRef?, RHS: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge627(arg0.rawValue, Op.value, LHS.rawValue, RHS.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildPhi(arg0: LLVMBuilderRef?, Ty: LLVMTypeRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge628(arg0.rawValue, Ty.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildCall2(arg0: LLVMBuilderRef?, arg1: LLVMTypeRef?, Fn: LLVMValueRef?, Args: CValuesRef<LLVMValueRefVar>?, NumArgs: Int, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge629(arg0.rawValue, arg1.rawValue, Fn.rawValue, Args?.getPointer(memScope).rawValue, NumArgs, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildSelect(arg0: LLVMBuilderRef?, If: LLVMValueRef?, Then: LLVMValueRef?, Else: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge630(arg0.rawValue, If.rawValue, Then.rawValue, Else.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildVAArg(arg0: LLVMBuilderRef?, List: LLVMValueRef?, Ty: LLVMTypeRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge631(arg0.rawValue, List.rawValue, Ty.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildExtractElement(arg0: LLVMBuilderRef?, VecVal: LLVMValueRef?, Index: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge632(arg0.rawValue, VecVal.rawValue, Index.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildInsertElement(arg0: LLVMBuilderRef?, VecVal: LLVMValueRef?, EltVal: LLVMValueRef?, Index: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge633(arg0.rawValue, VecVal.rawValue, EltVal.rawValue, Index.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildShuffleVector(arg0: LLVMBuilderRef?, V1: LLVMValueRef?, V2: LLVMValueRef?, Mask: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge634(arg0.rawValue, V1.rawValue, V2.rawValue, Mask.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildExtractValue(arg0: LLVMBuilderRef?, AggVal: LLVMValueRef?, Index: Int, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge635(arg0.rawValue, AggVal.rawValue, Index, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildInsertValue(arg0: LLVMBuilderRef?, AggVal: LLVMValueRef?, EltVal: LLVMValueRef?, Index: Int, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge636(arg0.rawValue, AggVal.rawValue, EltVal.rawValue, Index, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildFreeze(arg0: LLVMBuilderRef?, Val: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge637(arg0.rawValue, Val.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildIsNull(arg0: LLVMBuilderRef?, Val: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge638(arg0.rawValue, Val.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildIsNotNull(arg0: LLVMBuilderRef?, Val: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge639(arg0.rawValue, Val.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildPtrDiff2(arg0: LLVMBuilderRef?, ElemTy: LLVMTypeRef?, LHS: LLVMValueRef?, RHS: LLVMValueRef?, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge640(arg0.rawValue, ElemTy.rawValue, LHS.rawValue, RHS.rawValue, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildFence(B: LLVMBuilderRef?, ordering: LLVMAtomicOrdering, singleThread: LLVMBool, Name: String?): LLVMValueRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueValue>(kniBridge641(B.rawValue, ordering.value, singleThread, Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMBuildAtomicRMW(B: LLVMBuilderRef?, op: LLVMAtomicRMWBinOp, PTR: LLVMValueRef?, Val: LLVMValueRef?, ordering: LLVMAtomicOrdering, singleThread: LLVMBool): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge642(B.rawValue, op.value, PTR.rawValue, Val.rawValue, ordering.value, singleThread))
}

@ExperimentalForeignApi
fun LLVMBuildAtomicCmpXchg(B: LLVMBuilderRef?, Ptr: LLVMValueRef?, Cmp: LLVMValueRef?, New: LLVMValueRef?, SuccessOrdering: LLVMAtomicOrdering, FailureOrdering: LLVMAtomicOrdering, SingleThread: LLVMBool): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge643(B.rawValue, Ptr.rawValue, Cmp.rawValue, New.rawValue, SuccessOrdering.value, FailureOrdering.value, SingleThread))
}

@ExperimentalForeignApi
fun LLVMGetNumMaskElements(ShuffleVectorInst: LLVMValueRef?): Int {
    return kniBridge644(ShuffleVectorInst.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetUndefMaskElem(): Int {
    return kniBridge645()
}

@ExperimentalForeignApi
fun LLVMGetMaskValue(ShuffleVectorInst: LLVMValueRef?, Elt: Int): Int {
    return kniBridge646(ShuffleVectorInst.rawValue, Elt)
}

@ExperimentalForeignApi
fun LLVMIsAtomicSingleThread(AtomicInst: LLVMValueRef?): LLVMBool {
    return kniBridge647(AtomicInst.rawValue)
}

@ExperimentalForeignApi
fun LLVMSetAtomicSingleThread(AtomicInst: LLVMValueRef?, SingleThread: LLVMBool): Unit {
    return kniBridge648(AtomicInst.rawValue, SingleThread)
}

@ExperimentalForeignApi
fun LLVMGetCmpXchgSuccessOrdering(CmpXchgInst: LLVMValueRef?): LLVMAtomicOrdering {
    return LLVMAtomicOrdering.byValue(kniBridge649(CmpXchgInst.rawValue))
}

@ExperimentalForeignApi
fun LLVMSetCmpXchgSuccessOrdering(CmpXchgInst: LLVMValueRef?, Ordering: LLVMAtomicOrdering): Unit {
    return kniBridge650(CmpXchgInst.rawValue, Ordering.value)
}

@ExperimentalForeignApi
fun LLVMGetCmpXchgFailureOrdering(CmpXchgInst: LLVMValueRef?): LLVMAtomicOrdering {
    return LLVMAtomicOrdering.byValue(kniBridge651(CmpXchgInst.rawValue))
}

@ExperimentalForeignApi
fun LLVMSetCmpXchgFailureOrdering(CmpXchgInst: LLVMValueRef?, Ordering: LLVMAtomicOrdering): Unit {
    return kniBridge652(CmpXchgInst.rawValue, Ordering.value)
}

@ExperimentalForeignApi
fun LLVMCreateModuleProviderForExistingModule(M: LLVMModuleRef?): LLVMModuleProviderRef? {
    return interpretCPointer<LLVMOpaqueModuleProvider>(kniBridge653(M.rawValue))
}

@ExperimentalForeignApi
fun LLVMDisposeModuleProvider(M: LLVMModuleProviderRef?): Unit {
    return kniBridge654(M.rawValue)
}

@ExperimentalForeignApi
fun LLVMCreateMemoryBufferWithContentsOfFile(Path: String?, OutMemBuf: CValuesRef<LLVMMemoryBufferRefVar>?, OutMessage: CValuesRef<CPointerVar<ByteVar>>?): LLVMBool {
    memScoped {
        return kniBridge655(Path?.cstr?.getPointer(memScope).rawValue, OutMemBuf?.getPointer(memScope).rawValue, OutMessage?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMCreateMemoryBufferWithSTDIN(OutMemBuf: CValuesRef<LLVMMemoryBufferRefVar>?, OutMessage: CValuesRef<CPointerVar<ByteVar>>?): LLVMBool {
    memScoped {
        return kniBridge656(OutMemBuf?.getPointer(memScope).rawValue, OutMessage?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMCreateMemoryBufferWithMemoryRange(InputData: String?, InputDataLength: size_t, BufferName: String?, RequiresNullTerminator: LLVMBool): LLVMMemoryBufferRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMemoryBuffer>(kniBridge657(InputData?.cstr?.getPointer(memScope).rawValue, InputDataLength, BufferName?.cstr?.getPointer(memScope).rawValue, RequiresNullTerminator))
    }
}

@ExperimentalForeignApi
fun LLVMCreateMemoryBufferWithMemoryRangeCopy(InputData: String?, InputDataLength: size_t, BufferName: String?): LLVMMemoryBufferRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMemoryBuffer>(kniBridge658(InputData?.cstr?.getPointer(memScope).rawValue, InputDataLength, BufferName?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMGetBufferStart(MemBuf: LLVMMemoryBufferRef?): CPointer<ByteVar>? {
    return interpretCPointer<ByteVar>(kniBridge659(MemBuf.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetBufferSize(MemBuf: LLVMMemoryBufferRef?): size_t {
    return kniBridge660(MemBuf.rawValue)
}

@ExperimentalForeignApi
fun LLVMDisposeMemoryBuffer(MemBuf: LLVMMemoryBufferRef?): Unit {
    return kniBridge661(MemBuf.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetGlobalPassRegistry(): LLVMPassRegistryRef? {
    return interpretCPointer<LLVMOpaquePassRegistry>(kniBridge662())
}

@ExperimentalForeignApi
fun LLVMCreatePassManager(): LLVMPassManagerRef? {
    return interpretCPointer<LLVMOpaquePassManager>(kniBridge663())
}

@ExperimentalForeignApi
fun LLVMCreateFunctionPassManagerForModule(M: LLVMModuleRef?): LLVMPassManagerRef? {
    return interpretCPointer<LLVMOpaquePassManager>(kniBridge664(M.rawValue))
}

@ExperimentalForeignApi
fun LLVMCreateFunctionPassManager(MP: LLVMModuleProviderRef?): LLVMPassManagerRef? {
    return interpretCPointer<LLVMOpaquePassManager>(kniBridge665(MP.rawValue))
}

@ExperimentalForeignApi
fun LLVMRunPassManager(PM: LLVMPassManagerRef?, M: LLVMModuleRef?): LLVMBool {
    return kniBridge666(PM.rawValue, M.rawValue)
}

@ExperimentalForeignApi
fun LLVMInitializeFunctionPassManager(FPM: LLVMPassManagerRef?): LLVMBool {
    return kniBridge667(FPM.rawValue)
}

@ExperimentalForeignApi
fun LLVMRunFunctionPassManager(FPM: LLVMPassManagerRef?, F: LLVMValueRef?): LLVMBool {
    return kniBridge668(FPM.rawValue, F.rawValue)
}

@ExperimentalForeignApi
fun LLVMFinalizeFunctionPassManager(FPM: LLVMPassManagerRef?): LLVMBool {
    return kniBridge669(FPM.rawValue)
}

@ExperimentalForeignApi
fun LLVMDisposePassManager(PM: LLVMPassManagerRef?): Unit {
    return kniBridge670(PM.rawValue)
}

@ExperimentalForeignApi
fun LLVMStartMultithreaded(): LLVMBool {
    return kniBridge671()
}

@ExperimentalForeignApi
fun LLVMStopMultithreaded(): Unit {
    return kniBridge672()
}

@ExperimentalForeignApi
fun LLVMIsMultithreaded(): LLVMBool {
    return kniBridge673()
}

@ExperimentalForeignApi
fun LLVMGetModuleDataLayout(M: LLVMModuleRef?): LLVMTargetDataRef? {
    return interpretCPointer<LLVMOpaqueTargetData>(kniBridge674(M.rawValue))
}

@ExperimentalForeignApi
fun LLVMSetModuleDataLayout(M: LLVMModuleRef?, DL: LLVMTargetDataRef?): Unit {
    return kniBridge675(M.rawValue, DL.rawValue)
}

@ExperimentalForeignApi
fun LLVMCreateTargetData(StringRep: String?): LLVMTargetDataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueTargetData>(kniBridge676(StringRep?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMDisposeTargetData(TD: LLVMTargetDataRef?): Unit {
    return kniBridge677(TD.rawValue)
}

@ExperimentalForeignApi
fun LLVMAddTargetLibraryInfo(TLI: LLVMTargetLibraryInfoRef?, PM: LLVMPassManagerRef?): Unit {
    return kniBridge678(TLI.rawValue, PM.rawValue)
}

@ExperimentalForeignApi
fun LLVMCopyStringRepOfTargetData(TD: LLVMTargetDataRef?): CPointer<ByteVar>? {
    return interpretCPointer<ByteVar>(kniBridge679(TD.rawValue))
}

@ExperimentalForeignApi
fun LLVMByteOrder(TD: LLVMTargetDataRef?): LLVMByteOrdering {
    return LLVMByteOrdering.byValue(kniBridge680(TD.rawValue))
}

@ExperimentalForeignApi
fun LLVMPointerSize(TD: LLVMTargetDataRef?): Int {
    return kniBridge681(TD.rawValue)
}

@ExperimentalForeignApi
fun LLVMPointerSizeForAS(TD: LLVMTargetDataRef?, AS: Int): Int {
    return kniBridge682(TD.rawValue, AS)
}

@ExperimentalForeignApi
fun LLVMIntPtrTypeInContext(C: LLVMContextRef?, TD: LLVMTargetDataRef?): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge683(C.rawValue, TD.rawValue))
}

@ExperimentalForeignApi
fun LLVMIntPtrTypeForASInContext(C: LLVMContextRef?, TD: LLVMTargetDataRef?, AS: Int): LLVMTypeRef? {
    return interpretCPointer<LLVMOpaqueType>(kniBridge684(C.rawValue, TD.rawValue, AS))
}

@ExperimentalForeignApi
fun LLVMSizeOfTypeInBits(TD: LLVMTargetDataRef?, Ty: LLVMTypeRef?): Long {
    return kniBridge685(TD.rawValue, Ty.rawValue)
}

@ExperimentalForeignApi
fun LLVMStoreSizeOfType(TD: LLVMTargetDataRef?, Ty: LLVMTypeRef?): Long {
    return kniBridge686(TD.rawValue, Ty.rawValue)
}

@ExperimentalForeignApi
fun LLVMABISizeOfType(TD: LLVMTargetDataRef?, Ty: LLVMTypeRef?): Long {
    return kniBridge687(TD.rawValue, Ty.rawValue)
}

@ExperimentalForeignApi
fun LLVMABIAlignmentOfType(TD: LLVMTargetDataRef?, Ty: LLVMTypeRef?): Int {
    return kniBridge688(TD.rawValue, Ty.rawValue)
}

@ExperimentalForeignApi
fun LLVMCallFrameAlignmentOfType(TD: LLVMTargetDataRef?, Ty: LLVMTypeRef?): Int {
    return kniBridge689(TD.rawValue, Ty.rawValue)
}

@ExperimentalForeignApi
fun LLVMPreferredAlignmentOfType(TD: LLVMTargetDataRef?, Ty: LLVMTypeRef?): Int {
    return kniBridge690(TD.rawValue, Ty.rawValue)
}

@ExperimentalForeignApi
fun LLVMPreferredAlignmentOfGlobal(TD: LLVMTargetDataRef?, GlobalVar: LLVMValueRef?): Int {
    return kniBridge691(TD.rawValue, GlobalVar.rawValue)
}

@ExperimentalForeignApi
fun LLVMElementAtOffset(TD: LLVMTargetDataRef?, StructTy: LLVMTypeRef?, Offset: Long): Int {
    return kniBridge692(TD.rawValue, StructTy.rawValue, Offset)
}

@ExperimentalForeignApi
fun LLVMOffsetOfElement(TD: LLVMTargetDataRef?, StructTy: LLVMTypeRef?, Element: Int): Long {
    return kniBridge693(TD.rawValue, StructTy.rawValue, Element)
}

@ExperimentalForeignApi
fun LLVMVerifyModule(M: LLVMModuleRef?, Action: LLVMVerifierFailureAction, OutMessage: CValuesRef<CPointerVar<ByteVar>>?): LLVMBool {
    memScoped {
        return kniBridge694(M.rawValue, Action.value, OutMessage?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMVerifyFunction(Fn: LLVMValueRef?, Action: LLVMVerifierFailureAction): LLVMBool {
    return kniBridge695(Fn.rawValue, Action.value)
}

@ExperimentalForeignApi
fun LLVMViewFunctionCFG(Fn: LLVMValueRef?): Unit {
    return kniBridge696(Fn.rawValue)
}

@ExperimentalForeignApi
fun LLVMViewFunctionCFGOnly(Fn: LLVMValueRef?): Unit {
    return kniBridge697(Fn.rawValue)
}

@ExperimentalForeignApi
fun LLVMWriteBitcodeToFile(M: LLVMModuleRef?, Path: String?): Int {
    memScoped {
        return kniBridge698(M.rawValue, Path?.cstr?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMWriteBitcodeToFD(M: LLVMModuleRef?, FD: Int, ShouldClose: Int, Unbuffered: Int): Int {
    return kniBridge699(M.rawValue, FD, ShouldClose, Unbuffered)
}

@ExperimentalForeignApi
fun LLVMWriteBitcodeToFileHandle(M: LLVMModuleRef?, Handle: Int): Int {
    return kniBridge700(M.rawValue, Handle)
}

@ExperimentalForeignApi
fun LLVMWriteBitcodeToMemoryBuffer(M: LLVMModuleRef?): LLVMMemoryBufferRef? {
    return interpretCPointer<LLVMOpaqueMemoryBuffer>(kniBridge701(M.rawValue))
}

@ExperimentalForeignApi
fun LLVMParseBitcodeInContext(ContextRef: LLVMContextRef?, MemBuf: LLVMMemoryBufferRef?, OutModule: CValuesRef<LLVMModuleRefVar>?, OutMessage: CValuesRef<CPointerVar<ByteVar>>?): LLVMBool {
    memScoped {
        return kniBridge702(ContextRef.rawValue, MemBuf.rawValue, OutModule?.getPointer(memScope).rawValue, OutMessage?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMParseBitcodeInContext2(ContextRef: LLVMContextRef?, MemBuf: LLVMMemoryBufferRef?, OutModule: CValuesRef<LLVMModuleRefVar>?): LLVMBool {
    memScoped {
        return kniBridge703(ContextRef.rawValue, MemBuf.rawValue, OutModule?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMGetBitcodeModuleInContext(ContextRef: LLVMContextRef?, MemBuf: LLVMMemoryBufferRef?, OutM: CValuesRef<LLVMModuleRefVar>?, OutMessage: CValuesRef<CPointerVar<ByteVar>>?): LLVMBool {
    memScoped {
        return kniBridge704(ContextRef.rawValue, MemBuf.rawValue, OutM?.getPointer(memScope).rawValue, OutMessage?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMGetBitcodeModuleInContext2(ContextRef: LLVMContextRef?, MemBuf: LLVMMemoryBufferRef?, OutM: CValuesRef<LLVMModuleRefVar>?): LLVMBool {
    memScoped {
        return kniBridge705(ContextRef.rawValue, MemBuf.rawValue, OutM?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMGetErrorTypeId(Err: LLVMErrorRef?): LLVMErrorTypeId? {
    return interpretCPointer<COpaque>(kniBridge706(Err.rawValue))
}

@ExperimentalForeignApi
fun LLVMConsumeError(Err: LLVMErrorRef?): Unit {
    return kniBridge707(Err.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetErrorMessage(Err: LLVMErrorRef?): CPointer<ByteVar>? {
    return interpretCPointer<ByteVar>(kniBridge708(Err.rawValue))
}

@ExperimentalForeignApi
fun LLVMDisposeErrorMessage(ErrMsg: CValuesRef<ByteVar>?): Unit {
    memScoped {
        return kniBridge709(ErrMsg?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMGetStringErrorTypeId(): LLVMErrorTypeId? {
    return interpretCPointer<COpaque>(kniBridge710())
}

@ExperimentalForeignApi
fun LLVMCreateStringError(ErrMsg: String?): LLVMErrorRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueError>(kniBridge711(ErrMsg?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMGetFirstTarget(): LLVMTargetRef? {
    return interpretCPointer<LLVMTarget>(kniBridge712())
}

@ExperimentalForeignApi
fun LLVMGetNextTarget(T: LLVMTargetRef?): LLVMTargetRef? {
    return interpretCPointer<LLVMTarget>(kniBridge713(T.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetTargetFromName(Name: String?): LLVMTargetRef? {
    memScoped {
        return interpretCPointer<LLVMTarget>(kniBridge714(Name?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMGetTargetFromTriple(Triple: String?, T: CValuesRef<LLVMTargetRefVar>?, ErrorMessage: CValuesRef<CPointerVar<ByteVar>>?): LLVMBool {
    memScoped {
        return kniBridge715(Triple?.cstr?.getPointer(memScope).rawValue, T?.getPointer(memScope).rawValue, ErrorMessage?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMGetTargetName(T: LLVMTargetRef?): CPointer<ByteVar>? {
    return interpretCPointer<ByteVar>(kniBridge716(T.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetTargetDescription(T: LLVMTargetRef?): CPointer<ByteVar>? {
    return interpretCPointer<ByteVar>(kniBridge717(T.rawValue))
}

@ExperimentalForeignApi
fun LLVMTargetHasJIT(T: LLVMTargetRef?): LLVMBool {
    return kniBridge718(T.rawValue)
}

@ExperimentalForeignApi
fun LLVMTargetHasTargetMachine(T: LLVMTargetRef?): LLVMBool {
    return kniBridge719(T.rawValue)
}

@ExperimentalForeignApi
fun LLVMTargetHasAsmBackend(T: LLVMTargetRef?): LLVMBool {
    return kniBridge720(T.rawValue)
}

@ExperimentalForeignApi
fun LLVMCreateTargetMachine(T: LLVMTargetRef?, Triple: String?, CPU: String?, Features: String?, Level: LLVMCodeGenOptLevel, Reloc: LLVMRelocMode, CodeModel: LLVMCodeModel): LLVMTargetMachineRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueTargetMachine>(kniBridge721(T.rawValue, Triple?.cstr?.getPointer(memScope).rawValue, CPU?.cstr?.getPointer(memScope).rawValue, Features?.cstr?.getPointer(memScope).rawValue, Level.value, Reloc.value, CodeModel.value))
    }
}

@ExperimentalForeignApi
fun LLVMDisposeTargetMachine(T: LLVMTargetMachineRef?): Unit {
    return kniBridge722(T.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetTargetMachineTarget(T: LLVMTargetMachineRef?): LLVMTargetRef? {
    return interpretCPointer<LLVMTarget>(kniBridge723(T.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetTargetMachineTriple(T: LLVMTargetMachineRef?): CPointer<ByteVar>? {
    return interpretCPointer<ByteVar>(kniBridge724(T.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetTargetMachineCPU(T: LLVMTargetMachineRef?): CPointer<ByteVar>? {
    return interpretCPointer<ByteVar>(kniBridge725(T.rawValue))
}

@ExperimentalForeignApi
fun LLVMGetTargetMachineFeatureString(T: LLVMTargetMachineRef?): CPointer<ByteVar>? {
    return interpretCPointer<ByteVar>(kniBridge726(T.rawValue))
}

@ExperimentalForeignApi
fun LLVMCreateTargetDataLayout(T: LLVMTargetMachineRef?): LLVMTargetDataRef? {
    return interpretCPointer<LLVMOpaqueTargetData>(kniBridge727(T.rawValue))
}

@ExperimentalForeignApi
fun LLVMSetTargetMachineAsmVerbosity(T: LLVMTargetMachineRef?, VerboseAsm: LLVMBool): Unit {
    return kniBridge728(T.rawValue, VerboseAsm)
}

@ExperimentalForeignApi
fun LLVMTargetMachineEmitToFile(T: LLVMTargetMachineRef?, M: LLVMModuleRef?, Filename: String?, codegen: LLVMCodeGenFileType, ErrorMessage: CValuesRef<CPointerVar<ByteVar>>?): LLVMBool {
    memScoped {
        return kniBridge729(T.rawValue, M.rawValue, Filename?.cstr?.getPointer(memScope).rawValue, codegen.value, ErrorMessage?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMTargetMachineEmitToMemoryBuffer(T: LLVMTargetMachineRef?, M: LLVMModuleRef?, codegen: LLVMCodeGenFileType, ErrorMessage: CValuesRef<CPointerVar<ByteVar>>?, OutMemBuf: CValuesRef<LLVMMemoryBufferRefVar>?): LLVMBool {
    memScoped {
        return kniBridge730(T.rawValue, M.rawValue, codegen.value, ErrorMessage?.getPointer(memScope).rawValue, OutMemBuf?.getPointer(memScope).rawValue)
    }
}

@ExperimentalForeignApi
fun LLVMGetDefaultTargetTriple(): CPointer<ByteVar>? {
    return interpretCPointer<ByteVar>(kniBridge731())
}

@ExperimentalForeignApi
fun LLVMNormalizeTargetTriple(triple: String?): CPointer<ByteVar>? {
    memScoped {
        return interpretCPointer<ByteVar>(kniBridge732(triple?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMGetHostCPUName(): CPointer<ByteVar>? {
    return interpretCPointer<ByteVar>(kniBridge733())
}

@ExperimentalForeignApi
fun LLVMGetHostCPUFeatures(): CPointer<ByteVar>? {
    return interpretCPointer<ByteVar>(kniBridge734())
}

@ExperimentalForeignApi
fun LLVMAddAnalysisPasses(T: LLVMTargetMachineRef?, PM: LLVMPassManagerRef?): Unit {
    return kniBridge735(T.rawValue, PM.rawValue)
}

@ExperimentalForeignApi
fun LLVMRunPasses(M: LLVMModuleRef?, Passes: String?, TM: LLVMTargetMachineRef?, Options: LLVMPassBuilderOptionsRef?): LLVMErrorRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueError>(kniBridge736(M.rawValue, Passes?.cstr?.getPointer(memScope).rawValue, TM.rawValue, Options.rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMCreatePassBuilderOptions(): LLVMPassBuilderOptionsRef? {
    return interpretCPointer<LLVMOpaquePassBuilderOptions>(kniBridge737())
}

@ExperimentalForeignApi
fun LLVMPassBuilderOptionsSetVerifyEach(Options: LLVMPassBuilderOptionsRef?, VerifyEach: LLVMBool): Unit {
    return kniBridge738(Options.rawValue, VerifyEach)
}

@ExperimentalForeignApi
fun LLVMPassBuilderOptionsSetDebugLogging(Options: LLVMPassBuilderOptionsRef?, DebugLogging: LLVMBool): Unit {
    return kniBridge739(Options.rawValue, DebugLogging)
}

@ExperimentalForeignApi
fun LLVMPassBuilderOptionsSetLoopInterleaving(Options: LLVMPassBuilderOptionsRef?, LoopInterleaving: LLVMBool): Unit {
    return kniBridge740(Options.rawValue, LoopInterleaving)
}

@ExperimentalForeignApi
fun LLVMPassBuilderOptionsSetLoopVectorization(Options: LLVMPassBuilderOptionsRef?, LoopVectorization: LLVMBool): Unit {
    return kniBridge741(Options.rawValue, LoopVectorization)
}

@ExperimentalForeignApi
fun LLVMPassBuilderOptionsSetSLPVectorization(Options: LLVMPassBuilderOptionsRef?, SLPVectorization: LLVMBool): Unit {
    return kniBridge742(Options.rawValue, SLPVectorization)
}

@ExperimentalForeignApi
fun LLVMPassBuilderOptionsSetLoopUnrolling(Options: LLVMPassBuilderOptionsRef?, LoopUnrolling: LLVMBool): Unit {
    return kniBridge743(Options.rawValue, LoopUnrolling)
}

@ExperimentalForeignApi
fun LLVMPassBuilderOptionsSetForgetAllSCEVInLoopUnroll(Options: LLVMPassBuilderOptionsRef?, ForgetAllSCEVInLoopUnroll: LLVMBool): Unit {
    return kniBridge744(Options.rawValue, ForgetAllSCEVInLoopUnroll)
}

@ExperimentalForeignApi
fun LLVMPassBuilderOptionsSetLicmMssaOptCap(Options: LLVMPassBuilderOptionsRef?, LicmMssaOptCap: Int): Unit {
    return kniBridge745(Options.rawValue, LicmMssaOptCap)
}

@ExperimentalForeignApi
fun LLVMPassBuilderOptionsSetLicmMssaNoAccForPromotionCap(Options: LLVMPassBuilderOptionsRef?, LicmMssaNoAccForPromotionCap: Int): Unit {
    return kniBridge746(Options.rawValue, LicmMssaNoAccForPromotionCap)
}

@ExperimentalForeignApi
fun LLVMPassBuilderOptionsSetCallGraphProfile(Options: LLVMPassBuilderOptionsRef?, CallGraphProfile: LLVMBool): Unit {
    return kniBridge747(Options.rawValue, CallGraphProfile)
}

@ExperimentalForeignApi
fun LLVMPassBuilderOptionsSetMergeFunctions(Options: LLVMPassBuilderOptionsRef?, MergeFunctions: LLVMBool): Unit {
    return kniBridge748(Options.rawValue, MergeFunctions)
}

@ExperimentalForeignApi
fun LLVMPassBuilderOptionsSetInlinerThreshold(Options: LLVMPassBuilderOptionsRef?, Threshold: Int): Unit {
    return kniBridge749(Options.rawValue, Threshold)
}

@ExperimentalForeignApi
fun LLVMPassBuilderOptionsSetMaxDevirtIterations(Options: LLVMPassBuilderOptionsRef?, Iterations: Int): Unit {
    return kniBridge750(Options.rawValue, Iterations)
}

@ExperimentalForeignApi
fun LLVMDisposePassBuilderOptions(Options: LLVMPassBuilderOptionsRef?): Unit {
    return kniBridge751(Options.rawValue)
}

@ExperimentalForeignApi
fun LLVMLinkModules2(Dest: LLVMModuleRef?, Src: LLVMModuleRef?): LLVMBool {
    return kniBridge752(Dest.rawValue, Src.rawValue)
}

@ExperimentalForeignApi
fun LLVMDebugMetadataVersion(): Int {
    return kniBridge753()
}

@ExperimentalForeignApi
fun LLVMGetModuleDebugMetadataVersion(Module: LLVMModuleRef?): Int {
    return kniBridge754(Module.rawValue)
}

@ExperimentalForeignApi
fun LLVMStripModuleDebugInfo(Module: LLVMModuleRef?): LLVMBool {
    return kniBridge755(Module.rawValue)
}

@ExperimentalForeignApi
fun LLVMCreateDIBuilderDisallowUnresolved(M: LLVMModuleRef?): LLVMDIBuilderRef? {
    return interpretCPointer<LLVMOpaqueDIBuilder>(kniBridge756(M.rawValue))
}

@ExperimentalForeignApi
fun LLVMCreateDIBuilder(M: LLVMModuleRef?): LLVMDIBuilderRef? {
    return interpretCPointer<LLVMOpaqueDIBuilder>(kniBridge757(M.rawValue))
}

@ExperimentalForeignApi
fun LLVMDisposeDIBuilder(Builder: LLVMDIBuilderRef?): Unit {
    return kniBridge758(Builder.rawValue)
}

@ExperimentalForeignApi
fun LLVMDIBuilderFinalize(Builder: LLVMDIBuilderRef?): Unit {
    return kniBridge759(Builder.rawValue)
}

@ExperimentalForeignApi
fun LLVMDIBuilderFinalizeSubprogram(Builder: LLVMDIBuilderRef?, Subprogram: LLVMMetadataRef?): Unit {
    return kniBridge760(Builder.rawValue, Subprogram.rawValue)
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateCompileUnit(Builder: LLVMDIBuilderRef?, Lang: LLVMDWARFSourceLanguage, FileRef: LLVMMetadataRef?, Producer: String?, ProducerLen: size_t, isOptimized: LLVMBool, Flags: String?, FlagsLen: size_t, RuntimeVer: Int, SplitName: String?, SplitNameLen: size_t, Kind: LLVMDWARFEmissionKind, DWOId: Int, SplitDebugInlining: LLVMBool, DebugInfoForProfiling: LLVMBool, SysRoot: String?, SysRootLen: size_t, SDK: String?, SDKLen: size_t): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge761(Builder.rawValue, Lang.value, FileRef.rawValue, Producer?.cstr?.getPointer(memScope).rawValue, ProducerLen, isOptimized, Flags?.cstr?.getPointer(memScope).rawValue, FlagsLen, RuntimeVer, SplitName?.cstr?.getPointer(memScope).rawValue, SplitNameLen, Kind, DWOId, SplitDebugInlining, DebugInfoForProfiling, SysRoot?.cstr?.getPointer(memScope).rawValue, SysRootLen, SDK?.cstr?.getPointer(memScope).rawValue, SDKLen))
    }
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateFile(Builder: LLVMDIBuilderRef?, Filename: String?, FilenameLen: size_t, Directory: String?, DirectoryLen: size_t): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge762(Builder.rawValue, Filename?.cstr?.getPointer(memScope).rawValue, FilenameLen, Directory?.cstr?.getPointer(memScope).rawValue, DirectoryLen))
    }
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateModule(Builder: LLVMDIBuilderRef?, ParentScope: LLVMMetadataRef?, Name: String?, NameLen: size_t, ConfigMacros: String?, ConfigMacrosLen: size_t, IncludePath: String?, IncludePathLen: size_t, APINotesFile: String?, APINotesFileLen: size_t): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge763(Builder.rawValue, ParentScope.rawValue, Name?.cstr?.getPointer(memScope).rawValue, NameLen, ConfigMacros?.cstr?.getPointer(memScope).rawValue, ConfigMacrosLen, IncludePath?.cstr?.getPointer(memScope).rawValue, IncludePathLen, APINotesFile?.cstr?.getPointer(memScope).rawValue, APINotesFileLen))
    }
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateNameSpace(Builder: LLVMDIBuilderRef?, ParentScope: LLVMMetadataRef?, Name: String?, NameLen: size_t, ExportSymbols: LLVMBool): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge764(Builder.rawValue, ParentScope.rawValue, Name?.cstr?.getPointer(memScope).rawValue, NameLen, ExportSymbols))
    }
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateFunction(Builder: LLVMDIBuilderRef?, Scope: LLVMMetadataRef?, Name: String?, NameLen: size_t, LinkageName: String?, LinkageNameLen: size_t, File: LLVMMetadataRef?, LineNo: Int, Ty: LLVMMetadataRef?, IsLocalToUnit: LLVMBool, IsDefinition: LLVMBool, ScopeLine: Int, Flags: LLVMDIFlags, IsOptimized: LLVMBool): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge765(Builder.rawValue, Scope.rawValue, Name?.cstr?.getPointer(memScope).rawValue, NameLen, LinkageName?.cstr?.getPointer(memScope).rawValue, LinkageNameLen, File.rawValue, LineNo, Ty.rawValue, IsLocalToUnit, IsDefinition, ScopeLine, Flags, IsOptimized))
    }
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateLexicalBlock(Builder: LLVMDIBuilderRef?, Scope: LLVMMetadataRef?, File: LLVMMetadataRef?, Line: Int, Column: Int): LLVMMetadataRef? {
    return interpretCPointer<LLVMOpaqueMetadata>(kniBridge766(Builder.rawValue, Scope.rawValue, File.rawValue, Line, Column))
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateLexicalBlockFile(Builder: LLVMDIBuilderRef?, Scope: LLVMMetadataRef?, File: LLVMMetadataRef?, Discriminator: Int): LLVMMetadataRef? {
    return interpretCPointer<LLVMOpaqueMetadata>(kniBridge767(Builder.rawValue, Scope.rawValue, File.rawValue, Discriminator))
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateImportedModuleFromNamespace(Builder: LLVMDIBuilderRef?, Scope: LLVMMetadataRef?, NS: LLVMMetadataRef?, File: LLVMMetadataRef?, Line: Int): LLVMMetadataRef? {
    return interpretCPointer<LLVMOpaqueMetadata>(kniBridge768(Builder.rawValue, Scope.rawValue, NS.rawValue, File.rawValue, Line))
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateImportedModuleFromAlias(Builder: LLVMDIBuilderRef?, Scope: LLVMMetadataRef?, ImportedEntity: LLVMMetadataRef?, File: LLVMMetadataRef?, Line: Int, Elements: CValuesRef<LLVMMetadataRefVar>?, NumElements: Int): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge769(Builder.rawValue, Scope.rawValue, ImportedEntity.rawValue, File.rawValue, Line, Elements?.getPointer(memScope).rawValue, NumElements))
    }
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateImportedModuleFromModule(Builder: LLVMDIBuilderRef?, Scope: LLVMMetadataRef?, M: LLVMMetadataRef?, File: LLVMMetadataRef?, Line: Int, Elements: CValuesRef<LLVMMetadataRefVar>?, NumElements: Int): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge770(Builder.rawValue, Scope.rawValue, M.rawValue, File.rawValue, Line, Elements?.getPointer(memScope).rawValue, NumElements))
    }
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateImportedDeclaration(Builder: LLVMDIBuilderRef?, Scope: LLVMMetadataRef?, Decl: LLVMMetadataRef?, File: LLVMMetadataRef?, Line: Int, Name: String?, NameLen: size_t, Elements: CValuesRef<LLVMMetadataRefVar>?, NumElements: Int): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge771(Builder.rawValue, Scope.rawValue, Decl.rawValue, File.rawValue, Line, Name?.cstr?.getPointer(memScope).rawValue, NameLen, Elements?.getPointer(memScope).rawValue, NumElements))
    }
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateDebugLocation(Ctx: LLVMContextRef?, Line: Int, Column: Int, Scope: LLVMMetadataRef?, InlinedAt: LLVMMetadataRef?): LLVMMetadataRef? {
    return interpretCPointer<LLVMOpaqueMetadata>(kniBridge772(Ctx.rawValue, Line, Column, Scope.rawValue, InlinedAt.rawValue))
}

@ExperimentalForeignApi
fun LLVMDILocationGetLine(Location: LLVMMetadataRef?): Int {
    return kniBridge773(Location.rawValue)
}

@ExperimentalForeignApi
fun LLVMDILocationGetColumn(Location: LLVMMetadataRef?): Int {
    return kniBridge774(Location.rawValue)
}

@ExperimentalForeignApi
fun LLVMDILocationGetScope(Location: LLVMMetadataRef?): LLVMMetadataRef? {
    return interpretCPointer<LLVMOpaqueMetadata>(kniBridge775(Location.rawValue))
}

@ExperimentalForeignApi
fun LLVMDILocationGetInlinedAt(Location: LLVMMetadataRef?): LLVMMetadataRef? {
    return interpretCPointer<LLVMOpaqueMetadata>(kniBridge776(Location.rawValue))
}

@ExperimentalForeignApi
fun LLVMDIScopeGetFile(Scope: LLVMMetadataRef?): LLVMMetadataRef? {
    return interpretCPointer<LLVMOpaqueMetadata>(kniBridge777(Scope.rawValue))
}

@ExperimentalForeignApi
fun LLVMDIFileGetDirectory(File: LLVMMetadataRef?, Len: CValuesRef<IntVar>?): CPointer<ByteVar>? {
    memScoped {
        return interpretCPointer<ByteVar>(kniBridge778(File.rawValue, Len?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMDIFileGetFilename(File: LLVMMetadataRef?, Len: CValuesRef<IntVar>?): CPointer<ByteVar>? {
    memScoped {
        return interpretCPointer<ByteVar>(kniBridge779(File.rawValue, Len?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMDIFileGetSource(File: LLVMMetadataRef?, Len: CValuesRef<IntVar>?): CPointer<ByteVar>? {
    memScoped {
        return interpretCPointer<ByteVar>(kniBridge780(File.rawValue, Len?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMDIBuilderGetOrCreateTypeArray(Builder: LLVMDIBuilderRef?, Data: CValuesRef<LLVMMetadataRefVar>?, NumElements: size_t): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge781(Builder.rawValue, Data?.getPointer(memScope).rawValue, NumElements))
    }
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateSubroutineType(Builder: LLVMDIBuilderRef?, File: LLVMMetadataRef?, ParameterTypes: CValuesRef<LLVMMetadataRefVar>?, NumParameterTypes: Int, Flags: LLVMDIFlags): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge782(Builder.rawValue, File.rawValue, ParameterTypes?.getPointer(memScope).rawValue, NumParameterTypes, Flags))
    }
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateMacro(Builder: LLVMDIBuilderRef?, ParentMacroFile: LLVMMetadataRef?, Line: Int, RecordType: LLVMDWARFMacinfoRecordType, Name: String?, NameLen: size_t, Value: String?, ValueLen: size_t): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge783(Builder.rawValue, ParentMacroFile.rawValue, Line, RecordType, Name?.cstr?.getPointer(memScope).rawValue, NameLen, Value?.cstr?.getPointer(memScope).rawValue, ValueLen))
    }
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateTempMacroFile(Builder: LLVMDIBuilderRef?, ParentMacroFile: LLVMMetadataRef?, Line: Int, File: LLVMMetadataRef?): LLVMMetadataRef? {
    return interpretCPointer<LLVMOpaqueMetadata>(kniBridge784(Builder.rawValue, ParentMacroFile.rawValue, Line, File.rawValue))
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateEnumerator(Builder: LLVMDIBuilderRef?, Name: String?, NameLen: size_t, Value: int64_t, IsUnsigned: LLVMBool): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge785(Builder.rawValue, Name?.cstr?.getPointer(memScope).rawValue, NameLen, Value, IsUnsigned))
    }
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateEnumerationType(Builder: LLVMDIBuilderRef?, Scope: LLVMMetadataRef?, Name: String?, NameLen: size_t, File: LLVMMetadataRef?, LineNumber: Int, SizeInBits: uint64_t, AlignInBits: uint32_t, Elements: CValuesRef<LLVMMetadataRefVar>?, NumElements: Int, ClassTy: LLVMMetadataRef?): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge786(Builder.rawValue, Scope.rawValue, Name?.cstr?.getPointer(memScope).rawValue, NameLen, File.rawValue, LineNumber, SizeInBits, AlignInBits, Elements?.getPointer(memScope).rawValue, NumElements, ClassTy.rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateUnionType(Builder: LLVMDIBuilderRef?, Scope: LLVMMetadataRef?, Name: String?, NameLen: size_t, File: LLVMMetadataRef?, LineNumber: Int, SizeInBits: uint64_t, AlignInBits: uint32_t, Flags: LLVMDIFlags, Elements: CValuesRef<LLVMMetadataRefVar>?, NumElements: Int, RunTimeLang: Int, UniqueId: String?, UniqueIdLen: size_t): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge787(Builder.rawValue, Scope.rawValue, Name?.cstr?.getPointer(memScope).rawValue, NameLen, File.rawValue, LineNumber, SizeInBits, AlignInBits, Flags, Elements?.getPointer(memScope).rawValue, NumElements, RunTimeLang, UniqueId?.cstr?.getPointer(memScope).rawValue, UniqueIdLen))
    }
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateArrayType(Builder: LLVMDIBuilderRef?, Size: uint64_t, AlignInBits: uint32_t, Ty: LLVMMetadataRef?, Subscripts: CValuesRef<LLVMMetadataRefVar>?, NumSubscripts: Int): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge788(Builder.rawValue, Size, AlignInBits, Ty.rawValue, Subscripts?.getPointer(memScope).rawValue, NumSubscripts))
    }
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateVectorType(Builder: LLVMDIBuilderRef?, Size: uint64_t, AlignInBits: uint32_t, Ty: LLVMMetadataRef?, Subscripts: CValuesRef<LLVMMetadataRefVar>?, NumSubscripts: Int): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge789(Builder.rawValue, Size, AlignInBits, Ty.rawValue, Subscripts?.getPointer(memScope).rawValue, NumSubscripts))
    }
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateUnspecifiedType(Builder: LLVMDIBuilderRef?, Name: String?, NameLen: size_t): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge790(Builder.rawValue, Name?.cstr?.getPointer(memScope).rawValue, NameLen))
    }
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateBasicType(Builder: LLVMDIBuilderRef?, Name: String?, NameLen: size_t, SizeInBits: uint64_t, Encoding: LLVMDWARFTypeEncoding, Flags: LLVMDIFlags): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge791(Builder.rawValue, Name?.cstr?.getPointer(memScope).rawValue, NameLen, SizeInBits, Encoding, Flags))
    }
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreatePointerType(Builder: LLVMDIBuilderRef?, PointeeTy: LLVMMetadataRef?, SizeInBits: uint64_t, AlignInBits: uint32_t, AddressSpace: Int, Name: String?, NameLen: size_t): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge792(Builder.rawValue, PointeeTy.rawValue, SizeInBits, AlignInBits, AddressSpace, Name?.cstr?.getPointer(memScope).rawValue, NameLen))
    }
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateStructType(Builder: LLVMDIBuilderRef?, Scope: LLVMMetadataRef?, Name: String?, NameLen: size_t, File: LLVMMetadataRef?, LineNumber: Int, SizeInBits: uint64_t, AlignInBits: uint32_t, Flags: LLVMDIFlags, DerivedFrom: LLVMMetadataRef?, Elements: CValuesRef<LLVMMetadataRefVar>?, NumElements: Int, RunTimeLang: Int, VTableHolder: LLVMMetadataRef?, UniqueId: String?, UniqueIdLen: size_t): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge793(Builder.rawValue, Scope.rawValue, Name?.cstr?.getPointer(memScope).rawValue, NameLen, File.rawValue, LineNumber, SizeInBits, AlignInBits, Flags, DerivedFrom.rawValue, Elements?.getPointer(memScope).rawValue, NumElements, RunTimeLang, VTableHolder.rawValue, UniqueId?.cstr?.getPointer(memScope).rawValue, UniqueIdLen))
    }
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateMemberType(Builder: LLVMDIBuilderRef?, Scope: LLVMMetadataRef?, Name: String?, NameLen: size_t, File: LLVMMetadataRef?, LineNo: Int, SizeInBits: uint64_t, AlignInBits: uint32_t, OffsetInBits: uint64_t, Flags: LLVMDIFlags, Ty: LLVMMetadataRef?): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge794(Builder.rawValue, Scope.rawValue, Name?.cstr?.getPointer(memScope).rawValue, NameLen, File.rawValue, LineNo, SizeInBits, AlignInBits, OffsetInBits, Flags, Ty.rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateStaticMemberType(Builder: LLVMDIBuilderRef?, Scope: LLVMMetadataRef?, Name: String?, NameLen: size_t, File: LLVMMetadataRef?, LineNumber: Int, Type: LLVMMetadataRef?, Flags: LLVMDIFlags, ConstantVal: LLVMValueRef?, AlignInBits: uint32_t): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge795(Builder.rawValue, Scope.rawValue, Name?.cstr?.getPointer(memScope).rawValue, NameLen, File.rawValue, LineNumber, Type.rawValue, Flags, ConstantVal.rawValue, AlignInBits))
    }
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateMemberPointerType(Builder: LLVMDIBuilderRef?, PointeeType: LLVMMetadataRef?, ClassType: LLVMMetadataRef?, SizeInBits: uint64_t, AlignInBits: uint32_t, Flags: LLVMDIFlags): LLVMMetadataRef? {
    return interpretCPointer<LLVMOpaqueMetadata>(kniBridge796(Builder.rawValue, PointeeType.rawValue, ClassType.rawValue, SizeInBits, AlignInBits, Flags))
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateObjCIVar(Builder: LLVMDIBuilderRef?, Name: String?, NameLen: size_t, File: LLVMMetadataRef?, LineNo: Int, SizeInBits: uint64_t, AlignInBits: uint32_t, OffsetInBits: uint64_t, Flags: LLVMDIFlags, Ty: LLVMMetadataRef?, PropertyNode: LLVMMetadataRef?): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge797(Builder.rawValue, Name?.cstr?.getPointer(memScope).rawValue, NameLen, File.rawValue, LineNo, SizeInBits, AlignInBits, OffsetInBits, Flags, Ty.rawValue, PropertyNode.rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateObjCProperty(Builder: LLVMDIBuilderRef?, Name: String?, NameLen: size_t, File: LLVMMetadataRef?, LineNo: Int, GetterName: String?, GetterNameLen: size_t, SetterName: String?, SetterNameLen: size_t, PropertyAttributes: Int, Ty: LLVMMetadataRef?): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge798(Builder.rawValue, Name?.cstr?.getPointer(memScope).rawValue, NameLen, File.rawValue, LineNo, GetterName?.cstr?.getPointer(memScope).rawValue, GetterNameLen, SetterName?.cstr?.getPointer(memScope).rawValue, SetterNameLen, PropertyAttributes, Ty.rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateObjectPointerType(Builder: LLVMDIBuilderRef?, Type: LLVMMetadataRef?): LLVMMetadataRef? {
    return interpretCPointer<LLVMOpaqueMetadata>(kniBridge799(Builder.rawValue, Type.rawValue))
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateQualifiedType(Builder: LLVMDIBuilderRef?, Tag: Int, Type: LLVMMetadataRef?): LLVMMetadataRef? {
    return interpretCPointer<LLVMOpaqueMetadata>(kniBridge800(Builder.rawValue, Tag, Type.rawValue))
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateReferenceType(Builder: LLVMDIBuilderRef?, Tag: Int, Type: LLVMMetadataRef?): LLVMMetadataRef? {
    return interpretCPointer<LLVMOpaqueMetadata>(kniBridge801(Builder.rawValue, Tag, Type.rawValue))
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateNullPtrType(Builder: LLVMDIBuilderRef?): LLVMMetadataRef? {
    return interpretCPointer<LLVMOpaqueMetadata>(kniBridge802(Builder.rawValue))
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateTypedef(Builder: LLVMDIBuilderRef?, Type: LLVMMetadataRef?, Name: String?, NameLen: size_t, File: LLVMMetadataRef?, LineNo: Int, Scope: LLVMMetadataRef?, AlignInBits: uint32_t): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge803(Builder.rawValue, Type.rawValue, Name?.cstr?.getPointer(memScope).rawValue, NameLen, File.rawValue, LineNo, Scope.rawValue, AlignInBits))
    }
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateInheritance(Builder: LLVMDIBuilderRef?, Ty: LLVMMetadataRef?, BaseTy: LLVMMetadataRef?, BaseOffset: uint64_t, VBPtrOffset: uint32_t, Flags: LLVMDIFlags): LLVMMetadataRef? {
    return interpretCPointer<LLVMOpaqueMetadata>(kniBridge804(Builder.rawValue, Ty.rawValue, BaseTy.rawValue, BaseOffset, VBPtrOffset, Flags))
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateForwardDecl(Builder: LLVMDIBuilderRef?, Tag: Int, Name: String?, NameLen: size_t, Scope: LLVMMetadataRef?, File: LLVMMetadataRef?, Line: Int, RuntimeLang: Int, SizeInBits: uint64_t, AlignInBits: uint32_t, UniqueIdentifier: String?, UniqueIdentifierLen: size_t): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge805(Builder.rawValue, Tag, Name?.cstr?.getPointer(memScope).rawValue, NameLen, Scope.rawValue, File.rawValue, Line, RuntimeLang, SizeInBits, AlignInBits, UniqueIdentifier?.cstr?.getPointer(memScope).rawValue, UniqueIdentifierLen))
    }
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateReplaceableCompositeType(Builder: LLVMDIBuilderRef?, Tag: Int, Name: String?, NameLen: size_t, Scope: LLVMMetadataRef?, File: LLVMMetadataRef?, Line: Int, RuntimeLang: Int, SizeInBits: uint64_t, AlignInBits: uint32_t, Flags: LLVMDIFlags, UniqueIdentifier: String?, UniqueIdentifierLen: size_t): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge806(Builder.rawValue, Tag, Name?.cstr?.getPointer(memScope).rawValue, NameLen, Scope.rawValue, File.rawValue, Line, RuntimeLang, SizeInBits, AlignInBits, Flags, UniqueIdentifier?.cstr?.getPointer(memScope).rawValue, UniqueIdentifierLen))
    }
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateBitFieldMemberType(Builder: LLVMDIBuilderRef?, Scope: LLVMMetadataRef?, Name: String?, NameLen: size_t, File: LLVMMetadataRef?, LineNumber: Int, SizeInBits: uint64_t, OffsetInBits: uint64_t, StorageOffsetInBits: uint64_t, Flags: LLVMDIFlags, Type: LLVMMetadataRef?): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge807(Builder.rawValue, Scope.rawValue, Name?.cstr?.getPointer(memScope).rawValue, NameLen, File.rawValue, LineNumber, SizeInBits, OffsetInBits, StorageOffsetInBits, Flags, Type.rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateClassType(Builder: LLVMDIBuilderRef?, Scope: LLVMMetadataRef?, Name: String?, NameLen: size_t, File: LLVMMetadataRef?, LineNumber: Int, SizeInBits: uint64_t, AlignInBits: uint32_t, OffsetInBits: uint64_t, Flags: LLVMDIFlags, DerivedFrom: LLVMMetadataRef?, Elements: CValuesRef<LLVMMetadataRefVar>?, NumElements: Int, VTableHolder: LLVMMetadataRef?, TemplateParamsNode: LLVMMetadataRef?, UniqueIdentifier: String?, UniqueIdentifierLen: size_t): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge808(Builder.rawValue, Scope.rawValue, Name?.cstr?.getPointer(memScope).rawValue, NameLen, File.rawValue, LineNumber, SizeInBits, AlignInBits, OffsetInBits, Flags, DerivedFrom.rawValue, Elements?.getPointer(memScope).rawValue, NumElements, VTableHolder.rawValue, TemplateParamsNode.rawValue, UniqueIdentifier?.cstr?.getPointer(memScope).rawValue, UniqueIdentifierLen))
    }
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateArtificialType(Builder: LLVMDIBuilderRef?, Type: LLVMMetadataRef?): LLVMMetadataRef? {
    return interpretCPointer<LLVMOpaqueMetadata>(kniBridge809(Builder.rawValue, Type.rawValue))
}

@ExperimentalForeignApi
fun LLVMDITypeGetName(DType: LLVMMetadataRef?, Length: CValuesRef<size_tVar>?): CPointer<ByteVar>? {
    memScoped {
        return interpretCPointer<ByteVar>(kniBridge810(DType.rawValue, Length?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun LLVMDITypeGetSizeInBits(DType: LLVMMetadataRef?): uint64_t {
    return kniBridge811(DType.rawValue)
}

@ExperimentalForeignApi
fun LLVMDITypeGetOffsetInBits(DType: LLVMMetadataRef?): uint64_t {
    return kniBridge812(DType.rawValue)
}

@ExperimentalForeignApi
fun LLVMDITypeGetAlignInBits(DType: LLVMMetadataRef?): uint32_t {
    return kniBridge813(DType.rawValue)
}

@ExperimentalForeignApi
fun LLVMDITypeGetLine(DType: LLVMMetadataRef?): Int {
    return kniBridge814(DType.rawValue)
}

@ExperimentalForeignApi
fun LLVMDITypeGetFlags(DType: LLVMMetadataRef?): LLVMDIFlags {
    return kniBridge815(DType.rawValue)
}

@ExperimentalForeignApi
fun LLVMDIBuilderGetOrCreateSubrange(Builder: LLVMDIBuilderRef?, LowerBound: int64_t, Count: int64_t): LLVMMetadataRef? {
    return interpretCPointer<LLVMOpaqueMetadata>(kniBridge816(Builder.rawValue, LowerBound, Count))
}

@ExperimentalForeignApi
fun LLVMDIBuilderGetOrCreateArray(Builder: LLVMDIBuilderRef?, Data: CValuesRef<LLVMMetadataRefVar>?, NumElements: size_t): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge817(Builder.rawValue, Data?.getPointer(memScope).rawValue, NumElements))
    }
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateExpression(Builder: LLVMDIBuilderRef?, Addr: CValuesRef<uint64_tVar>?, Length: size_t): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge818(Builder.rawValue, Addr?.getPointer(memScope).rawValue, Length))
    }
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateConstantValueExpression(Builder: LLVMDIBuilderRef?, Value: uint64_t): LLVMMetadataRef? {
    return interpretCPointer<LLVMOpaqueMetadata>(kniBridge819(Builder.rawValue, Value))
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateGlobalVariableExpression(Builder: LLVMDIBuilderRef?, Scope: LLVMMetadataRef?, Name: String?, NameLen: size_t, Linkage: String?, LinkLen: size_t, File: LLVMMetadataRef?, LineNo: Int, Ty: LLVMMetadataRef?, LocalToUnit: LLVMBool, Expr: LLVMMetadataRef?, Decl: LLVMMetadataRef?, AlignInBits: uint32_t): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge820(Builder.rawValue, Scope.rawValue, Name?.cstr?.getPointer(memScope).rawValue, NameLen, Linkage?.cstr?.getPointer(memScope).rawValue, LinkLen, File.rawValue, LineNo, Ty.rawValue, LocalToUnit, Expr.rawValue, Decl.rawValue, AlignInBits))
    }
}

@ExperimentalForeignApi
fun LLVMDIGlobalVariableExpressionGetVariable(GVE: LLVMMetadataRef?): LLVMMetadataRef? {
    return interpretCPointer<LLVMOpaqueMetadata>(kniBridge821(GVE.rawValue))
}

@ExperimentalForeignApi
fun LLVMDIGlobalVariableExpressionGetExpression(GVE: LLVMMetadataRef?): LLVMMetadataRef? {
    return interpretCPointer<LLVMOpaqueMetadata>(kniBridge822(GVE.rawValue))
}

@ExperimentalForeignApi
fun LLVMDIVariableGetFile(Var: LLVMMetadataRef?): LLVMMetadataRef? {
    return interpretCPointer<LLVMOpaqueMetadata>(kniBridge823(Var.rawValue))
}

@ExperimentalForeignApi
fun LLVMDIVariableGetScope(Var: LLVMMetadataRef?): LLVMMetadataRef? {
    return interpretCPointer<LLVMOpaqueMetadata>(kniBridge824(Var.rawValue))
}

@ExperimentalForeignApi
fun LLVMDIVariableGetLine(Var: LLVMMetadataRef?): Int {
    return kniBridge825(Var.rawValue)
}

@ExperimentalForeignApi
fun LLVMTemporaryMDNode(Ctx: LLVMContextRef?, Data: CValuesRef<LLVMMetadataRefVar>?, NumElements: size_t): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge826(Ctx.rawValue, Data?.getPointer(memScope).rawValue, NumElements))
    }
}

@ExperimentalForeignApi
fun LLVMDisposeTemporaryMDNode(TempNode: LLVMMetadataRef?): Unit {
    return kniBridge827(TempNode.rawValue)
}

@ExperimentalForeignApi
fun LLVMMetadataReplaceAllUsesWith(TempTargetMetadata: LLVMMetadataRef?, Replacement: LLVMMetadataRef?): Unit {
    return kniBridge828(TempTargetMetadata.rawValue, Replacement.rawValue)
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateTempGlobalVariableFwdDecl(Builder: LLVMDIBuilderRef?, Scope: LLVMMetadataRef?, Name: String?, NameLen: size_t, Linkage: String?, LnkLen: size_t, File: LLVMMetadataRef?, LineNo: Int, Ty: LLVMMetadataRef?, LocalToUnit: LLVMBool, Decl: LLVMMetadataRef?, AlignInBits: uint32_t): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge829(Builder.rawValue, Scope.rawValue, Name?.cstr?.getPointer(memScope).rawValue, NameLen, Linkage?.cstr?.getPointer(memScope).rawValue, LnkLen, File.rawValue, LineNo, Ty.rawValue, LocalToUnit, Decl.rawValue, AlignInBits))
    }
}

@ExperimentalForeignApi
fun LLVMDIBuilderInsertDeclareBefore(Builder: LLVMDIBuilderRef?, Storage: LLVMValueRef?, VarInfo: LLVMMetadataRef?, Expr: LLVMMetadataRef?, DebugLoc: LLVMMetadataRef?, Instr: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge830(Builder.rawValue, Storage.rawValue, VarInfo.rawValue, Expr.rawValue, DebugLoc.rawValue, Instr.rawValue))
}

@ExperimentalForeignApi
fun LLVMDIBuilderInsertDeclareAtEnd(Builder: LLVMDIBuilderRef?, Storage: LLVMValueRef?, VarInfo: LLVMMetadataRef?, Expr: LLVMMetadataRef?, DebugLoc: LLVMMetadataRef?, Block: LLVMBasicBlockRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge831(Builder.rawValue, Storage.rawValue, VarInfo.rawValue, Expr.rawValue, DebugLoc.rawValue, Block.rawValue))
}

@ExperimentalForeignApi
fun LLVMDIBuilderInsertDbgValueBefore(Builder: LLVMDIBuilderRef?, Val: LLVMValueRef?, VarInfo: LLVMMetadataRef?, Expr: LLVMMetadataRef?, DebugLoc: LLVMMetadataRef?, Instr: LLVMValueRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge832(Builder.rawValue, Val.rawValue, VarInfo.rawValue, Expr.rawValue, DebugLoc.rawValue, Instr.rawValue))
}

@ExperimentalForeignApi
fun LLVMDIBuilderInsertDbgValueAtEnd(Builder: LLVMDIBuilderRef?, Val: LLVMValueRef?, VarInfo: LLVMMetadataRef?, Expr: LLVMMetadataRef?, DebugLoc: LLVMMetadataRef?, Block: LLVMBasicBlockRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge833(Builder.rawValue, Val.rawValue, VarInfo.rawValue, Expr.rawValue, DebugLoc.rawValue, Block.rawValue))
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateAutoVariable(Builder: LLVMDIBuilderRef?, Scope: LLVMMetadataRef?, Name: String?, NameLen: size_t, File: LLVMMetadataRef?, LineNo: Int, Ty: LLVMMetadataRef?, AlwaysPreserve: LLVMBool, Flags: LLVMDIFlags, AlignInBits: uint32_t): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge834(Builder.rawValue, Scope.rawValue, Name?.cstr?.getPointer(memScope).rawValue, NameLen, File.rawValue, LineNo, Ty.rawValue, AlwaysPreserve, Flags, AlignInBits))
    }
}

@ExperimentalForeignApi
fun LLVMDIBuilderCreateParameterVariable(Builder: LLVMDIBuilderRef?, Scope: LLVMMetadataRef?, Name: String?, NameLen: size_t, ArgNo: Int, File: LLVMMetadataRef?, LineNo: Int, Ty: LLVMMetadataRef?, AlwaysPreserve: LLVMBool, Flags: LLVMDIFlags): LLVMMetadataRef? {
    memScoped {
        return interpretCPointer<LLVMOpaqueMetadata>(kniBridge835(Builder.rawValue, Scope.rawValue, Name?.cstr?.getPointer(memScope).rawValue, NameLen, ArgNo, File.rawValue, LineNo, Ty.rawValue, AlwaysPreserve, Flags))
    }
}

@ExperimentalForeignApi
fun LLVMGetSubprogram(Func: LLVMValueRef?): LLVMMetadataRef? {
    return interpretCPointer<LLVMOpaqueMetadata>(kniBridge836(Func.rawValue))
}

@ExperimentalForeignApi
fun LLVMSetSubprogram(Func: LLVMValueRef?, SP: LLVMMetadataRef?): Unit {
    return kniBridge837(Func.rawValue, SP.rawValue)
}

@ExperimentalForeignApi
fun LLVMDISubprogramGetLine(Subprogram: LLVMMetadataRef?): Int {
    return kniBridge838(Subprogram.rawValue)
}

@ExperimentalForeignApi
fun LLVMInstructionGetDebugLoc(Inst: LLVMValueRef?): LLVMMetadataRef? {
    return interpretCPointer<LLVMOpaqueMetadata>(kniBridge839(Inst.rawValue))
}

@ExperimentalForeignApi
fun LLVMInstructionSetDebugLoc(Inst: LLVMValueRef?, Loc: LLVMMetadataRef?): Unit {
    return kniBridge840(Inst.rawValue, Loc.rawValue)
}

@ExperimentalForeignApi
fun LLVMGetMetadataKind(Metadata: LLVMMetadataRef?): LLVMMetadataKind {
    return kniBridge841(Metadata.rawValue)
}

@ExperimentalForeignApi
fun DIFinalize(builder: DIBuilderRef?): Unit {
    return kniBridge842(builder.rawValue)
}

@ExperimentalForeignApi
fun DICreateCompilationUnit(builder: DIBuilderRef?, lang: Int, File: String?, dir: String?, producer: String?, isOptimized: Int, flags: String?, rv: Int): DICompileUnitRef? {
    memScoped {
        return interpretCPointer<DICompileUnit>(kniBridge843(builder.rawValue, lang, File?.cstr?.getPointer(memScope).rawValue, dir?.cstr?.getPointer(memScope).rawValue, producer?.cstr?.getPointer(memScope).rawValue, isOptimized, flags?.cstr?.getPointer(memScope).rawValue, rv))
    }
}

@ExperimentalForeignApi
fun DICreateFile(builder: DIBuilderRef?, filename: String?, directory: String?): DIFileRef? {
    memScoped {
        return interpretCPointer<DIFile>(kniBridge844(builder.rawValue, filename?.cstr?.getPointer(memScope).rawValue, directory?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun DICreateBasicType(builder: DIBuilderRef?, name: String?, sizeInBits: uint64_t, alignment: uint64_t, encoding: Int): DIBasicTypeRef? {
    memScoped {
        return interpretCPointer<DIBasicType>(kniBridge845(builder.rawValue, name?.cstr?.getPointer(memScope).rawValue, sizeInBits, alignment, encoding))
    }
}

@ExperimentalForeignApi
fun DICreateStructType(refBuilder: DIBuilderRef?, scope: DIScopeOpaqueRef?, name: String?, file: DIFileRef?, lineNumber: Int, sizeInBits: uint64_t, alignInBits: uint64_t, flags: Int, derivedFrom: DITypeOpaqueRef?, elements: CValuesRef<DIDerivedTypeRefVar>?, elementsCount: uint64_t, refPlace: DICompositeTypeRef?): DICompositeTypeRef? {
    memScoped {
        return interpretCPointer<DICompositeType>(kniBridge846(refBuilder.rawValue, scope.rawValue, name?.cstr?.getPointer(memScope).rawValue, file.rawValue, lineNumber, sizeInBits, alignInBits, flags, derivedFrom.rawValue, elements?.getPointer(memScope).rawValue, elementsCount, refPlace.rawValue))
    }
}

@ExperimentalForeignApi
fun DICreateArrayType(refBuilder: DIBuilderRef?, size: uint64_t, alignInBits: uint64_t, type: DITypeOpaqueRef?, elementsCount: uint64_t): DICompositeTypeRef? {
    return interpretCPointer<DICompositeType>(kniBridge847(refBuilder.rawValue, size, alignInBits, type.rawValue, elementsCount))
}

@ExperimentalForeignApi
fun DICreateReferenceType(refBuilder: DIBuilderRef?, refType: DITypeOpaqueRef?): DIDerivedTypeRef? {
    return interpretCPointer<DIDerivedType>(kniBridge848(refBuilder.rawValue, refType.rawValue))
}

@ExperimentalForeignApi
fun DICreatePointerType(refBuilder: DIBuilderRef?, refType: DITypeOpaqueRef?): DIDerivedTypeRef? {
    return interpretCPointer<DIDerivedType>(kniBridge849(refBuilder.rawValue, refType.rawValue))
}

@ExperimentalForeignApi
fun DICreateReplaceableCompositeType(refBuilder: DIBuilderRef?, tag: Int, name: String?, refScope: DIScopeOpaqueRef?, refFile: DIFileRef?, line: Int): DICompositeTypeRef? {
    memScoped {
        return interpretCPointer<DICompositeType>(kniBridge850(refBuilder.rawValue, tag, name?.cstr?.getPointer(memScope).rawValue, refScope.rawValue, refFile.rawValue, line))
    }
}

@ExperimentalForeignApi
fun DICreateMemberType(refBuilder: DIBuilderRef?, refScope: DIScopeOpaqueRef?, name: String?, file: DIFileRef?, lineNum: Int, sizeInBits: uint64_t, alignInBits: uint64_t, offsetInBits: uint64_t, flags: Int, type: DITypeOpaqueRef?): DIDerivedTypeRef? {
    memScoped {
        return interpretCPointer<DIDerivedType>(kniBridge851(refBuilder.rawValue, refScope.rawValue, name?.cstr?.getPointer(memScope).rawValue, file.rawValue, lineNum, sizeInBits, alignInBits, offsetInBits, flags, type.rawValue))
    }
}

@ExperimentalForeignApi
fun DICreateModule(builder: DIBuilderRef?, scope: DIScopeOpaqueRef?, name: String?, configurationMacro: String?, includePath: String?, iSysRoot: String?): DIModuleRef? {
    memScoped {
        return interpretCPointer<DIModule>(kniBridge852(builder.rawValue, scope.rawValue, name?.cstr?.getPointer(memScope).rawValue, configurationMacro?.cstr?.getPointer(memScope).rawValue, includePath?.cstr?.getPointer(memScope).rawValue, iSysRoot?.cstr?.getPointer(memScope).rawValue))
    }
}

@ExperimentalForeignApi
fun DICreateLexicalBlockFile(builderRef: DIBuilderRef?, scopeRef: DIScopeOpaqueRef?, fileRef: DIFileRef?): DIScopeOpaqueRef? {
    return interpretCPointer<DIScope>(kniBridge853(builderRef.rawValue, scopeRef.rawValue, fileRef.rawValue))
}

@ExperimentalForeignApi
fun DICreateLexicalBlock(builderRef: DIBuilderRef?, scopeRef: DIScopeOpaqueRef?, fileRef: DIFileRef?, line: Int, column: Int): DIScopeOpaqueRef? {
    return interpretCPointer<DIScope>(kniBridge854(builderRef.rawValue, scopeRef.rawValue, fileRef.rawValue, line, column))
}

@ExperimentalForeignApi
fun DICreateFunction(builder: DIBuilderRef?, scope: DIScopeOpaqueRef?, name: String?, linkageName: String?, file: DIFileRef?, lineNo: Int, type: DISubroutineTypeRef?, isLocal: Int, isDefinition: Int, scopeLine: Int, isTransparentStepping: Int): DISubprogramRef? {
    memScoped {
        return interpretCPointer<DISubprogram>(kniBridge855(builder.rawValue, scope.rawValue, name?.cstr?.getPointer(memScope).rawValue, linkageName?.cstr?.getPointer(memScope).rawValue, file.rawValue, lineNo, type.rawValue, isLocal, isDefinition, scopeLine, isTransparentStepping))
    }
}

@ExperimentalForeignApi
fun DICreateBridgeFunction(builder: DIBuilderRef?, scope: DIScopeOpaqueRef?, function: LLVMValueRef?, file: DIFileRef?, lineNo: Int, type: DISubroutineTypeRef?, isLocal: Int, isDefinition: Int, scopeLine: Int, isTransparentStepping: Int): DISubprogramRef? {
    return interpretCPointer<DISubprogram>(kniBridge856(builder.rawValue, scope.rawValue, function.rawValue, file.rawValue, lineNo, type.rawValue, isLocal, isDefinition, scopeLine, isTransparentStepping))
}

@ExperimentalForeignApi
fun DICreateSubroutineType(builder: DIBuilderRef?, types: CValuesRef<DITypeOpaqueRefVar>?, typesCount: Int): DISubroutineTypeRef? {
    memScoped {
        return interpretCPointer<DISubroutineType>(kniBridge857(builder.rawValue, types?.getPointer(memScope).rawValue, typesCount))
    }
}

@ExperimentalForeignApi
fun DICreateAutoVariable(builder: DIBuilderRef?, scope: DIScopeOpaqueRef?, name: String?, file: DIFileRef?, line: Int, type: DITypeOpaqueRef?): DILocalVariableRef? {
    memScoped {
        return interpretCPointer<DILocalVariable>(kniBridge858(builder.rawValue, scope.rawValue, name?.cstr?.getPointer(memScope).rawValue, file.rawValue, line, type.rawValue))
    }
}

@ExperimentalForeignApi
fun DICreateParameterVariable(builder: DIBuilderRef?, scope: DIScopeOpaqueRef?, name: String?, argNo: Int, file: DIFileRef?, line: Int, type: DITypeOpaqueRef?): DILocalVariableRef? {
    memScoped {
        return interpretCPointer<DILocalVariable>(kniBridge859(builder.rawValue, scope.rawValue, name?.cstr?.getPointer(memScope).rawValue, argNo, file.rawValue, line, type.rawValue))
    }
}

@ExperimentalForeignApi
fun DIInsertDeclaration(builder: DIBuilderRef?, value: LLVMValueRef?, localVariable: DILocalVariableRef?, location: DILocationRef?, bb: LLVMBasicBlockRef?, expr: CValuesRef<int64_tVar>?, exprCount: uint64_t): Unit {
    memScoped {
        return kniBridge860(builder.rawValue, value.rawValue, localVariable.rawValue, location.rawValue, bb.rawValue, expr?.getPointer(memScope).rawValue, exprCount)
    }
}

@ExperimentalForeignApi
fun DICreateEmptyExpression(builder: DIBuilderRef?): DIExpressionRef? {
    return interpretCPointer<DIExpression>(kniBridge861(builder.rawValue))
}

@ExperimentalForeignApi
fun DIFunctionAddSubprogram(fn: LLVMValueRef?, sp: DISubprogramRef?): Unit {
    return kniBridge862(fn.rawValue, sp.rawValue)
}

@ExperimentalForeignApi
fun LLVMCreateLocation(contextRef: LLVMContextRef?, line: Int, col: Int, scope: DIScopeOpaqueRef?): DILocationRef? {
    return interpretCPointer<DILocation>(kniBridge863(contextRef.rawValue, line, col, scope.rawValue))
}

@ExperimentalForeignApi
fun LLVMCreateLocationInlinedAt(contextRef: LLVMContextRef?, line: Int, col: Int, scope: DIScopeOpaqueRef?, refLocation: DILocationRef?): DILocationRef? {
    return interpretCPointer<DILocation>(kniBridge864(contextRef.rawValue, line, col, scope.rawValue, refLocation.rawValue))
}

@ExperimentalForeignApi
fun LLVMBuilderSetDebugLocation(builder: LLVMBuilderRef?, refLocation: DILocationRef?): Unit {
    return kniBridge865(builder.rawValue, refLocation.rawValue)
}

@ExperimentalForeignApi
fun LLVMBuilderResetDebugLocation(builder: LLVMBuilderRef?): Unit {
    return kniBridge866(builder.rawValue)
}

@ExperimentalForeignApi
fun LLVMBuilderGetCurrentFunction(builder: LLVMBuilderRef?): LLVMValueRef? {
    return interpretCPointer<LLVMOpaqueValue>(kniBridge867(builder.rawValue))
}

@ExperimentalForeignApi
fun DISubprogramDescribesFunction(sp: DISubprogramRef?, fn: LLVMValueRef?): Int {
    return kniBridge868(sp.rawValue, fn.rawValue)
}

@ExperimentalForeignApi
fun LLVMKotlinInitializeTargets(): Unit {
    return kniBridge869()
}

@ExperimentalForeignApi
fun LLVMSetNoTailCall(Call: LLVMValueRef?): Unit {
    return kniBridge870(Call.rawValue)
}

@ExperimentalForeignApi
fun LLVMInlineCall(call: LLVMValueRef?): Int {
    return kniBridge871(call.rawValue)
}

@ExperimentalForeignApi
fun LLVMSetTimePasses(enabled: Int): Unit {
    return kniBridge872(enabled)
}

@ExperimentalForeignApi
fun LLVMPrintAllTimersToStdOut(): Unit {
    return kniBridge873()
}

@ExperimentalForeignApi
fun LLVMClearAllTimers(): Unit {
    return kniBridge874()
}

@ExperimentalForeignApi
fun LLVMKotlinRemoveRedundantSafepoints(module: LLVMModuleRef?, isSafePointInliningAllowed: Int): Unit {
    return kniBridge875(module.rawValue, isSafePointInliningAllowed)
}

@ExperimentalForeignApi
fun LLVMGetProgramAddressSpace(moduleRef: LLVMModuleRef?): Int {
    return kniBridge876(moduleRef.rawValue)
}

@ExperimentalForeignApi
val LLVMErrorSuccess: Int get() = 0

@ExperimentalForeignApi
typealias LLVMFatalErrorHandlerVar = CPointerVarOf<LLVMFatalErrorHandler>

@ExperimentalForeignApi
typealias LLVMFatalErrorHandler = CPointer<CFunction<(CPointer<ByteVar>?) -> Unit>>

@ExperimentalForeignApi
typealias LLVMBoolVar = IntVarOf<LLVMBool>

@ExperimentalForeignApi
typealias LLVMBool = Int

@ExperimentalForeignApi
typealias LLVMMemoryBufferRefVar = CPointerVarOf<LLVMMemoryBufferRef>

@ExperimentalForeignApi
typealias LLVMMemoryBufferRef = CPointer<LLVMOpaqueMemoryBuffer>

@ExperimentalForeignApi
typealias LLVMContextRefVar = CPointerVarOf<LLVMContextRef>

@ExperimentalForeignApi
typealias LLVMContextRef = CPointer<LLVMOpaqueContext>

@ExperimentalForeignApi
typealias LLVMModuleRefVar = CPointerVarOf<LLVMModuleRef>

@ExperimentalForeignApi
typealias LLVMModuleRef = CPointer<LLVMOpaqueModule>

@ExperimentalForeignApi
typealias LLVMTypeRefVar = CPointerVarOf<LLVMTypeRef>

@ExperimentalForeignApi
typealias LLVMTypeRef = CPointer<LLVMOpaqueType>

@ExperimentalForeignApi
typealias LLVMValueRefVar = CPointerVarOf<LLVMValueRef>

@ExperimentalForeignApi
typealias LLVMValueRef = CPointer<LLVMOpaqueValue>

@ExperimentalForeignApi
typealias LLVMBasicBlockRefVar = CPointerVarOf<LLVMBasicBlockRef>

@ExperimentalForeignApi
typealias LLVMBasicBlockRef = CPointer<LLVMOpaqueBasicBlock>

@ExperimentalForeignApi
typealias LLVMMetadataRefVar = CPointerVarOf<LLVMMetadataRef>

@ExperimentalForeignApi
typealias LLVMMetadataRef = CPointer<LLVMOpaqueMetadata>

@ExperimentalForeignApi
typealias LLVMNamedMDNodeRefVar = CPointerVarOf<LLVMNamedMDNodeRef>

@ExperimentalForeignApi
typealias LLVMNamedMDNodeRef = CPointer<LLVMOpaqueNamedMDNode>

@ExperimentalForeignApi
typealias LLVMValueMetadataEntry = LLVMOpaqueValueMetadataEntry

@ExperimentalForeignApi
typealias LLVMBuilderRefVar = CPointerVarOf<LLVMBuilderRef>

@ExperimentalForeignApi
typealias LLVMBuilderRef = CPointer<LLVMOpaqueBuilder>

@ExperimentalForeignApi
typealias LLVMDIBuilderRefVar = CPointerVarOf<LLVMDIBuilderRef>

@ExperimentalForeignApi
typealias LLVMDIBuilderRef = CPointer<LLVMOpaqueDIBuilder>

@ExperimentalForeignApi
typealias LLVMModuleProviderRefVar = CPointerVarOf<LLVMModuleProviderRef>

@ExperimentalForeignApi
typealias LLVMModuleProviderRef = CPointer<LLVMOpaqueModuleProvider>

@ExperimentalForeignApi
typealias LLVMPassManagerRefVar = CPointerVarOf<LLVMPassManagerRef>

@ExperimentalForeignApi
typealias LLVMPassManagerRef = CPointer<LLVMOpaquePassManager>

@ExperimentalForeignApi
typealias LLVMPassRegistryRefVar = CPointerVarOf<LLVMPassRegistryRef>

@ExperimentalForeignApi
typealias LLVMPassRegistryRef = CPointer<LLVMOpaquePassRegistry>

@ExperimentalForeignApi
typealias LLVMUseRefVar = CPointerVarOf<LLVMUseRef>

@ExperimentalForeignApi
typealias LLVMUseRef = CPointer<LLVMOpaqueUse>

@ExperimentalForeignApi
typealias LLVMAttributeRefVar = CPointerVarOf<LLVMAttributeRef>

@ExperimentalForeignApi
typealias LLVMAttributeRef = CPointer<LLVMOpaqueAttributeRef>

@ExperimentalForeignApi
typealias LLVMDiagnosticInfoRefVar = CPointerVarOf<LLVMDiagnosticInfoRef>

@ExperimentalForeignApi
typealias LLVMDiagnosticInfoRef = CPointer<LLVMOpaqueDiagnosticInfo>

@ExperimentalForeignApi
typealias LLVMComdatRefVar = CPointerVarOf<LLVMComdatRef>

@ExperimentalForeignApi
typealias LLVMComdatRef = CPointer<LLVMComdat>

@ExperimentalForeignApi
typealias LLVMModuleFlagEntry = LLVMOpaqueModuleFlagEntry

@ExperimentalForeignApi
typealias LLVMJITEventListenerRefVar = CPointerVarOf<LLVMJITEventListenerRef>

@ExperimentalForeignApi
typealias LLVMJITEventListenerRef = CPointer<LLVMOpaqueJITEventListener>

@ExperimentalForeignApi
typealias LLVMBinaryRefVar = CPointerVarOf<LLVMBinaryRef>

@ExperimentalForeignApi
typealias LLVMBinaryRef = CPointer<LLVMOpaqueBinary>

@ExperimentalForeignApi
typealias LLVMAttributeIndexVar = IntVarOf<LLVMAttributeIndex>

@ExperimentalForeignApi
typealias LLVMAttributeIndex = Int

@ExperimentalForeignApi
typealias LLVMDiagnosticHandlerVar = CPointerVarOf<LLVMDiagnosticHandler>

@ExperimentalForeignApi
typealias LLVMDiagnosticHandler = CPointer<CFunction<(LLVMDiagnosticInfoRef?, COpaquePointer?) -> Unit>>

@ExperimentalForeignApi
typealias LLVMYieldCallbackVar = CPointerVarOf<LLVMYieldCallback>

@ExperimentalForeignApi
typealias LLVMYieldCallback = CPointer<CFunction<(LLVMContextRef?, COpaquePointer?) -> Unit>>

@ExperimentalForeignApi
typealias size_tVar = LongVarOf<size_t>

@ExperimentalForeignApi
typealias size_t = Long

@ExperimentalForeignApi
typealias uint64_tVar = LongVarOf<uint64_t>

@ExperimentalForeignApi
typealias uint64_t = Long

@ExperimentalForeignApi
typealias uint8_tVar = ByteVarOf<uint8_t>

@ExperimentalForeignApi
typealias uint8_t = Byte

@ExperimentalForeignApi
typealias LLVMTargetDataRefVar = CPointerVarOf<LLVMTargetDataRef>

@ExperimentalForeignApi
typealias LLVMTargetDataRef = CPointer<LLVMOpaqueTargetData>

@ExperimentalForeignApi
typealias LLVMTargetLibraryInfoRefVar = CPointerVarOf<LLVMTargetLibraryInfoRef>

@ExperimentalForeignApi
typealias LLVMTargetLibraryInfoRef = CPointer<LLVMOpaqueTargetLibraryInfotData>

@ExperimentalForeignApi
typealias LLVMErrorRefVar = CPointerVarOf<LLVMErrorRef>

@ExperimentalForeignApi
typealias LLVMErrorRef = CPointer<LLVMOpaqueError>

@ExperimentalForeignApi
typealias LLVMErrorTypeIdVar = CPointerVarOf<LLVMErrorTypeId>

@ExperimentalForeignApi
typealias LLVMErrorTypeId = COpaquePointer

@ExperimentalForeignApi
typealias LLVMTargetMachineRefVar = CPointerVarOf<LLVMTargetMachineRef>

@ExperimentalForeignApi
typealias LLVMTargetMachineRef = CPointer<LLVMOpaqueTargetMachine>

@ExperimentalForeignApi
typealias LLVMTargetRefVar = CPointerVarOf<LLVMTargetRef>

@ExperimentalForeignApi
typealias LLVMTargetRef = CPointer<LLVMTarget>

@ExperimentalForeignApi
typealias LLVMPassBuilderOptionsRefVar = CPointerVarOf<LLVMPassBuilderOptionsRef>

@ExperimentalForeignApi
typealias LLVMPassBuilderOptionsRef = CPointer<LLVMOpaquePassBuilderOptions>

@ExperimentalForeignApi
typealias LLVMMetadataKindVar = IntVarOf<LLVMMetadataKind>

@ExperimentalForeignApi
typealias LLVMMetadataKind = Int

@ExperimentalForeignApi
typealias LLVMDWARFTypeEncodingVar = IntVarOf<LLVMDWARFTypeEncoding>

@ExperimentalForeignApi
typealias LLVMDWARFTypeEncoding = Int

@ExperimentalForeignApi
typealias int64_tVar = LongVarOf<int64_t>

@ExperimentalForeignApi
typealias int64_t = Long

@ExperimentalForeignApi
typealias uint32_tVar = IntVarOf<uint32_t>

@ExperimentalForeignApi
typealias uint32_t = Int

@ExperimentalForeignApi
typealias DIBuilderRefVar = CPointerVarOf<DIBuilderRef>

@ExperimentalForeignApi
typealias DIBuilderRef = CPointer<LLVMOpaqueDIBuilder>

@ExperimentalForeignApi
typealias DICompileUnitRefVar = CPointerVarOf<DICompileUnitRef>

@ExperimentalForeignApi
typealias DICompileUnitRef = CPointer<DICompileUnit>

@ExperimentalForeignApi
typealias DIFileRefVar = CPointerVarOf<DIFileRef>

@ExperimentalForeignApi
typealias DIFileRef = CPointer<DIFile>

@ExperimentalForeignApi
typealias DIBasicTypeRefVar = CPointerVarOf<DIBasicTypeRef>

@ExperimentalForeignApi
typealias DIBasicTypeRef = CPointer<DIBasicType>

@ExperimentalForeignApi
typealias DICompositeTypeRefVar = CPointerVarOf<DICompositeTypeRef>

@ExperimentalForeignApi
typealias DICompositeTypeRef = CPointer<DICompositeType>

@ExperimentalForeignApi
typealias DIDerivedTypeRefVar = CPointerVarOf<DIDerivedTypeRef>

@ExperimentalForeignApi
typealias DIDerivedTypeRef = CPointer<DIDerivedType>

@ExperimentalForeignApi
typealias DITypeOpaqueRefVar = CPointerVarOf<DITypeOpaqueRef>

@ExperimentalForeignApi
typealias DITypeOpaqueRef = CPointer<DIType>

@ExperimentalForeignApi
typealias DISubprogramRefVar = CPointerVarOf<DISubprogramRef>

@ExperimentalForeignApi
typealias DISubprogramRef = CPointer<DISubprogram>

@ExperimentalForeignApi
typealias DIModuleRefVar = CPointerVarOf<DIModuleRef>

@ExperimentalForeignApi
typealias DIModuleRef = CPointer<DIModule>

@ExperimentalForeignApi
typealias DIScopeOpaqueRefVar = CPointerVarOf<DIScopeOpaqueRef>

@ExperimentalForeignApi
typealias DIScopeOpaqueRef = CPointer<DIScope>

@ExperimentalForeignApi
typealias DISubroutineTypeRefVar = CPointerVarOf<DISubroutineTypeRef>

@ExperimentalForeignApi
typealias DISubroutineTypeRef = CPointer<DISubroutineType>

@ExperimentalForeignApi
typealias DILocationRefVar = CPointerVarOf<DILocationRef>

@ExperimentalForeignApi
typealias DILocationRef = CPointer<DILocation>

@ExperimentalForeignApi
typealias DILocalVariableRefVar = CPointerVarOf<DILocalVariableRef>

@ExperimentalForeignApi
typealias DILocalVariableRef = CPointer<DILocalVariable>

@ExperimentalForeignApi
typealias DIExpressionRefVar = CPointerVarOf<DIExpressionRef>

@ExperimentalForeignApi
typealias DIExpressionRef = CPointer<DIExpression>

// enum (unnamed at /home/teamcity/.konan/dependencies/llvm-16.0.0-x86_64-linux-dev-80/include/llvm-c/Core.h:463:1):

@ExperimentalForeignApi
val LLVMAttributeReturnIndex: Int get() = 0

@ExperimentalForeignApi
val LLVMAttributeFunctionIndex: Int get() = -1


@ExperimentalForeignApi
val LLVMLinkerDestroySource: LLVMLinkerMode get() = 0

@ExperimentalForeignApi
val LLVMLinkerPreserveSource_Removed: LLVMLinkerMode get() = 1

@ExperimentalForeignApi
typealias LLVMLinkerModeVar = IntVarOf<LLVMLinkerMode>

@ExperimentalForeignApi
typealias LLVMLinkerMode = Int


@ExperimentalForeignApi
val LLVMDIFlagZero: LLVMDIFlags get() = 0

@ExperimentalForeignApi
val LLVMDIFlagPrivate: LLVMDIFlags get() = 1

@ExperimentalForeignApi
val LLVMDIFlagProtected: LLVMDIFlags get() = 2

@ExperimentalForeignApi
val LLVMDIFlagPublic: LLVMDIFlags get() = 3

@ExperimentalForeignApi
val LLVMDIFlagFwdDecl: LLVMDIFlags get() = 4

@ExperimentalForeignApi
val LLVMDIFlagAppleBlock: LLVMDIFlags get() = 8

@ExperimentalForeignApi
val LLVMDIFlagReservedBit4: LLVMDIFlags get() = 16

@ExperimentalForeignApi
val LLVMDIFlagVirtual: LLVMDIFlags get() = 32

@ExperimentalForeignApi
val LLVMDIFlagArtificial: LLVMDIFlags get() = 64

@ExperimentalForeignApi
val LLVMDIFlagExplicit: LLVMDIFlags get() = 128

@ExperimentalForeignApi
val LLVMDIFlagPrototyped: LLVMDIFlags get() = 256

@ExperimentalForeignApi
val LLVMDIFlagObjcClassComplete: LLVMDIFlags get() = 512

@ExperimentalForeignApi
val LLVMDIFlagObjectPointer: LLVMDIFlags get() = 1024

@ExperimentalForeignApi
val LLVMDIFlagVector: LLVMDIFlags get() = 2048

@ExperimentalForeignApi
val LLVMDIFlagStaticMember: LLVMDIFlags get() = 4096

@ExperimentalForeignApi
val LLVMDIFlagLValueReference: LLVMDIFlags get() = 8192

@ExperimentalForeignApi
val LLVMDIFlagRValueReference: LLVMDIFlags get() = 16384

@ExperimentalForeignApi
val LLVMDIFlagReserved: LLVMDIFlags get() = 32768

@ExperimentalForeignApi
val LLVMDIFlagSingleInheritance: LLVMDIFlags get() = 65536

@ExperimentalForeignApi
val LLVMDIFlagMultipleInheritance: LLVMDIFlags get() = 131072

@ExperimentalForeignApi
val LLVMDIFlagVirtualInheritance: LLVMDIFlags get() = 196608

@ExperimentalForeignApi
val LLVMDIFlagIntroducedVirtual: LLVMDIFlags get() = 262144

@ExperimentalForeignApi
val LLVMDIFlagBitField: LLVMDIFlags get() = 524288

@ExperimentalForeignApi
val LLVMDIFlagNoReturn: LLVMDIFlags get() = 1048576

@ExperimentalForeignApi
val LLVMDIFlagTypePassByValue: LLVMDIFlags get() = 4194304

@ExperimentalForeignApi
val LLVMDIFlagTypePassByReference: LLVMDIFlags get() = 8388608

@ExperimentalForeignApi
val LLVMDIFlagEnumClass: LLVMDIFlags get() = 16777216

@ExperimentalForeignApi
val LLVMDIFlagFixedEnum: LLVMDIFlags get() = 16777216

@ExperimentalForeignApi
val LLVMDIFlagThunk: LLVMDIFlags get() = 33554432

@ExperimentalForeignApi
val LLVMDIFlagNonTrivial: LLVMDIFlags get() = 67108864

@ExperimentalForeignApi
val LLVMDIFlagBigEndian: LLVMDIFlags get() = 134217728

@ExperimentalForeignApi
val LLVMDIFlagLittleEndian: LLVMDIFlags get() = 268435456

@ExperimentalForeignApi
val LLVMDIFlagIndirectVirtualBase: LLVMDIFlags get() = 36

@ExperimentalForeignApi
val LLVMDIFlagAccessibility: LLVMDIFlags get() = 3

@ExperimentalForeignApi
val LLVMDIFlagPtrToMemberRep: LLVMDIFlags get() = 196608

@ExperimentalForeignApi
typealias LLVMDIFlagsVar = IntVarOf<LLVMDIFlags>

@ExperimentalForeignApi
typealias LLVMDIFlags = Int


@ExperimentalForeignApi
val LLVMDWARFEmissionNone: LLVMDWARFEmissionKind get() = 0

@ExperimentalForeignApi
val LLVMDWARFEmissionFull: LLVMDWARFEmissionKind get() = 1

@ExperimentalForeignApi
val LLVMDWARFEmissionLineTablesOnly: LLVMDWARFEmissionKind get() = 2

@ExperimentalForeignApi
typealias LLVMDWARFEmissionKindVar = IntVarOf<LLVMDWARFEmissionKind>

@ExperimentalForeignApi
typealias LLVMDWARFEmissionKind = Int

// enum (unnamed at /home/teamcity/.konan/dependencies/llvm-16.0.0-x86_64-linux-dev-80/include/llvm-c/DebugInfo.h:146:1):

@ExperimentalForeignApi
val LLVMMDStringMetadataKind: Int get() = 0

@ExperimentalForeignApi
val LLVMConstantAsMetadataMetadataKind: Int get() = 1

@ExperimentalForeignApi
val LLVMLocalAsMetadataMetadataKind: Int get() = 2

@ExperimentalForeignApi
val LLVMDistinctMDOperandPlaceholderMetadataKind: Int get() = 3

@ExperimentalForeignApi
val LLVMMDTupleMetadataKind: Int get() = 4

@ExperimentalForeignApi
val LLVMDILocationMetadataKind: Int get() = 5

@ExperimentalForeignApi
val LLVMDIExpressionMetadataKind: Int get() = 6

@ExperimentalForeignApi
val LLVMDIGlobalVariableExpressionMetadataKind: Int get() = 7

@ExperimentalForeignApi
val LLVMGenericDINodeMetadataKind: Int get() = 8

@ExperimentalForeignApi
val LLVMDISubrangeMetadataKind: Int get() = 9

@ExperimentalForeignApi
val LLVMDIEnumeratorMetadataKind: Int get() = 10

@ExperimentalForeignApi
val LLVMDIBasicTypeMetadataKind: Int get() = 11

@ExperimentalForeignApi
val LLVMDIDerivedTypeMetadataKind: Int get() = 12

@ExperimentalForeignApi
val LLVMDICompositeTypeMetadataKind: Int get() = 13

@ExperimentalForeignApi
val LLVMDISubroutineTypeMetadataKind: Int get() = 14

@ExperimentalForeignApi
val LLVMDIFileMetadataKind: Int get() = 15

@ExperimentalForeignApi
val LLVMDICompileUnitMetadataKind: Int get() = 16

@ExperimentalForeignApi
val LLVMDISubprogramMetadataKind: Int get() = 17

@ExperimentalForeignApi
val LLVMDILexicalBlockMetadataKind: Int get() = 18

@ExperimentalForeignApi
val LLVMDILexicalBlockFileMetadataKind: Int get() = 19

@ExperimentalForeignApi
val LLVMDINamespaceMetadataKind: Int get() = 20

@ExperimentalForeignApi
val LLVMDIModuleMetadataKind: Int get() = 21

@ExperimentalForeignApi
val LLVMDITemplateTypeParameterMetadataKind: Int get() = 22

@ExperimentalForeignApi
val LLVMDITemplateValueParameterMetadataKind: Int get() = 23

@ExperimentalForeignApi
val LLVMDIGlobalVariableMetadataKind: Int get() = 24

@ExperimentalForeignApi
val LLVMDILocalVariableMetadataKind: Int get() = 25

@ExperimentalForeignApi
val LLVMDILabelMetadataKind: Int get() = 26

@ExperimentalForeignApi
val LLVMDIObjCPropertyMetadataKind: Int get() = 27

@ExperimentalForeignApi
val LLVMDIImportedEntityMetadataKind: Int get() = 28

@ExperimentalForeignApi
val LLVMDIMacroMetadataKind: Int get() = 29

@ExperimentalForeignApi
val LLVMDIMacroFileMetadataKind: Int get() = 30

@ExperimentalForeignApi
val LLVMDICommonBlockMetadataKind: Int get() = 31

@ExperimentalForeignApi
val LLVMDIStringTypeMetadataKind: Int get() = 32

@ExperimentalForeignApi
val LLVMDIGenericSubrangeMetadataKind: Int get() = 33

@ExperimentalForeignApi
val LLVMDIArgListMetadataKind: Int get() = 34

@ExperimentalForeignApi
val LLVMDIAssignIDMetadataKind: Int get() = 35


@ExperimentalForeignApi
val LLVMDWARFMacinfoRecordTypeDefine: LLVMDWARFMacinfoRecordType get() = 1

@ExperimentalForeignApi
val LLVMDWARFMacinfoRecordTypeMacro: LLVMDWARFMacinfoRecordType get() = 2

@ExperimentalForeignApi
val LLVMDWARFMacinfoRecordTypeStartFile: LLVMDWARFMacinfoRecordType get() = 3

@ExperimentalForeignApi
val LLVMDWARFMacinfoRecordTypeEndFile: LLVMDWARFMacinfoRecordType get() = 4

@ExperimentalForeignApi
val LLVMDWARFMacinfoRecordTypeVendorExt: LLVMDWARFMacinfoRecordType get() = 255

@ExperimentalForeignApi
typealias LLVMDWARFMacinfoRecordTypeVar = IntVarOf<LLVMDWARFMacinfoRecordType>

@ExperimentalForeignApi
typealias LLVMDWARFMacinfoRecordType = Int
private external fun kniBridge0(p0: NativePtr): Unit
private external fun kniBridge1(): Unit
private external fun kniBridge2(): Unit
private external fun kniBridge3(p0: NativePtr): Unit
private external fun kniBridge4(): Unit
private external fun kniBridge5(p0: NativePtr, p1: NativePtr, p2: NativePtr): Unit
private external fun kniBridge6(p0: NativePtr): NativePtr
private external fun kniBridge7(p0: NativePtr): Unit
private external fun kniBridge8(): NativePtr
private external fun kniBridge9(p0: NativePtr, p1: NativePtr, p2: NativePtr): Unit
private external fun kniBridge10(p0: NativePtr): NativePtr
private external fun kniBridge11(p0: NativePtr): NativePtr
private external fun kniBridge12(p0: NativePtr, p1: NativePtr, p2: NativePtr): Unit
private external fun kniBridge13(p0: NativePtr): Int
private external fun kniBridge14(p0: NativePtr, p1: Int): Unit
private external fun kniBridge15(p0: NativePtr, p1: Int): Unit
private external fun kniBridge16(p0: NativePtr): Unit
private external fun kniBridge17(p0: NativePtr): NativePtr
private external fun kniBridge18(p0: NativePtr): Int
private external fun kniBridge19(p0: NativePtr, p1: NativePtr, p2: Int): Int
private external fun kniBridge20(p0: NativePtr, p1: Long): Int
private external fun kniBridge21(): Int
private external fun kniBridge22(p0: NativePtr, p1: Int, p2: Long): NativePtr
private external fun kniBridge23(p0: NativePtr): Int
private external fun kniBridge24(p0: NativePtr): Long
private external fun kniBridge25(p0: NativePtr, p1: Int, p2: NativePtr): NativePtr
private external fun kniBridge26(p0: NativePtr): NativePtr
private external fun kniBridge27(p0: NativePtr, p1: NativePtr, p2: Int, p3: NativePtr, p4: Int): NativePtr
private external fun kniBridge28(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge29(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge30(p0: NativePtr): Int
private external fun kniBridge31(p0: NativePtr): Int
private external fun kniBridge32(p0: NativePtr): Int
private external fun kniBridge33(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge34(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge35(p0: NativePtr): NativePtr
private external fun kniBridge36(p0: NativePtr): Unit
private external fun kniBridge37(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge38(p0: NativePtr, p1: NativePtr, p2: Long): Unit
private external fun kniBridge39(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge40(p0: NativePtr, p1: NativePtr, p2: Long): Unit
private external fun kniBridge41(p0: NativePtr): NativePtr
private external fun kniBridge42(p0: NativePtr): NativePtr
private external fun kniBridge43(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge44(p0: NativePtr): NativePtr
private external fun kniBridge45(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge46(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge47(p0: NativePtr): Unit
private external fun kniBridge48(p0: NativePtr, p1: Int): Int
private external fun kniBridge49(p0: NativePtr, p1: Int, p2: NativePtr): NativePtr
private external fun kniBridge50(p0: NativePtr, p1: Int): NativePtr
private external fun kniBridge51(p0: NativePtr, p1: NativePtr, p2: Long): NativePtr
private external fun kniBridge52(p0: NativePtr, p1: Int, p2: NativePtr, p3: Long, p4: NativePtr): Unit
private external fun kniBridge53(p0: NativePtr): Unit
private external fun kniBridge54(p0: NativePtr, p1: NativePtr, p2: NativePtr): Int
private external fun kniBridge55(p0: NativePtr): NativePtr
private external fun kniBridge56(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge57(p0: NativePtr, p1: NativePtr, p2: Long): Unit
private external fun kniBridge58(p0: NativePtr, p1: NativePtr, p2: Long): Unit
private external fun kniBridge59(p0: NativePtr, p1: NativePtr, p2: Long, p3: NativePtr, p4: Long, p5: Int, p6: Int, p7: Int, p8: Int): NativePtr
private external fun kniBridge60(p0: NativePtr): NativePtr
private external fun kniBridge61(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge62(p0: NativePtr): NativePtr
private external fun kniBridge63(p0: NativePtr): NativePtr
private external fun kniBridge64(p0: NativePtr): NativePtr
private external fun kniBridge65(p0: NativePtr): NativePtr
private external fun kniBridge66(p0: NativePtr, p1: NativePtr, p2: Long): NativePtr
private external fun kniBridge67(p0: NativePtr, p1: NativePtr, p2: Long): NativePtr
private external fun kniBridge68(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge69(p0: NativePtr, p1: NativePtr): Int
private external fun kniBridge70(p0: NativePtr, p1: NativePtr, p2: NativePtr): Unit
private external fun kniBridge71(p0: NativePtr, p1: NativePtr, p2: NativePtr): Unit
private external fun kniBridge72(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge73(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge74(p0: NativePtr): Int
private external fun kniBridge75(p0: NativePtr): Int
private external fun kniBridge76(p0: NativePtr, p1: NativePtr, p2: NativePtr): NativePtr
private external fun kniBridge77(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge78(p0: NativePtr): NativePtr
private external fun kniBridge79(p0: NativePtr): NativePtr
private external fun kniBridge80(p0: NativePtr): NativePtr
private external fun kniBridge81(p0: NativePtr): NativePtr
private external fun kniBridge82(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge83(p0: NativePtr): Int
private external fun kniBridge84(p0: NativePtr): Int
private external fun kniBridge85(p0: NativePtr): NativePtr
private external fun kniBridge86(p0: NativePtr): Unit
private external fun kniBridge87(p0: NativePtr): NativePtr
private external fun kniBridge88(p0: NativePtr): NativePtr
private external fun kniBridge89(p0: NativePtr): NativePtr
private external fun kniBridge90(p0: NativePtr): NativePtr
private external fun kniBridge91(p0: NativePtr): NativePtr
private external fun kniBridge92(p0: NativePtr): NativePtr
private external fun kniBridge93(p0: NativePtr): NativePtr
private external fun kniBridge94(p0: NativePtr, p1: Int): NativePtr
private external fun kniBridge95(p0: NativePtr): Int
private external fun kniBridge96(p0: NativePtr): NativePtr
private external fun kniBridge97(p0: NativePtr): NativePtr
private external fun kniBridge98(p0: NativePtr): NativePtr
private external fun kniBridge99(p0: NativePtr): NativePtr
private external fun kniBridge100(p0: NativePtr): NativePtr
private external fun kniBridge101(p0: NativePtr): NativePtr
private external fun kniBridge102(p0: NativePtr): NativePtr
private external fun kniBridge103(): NativePtr
private external fun kniBridge104(p0: NativePtr, p1: NativePtr, p2: Int, p3: Int): NativePtr
private external fun kniBridge105(p0: NativePtr): Int
private external fun kniBridge106(p0: NativePtr): NativePtr
private external fun kniBridge107(p0: NativePtr): Int
private external fun kniBridge108(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge109(p0: NativePtr, p1: NativePtr, p2: Int, p3: Int): NativePtr
private external fun kniBridge110(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge111(p0: NativePtr): NativePtr
private external fun kniBridge112(p0: NativePtr, p1: NativePtr, p2: Int, p3: Int): Unit
private external fun kniBridge113(p0: NativePtr): Int
private external fun kniBridge114(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge115(p0: NativePtr, p1: Int): NativePtr
private external fun kniBridge116(p0: NativePtr): Int
private external fun kniBridge117(p0: NativePtr): Int
private external fun kniBridge118(p0: NativePtr): Int
private external fun kniBridge119(p0: NativePtr): NativePtr
private external fun kniBridge120(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge121(p0: NativePtr): Int
private external fun kniBridge122(p0: NativePtr, p1: Int): NativePtr
private external fun kniBridge123(p0: NativePtr): Int
private external fun kniBridge124(p0: NativePtr, p1: Int): NativePtr
private external fun kniBridge125(p0: NativePtr): Int
private external fun kniBridge126(p0: NativePtr, p1: Int): NativePtr
private external fun kniBridge127(p0: NativePtr): Int
private external fun kniBridge128(p0: NativePtr, p1: Int): NativePtr
private external fun kniBridge129(p0: NativePtr, p1: Int): NativePtr
private external fun kniBridge130(p0: NativePtr): Int
private external fun kniBridge131(p0: NativePtr): NativePtr
private external fun kniBridge132(p0: NativePtr): NativePtr
private external fun kniBridge133(p0: NativePtr): NativePtr
private external fun kniBridge134(p0: NativePtr): NativePtr
private external fun kniBridge135(p0: NativePtr): NativePtr
private external fun kniBridge136(p0: NativePtr): NativePtr
private external fun kniBridge137(): NativePtr
private external fun kniBridge138(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Int, p4: NativePtr, p5: Int): NativePtr
private external fun kniBridge139(p0: NativePtr): NativePtr
private external fun kniBridge140(p0: NativePtr): Int
private external fun kniBridge141(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge142(p0: NativePtr, p1: NativePtr, p2: Long): Unit
private external fun kniBridge143(p0: NativePtr): Unit
private external fun kniBridge144(p0: NativePtr): NativePtr
private external fun kniBridge145(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge146(p0: NativePtr): Int
private external fun kniBridge147(p0: NativePtr): Int
private external fun kniBridge148(p0: NativePtr): Int
private external fun kniBridge149(p0: NativePtr): NativePtr
private external fun kniBridge150(p0: NativePtr): NativePtr
private external fun kniBridge151(p0: NativePtr): NativePtr
private external fun kniBridge152(p0: NativePtr): NativePtr
private external fun kniBridge153(p0: NativePtr): NativePtr
private external fun kniBridge154(p0: NativePtr): NativePtr
private external fun kniBridge155(p0: NativePtr): NativePtr
private external fun kniBridge156(p0: NativePtr): NativePtr
private external fun kniBridge157(p0: NativePtr): NativePtr
private external fun kniBridge158(p0: NativePtr): NativePtr
private external fun kniBridge159(p0: NativePtr): NativePtr
private external fun kniBridge160(p0: NativePtr): NativePtr
private external fun kniBridge161(p0: NativePtr): NativePtr
private external fun kniBridge162(p0: NativePtr): NativePtr
private external fun kniBridge163(p0: NativePtr): NativePtr
private external fun kniBridge164(p0: NativePtr): NativePtr
private external fun kniBridge165(p0: NativePtr): NativePtr
private external fun kniBridge166(p0: NativePtr): NativePtr
private external fun kniBridge167(p0: NativePtr): NativePtr
private external fun kniBridge168(p0: NativePtr): NativePtr
private external fun kniBridge169(p0: NativePtr): NativePtr
private external fun kniBridge170(p0: NativePtr): NativePtr
private external fun kniBridge171(p0: NativePtr): NativePtr
private external fun kniBridge172(p0: NativePtr): NativePtr
private external fun kniBridge173(p0: NativePtr): NativePtr
private external fun kniBridge174(p0: NativePtr): NativePtr
private external fun kniBridge175(p0: NativePtr): NativePtr
private external fun kniBridge176(p0: NativePtr): NativePtr
private external fun kniBridge177(p0: NativePtr): NativePtr
private external fun kniBridge178(p0: NativePtr): NativePtr
private external fun kniBridge179(p0: NativePtr): NativePtr
private external fun kniBridge180(p0: NativePtr): NativePtr
private external fun kniBridge181(p0: NativePtr): NativePtr
private external fun kniBridge182(p0: NativePtr): NativePtr
private external fun kniBridge183(p0: NativePtr): NativePtr
private external fun kniBridge184(p0: NativePtr): NativePtr
private external fun kniBridge185(p0: NativePtr): NativePtr
private external fun kniBridge186(p0: NativePtr): NativePtr
private external fun kniBridge187(p0: NativePtr): NativePtr
private external fun kniBridge188(p0: NativePtr): NativePtr
private external fun kniBridge189(p0: NativePtr): NativePtr
private external fun kniBridge190(p0: NativePtr): NativePtr
private external fun kniBridge191(p0: NativePtr): NativePtr
private external fun kniBridge192(p0: NativePtr): NativePtr
private external fun kniBridge193(p0: NativePtr): NativePtr
private external fun kniBridge194(p0: NativePtr): NativePtr
private external fun kniBridge195(p0: NativePtr): NativePtr
private external fun kniBridge196(p0: NativePtr): NativePtr
private external fun kniBridge197(p0: NativePtr): NativePtr
private external fun kniBridge198(p0: NativePtr): NativePtr
private external fun kniBridge199(p0: NativePtr): NativePtr
private external fun kniBridge200(p0: NativePtr): NativePtr
private external fun kniBridge201(p0: NativePtr): NativePtr
private external fun kniBridge202(p0: NativePtr): NativePtr
private external fun kniBridge203(p0: NativePtr): NativePtr
private external fun kniBridge204(p0: NativePtr): NativePtr
private external fun kniBridge205(p0: NativePtr): NativePtr
private external fun kniBridge206(p0: NativePtr): NativePtr
private external fun kniBridge207(p0: NativePtr): NativePtr
private external fun kniBridge208(p0: NativePtr): NativePtr
private external fun kniBridge209(p0: NativePtr): NativePtr
private external fun kniBridge210(p0: NativePtr): NativePtr
private external fun kniBridge211(p0: NativePtr): NativePtr
private external fun kniBridge212(p0: NativePtr): NativePtr
private external fun kniBridge213(p0: NativePtr): NativePtr
private external fun kniBridge214(p0: NativePtr): NativePtr
private external fun kniBridge215(p0: NativePtr): NativePtr
private external fun kniBridge216(p0: NativePtr): NativePtr
private external fun kniBridge217(p0: NativePtr): NativePtr
private external fun kniBridge218(p0: NativePtr): NativePtr
private external fun kniBridge219(p0: NativePtr): NativePtr
private external fun kniBridge220(p0: NativePtr): NativePtr
private external fun kniBridge221(p0: NativePtr): NativePtr
private external fun kniBridge222(p0: NativePtr): NativePtr
private external fun kniBridge223(p0: NativePtr): NativePtr
private external fun kniBridge224(p0: NativePtr): NativePtr
private external fun kniBridge225(p0: NativePtr): NativePtr
private external fun kniBridge226(p0: NativePtr): NativePtr
private external fun kniBridge227(p0: NativePtr): NativePtr
private external fun kniBridge228(p0: NativePtr): NativePtr
private external fun kniBridge229(p0: NativePtr): NativePtr
private external fun kniBridge230(p0: NativePtr): NativePtr
private external fun kniBridge231(p0: NativePtr): NativePtr
private external fun kniBridge232(p0: NativePtr): NativePtr
private external fun kniBridge233(p0: NativePtr): NativePtr
private external fun kniBridge234(p0: NativePtr): NativePtr
private external fun kniBridge235(p0: NativePtr): NativePtr
private external fun kniBridge236(p0: NativePtr): NativePtr
private external fun kniBridge237(p0: NativePtr): NativePtr
private external fun kniBridge238(p0: NativePtr): NativePtr
private external fun kniBridge239(p0: NativePtr): NativePtr
private external fun kniBridge240(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge241(p0: NativePtr): NativePtr
private external fun kniBridge242(p0: NativePtr): NativePtr
private external fun kniBridge243(p0: NativePtr): NativePtr
private external fun kniBridge244(p0: NativePtr): NativePtr
private external fun kniBridge245(p0: NativePtr, p1: Int): NativePtr
private external fun kniBridge246(p0: NativePtr, p1: Int): NativePtr
private external fun kniBridge247(p0: NativePtr, p1: Int, p2: NativePtr): Unit
private external fun kniBridge248(p0: NativePtr): Int
private external fun kniBridge249(p0: NativePtr): NativePtr
private external fun kniBridge250(p0: NativePtr): NativePtr
private external fun kniBridge251(p0: NativePtr): NativePtr
private external fun kniBridge252(p0: NativePtr): NativePtr
private external fun kniBridge253(p0: NativePtr): Int
private external fun kniBridge254(p0: NativePtr): NativePtr
private external fun kniBridge255(p0: NativePtr, p1: Long, p2: Int): NativePtr
private external fun kniBridge256(p0: NativePtr, p1: Int, p2: NativePtr): NativePtr
private external fun kniBridge257(p0: NativePtr, p1: NativePtr, p2: Byte): NativePtr
private external fun kniBridge258(p0: NativePtr, p1: NativePtr, p2: Int, p3: Byte): NativePtr
private external fun kniBridge259(p0: NativePtr, p1: Double): NativePtr
private external fun kniBridge260(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge261(p0: NativePtr, p1: NativePtr, p2: Int): NativePtr
private external fun kniBridge262(p0: NativePtr): Long
private external fun kniBridge263(p0: NativePtr): Long
private external fun kniBridge264(p0: NativePtr, p1: NativePtr): Double
private external fun kniBridge265(p0: NativePtr, p1: NativePtr, p2: Int, p3: Int): NativePtr
private external fun kniBridge266(p0: NativePtr): Int
private external fun kniBridge267(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge268(p0: NativePtr, p1: NativePtr, p2: Int, p3: Int): NativePtr
private external fun kniBridge269(p0: NativePtr, p1: NativePtr, p2: Int): NativePtr
private external fun kniBridge270(p0: NativePtr, p1: NativePtr, p2: Int): NativePtr
private external fun kniBridge271(p0: NativePtr, p1: Int): NativePtr
private external fun kniBridge272(p0: NativePtr, p1: Int): NativePtr
private external fun kniBridge273(p0: NativePtr, p1: Int): NativePtr
private external fun kniBridge274(p0: NativePtr): Int
private external fun kniBridge275(p0: NativePtr): NativePtr
private external fun kniBridge276(p0: NativePtr): NativePtr
private external fun kniBridge277(p0: NativePtr): NativePtr
private external fun kniBridge278(p0: NativePtr): NativePtr
private external fun kniBridge279(p0: NativePtr): NativePtr
private external fun kniBridge280(p0: NativePtr): NativePtr
private external fun kniBridge281(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge282(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge283(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge284(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge285(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge286(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge287(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge288(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge289(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge290(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge291(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge292(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge293(p0: Int, p1: NativePtr, p2: NativePtr): NativePtr
private external fun kniBridge294(p0: Int, p1: NativePtr, p2: NativePtr): NativePtr
private external fun kniBridge295(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge296(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge297(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge298(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Int): NativePtr
private external fun kniBridge299(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Int): NativePtr
private external fun kniBridge300(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge301(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge302(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge303(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge304(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge305(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge306(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge307(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge308(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge309(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge310(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge311(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge312(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge313(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge314(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge315(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge316(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge317(p0: NativePtr, p1: NativePtr, p2: Int): NativePtr
private external fun kniBridge318(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge319(p0: NativePtr, p1: NativePtr, p2: NativePtr): NativePtr
private external fun kniBridge320(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge321(p0: NativePtr, p1: NativePtr, p2: NativePtr): NativePtr
private external fun kniBridge322(p0: NativePtr, p1: NativePtr, p2: NativePtr): NativePtr
private external fun kniBridge323(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge324(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Int, p4: Int): NativePtr
private external fun kniBridge325(p0: NativePtr): NativePtr
private external fun kniBridge326(p0: NativePtr): Int
private external fun kniBridge327(p0: NativePtr): Int
private external fun kniBridge328(p0: NativePtr, p1: Int): Unit
private external fun kniBridge329(p0: NativePtr): NativePtr
private external fun kniBridge330(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge331(p0: NativePtr): Int
private external fun kniBridge332(p0: NativePtr, p1: Int): Unit
private external fun kniBridge333(p0: NativePtr): Int
private external fun kniBridge334(p0: NativePtr, p1: Int): Unit
private external fun kniBridge335(p0: NativePtr): Int
private external fun kniBridge336(p0: NativePtr, p1: Int): Unit
private external fun kniBridge337(p0: NativePtr): NativePtr
private external fun kniBridge338(p0: NativePtr): Int
private external fun kniBridge339(p0: NativePtr, p1: Int): Unit
private external fun kniBridge340(p0: NativePtr): Int
private external fun kniBridge341(p0: NativePtr, p1: Int): Unit
private external fun kniBridge342(p0: NativePtr, p1: Int, p2: NativePtr): Unit
private external fun kniBridge343(p0: NativePtr, p1: Int): Unit
private external fun kniBridge344(p0: NativePtr): Unit
private external fun kniBridge345(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge346(p0: NativePtr): Unit
private external fun kniBridge347(p0: NativePtr, p1: Int): Int
private external fun kniBridge348(p0: NativePtr, p1: Int): NativePtr
private external fun kniBridge349(p0: NativePtr, p1: NativePtr, p2: NativePtr): NativePtr
private external fun kniBridge350(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Int): NativePtr
private external fun kniBridge351(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge352(p0: NativePtr): NativePtr
private external fun kniBridge353(p0: NativePtr): NativePtr
private external fun kniBridge354(p0: NativePtr): NativePtr
private external fun kniBridge355(p0: NativePtr): NativePtr
private external fun kniBridge356(p0: NativePtr): Unit
private external fun kniBridge357(p0: NativePtr): NativePtr
private external fun kniBridge358(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge359(p0: NativePtr): Int
private external fun kniBridge360(p0: NativePtr, p1: Int): Unit
private external fun kniBridge361(p0: NativePtr): Int
private external fun kniBridge362(p0: NativePtr, p1: Int): Unit
private external fun kniBridge363(p0: NativePtr): Int
private external fun kniBridge364(p0: NativePtr, p1: Int): Unit
private external fun kniBridge365(p0: NativePtr): Int
private external fun kniBridge366(p0: NativePtr, p1: Int): Unit
private external fun kniBridge367(p0: NativePtr, p1: NativePtr, p2: Int, p3: NativePtr, p4: NativePtr): NativePtr
private external fun kniBridge368(p0: NativePtr, p1: NativePtr, p2: Long): NativePtr
private external fun kniBridge369(p0: NativePtr): NativePtr
private external fun kniBridge370(p0: NativePtr): NativePtr
private external fun kniBridge371(p0: NativePtr): NativePtr
private external fun kniBridge372(p0: NativePtr): NativePtr
private external fun kniBridge373(p0: NativePtr): NativePtr
private external fun kniBridge374(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge375(p0: NativePtr): Unit
private external fun kniBridge376(p0: NativePtr): Int
private external fun kniBridge377(p0: NativePtr): NativePtr
private external fun kniBridge378(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge379(p0: NativePtr, p1: Long): Int
private external fun kniBridge380(p0: NativePtr): Int
private external fun kniBridge381(p0: NativePtr, p1: Int, p2: NativePtr, p3: Long): NativePtr
private external fun kniBridge382(p0: NativePtr, p1: Int, p2: NativePtr, p3: Long): NativePtr
private external fun kniBridge383(p0: Int, p1: NativePtr): NativePtr
private external fun kniBridge384(p0: Int, p1: NativePtr, p2: Long, p3: NativePtr): NativePtr
private external fun kniBridge385(p0: NativePtr, p1: Int, p2: NativePtr, p3: Long, p4: NativePtr): NativePtr
private external fun kniBridge386(p0: Int): Int
private external fun kniBridge387(p0: NativePtr): Int
private external fun kniBridge388(p0: NativePtr, p1: Int): Unit
private external fun kniBridge389(p0: NativePtr): NativePtr
private external fun kniBridge390(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge391(p0: NativePtr, p1: Int, p2: NativePtr): Unit
private external fun kniBridge392(p0: NativePtr, p1: Int): Int
private external fun kniBridge393(p0: NativePtr, p1: Int, p2: NativePtr): Unit
private external fun kniBridge394(p0: NativePtr, p1: Int, p2: Int): NativePtr
private external fun kniBridge395(p0: NativePtr, p1: Int, p2: NativePtr, p3: Int): NativePtr
private external fun kniBridge396(p0: NativePtr, p1: Int, p2: Int): Unit
private external fun kniBridge397(p0: NativePtr, p1: Int, p2: NativePtr, p3: Int): Unit
private external fun kniBridge398(p0: NativePtr, p1: NativePtr, p2: NativePtr): Unit
private external fun kniBridge399(p0: NativePtr): Int
private external fun kniBridge400(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge401(p0: NativePtr, p1: Int): NativePtr
private external fun kniBridge402(p0: NativePtr): NativePtr
private external fun kniBridge403(p0: NativePtr): NativePtr
private external fun kniBridge404(p0: NativePtr): NativePtr
private external fun kniBridge405(p0: NativePtr): NativePtr
private external fun kniBridge406(p0: NativePtr): NativePtr
private external fun kniBridge407(p0: NativePtr, p1: Int): Unit
private external fun kniBridge408(p0: NativePtr, p1: NativePtr, p2: Long, p3: NativePtr, p4: Int, p5: NativePtr): NativePtr
private external fun kniBridge409(p0: NativePtr, p1: NativePtr, p2: Long): NativePtr
private external fun kniBridge410(p0: NativePtr): NativePtr
private external fun kniBridge411(p0: NativePtr): NativePtr
private external fun kniBridge412(p0: NativePtr): NativePtr
private external fun kniBridge413(p0: NativePtr): NativePtr
private external fun kniBridge414(p0: NativePtr): NativePtr
private external fun kniBridge415(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge416(p0: NativePtr): Unit
private external fun kniBridge417(p0: NativePtr): Unit
private external fun kniBridge418(p0: NativePtr, p1: NativePtr, p2: Long): NativePtr
private external fun kniBridge419(p0: NativePtr, p1: NativePtr, p2: Long): NativePtr
private external fun kniBridge420(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge421(p0: NativePtr): NativePtr
private external fun kniBridge422(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge423(p0: NativePtr): Int
private external fun kniBridge424(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge425(p0: NativePtr, p1: NativePtr, p2: Int): NativePtr
private external fun kniBridge426(p0: NativePtr, p1: NativePtr, p2: Int): NativePtr
private external fun kniBridge427(p0: NativePtr): NativePtr
private external fun kniBridge428(p0: NativePtr): Int
private external fun kniBridge429(p0: NativePtr): NativePtr
private external fun kniBridge430(p0: NativePtr): NativePtr
private external fun kniBridge431(p0: NativePtr): NativePtr
private external fun kniBridge432(p0: NativePtr): NativePtr
private external fun kniBridge433(p0: NativePtr): Int
private external fun kniBridge434(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge435(p0: NativePtr): NativePtr
private external fun kniBridge436(p0: NativePtr): NativePtr
private external fun kniBridge437(p0: NativePtr): NativePtr
private external fun kniBridge438(p0: NativePtr): NativePtr
private external fun kniBridge439(p0: NativePtr): NativePtr
private external fun kniBridge440(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge441(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge442(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge443(p0: NativePtr, p1: NativePtr, p2: NativePtr): NativePtr
private external fun kniBridge444(p0: NativePtr, p1: NativePtr, p2: NativePtr): NativePtr
private external fun kniBridge445(p0: NativePtr): Unit
private external fun kniBridge446(p0: NativePtr): Unit
private external fun kniBridge447(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge448(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge449(p0: NativePtr): NativePtr
private external fun kniBridge450(p0: NativePtr): NativePtr
private external fun kniBridge451(p0: NativePtr): Int
private external fun kniBridge452(p0: NativePtr, p1: Int): NativePtr
private external fun kniBridge453(p0: NativePtr, p1: Int, p2: NativePtr): Unit
private external fun kniBridge454(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge455(p0: NativePtr): NativePtr
private external fun kniBridge456(p0: NativePtr): NativePtr
private external fun kniBridge457(p0: NativePtr): NativePtr
private external fun kniBridge458(p0: NativePtr): Unit
private external fun kniBridge459(p0: NativePtr): Unit
private external fun kniBridge460(p0: NativePtr): Unit
private external fun kniBridge461(p0: NativePtr): Int
private external fun kniBridge462(p0: NativePtr): Int
private external fun kniBridge463(p0: NativePtr): Int
private external fun kniBridge464(p0: NativePtr): NativePtr
private external fun kniBridge465(p0: NativePtr): NativePtr
private external fun kniBridge466(p0: NativePtr): Int
private external fun kniBridge467(p0: NativePtr, p1: Int): Unit
private external fun kniBridge468(p0: NativePtr): Int
private external fun kniBridge469(p0: NativePtr, p1: Int, p2: Int): Unit
private external fun kniBridge470(p0: NativePtr, p1: Int, p2: NativePtr): Unit
private external fun kniBridge471(p0: NativePtr, p1: Int): Int
private external fun kniBridge472(p0: NativePtr, p1: Int, p2: NativePtr): Unit
private external fun kniBridge473(p0: NativePtr, p1: Int, p2: Int): NativePtr
private external fun kniBridge474(p0: NativePtr, p1: Int, p2: NativePtr, p3: Int): NativePtr
private external fun kniBridge475(p0: NativePtr, p1: Int, p2: Int): Unit
private external fun kniBridge476(p0: NativePtr, p1: Int, p2: NativePtr, p3: Int): Unit
private external fun kniBridge477(p0: NativePtr): NativePtr
private external fun kniBridge478(p0: NativePtr): NativePtr
private external fun kniBridge479(p0: NativePtr): Int
private external fun kniBridge480(p0: NativePtr, p1: Int): Unit
private external fun kniBridge481(p0: NativePtr): NativePtr
private external fun kniBridge482(p0: NativePtr): NativePtr
private external fun kniBridge483(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge484(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge485(p0: NativePtr): Int
private external fun kniBridge486(p0: NativePtr, p1: Int): NativePtr
private external fun kniBridge487(p0: NativePtr, p1: Int, p2: NativePtr): Unit
private external fun kniBridge488(p0: NativePtr): Int
private external fun kniBridge489(p0: NativePtr): NativePtr
private external fun kniBridge490(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge491(p0: NativePtr): NativePtr
private external fun kniBridge492(p0: NativePtr): NativePtr
private external fun kniBridge493(p0: NativePtr): Int
private external fun kniBridge494(p0: NativePtr, p1: Int): Unit
private external fun kniBridge495(p0: NativePtr): NativePtr
private external fun kniBridge496(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Int): Unit
private external fun kniBridge497(p0: NativePtr): Int
private external fun kniBridge498(p0: NativePtr, p1: Int): NativePtr
private external fun kniBridge499(p0: NativePtr, p1: Int): NativePtr
private external fun kniBridge500(p0: NativePtr): Int
private external fun kniBridge501(p0: NativePtr): NativePtr
private external fun kniBridge502(p0: NativePtr): NativePtr
private external fun kniBridge503(p0: NativePtr, p1: NativePtr, p2: NativePtr): Unit
private external fun kniBridge504(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge505(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge506(p0: NativePtr): NativePtr
private external fun kniBridge507(p0: NativePtr): Unit
private external fun kniBridge508(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge509(p0: NativePtr, p1: NativePtr, p2: NativePtr): Unit
private external fun kniBridge510(p0: NativePtr): Unit
private external fun kniBridge511(p0: NativePtr): NativePtr
private external fun kniBridge512(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge513(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge514(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge515(p0: NativePtr): NativePtr
private external fun kniBridge516(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge517(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge518(p0: NativePtr): NativePtr
private external fun kniBridge519(p0: NativePtr): NativePtr
private external fun kniBridge520(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge521(p0: NativePtr, p1: NativePtr, p2: Int): NativePtr
private external fun kniBridge522(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge523(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge524(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Int): NativePtr
private external fun kniBridge525(p0: NativePtr, p1: NativePtr, p2: Int): NativePtr
private external fun kniBridge526(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr, p4: Int, p5: NativePtr, p6: NativePtr, p7: NativePtr): NativePtr
private external fun kniBridge527(p0: NativePtr): NativePtr
private external fun kniBridge528(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge529(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Int, p4: NativePtr): NativePtr
private external fun kniBridge530(p0: NativePtr, p1: NativePtr, p2: NativePtr): NativePtr
private external fun kniBridge531(p0: NativePtr, p1: NativePtr, p2: NativePtr): NativePtr
private external fun kniBridge532(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Int, p4: NativePtr): NativePtr
private external fun kniBridge533(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Int, p4: NativePtr): NativePtr
private external fun kniBridge534(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Int, p4: NativePtr): NativePtr
private external fun kniBridge535(p0: NativePtr, p1: NativePtr, p2: NativePtr): Unit
private external fun kniBridge536(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge537(p0: NativePtr): Int
private external fun kniBridge538(p0: NativePtr, p1: Int): NativePtr
private external fun kniBridge539(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge540(p0: NativePtr): Int
private external fun kniBridge541(p0: NativePtr, p1: Int): Unit
private external fun kniBridge542(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge543(p0: NativePtr): Int
private external fun kniBridge544(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge545(p0: NativePtr, p1: Int): NativePtr
private external fun kniBridge546(p0: NativePtr, p1: Int, p2: NativePtr): Unit
private external fun kniBridge547(p0: NativePtr): NativePtr
private external fun kniBridge548(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge549(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge550(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge551(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge552(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge553(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge554(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge555(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge556(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge557(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge558(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge559(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge560(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge561(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge562(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge563(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge564(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge565(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge566(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge567(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge568(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge569(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge570(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge571(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge572(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge573(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge574(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge575(p0: NativePtr, p1: Int, p2: NativePtr, p3: NativePtr, p4: NativePtr): NativePtr
private external fun kniBridge576(p0: NativePtr, p1: NativePtr, p2: NativePtr): NativePtr
private external fun kniBridge577(p0: NativePtr, p1: NativePtr, p2: NativePtr): NativePtr
private external fun kniBridge578(p0: NativePtr, p1: NativePtr, p2: NativePtr): NativePtr
private external fun kniBridge579(p0: NativePtr, p1: NativePtr, p2: NativePtr): NativePtr
private external fun kniBridge580(p0: NativePtr, p1: NativePtr, p2: NativePtr): NativePtr
private external fun kniBridge581(p0: NativePtr, p1: NativePtr, p2: NativePtr): NativePtr
private external fun kniBridge582(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge583(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr, p4: Int): NativePtr
private external fun kniBridge584(p0: NativePtr, p1: NativePtr, p2: Int, p3: NativePtr, p4: Int, p5: NativePtr): NativePtr
private external fun kniBridge585(p0: NativePtr, p1: NativePtr, p2: Int, p3: NativePtr, p4: Int, p5: NativePtr): NativePtr
private external fun kniBridge586(p0: NativePtr, p1: NativePtr, p2: NativePtr): NativePtr
private external fun kniBridge587(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge588(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge589(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge590(p0: NativePtr, p1: NativePtr, p2: NativePtr): NativePtr
private external fun kniBridge591(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr, p4: Int, p5: NativePtr): NativePtr
private external fun kniBridge592(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr, p4: Int, p5: NativePtr): NativePtr
private external fun kniBridge593(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Int, p4: NativePtr): NativePtr
private external fun kniBridge594(p0: NativePtr, p1: NativePtr, p2: NativePtr): NativePtr
private external fun kniBridge595(p0: NativePtr, p1: NativePtr, p2: NativePtr): NativePtr
private external fun kniBridge596(p0: NativePtr): Int
private external fun kniBridge597(p0: NativePtr, p1: Int): Unit
private external fun kniBridge598(p0: NativePtr): Int
private external fun kniBridge599(p0: NativePtr, p1: Int): Unit
private external fun kniBridge600(p0: NativePtr): Int
private external fun kniBridge601(p0: NativePtr, p1: Int): Unit
private external fun kniBridge602(p0: NativePtr): Int
private external fun kniBridge603(p0: NativePtr, p1: Int): Unit
private external fun kniBridge604(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge605(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge606(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge607(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge608(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge609(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge610(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge611(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge612(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge613(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge614(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge615(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge616(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge617(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge618(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge619(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge620(p0: NativePtr, p1: Int, p2: NativePtr, p3: NativePtr, p4: NativePtr): NativePtr
private external fun kniBridge621(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge622(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Int, p4: NativePtr): NativePtr
private external fun kniBridge623(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge624(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge625(p0: NativePtr, p1: Int, p2: NativePtr, p3: Int): Int
private external fun kniBridge626(p0: NativePtr, p1: Int, p2: NativePtr, p3: NativePtr, p4: NativePtr): NativePtr
private external fun kniBridge627(p0: NativePtr, p1: Int, p2: NativePtr, p3: NativePtr, p4: NativePtr): NativePtr
private external fun kniBridge628(p0: NativePtr, p1: NativePtr, p2: NativePtr): NativePtr
private external fun kniBridge629(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr, p4: Int, p5: NativePtr): NativePtr
private external fun kniBridge630(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr, p4: NativePtr): NativePtr
private external fun kniBridge631(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge632(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge633(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr, p4: NativePtr): NativePtr
private external fun kniBridge634(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr, p4: NativePtr): NativePtr
private external fun kniBridge635(p0: NativePtr, p1: NativePtr, p2: Int, p3: NativePtr): NativePtr
private external fun kniBridge636(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Int, p4: NativePtr): NativePtr
private external fun kniBridge637(p0: NativePtr, p1: NativePtr, p2: NativePtr): NativePtr
private external fun kniBridge638(p0: NativePtr, p1: NativePtr, p2: NativePtr): NativePtr
private external fun kniBridge639(p0: NativePtr, p1: NativePtr, p2: NativePtr): NativePtr
private external fun kniBridge640(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr, p4: NativePtr): NativePtr
private external fun kniBridge641(p0: NativePtr, p1: Int, p2: Int, p3: NativePtr): NativePtr
private external fun kniBridge642(p0: NativePtr, p1: Int, p2: NativePtr, p3: NativePtr, p4: Int, p5: Int): NativePtr
private external fun kniBridge643(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr, p4: Int, p5: Int, p6: Int): NativePtr
private external fun kniBridge644(p0: NativePtr): Int
private external fun kniBridge645(): Int
private external fun kniBridge646(p0: NativePtr, p1: Int): Int
private external fun kniBridge647(p0: NativePtr): Int
private external fun kniBridge648(p0: NativePtr, p1: Int): Unit
private external fun kniBridge649(p0: NativePtr): Int
private external fun kniBridge650(p0: NativePtr, p1: Int): Unit
private external fun kniBridge651(p0: NativePtr): Int
private external fun kniBridge652(p0: NativePtr, p1: Int): Unit
private external fun kniBridge653(p0: NativePtr): NativePtr
private external fun kniBridge654(p0: NativePtr): Unit
private external fun kniBridge655(p0: NativePtr, p1: NativePtr, p2: NativePtr): Int
private external fun kniBridge656(p0: NativePtr, p1: NativePtr): Int
private external fun kniBridge657(p0: NativePtr, p1: Long, p2: NativePtr, p3: Int): NativePtr
private external fun kniBridge658(p0: NativePtr, p1: Long, p2: NativePtr): NativePtr
private external fun kniBridge659(p0: NativePtr): NativePtr
private external fun kniBridge660(p0: NativePtr): Long
private external fun kniBridge661(p0: NativePtr): Unit
private external fun kniBridge662(): NativePtr
private external fun kniBridge663(): NativePtr
private external fun kniBridge664(p0: NativePtr): NativePtr
private external fun kniBridge665(p0: NativePtr): NativePtr
private external fun kniBridge666(p0: NativePtr, p1: NativePtr): Int
private external fun kniBridge667(p0: NativePtr): Int
private external fun kniBridge668(p0: NativePtr, p1: NativePtr): Int
private external fun kniBridge669(p0: NativePtr): Int
private external fun kniBridge670(p0: NativePtr): Unit
private external fun kniBridge671(): Int
private external fun kniBridge672(): Unit
private external fun kniBridge673(): Int
private external fun kniBridge674(p0: NativePtr): NativePtr
private external fun kniBridge675(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge676(p0: NativePtr): NativePtr
private external fun kniBridge677(p0: NativePtr): Unit
private external fun kniBridge678(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge679(p0: NativePtr): NativePtr
private external fun kniBridge680(p0: NativePtr): Int
private external fun kniBridge681(p0: NativePtr): Int
private external fun kniBridge682(p0: NativePtr, p1: Int): Int
private external fun kniBridge683(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge684(p0: NativePtr, p1: NativePtr, p2: Int): NativePtr
private external fun kniBridge685(p0: NativePtr, p1: NativePtr): Long
private external fun kniBridge686(p0: NativePtr, p1: NativePtr): Long
private external fun kniBridge687(p0: NativePtr, p1: NativePtr): Long
private external fun kniBridge688(p0: NativePtr, p1: NativePtr): Int
private external fun kniBridge689(p0: NativePtr, p1: NativePtr): Int
private external fun kniBridge690(p0: NativePtr, p1: NativePtr): Int
private external fun kniBridge691(p0: NativePtr, p1: NativePtr): Int
private external fun kniBridge692(p0: NativePtr, p1: NativePtr, p2: Long): Int
private external fun kniBridge693(p0: NativePtr, p1: NativePtr, p2: Int): Long
private external fun kniBridge694(p0: NativePtr, p1: Int, p2: NativePtr): Int
private external fun kniBridge695(p0: NativePtr, p1: Int): Int
private external fun kniBridge696(p0: NativePtr): Unit
private external fun kniBridge697(p0: NativePtr): Unit
private external fun kniBridge698(p0: NativePtr, p1: NativePtr): Int
private external fun kniBridge699(p0: NativePtr, p1: Int, p2: Int, p3: Int): Int
private external fun kniBridge700(p0: NativePtr, p1: Int): Int
private external fun kniBridge701(p0: NativePtr): NativePtr
private external fun kniBridge702(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): Int
private external fun kniBridge703(p0: NativePtr, p1: NativePtr, p2: NativePtr): Int
private external fun kniBridge704(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): Int
private external fun kniBridge705(p0: NativePtr, p1: NativePtr, p2: NativePtr): Int
private external fun kniBridge706(p0: NativePtr): NativePtr
private external fun kniBridge707(p0: NativePtr): Unit
private external fun kniBridge708(p0: NativePtr): NativePtr
private external fun kniBridge709(p0: NativePtr): Unit
private external fun kniBridge710(): NativePtr
private external fun kniBridge711(p0: NativePtr): NativePtr
private external fun kniBridge712(): NativePtr
private external fun kniBridge713(p0: NativePtr): NativePtr
private external fun kniBridge714(p0: NativePtr): NativePtr
private external fun kniBridge715(p0: NativePtr, p1: NativePtr, p2: NativePtr): Int
private external fun kniBridge716(p0: NativePtr): NativePtr
private external fun kniBridge717(p0: NativePtr): NativePtr
private external fun kniBridge718(p0: NativePtr): Int
private external fun kniBridge719(p0: NativePtr): Int
private external fun kniBridge720(p0: NativePtr): Int
private external fun kniBridge721(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr, p4: Int, p5: Int, p6: Int): NativePtr
private external fun kniBridge722(p0: NativePtr): Unit
private external fun kniBridge723(p0: NativePtr): NativePtr
private external fun kniBridge724(p0: NativePtr): NativePtr
private external fun kniBridge725(p0: NativePtr): NativePtr
private external fun kniBridge726(p0: NativePtr): NativePtr
private external fun kniBridge727(p0: NativePtr): NativePtr
private external fun kniBridge728(p0: NativePtr, p1: Int): Unit
private external fun kniBridge729(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Int, p4: NativePtr): Int
private external fun kniBridge730(p0: NativePtr, p1: NativePtr, p2: Int, p3: NativePtr, p4: NativePtr): Int
private external fun kniBridge731(): NativePtr
private external fun kniBridge732(p0: NativePtr): NativePtr
private external fun kniBridge733(): NativePtr
private external fun kniBridge734(): NativePtr
private external fun kniBridge735(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge736(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): NativePtr
private external fun kniBridge737(): NativePtr
private external fun kniBridge738(p0: NativePtr, p1: Int): Unit
private external fun kniBridge739(p0: NativePtr, p1: Int): Unit
private external fun kniBridge740(p0: NativePtr, p1: Int): Unit
private external fun kniBridge741(p0: NativePtr, p1: Int): Unit
private external fun kniBridge742(p0: NativePtr, p1: Int): Unit
private external fun kniBridge743(p0: NativePtr, p1: Int): Unit
private external fun kniBridge744(p0: NativePtr, p1: Int): Unit
private external fun kniBridge745(p0: NativePtr, p1: Int): Unit
private external fun kniBridge746(p0: NativePtr, p1: Int): Unit
private external fun kniBridge747(p0: NativePtr, p1: Int): Unit
private external fun kniBridge748(p0: NativePtr, p1: Int): Unit
private external fun kniBridge749(p0: NativePtr, p1: Int): Unit
private external fun kniBridge750(p0: NativePtr, p1: Int): Unit
private external fun kniBridge751(p0: NativePtr): Unit
private external fun kniBridge752(p0: NativePtr, p1: NativePtr): Int
private external fun kniBridge753(): Int
private external fun kniBridge754(p0: NativePtr): Int
private external fun kniBridge755(p0: NativePtr): Int
private external fun kniBridge756(p0: NativePtr): NativePtr
private external fun kniBridge757(p0: NativePtr): NativePtr
private external fun kniBridge758(p0: NativePtr): Unit
private external fun kniBridge759(p0: NativePtr): Unit
private external fun kniBridge760(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge761(p0: NativePtr, p1: Int, p2: NativePtr, p3: NativePtr, p4: Long, p5: Int, p6: NativePtr, p7: Long, p8: Int, p9: NativePtr, p10: Long, p11: Int, p12: Int, p13: Int, p14: Int, p15: NativePtr, p16: Long, p17: NativePtr, p18: Long): NativePtr
private external fun kniBridge762(p0: NativePtr, p1: NativePtr, p2: Long, p3: NativePtr, p4: Long): NativePtr
private external fun kniBridge763(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Long, p4: NativePtr, p5: Long, p6: NativePtr, p7: Long, p8: NativePtr, p9: Long): NativePtr
private external fun kniBridge764(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Long, p4: Int): NativePtr
private external fun kniBridge765(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Long, p4: NativePtr, p5: Long, p6: NativePtr, p7: Int, p8: NativePtr, p9: Int, p10: Int, p11: Int, p12: Int, p13: Int): NativePtr
private external fun kniBridge766(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Int, p4: Int): NativePtr
private external fun kniBridge767(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Int): NativePtr
private external fun kniBridge768(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr, p4: Int): NativePtr
private external fun kniBridge769(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr, p4: Int, p5: NativePtr, p6: Int): NativePtr
private external fun kniBridge770(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr, p4: Int, p5: NativePtr, p6: Int): NativePtr
private external fun kniBridge771(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr, p4: Int, p5: NativePtr, p6: Long, p7: NativePtr, p8: Int): NativePtr
private external fun kniBridge772(p0: NativePtr, p1: Int, p2: Int, p3: NativePtr, p4: NativePtr): NativePtr
private external fun kniBridge773(p0: NativePtr): Int
private external fun kniBridge774(p0: NativePtr): Int
private external fun kniBridge775(p0: NativePtr): NativePtr
private external fun kniBridge776(p0: NativePtr): NativePtr
private external fun kniBridge777(p0: NativePtr): NativePtr
private external fun kniBridge778(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge779(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge780(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge781(p0: NativePtr, p1: NativePtr, p2: Long): NativePtr
private external fun kniBridge782(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Int, p4: Int): NativePtr
private external fun kniBridge783(p0: NativePtr, p1: NativePtr, p2: Int, p3: Int, p4: NativePtr, p5: Long, p6: NativePtr, p7: Long): NativePtr
private external fun kniBridge784(p0: NativePtr, p1: NativePtr, p2: Int, p3: NativePtr): NativePtr
private external fun kniBridge785(p0: NativePtr, p1: NativePtr, p2: Long, p3: Long, p4: Int): NativePtr
private external fun kniBridge786(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Long, p4: NativePtr, p5: Int, p6: Long, p7: Int, p8: NativePtr, p9: Int, p10: NativePtr): NativePtr
private external fun kniBridge787(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Long, p4: NativePtr, p5: Int, p6: Long, p7: Int, p8: Int, p9: NativePtr, p10: Int, p11: Int, p12: NativePtr, p13: Long): NativePtr
private external fun kniBridge788(p0: NativePtr, p1: Long, p2: Int, p3: NativePtr, p4: NativePtr, p5: Int): NativePtr
private external fun kniBridge789(p0: NativePtr, p1: Long, p2: Int, p3: NativePtr, p4: NativePtr, p5: Int): NativePtr
private external fun kniBridge790(p0: NativePtr, p1: NativePtr, p2: Long): NativePtr
private external fun kniBridge791(p0: NativePtr, p1: NativePtr, p2: Long, p3: Long, p4: Int, p5: Int): NativePtr
private external fun kniBridge792(p0: NativePtr, p1: NativePtr, p2: Long, p3: Int, p4: Int, p5: NativePtr, p6: Long): NativePtr
private external fun kniBridge793(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Long, p4: NativePtr, p5: Int, p6: Long, p7: Int, p8: Int, p9: NativePtr, p10: NativePtr, p11: Int, p12: Int, p13: NativePtr, p14: NativePtr, p15: Long): NativePtr
private external fun kniBridge794(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Long, p4: NativePtr, p5: Int, p6: Long, p7: Int, p8: Long, p9: Int, p10: NativePtr): NativePtr
private external fun kniBridge795(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Long, p4: NativePtr, p5: Int, p6: NativePtr, p7: Int, p8: NativePtr, p9: Int): NativePtr
private external fun kniBridge796(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Long, p4: Int, p5: Int): NativePtr
private external fun kniBridge797(p0: NativePtr, p1: NativePtr, p2: Long, p3: NativePtr, p4: Int, p5: Long, p6: Int, p7: Long, p8: Int, p9: NativePtr, p10: NativePtr): NativePtr
private external fun kniBridge798(p0: NativePtr, p1: NativePtr, p2: Long, p3: NativePtr, p4: Int, p5: NativePtr, p6: Long, p7: NativePtr, p8: Long, p9: Int, p10: NativePtr): NativePtr
private external fun kniBridge799(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge800(p0: NativePtr, p1: Int, p2: NativePtr): NativePtr
private external fun kniBridge801(p0: NativePtr, p1: Int, p2: NativePtr): NativePtr
private external fun kniBridge802(p0: NativePtr): NativePtr
private external fun kniBridge803(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Long, p4: NativePtr, p5: Int, p6: NativePtr, p7: Int): NativePtr
private external fun kniBridge804(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Long, p4: Int, p5: Int): NativePtr
private external fun kniBridge805(p0: NativePtr, p1: Int, p2: NativePtr, p3: Long, p4: NativePtr, p5: NativePtr, p6: Int, p7: Int, p8: Long, p9: Int, p10: NativePtr, p11: Long): NativePtr
private external fun kniBridge806(p0: NativePtr, p1: Int, p2: NativePtr, p3: Long, p4: NativePtr, p5: NativePtr, p6: Int, p7: Int, p8: Long, p9: Int, p10: Int, p11: NativePtr, p12: Long): NativePtr
private external fun kniBridge807(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Long, p4: NativePtr, p5: Int, p6: Long, p7: Long, p8: Long, p9: Int, p10: NativePtr): NativePtr
private external fun kniBridge808(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Long, p4: NativePtr, p5: Int, p6: Long, p7: Int, p8: Long, p9: Int, p10: NativePtr, p11: NativePtr, p12: Int, p13: NativePtr, p14: NativePtr, p15: NativePtr, p16: Long): NativePtr
private external fun kniBridge809(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge810(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge811(p0: NativePtr): Long
private external fun kniBridge812(p0: NativePtr): Long
private external fun kniBridge813(p0: NativePtr): Int
private external fun kniBridge814(p0: NativePtr): Int
private external fun kniBridge815(p0: NativePtr): Int
private external fun kniBridge816(p0: NativePtr, p1: Long, p2: Long): NativePtr
private external fun kniBridge817(p0: NativePtr, p1: NativePtr, p2: Long): NativePtr
private external fun kniBridge818(p0: NativePtr, p1: NativePtr, p2: Long): NativePtr
private external fun kniBridge819(p0: NativePtr, p1: Long): NativePtr
private external fun kniBridge820(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Long, p4: NativePtr, p5: Long, p6: NativePtr, p7: Int, p8: NativePtr, p9: Int, p10: NativePtr, p11: NativePtr, p12: Int): NativePtr
private external fun kniBridge821(p0: NativePtr): NativePtr
private external fun kniBridge822(p0: NativePtr): NativePtr
private external fun kniBridge823(p0: NativePtr): NativePtr
private external fun kniBridge824(p0: NativePtr): NativePtr
private external fun kniBridge825(p0: NativePtr): Int
private external fun kniBridge826(p0: NativePtr, p1: NativePtr, p2: Long): NativePtr
private external fun kniBridge827(p0: NativePtr): Unit
private external fun kniBridge828(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge829(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Long, p4: NativePtr, p5: Long, p6: NativePtr, p7: Int, p8: NativePtr, p9: Int, p10: NativePtr, p11: Int): NativePtr
private external fun kniBridge830(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr, p4: NativePtr, p5: NativePtr): NativePtr
private external fun kniBridge831(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr, p4: NativePtr, p5: NativePtr): NativePtr
private external fun kniBridge832(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr, p4: NativePtr, p5: NativePtr): NativePtr
private external fun kniBridge833(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr, p4: NativePtr, p5: NativePtr): NativePtr
private external fun kniBridge834(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Long, p4: NativePtr, p5: Int, p6: NativePtr, p7: Int, p8: Int, p9: Int): NativePtr
private external fun kniBridge835(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Long, p4: Int, p5: NativePtr, p6: Int, p7: NativePtr, p8: Int, p9: Int): NativePtr
private external fun kniBridge836(p0: NativePtr): NativePtr
private external fun kniBridge837(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge838(p0: NativePtr): Int
private external fun kniBridge839(p0: NativePtr): NativePtr
private external fun kniBridge840(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge841(p0: NativePtr): Int
private external fun kniBridge842(p0: NativePtr): Unit
private external fun kniBridge843(p0: NativePtr, p1: Int, p2: NativePtr, p3: NativePtr, p4: NativePtr, p5: Int, p6: NativePtr, p7: Int): NativePtr
private external fun kniBridge844(p0: NativePtr, p1: NativePtr, p2: NativePtr): NativePtr
private external fun kniBridge845(p0: NativePtr, p1: NativePtr, p2: Long, p3: Long, p4: Int): NativePtr
private external fun kniBridge846(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr, p4: Int, p5: Long, p6: Long, p7: Int, p8: NativePtr, p9: NativePtr, p10: Long, p11: NativePtr): NativePtr
private external fun kniBridge847(p0: NativePtr, p1: Long, p2: Long, p3: NativePtr, p4: Long): NativePtr
private external fun kniBridge848(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge849(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge850(p0: NativePtr, p1: Int, p2: NativePtr, p3: NativePtr, p4: NativePtr, p5: Int): NativePtr
private external fun kniBridge851(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr, p4: Int, p5: Long, p6: Long, p7: Long, p8: Int, p9: NativePtr): NativePtr
private external fun kniBridge852(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr, p4: NativePtr, p5: NativePtr): NativePtr
private external fun kniBridge853(p0: NativePtr, p1: NativePtr, p2: NativePtr): NativePtr
private external fun kniBridge854(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Int, p4: Int): NativePtr
private external fun kniBridge855(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr, p4: NativePtr, p5: Int, p6: NativePtr, p7: Int, p8: Int, p9: Int, p10: Int): NativePtr
private external fun kniBridge856(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr, p4: Int, p5: NativePtr, p6: Int, p7: Int, p8: Int, p9: Int): NativePtr
private external fun kniBridge857(p0: NativePtr, p1: NativePtr, p2: Int): NativePtr
private external fun kniBridge858(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr, p4: Int, p5: NativePtr): NativePtr
private external fun kniBridge859(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Int, p4: NativePtr, p5: Int, p6: NativePtr): NativePtr
private external fun kniBridge860(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr, p4: NativePtr, p5: NativePtr, p6: Long): Unit
private external fun kniBridge861(p0: NativePtr): NativePtr
private external fun kniBridge862(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge863(p0: NativePtr, p1: Int, p2: Int, p3: NativePtr): NativePtr
private external fun kniBridge864(p0: NativePtr, p1: Int, p2: Int, p3: NativePtr, p4: NativePtr): NativePtr
private external fun kniBridge865(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge866(p0: NativePtr): Unit
private external fun kniBridge867(p0: NativePtr): NativePtr
private external fun kniBridge868(p0: NativePtr, p1: NativePtr): Int
private external fun kniBridge869(): Unit
private external fun kniBridge870(p0: NativePtr): Unit
private external fun kniBridge871(p0: NativePtr): Int
private external fun kniBridge872(p0: Int): Unit
private external fun kniBridge873(): Unit
private external fun kniBridge874(): Unit
private external fun kniBridge875(p0: NativePtr, p1: Int): Unit
private external fun kniBridge876(p0: NativePtr): Int
private val loadLibrary = loadKonanLibrary("llvmstubs")
