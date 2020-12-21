package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.name.FqName

object RuntimeNames {
    val symbolNameAnnotation = FqName("kotlin.native.SymbolName")
    val cnameAnnotation = FqName("kotlin.native.CName")
    val frozenAnnotation = FqName("kotlin.native.internal.Frozen")
    val exportForCppRuntime = FqName("kotlin.native.internal.ExportForCppRuntime")
    val exportForCompilerAnnotation = FqName("kotlin.native.internal.ExportForCompiler")
    val exportTypeInfoAnnotation = FqName("kotlin.native.internal.ExportTypeInfo")
    val cCall = FqName("kotlinx.cinterop.internal.CCall")
    val cStructMemberAt = FqName("kotlinx.cinterop.internal.CStruct.MemberAt")
    val cStructArrayMemberAt = FqName("kotlinx.cinterop.internal.CStruct.ArrayMemberAt")
    val cStructBitField = FqName("kotlinx.cinterop.internal.CStruct.BitField")
    val cStruct = FqName("kotlinx.cinterop.internal.CStruct")
    val managedType = FqName("kotlinx.cinterop.internal.CStruct.ManagedType")
    val objCMethodAnnotation = FqName("kotlinx.cinterop.ObjCMethod")
    val objCMethodImp = FqName("kotlinx.cinterop.ObjCMethodImp")
    val independent = FqName("kotlin.native.internal.Independent")
    val filterExceptions = FqName("kotlin.native.internal.FilterExceptions")
    val kotlinNativeInternalPackageName = FqName.fromSegments(listOf("kotlin", "native", "internal"))
    val associatedObjectKey = FqName("kotlin.reflect.AssociatedObjectKey")
    val typedIntrinsicAnnotation = FqName("kotlin.native.internal.TypedIntrinsic")
}
