/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.llvm.LlvmParameterAttribute
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.KonanTarget

// TODO: We have [org.jetbrains.kotlin.native.interop.gen.ObjCAbiInfo] which does similar thing.
//  Consider unifying these classes.
//  Also there are ABI-specific pieces in codegen and ObjCExport. It makes sense to move them here as well.
/**
 * Encapsulates ABI-specific logic for code generation.
 *
 * Similar Clang classes:
 *  * [ABIInfo](https://github.com/llvm/llvm-project/blob/2a36f29fce91b6242ebd926d7c08381c31138d2c/clang/lib/CodeGen/ABIInfo.h#L50)
 *  * [TargetCodeGenInfo](https://github.com/llvm/llvm-project/blob/2831a317b689c7f005a29f008a8e4c24485c0711/clang/lib/CodeGen/TargetInfo.h#L45)
 */
sealed interface TargetAbiInfo {
    fun defaultParameterAttributesForIrType(irType: IrType): List<LlvmParameterAttribute>
}

class ExtendOnCallerSideTargetAbiInfo : TargetAbiInfo {
    private val typeAttributesCache = mutableMapOf<IrType, List<LlvmParameterAttribute>>()

    override fun defaultParameterAttributesForIrType(irType: IrType): List<LlvmParameterAttribute> {
        // TODO: We perform type unwrapping twice: one to get the underlying type, and then this one.
        //  Unwrapping is not cheap, so it might affect compilation time.
        return typeAttributesCache.getOrPut(irType) {
            irType.unwrapToPrimitiveOrReference(
                    eachInlinedClass = { inlinedClass, _ ->
                        when (inlinedClass.classId) {
                            UnsignedType.UBYTE.classId -> return listOf(LlvmParameterAttribute.ZeroExt)
                            UnsignedType.USHORT.classId -> return listOf(LlvmParameterAttribute.ZeroExt)
                        }
                    },
                    ifPrimitive = { primitiveType, _ ->
                        when (primitiveType) {
                            KonanPrimitiveType.BOOLEAN -> listOf(LlvmParameterAttribute.ZeroExt)
                            KonanPrimitiveType.CHAR -> listOf(LlvmParameterAttribute.ZeroExt)
                            KonanPrimitiveType.BYTE -> listOf(LlvmParameterAttribute.SignExt)
                            KonanPrimitiveType.SHORT -> listOf(LlvmParameterAttribute.SignExt)
                            KonanPrimitiveType.INT -> emptyList()
                            KonanPrimitiveType.LONG -> emptyList()
                            KonanPrimitiveType.FLOAT -> emptyList()
                            KonanPrimitiveType.DOUBLE -> emptyList()
                            KonanPrimitiveType.NON_NULL_NATIVE_PTR -> emptyList()
                            KonanPrimitiveType.VECTOR128 -> emptyList()
                        }
                    },
                    ifReference = {
                        return emptyList()
                    },
            )
        }
    }
}

sealed class ExtendOnCalleeSideTargetAbiInfo(private val shouldZeroExtBoolean: Boolean) : TargetAbiInfo {
    private val typeAttributesCache = mutableMapOf<IrType, List<LlvmParameterAttribute>>()

    override fun defaultParameterAttributesForIrType(irType: IrType): List<LlvmParameterAttribute> {
        return typeAttributesCache.getOrPut(irType) {
            if (shouldZeroExtBoolean && irType.computePrimitiveBinaryTypeOrNull() == PrimitiveBinaryType.BOOLEAN) {
                listOf(LlvmParameterAttribute.ZeroExt)
            } else {
                emptyList()
            }
        }
    }
}

/**
 * Procedure Call Standard for the Arm 64-bit Architecture.
 * http://infocenter.arm.com/help/topic/com.arm.doc.ihi0055a/IHI0055A_aapcs64.pdf
 *
 * Note that Apple uses different its own variant called DarwinPCS.
 */
class AAPCS64TargetAbiInfo : ExtendOnCalleeSideTargetAbiInfo(shouldZeroExtBoolean = false)

/**
 * Windows x64 ABI.
 * https://docs.microsoft.com/en-us/cpp/build/x64-calling-convention
 *
 * Regarding zero-extension of boolean type: I wasn't able to find any documentation about it,
 * so we follow Clang's behavior here, which in turn follows MSVC.
 * https://github.com/llvm/llvm-project/blob/1fdec59bffc11ae37eb51a1b9869f0696bfd5312/clang/lib/CodeGen/TargetInfo.cpp#L4234
 */
class WindowsX64TargetAbiInfo : ExtendOnCalleeSideTargetAbiInfo(shouldZeroExtBoolean = true)

/**
 * "Generic" ABI info that is applicable for the most of our current targets.
 * In time will be replaced by more specific [TargetAbiInfo] inheritors.
 */
typealias DefaultTargetAbiInfo = ExtendOnCallerSideTargetAbiInfo

/// Equivalent to TargetCodeGenInfo.markARCOptimizedReturnCallsAsNoTail in Clang.
///
/// Determine whether a call to objc_retainAutoreleasedReturnValue or
/// objc_unsafeClaimAutoreleasedReturnValue should be marked as 'notail'.
fun KonanTarget.markARCOptimizedReturnCallsAsNoTail(): Boolean = when (this.architecture) {
    Architecture.X64 -> {
        /// Disable tail call on x86-64. The epilogue code before the tail jump blocks
        /// autoreleaseRV/retainRV and autoreleaseRV/unsafeClaimRV optimizations.
        true
    }
    else -> false
}

/// Equivalent to TargetCodeGenInfo.getARCRetainAutoreleasedReturnValueMarker in Clang.
///
/// Retrieve the address of a function to call immediately before
/// calling objc_retainAutoreleasedReturnValue.  The
/// implementation of objc_autoreleaseReturnValue sniffs the
/// instruction stream following its return address to decide
/// whether it's a call to objc_retainAutoreleasedReturnValue.
/// This can be prohibitively expensive, depending on the
/// relocation model, and so on some targets it instead sniffs for
/// a particular instruction sequence.  This functions returns
/// that instruction sequence in inline assembly, which will be
/// empty if none is required.
fun KonanTarget.getARCRetainAutoreleasedReturnValueMarker(): String? = when (this.architecture) {
    Architecture.X86 -> "movl\t%ebp, %ebp\t\t// marker for objc_retainAutoreleaseReturnValue"
    Architecture.ARM64 -> "mov\tfp, fp\t\t// marker for objc_retainAutoreleaseReturnValue"
    Architecture.ARM32 -> "mov\tr7, r7\t\t// marker for objc_retainAutoreleaseReturnValue"
    else -> null
}

val KonanTarget.abiInfo: TargetAbiInfo
    get() = when {
        this == KonanTarget.MINGW_X64 -> {
            WindowsX64TargetAbiInfo()
        }
        !family.isAppleFamily && architecture == Architecture.ARM64 -> {
            AAPCS64TargetAbiInfo()
        }
        else -> {
            DefaultTargetAbiInfo()
        }
    }