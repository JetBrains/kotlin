/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SourceFile

@InternalKotlinNativeApi
class ObjCExportedInterface internal constructor(
    val generatedClasses: Set<ClassDescriptor>,
    val categoryMembers: Map<ClassDescriptor, List<CallableMemberDescriptor>>,
    val topLevel: Map<SourceFile, List<CallableMemberDescriptor>>,
    val headerLines: List<String>,
    val namer: ObjCExportNamer,
    val mapper: ObjCExportMapper,
)