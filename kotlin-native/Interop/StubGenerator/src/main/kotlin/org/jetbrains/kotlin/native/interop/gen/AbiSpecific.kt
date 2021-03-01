/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.native.interop.indexer.*

/**
 * objc_msgSend*_stret functions must be used when return value is returned through memory
 * pointed by implicit argument, which is passed on the register that would otherwise be used for receiver.
 *
 * The entire implementation is just the real ABI approximation which is enough for practical cases.
 */
internal fun Type.isStret(target: KonanTarget): Boolean {
    val unwrappedType = this.unwrapTypedefs()
    val abiInfo: ObjCAbiInfo = when (target) {
        KonanTarget.IOS_ARM64,
        KonanTarget.TVOS_ARM64,
        KonanTarget.MACOS_ARM64 -> DarwinArm64AbiInfo()

        KonanTarget.IOS_X64,
        KonanTarget.MACOS_X64,
        KonanTarget.WATCHOS_X64,
        KonanTarget.TVOS_X64 -> DarwinX64AbiInfo()

        KonanTarget.WATCHOS_X86 -> DarwinX86AbiInfo()

        KonanTarget.IOS_ARM32,
        KonanTarget.WATCHOS_ARM32 -> DarwinArm32AbiInfo(target)

        else -> error("Cannot generate ObjC stubs for $target.")
    }
    return abiInfo.shouldUseStret(unwrappedType)
}

/**
 * Provides ABI-specific information about target for Objective C interop.
 */
interface ObjCAbiInfo {
    fun shouldUseStret(returnType: Type): Boolean
}

class DarwinX64AbiInfo : ObjCAbiInfo {
    override fun shouldUseStret(returnType: Type): Boolean {
        return when (returnType) {
            is RecordType -> returnType.decl.def!!.size > 16 || returnType.hasUnalignedMembers()
            else -> false
        }
    }
}

class DarwinX86AbiInfo : ObjCAbiInfo {
    override fun shouldUseStret(returnType: Type): Boolean {
        // https://github.com/llvm/llvm-project/blob/6c8a34ed9b49704bdd60838143047c62ba9f2502/clang/lib/CodeGen/TargetInfo.cpp#L1243
        return when (returnType) {
            is RecordType -> {
                val size = returnType.decl.def!!.size
                val canBePassedInRegisters = (size == 1L || size == 2L || size == 4L || size == 8L)
                return !canBePassedInRegisters
            }
            else -> false
        }
    }
}

class DarwinArm32AbiInfo(private val target: KonanTarget) : ObjCAbiInfo {
    override fun shouldUseStret(returnType: Type): Boolean = when (target) {
        KonanTarget.IOS_ARM32 -> when (returnType) {
            is RecordType -> !returnType.isIntegerLikeType()
            else -> false
        }
        // 32-bit watchOS uses armv7k which is effectively Cortex-A7 and
        // uses AAPCS16 VPF.
        KonanTarget.WATCHOS_ARM32 -> when (returnType) {
            is RecordType -> {
                // https://github.com/llvm/llvm-project/blob/6c8a34ed9b49704bdd60838143047c62ba9f2502/clang/lib/CodeGen/TargetInfo.cpp#L6165
                when {
                    returnType.decl.def!!.size <= 16 -> false
                    else -> true
                }
            }
            else -> false
        }
        else -> error("Unexpected target")
    }
}

class DarwinArm64AbiInfo : ObjCAbiInfo {
    override fun shouldUseStret(returnType: Type): Boolean {
        // On aarch64 stret is never the case, since an implicit argument gets passed on x8.
        return false
    }
}

private fun Type.isIntegerLikeType(): Boolean = when (this) {
    is RecordType -> {
        val def = this.decl.def
        if (def == null) {
            false
        } else {
            def.size <= 4 &&
                    def.members.all {
                        when (it) {
                            is BitField -> it.type.isIntegerLikeType()
                            is Field -> it.offset == 0L && it.type.isIntegerLikeType()
                            is IncompleteField -> false
                        }
                    }
        }
    }
    is ObjCPointer, is PointerType, CharType, is BoolType -> true
    is IntegerType -> this.size <= 4
    is Typedef -> this.def.aliased.isIntegerLikeType()
    is EnumType -> this.def.baseType.isIntegerLikeType()

    else -> false
}

private fun Type.hasUnalignedMembers(): Boolean = when (this) {
    is Typedef -> this.def.aliased.hasUnalignedMembers()
    is RecordType -> this.decl.def!!.let { def ->
        def.fields.any {
            !it.isAligned ||
                    // Check members of fields too:
                    it.type.hasUnalignedMembers()
        }
    }
    is ArrayType -> this.elemType.hasUnalignedMembers()
    else -> false

// TODO: should the recursive checks be made in indexer when computing `hasUnalignedFields`?
}