/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.konan

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.konan.library.KONAN_STDLIB_NAME
import org.jetbrains.kotlin.name.NativeForwardDeclarationKind
import org.jetbrains.kotlin.name.Name

val NATIVE_STDLIB_MODULE_NAME = Name.special("<$KONAN_STDLIB_NAME>")

fun ModuleDescriptor.isNativeStdlib() = name == NATIVE_STDLIB_MODULE_NAME
fun ClassDescriptor.getForwardDeclarationKindOrNull() =
    NativeForwardDeclarationKind.packageFqNameToKind[(containingDeclaration as? PackageFragmentDescriptor)?.fqName]