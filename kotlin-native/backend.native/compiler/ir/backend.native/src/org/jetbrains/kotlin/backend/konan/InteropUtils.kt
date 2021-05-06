/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.types.getPublicSignature
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.TypeUtils

object InteropFqNames {

    const val cPointerName = "CPointer"
    const val nativePointedName = "NativePointed"

    val packageName = FqName("kotlinx.cinterop")

    val cPointer = packageName.child(Name.identifier(cPointerName)).toUnsafe()
    val nativePointed = packageName.child(Name.identifier(nativePointedName)).toUnsafe()
}

object InteropIdSignatures {
    val nativePointed = getPublicSignature(InteropFqNames.packageName, InteropFqNames.nativePointedName)
}

internal class InteropBuiltIns(builtIns: KonanBuiltIns) {

    val packageScope = builtIns.builtInsModule.getPackage(InteropFqNames.packageName).memberScope

    val nativePointed = packageScope.getContributedClass(InteropFqNames.nativePointedName)

    val cValuesRef = this.packageScope.getContributedClass("CValuesRef")
    val cValues = this.packageScope.getContributedClass("CValues")
    val cValue = this.packageScope.getContributedClass("CValue")
    val cOpaque = this.packageScope.getContributedClass("COpaque")
    val cValueWrite = this.packageScope.getContributedFunctions("write")
            .single { it.extensionReceiverParameter?.type?.constructor?.declarationDescriptor == cValue }
    val cValueRead = this.packageScope.getContributedFunctions("readValue")
            .single { it.valueParameters.size == 1 }

    val cEnum = this.packageScope.getContributedClass("CEnum")
    val cEnumVar = this.packageScope.getContributedClass("CEnumVar")
    val cStructVar = this.packageScope.getContributedClass("CStructVar")
    val cStructVarType = cStructVar.defaultType.memberScope.getContributedClass("Type")
    val cPrimitiveVar = this.packageScope.getContributedClass("CPrimitiveVar")
    val cPrimitiveVarType = cPrimitiveVar.defaultType.memberScope.getContributedClass("Type")

    val nativeMemUtils = this.packageScope.getContributedClass("nativeMemUtils")

    val allocType = this.packageScope.getContributedFunctions("alloc")
            .single { it.extensionReceiverParameter != null
                    && it.valueParameters.singleOrNull()?.name?.toString() == "type" }

    val cPointer = this.packageScope.getContributedClass(InteropFqNames.cPointerName)

    val cPointerRawValue = cPointer.unsubstitutedMemberScope.getContributedVariables("rawValue").single()

    val cPointerGetRawValue = packageScope.getContributedFunctions("getRawValue").single {
        val extensionReceiverParameter = it.extensionReceiverParameter
        extensionReceiverParameter != null &&
                TypeUtils.getClassDescriptor(extensionReceiverParameter.type) == cPointer
    }

    val cstr = packageScope.getContributedVariables("cstr").single()
    val wcstr = packageScope.getContributedVariables("wcstr").single()
    val memScope = packageScope.getContributedClass("MemScope")

    val nativePointedRawPtrGetter =
            nativePointed.unsubstitutedMemberScope.getContributedVariables("rawPtr").single().getter!!

    val nativePointedGetRawPointer = packageScope.getContributedFunctions("getRawPointer").single {
        val extensionReceiverParameter = it.extensionReceiverParameter
        extensionReceiverParameter != null &&
                TypeUtils.getClassDescriptor(extensionReceiverParameter.type) == nativePointed
    }

    val typeOf = packageScope.getContributedFunctions("typeOf").single()

    private fun KonanBuiltIns.getUnsignedClass(unsignedType: UnsignedType): ClassDescriptor =
            this.builtInsModule.findClassAcrossModuleDependencies(unsignedType.classId)!!

    val objCObject = packageScope.getContributedClass("ObjCObject")

    val objCObjectBase = packageScope.getContributedClass("ObjCObjectBase")

    val allocObjCObject = packageScope.getContributedFunctions("allocObjCObject").single()

    val getObjCClass = packageScope.getContributedFunctions("getObjCClass").single()

    val objCObjectRawPtr = packageScope.getContributedFunctions("objcPtr").single()

    val interpretObjCPointerOrNull = packageScope.getContributedFunctions("interpretObjCPointerOrNull").single()
    val interpretObjCPointer = packageScope.getContributedFunctions("interpretObjCPointer").single()
    val interpretNullablePointed = packageScope.getContributedFunctions("interpretNullablePointed").single()
    val interpretCPointer = packageScope.getContributedFunctions("interpretCPointer").single()

    val objCObjectSuperInitCheck = packageScope.getContributedFunctions("superInitCheck").single()
    val objCObjectInitBy = packageScope.getContributedFunctions("initBy").single()

    val objCAction = packageScope.getContributedClass("ObjCAction")

    val objCOutlet = packageScope.getContributedClass("ObjCOutlet")

    val objCOverrideInit = objCObjectBase.unsubstitutedMemberScope.getContributedClass("OverrideInit")

    val objCMethodImp = packageScope.getContributedClass("ObjCMethodImp")

    val exportObjCClass = packageScope.getContributedClass("ExportObjCClass")

    val CreateNSStringFromKString = packageScope.getContributedFunctions("CreateNSStringFromKString").single()
    val nativeHeap = packageScope.getContributedClass("nativeHeap")
    val cPointed = packageScope.getContributedClass("CPointed")
    val managedType = packageScope.getContributedClass("ManagedType")
    val cPlusPlusClass = packageScope.getContributedClass("CPlusPlusClass")
    val skiaRefCnt = packageScope.getContributedClass("SkiaRefCnt")

    val interopGetPtr = packageScope.getContributedVariables("ptr").single {
        val singleTypeParameter = it.typeParameters.singleOrNull()
        val singleTypeParameterUpperBound = singleTypeParameter?.upperBounds?.singleOrNull()
        val extensionReceiverParameter = it.extensionReceiverParameter

        singleTypeParameterUpperBound != null &&
        extensionReceiverParameter != null &&
        TypeUtils.getClassDescriptor(singleTypeParameterUpperBound) == cPointed &&
        extensionReceiverParameter.type == singleTypeParameter.defaultType
    }.getter!!

    val interopGetPointed = packageScope.getContributedVariables("pointed").single {
        val singleTypeParameter = it.typeParameters.singleOrNull()
        val singleTypeParameterUpperBound = singleTypeParameter?.upperBounds?.singleOrNull()
        val extensionReceiverParameter = it.extensionReceiverParameter

        singleTypeParameterUpperBound != null &&
        extensionReceiverParameter != null &&
        TypeUtils.getClassDescriptor(singleTypeParameterUpperBound) == cPointed &&
        TypeUtils.getClassDescriptor(extensionReceiverParameter.type) == cPointer
    }.getter!!

}

private fun MemberScope.getContributedVariables(name: String) =
        this.getContributedVariables(Name.identifier(name), NoLookupLocation.FROM_BUILTINS)

private fun MemberScope.getContributedClass(name: String): ClassDescriptor =
        this.getContributedClassifier(Name.identifier(name), NoLookupLocation.FROM_BUILTINS) as ClassDescriptor

private fun MemberScope.getContributedFunctions(name: String) =
        this.getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BUILTINS)
