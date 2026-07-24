/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.hasEqualClassId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.NativeForwardDeclarationKind
import org.jetbrains.kotlin.name.NativeStandardInteropNames
import org.jetbrains.kotlin.name.isSubpackageOf

fun IrClass.inheritsFromCStruct(): Boolean = superTypes.any { it.classOrFail.hasEqualClassId(NativeStandardInteropNames.CStructVarClassId) }
fun IrClass.inheritsFromCEnum(): Boolean = superTypes.any { it.classOrFail.hasEqualClassId(NativeStandardInteropNames.CEnumClassId) }

fun ClassId.definitelyNotFromCInterop(): Boolean = isDeclaredInStdlib() || isForwardDeclaration()

private fun ClassId.isDeclaredInStdlib(): Boolean =
    packageFqName.isSubpackageOf(StandardNames.BUILT_INS_PACKAGE_FQ_NAME) ||
            packageFqName.isSubpackageOf(NativeStandardInteropNames.cInteropPackage)

private fun ClassId.isForwardDeclaration(): Boolean = packageFqName in NativeForwardDeclarationKind.packageFqNameToKind

fun ClassId.toCInteropSignature(isCInterop: Boolean): IdSignature.CommonSignature = IdSignature.CommonSignature(
    packageFqName = packageFqName.asString(),
    declarationFqName = relativeClassName.asString(),
    id = null,
    mask = IdSignature.Flags.IS_NATIVE_INTEROP_LIBRARY.encode(isCInterop),
    description = null,
)
