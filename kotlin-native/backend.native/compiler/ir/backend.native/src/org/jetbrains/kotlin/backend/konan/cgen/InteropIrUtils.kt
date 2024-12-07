/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.cgen

import org.jetbrains.kotlin.backend.konan.KonanFqNames
import org.jetbrains.kotlin.backend.konan.RuntimeNames
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.ir.isAny
import org.jetbrains.kotlin.backend.konan.ir.superClasses
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.objcinterop.isObjCObjectType
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.isSubtypeOfClass
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal fun IrType.isCEnumType(): Boolean {
    if (isNullable()) return false
    val enumClass = classOrNull?.owner ?: return false
    if (!enumClass.isEnumClass) return false

    return enumClass.superTypes
            .any { (it.classifierOrNull?.owner as? IrClass)?.fqNameForIrSerialization == FqName("kotlinx.cinterop.CEnum") }
}

private val cCall = RuntimeNames.cCall

// Make sure external stubs always get proper annotaions.
@OptIn(ObsoleteDescriptorBasedAPI::class)
fun IrDeclaration.hasCCallAnnotation(name: String): Boolean =
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

internal fun IrType.isTypeOfNullLiteral(): Boolean = isNullableNothing()

internal fun IrType.isVector(): Boolean {
    if (this is IrSimpleType && !this.isNullable()) {
        return classifier.isClassWithFqName(KonanFqNames.Vector128.toUnsafe())
    }
    return false
}

internal fun IrType.isObjCReferenceType(target: KonanTarget, irBuiltIns: IrBuiltIns): Boolean {
    if (!target.family.isAppleFamily) return false

    // Handle the same types as produced by [objCPointerMirror] in Interop/StubGenerator/.../Mappings.kt.

    if (isObjCObjectType()) return true

    return when (classifierOrNull) {
        irBuiltIns.anyClass,
        irBuiltIns.stringClass,
        irBuiltIns.listClass,
        irBuiltIns.mutableListClass,
        irBuiltIns.setClass,
        irBuiltIns.mapClass -> true
        else -> false
    }
}

internal fun IrType.isCPointer(symbols: KonanSymbols): Boolean = this.classOrNull == symbols.interopCPointer
internal fun IrType.isCValue(symbols: KonanSymbols): Boolean = this.classOrNull == symbols.interopCValue
internal fun IrType.isCValuesRef(symbols: KonanSymbols): Boolean = this.classOrNull == symbols.interopCValuesRef

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

