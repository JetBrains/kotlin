/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SourceFile

sealed class Event {
    data class TranslateClass(val declaration: ClassDescriptor) : Event()

    data class TranslateInterface(val declaration: ClassDescriptor) : Event()

    data class TranslateUnexposedClass(val classDescriptor: ClassDescriptor) : Event()

    data class TranslateUnexposedInterface(val classDescriptor: ClassDescriptor) : Event()

    data class TranslateTopLevel(
            val sourceFile: SourceFile,
            val declaration: CallableMemberDescriptor,
    ) : Event()

    data class TranslateExtension(
            val classDescriptor: ClassDescriptor,
            val extension: CallableMemberDescriptor,
    ) : Event()

    data class TranslateClassForwardDeclaration(
            val classDescriptor: ClassDescriptor
    ) : Event()

    data class TranslateInterfaceForwardDeclaration(
            val classDescriptor: ClassDescriptor
    ) : Event()
}
