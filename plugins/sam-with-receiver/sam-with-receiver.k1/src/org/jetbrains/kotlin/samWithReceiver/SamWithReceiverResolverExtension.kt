/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.samWithReceiver

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.extensions.AnnotationBasedExtension
import org.jetbrains.kotlin.resolve.sam.SamWithReceiverResolver
import org.jetbrains.kotlin.psi.KtModifierListOwner

class SamWithReceiverResolverExtension(
    private val annotations: List<String>
) : SamWithReceiverResolver, AnnotationBasedExtension {
    override fun getAnnotationFqNames(modifierListOwner: KtModifierListOwner?) = annotations

    override fun shouldConvertFirstSamParameterToReceiver(function: FunctionDescriptor): Boolean {
        return (function.containingDeclaration as? ClassDescriptor)?.hasSpecialAnnotation(null) ?: false
    }
}
