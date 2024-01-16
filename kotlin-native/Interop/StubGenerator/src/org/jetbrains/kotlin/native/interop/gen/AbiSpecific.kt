/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.konan.target.Architecture
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
    val abiInfo: ObjCAbiInfo = when (target.architecture) {
        Architecture.ARM64 -> DarwinArm64AbiInfo()

        Architecture.X64 -> DarwinX64AbiInfo()

        Architecture.X86 -> DarwinX86AbiInfo()

        Architecture.ARM32 -> DarwinArm32AbiInfo(target)

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

/**
 * Remember about arm64_32!
 */
class DarwinArm64AbiInfo : ObjCAbiInfo {
    override fun shouldUseStret(returnType: Type): Boolean {
        // On aarch64 stret is never the case, since an implicit argument gets passed on x8.
        return false
    }
}

/*
Consider edge cases with anonymous inner:
hasIntegerLikeLayout
1   N   struct X { struct {}; int v1; };                // despite the offset(v1) == 0 and sizeof(X) == 4
2   N   struct X { struct {}; char v1; };               // same with char
3   N   struct X { struct {} v1; short v2; };           // same with named empty field; sizeof == 2, offset(v2) == 0
4   N   struct X { int v1; struct {}; };                // despite there is only one field but empty struct has offset == 4
5   N   struct X { char v1; struct {}; };               // same, sizeof is 1
6   N   struct X { char v1; struct {char v2:4;}; };     // despite v2 is bitfield
7   Y   struct X { char v1; char v2:4; };               // but this is OK (bitfield)
8   Y   struct X { struct {char v1;}; char v2:4; };     // same,  bitfield is OK
9   Y   struct X { struct {char v1;} v1; char v2:4; };  // same, OK v2 is bitfield
10  Y   struct X { struct {} v1; char v2:4; };          // OK, v2 is bitfield
11  Y   struct X { struct {char v1;}; short v2:16; };   // OK, v2 is bitfield even if it has full size

#1..3: the field offset == 0 but still not eligible for `hasIntegerLikeLayout`
Looks like we have to use the field' sequential number instead of offset
 */
private fun StructDef.hasIntegerLikeLayout(): Boolean {
    return size <= 4 &&
            members.mapIndexed { index, it ->
                // Assuming the member order has not been changed
                when (it) {
                    is BitField -> it.type.isIntegerLikeType()
                    is Field -> index == 0 && it.type.isIntegerLikeType() // assert(offset == 0)
                    is AnonymousInnerRecord -> index == 0 && it.def.hasIntegerLikeLayout()
                    is IncompleteField -> false
                }
            }.all {it}
}

private fun Type.isIntegerLikeType(): Boolean = when (this) {
    is RecordType -> decl.def?.hasIntegerLikeLayout() ?: false
    is ObjCPointer, is PointerType, CharType, is BoolType -> true
    is IntegerType -> this.size <= 4
    is Typedef -> this.def.aliased.isIntegerLikeType()
    is EnumType -> this.def.baseType.isIntegerLikeType()

    else -> false
}

private fun Type.hasUnalignedMembers(): Boolean = when (this) {
    is Typedef -> this.def.aliased.hasUnalignedMembers()
    is RecordType -> this.decl.def!!.let { def ->
        def.fields.any { // TODO: what about bitfields?
            !it.isAligned ||
                    // Check members of fields too:
                    it.type.hasUnalignedMembers()
        }
    }
    is ArrayType -> this.elemType.hasUnalignedMembers()
    else -> false

// TODO: should the recursive checks be made in indexer when computing `hasUnalignedFields`?
}
