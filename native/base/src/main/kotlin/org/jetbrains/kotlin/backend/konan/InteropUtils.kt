/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope

object InteropFqNames {

    const val cPointerName = "CPointer"
    const val nativePointedName = "NativePointed"

    const val objCObjectBaseName = "ObjCObjectBase"
    const val objCOverrideInitName = "OverrideInit"
    const val objCOutletName = "ObjCOutlet"
    const val objCMethodImpName = "ObjCMethodImp"
    const val exportObjCClassName = "ExportObjCClass"
    const val nativeHeapName = "nativeHeap"

    const val cValueName = "CValue"
    const val cValuesName = "CValues"
    const val cValuesRefName = "CValuesRef"
    const val cEnumName = "CEnum"
    const val cStructVarName = "CStructVar"
    const val cEnumVarName = "CEnumVar"
    const val cPrimitiveVarName = "CPrimitiveVar"
    const val cPointedName = "CPointed"

    const val interopStubsName = "InteropStubs"
    const val managedTypeName = "ManagedType"
    const val memScopeName = "MemScope"
    const val foreignObjCObjectName = "ForeignObjCObject"
    const val cOpaqueName = "COpaque"
    const val objCObjectName = "ObjCObject"
    const val objCObjectBaseMetaName = "ObjCObjectBaseMeta"
    const val objCClassName = "ObjCClass"
    const val objCClassOfName = "ObjCClassOf"
    const val objCProtocolName = "ObjCProtocol"
    const val nativeMemUtilsName = "nativeMemUtils"
    const val cPlusPlusClassName = "CPlusPlusClass"
    const val skiaRefCntName = "SkiaRefCnt"
    const val TypeName = "Type"

    const val cstrPropertyName = "cstr"
    const val wcstrPropertyName = "wcstr"
    const val nativePointedRawPtrPropertyName = "rawPtr"
    const val cPointerRawValuePropertyName = "rawValue"

    const val getObjCClassFunName = "getObjCClass"
    const val objCObjectSuperInitCheckFunName = "superInitCheck"
    const val allocObjCObjectFunName = "allocObjCObject"
    const val typeOfFunName = "typeOf"
    const val objCObjectInitByFunName = "initBy"
    const val objCObjectRawPtrFunName = "objcPtr"
    const val interpretObjCPointerFunName = "interpretObjCPointer"
    const val interpretObjCPointerOrNullFunName = "interpretObjCPointerOrNull"
    const val interpretNullablePointedFunName = "interpretNullablePointed"
    const val interpretCPointerFunName = "interpretCPointer"
    const val nativePointedGetRawPointerFunName = "getRawPointer"
    const val cPointerGetRawValueFunName = "getRawValue"
    const val cValueWriteFunName = "write"
    const val cValueReadFunName = "readValue"
    const val allocTypeFunName = "alloc"

    val packageName = FqName("kotlinx.cinterop")

    val cPointer = packageName.child(cPointerName).toUnsafe()
    val nativePointed = packageName.child(nativePointedName).toUnsafe()

    val objCObjectBase = packageName.child(objCObjectBaseName)
    val objCOverrideInit = objCObjectBase.child(objCOverrideInitName)
    val objCOutlet = packageName.child(objCOutletName)
    val objCMethodImp = packageName.child(objCMethodImpName)
    val exportObjCClass = packageName.child(exportObjCClassName)

    val cValue = packageName.child(cValueName)
    val cValues = packageName.child(cValuesName)
    val cValuesRef = packageName.child(cValuesRefName)
    val cEnum = packageName.child(cEnumName)
    val cStructVar = packageName.child(cStructVarName)
    val cPointed = packageName.child(cPointedName)

    val interopStubs = packageName.child(interopStubsName)
    val managedType = packageName.child(managedTypeName)
}

private fun FqName.child(nameIdent: String) = child(Name.identifier(nameIdent))

@InternalKotlinNativeApi
class InteropBuiltIns(builtIns: KonanBuiltIns) {

    private val packageScope = builtIns.builtInsModule.getPackage(InteropFqNames.packageName).memberScope

    internal fun getContributedVariables(name: String) = packageScope.getContributedVariables(name)
    internal fun getContributedFunctions(name: String) = packageScope.getContributedFunctions(name)
    internal fun getContributedClass(name: String) = packageScope.getContributedClass(name)
}

private fun MemberScope.getContributedVariables(name: String) =
    this.getContributedVariables(Name.identifier(name), NoLookupLocation.FROM_BUILTINS)

internal fun MemberScope.getContributedClass(name: String): ClassDescriptor =
    this.getContributedClassifier(Name.identifier(name), NoLookupLocation.FROM_BUILTINS) as ClassDescriptor

private fun MemberScope.getContributedFunctions(name: String) =
    this.getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BUILTINS)

