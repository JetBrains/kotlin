/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor

fun ObjCGenericTypeParameterUsage(
    typeParameterDescriptor: TypeParameterDescriptor,
    namer: ObjCExportNamer,
) = ObjCGenericTypeParameterUsage(
    typeName = namer.getTypeParameterName(typeParameterDescriptor)
)

fun ObjCGenericTypeParameterDeclaration(
    typeParameterDescriptor: TypeParameterDescriptor,
    namer: ObjCExportNamer,
) = ObjCGenericTypeParameterDeclaration(
    typeName = namer.getTypeParameterName(typeParameterDescriptor),
    variance = ObjCVariance.fromKotlinVariance(typeParameterDescriptor.variance)
)