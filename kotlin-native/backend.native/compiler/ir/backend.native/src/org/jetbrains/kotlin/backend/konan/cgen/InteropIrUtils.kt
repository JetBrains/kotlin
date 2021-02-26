/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.cgen

import org.jetbrains.kotlin.backend.jvm.ir.propertyIfAccessor
import org.jetbrains.kotlin.backend.konan.KonanFqNames
import org.jetbrains.kotlin.backend.konan.RuntimeNames
import org.jetbrains.kotlin.backend.konan.ir.*
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.ir.isObjCObjectType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal fun IrType.isCEnumType(): Boolean {
    val simpleType = this as? IrSimpleType ?: return false
    if (simpleType.hasQuestionMark) return false
    val enumClass = simpleType.classifier.owner as? IrClass ?: return false
    if (!enumClass.isEnumClass) return false

    return enumClass.superTypes
            .any { (it.classifierOrNull?.owner as? IrClass)?.fqNameForIrSerialization == FqName("kotlinx.cinterop.CEnum") }
}

private val cCall = RuntimeNames.cCall

// Make sure external stubs always get proper annotaions.
private fun IrDeclaration.hasCCallAnnotation(name: String): Boolean =
        this.annotations.hasAnnotation(cCall.child(Name.identifier(name)))
                // LazyIr doesn't pass annotations from descriptor to IrValueParameter.
                || this.descriptor.annotations.hasAnnotation(cCall.child(Name.identifier(name)))

internal fun IrValueParameter.isWCStringParameter() = hasCCallAnnotation("WCString")

internal fun IrValueParameter.isCStringParameter() = hasCCallAnnotation("CString")

internal fun IrValueParameter.isObjCConsumed() = hasCCallAnnotation("Consumed")

internal fun IrSimpleFunction.objCConsumesReceiver() = hasCCallAnnotation("ConsumesReceiver")

internal fun IrSimpleFunction.objCReturnsRetained() = hasCCallAnnotation("ReturnsRetained")

internal fun IrClass.getCStructSpelling(): String? =
        getAnnotationArgumentValue(FqName("kotlinx.cinterop.internal.CStruct"), "spelling")

internal fun IrType.isTypeOfNullLiteral(): Boolean = this is IrSimpleType && hasQuestionMark
        && classifier.isClassWithFqName(StandardNames.FqNames.nothing)

internal fun IrType.isVector(): Boolean {
    if (this is IrSimpleType && !this.hasQuestionMark) {
        return classifier.isClassWithFqName(KonanFqNames.Vector128.toUnsafe())
    }
    return false
}

internal fun IrType.isObjCReferenceType(target: KonanTarget, irBuiltIns: IrBuiltIns): Boolean {
    if (!target.family.isAppleFamily) return false

    // Handle the same types as produced by [objCPointerMirror] in Interop/StubGenerator/.../Mappings.kt.

    if (isObjCObjectType()) return true

    val descriptor = classifierOrNull?.descriptor ?: return false
    val builtIns = irBuiltIns.builtIns

    return when (descriptor) {
        builtIns.any,
        builtIns.string,
        builtIns.list, builtIns.mutableList,
        builtIns.set,
        builtIns.map -> true
        else -> false
    }
}

internal fun IrType.isCPointer(symbols: KonanSymbols): Boolean = this.classOrNull == symbols.interopCPointer
internal fun IrType.isCValue(symbols: KonanSymbols): Boolean = this.classOrNull == symbols.interopCValue

internal fun IrType.isNativePointed(symbols: KonanSymbols): Boolean = isSubtypeOfClass(symbols.nativePointed)

internal fun IrType.isCStructFieldTypeStoredInMemoryDirectly(): Boolean = isPrimitiveType() || isUnsigned() || isVector()

internal fun IrType.isCStructFieldSupportedReferenceType(symbols: KonanSymbols): Boolean =
        isObjCObjectType()
                || getClass()?.isAny() == true
                || isStringClassType()
                || classOrNull == symbols.list
                || classOrNull == symbols.mutableList
                || classOrNull == symbols.set
                || classOrNull == symbols.map

/**
 * Check given function is a getter or setter
 * for `value` property of CEnumVar subclass.
 */
internal fun IrFunction.isCEnumVarValueAccessor(symbols: KonanSymbols): Boolean {
    val parent = parent as? IrClass ?: return false
    return if (symbols.interopCEnumVar in parent.superClasses && isPropertyAccessor) {
        (propertyIfAccessor as IrProperty).name.asString() == "value"
    } else {
        false
    }
}

internal fun IrFunction.isCStructMemberAtAccessor() = hasAnnotation(RuntimeNames.cStructMemberAt)

internal fun IrFunction.isCStructArrayMemberAtAccessor() = hasAnnotation(RuntimeNames.cStructArrayMemberAt)

internal fun IrFunction.isCStructBitFieldAccessor() = hasAnnotation(RuntimeNames.cStructBitField)

