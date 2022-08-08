/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.objcexport.sx.SXClangModule
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SourceFile

internal class ObjCExportedInterface(
    val generatedClasses: Set<ClassDescriptor>,
    val categoryMembers: Map<ClassDescriptor, List<CallableMemberDescriptor>>,
    val topLevel: Map<SourceFile, List<CallableMemberDescriptor>>,
    val namer: ObjCExportNamer,
    val mapper: ObjCExportMapper,
    val clangModule: SXClangModule,
    val frameworkName: String,
)