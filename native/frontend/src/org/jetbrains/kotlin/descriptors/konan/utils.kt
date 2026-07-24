/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.konan

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.explicitParameters
import org.jetbrains.kotlin.konan.library.KONAN_STDLIB_NAME
import org.jetbrains.kotlin.name.NativeForwardDeclarationKind
import org.jetbrains.kotlin.name.Name

@K1Deprecation
val NATIVE_STDLIB_MODULE_NAME: Name = Name.special("<$KONAN_STDLIB_NAME>")

@K1Deprecation
fun ModuleDescriptor.isNativeStdlib(): Boolean = name == NATIVE_STDLIB_MODULE_NAME

@K1Deprecation
fun ClassDescriptor.getForwardDeclarationKindOrNull(): NativeForwardDeclarationKind? =
    NativeForwardDeclarationKind.packageFqNameToKind[(containingDeclaration as? PackageFragmentDescriptor)?.fqName]

/**
 * @return naturally-ordered list of all parameters available inside the function body.
 */
@K1Deprecation
val CallableDescriptor.allParameters: List<ParameterDescriptor>
    get() = if (this is ConstructorDescriptor) {
        listOf(this.constructedClass.thisAsReceiverParameter) + explicitParameters
    } else {
        explicitParameters
    }