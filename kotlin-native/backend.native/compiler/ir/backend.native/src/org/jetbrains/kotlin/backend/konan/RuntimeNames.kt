package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.NativeRuntimeNames

object RuntimeNames {
    val symbolNameAnnotation = NativeRuntimeNames.Annotations.symbolNameClassId.asSingleFqName()
    val cnameAnnotation = NativeRuntimeNames.Annotations.cNameClassId.asSingleFqName()
    val frozenAnnotation = FqName("kotlin.native.internal.Frozen")
    val exportForCppRuntime = NativeRuntimeNames.Annotations.exportForCppRuntimeClassId.asSingleFqName()
    val exportForCompilerAnnotation = NativeRuntimeNames.Annotations.exportForCompilerClassId.asSingleFqName()
    val exportTypeInfoAnnotation = FqName("kotlin.native.internal.ExportTypeInfo")
    val cCall = FqName("kotlinx.cinterop.internal.CCall")
    val cStructMemberAt = FqName("kotlinx.cinterop.internal.CStruct.MemberAt")
    val cStructArrayMemberAt = FqName("kotlinx.cinterop.internal.CStruct.ArrayMemberAt")
    val cStructBitField = FqName("kotlinx.cinterop.internal.CStruct.BitField")
    val cStruct = FqName("kotlinx.cinterop.internal.CStruct")
    val cppClass = FqName("kotlinx.cinterop.internal.CStruct.CPlusPlusClass")
    val managedType = FqName("kotlinx.cinterop.internal.CStruct.ManagedType")
    val skiaRefCnt = FqName("kotlinx.cinterop.SkiaRefCnt") // TODO: move me to the plugin?
    val objCMethodAnnotation = FqName("kotlinx.cinterop.ObjCMethod")
    val objCMethodImp = FqName("kotlinx.cinterop.ObjCMethodImp")
    val independent = FqName("kotlin.native.internal.Independent")
    val filterExceptions = FqName("kotlin.native.internal.FilterExceptions")
    val kotlinNativeInternalPackageName = FqName.fromSegments(listOf("kotlin", "native", "internal"))
    val kotlinNativeCoroutinesInternalPackageName = FqName.fromSegments(listOf("kotlin", "coroutines", "native", "internal"))
    val associatedObjectKey = FqName("kotlin.reflect.AssociatedObjectKey")
    val typedIntrinsicAnnotation = FqName("kotlin.native.internal.TypedIntrinsic")
    val cleaner = FqName("kotlin.native.ref.Cleaner")
}
