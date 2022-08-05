/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SourceFile
import org.jetbrains.kotlin.types.KotlinType

interface ObjCExportTranslator {
    fun generateBaseDeclarations(): List<ObjCTopLevel<*>>
    fun getClassIfExtension(receiverType: KotlinType): ClassDescriptor?
    fun translateFile(file: SourceFile, declarations: List<CallableMemberDescriptor>): ObjCInterface
    fun translateClass(descriptor: ClassDescriptor): ObjCInterface
    fun translateInterface(descriptor: ClassDescriptor): ObjCProtocol
    fun translateExtensions(classDescriptor: ClassDescriptor, declarations: List<CallableMemberDescriptor>): ObjCInterface
}