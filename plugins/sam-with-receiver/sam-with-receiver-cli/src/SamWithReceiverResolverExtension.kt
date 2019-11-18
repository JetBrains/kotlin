/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.samWithReceiver

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.extensions.AnnotationBasedExtension
import org.jetbrains.kotlin.load.kotlin.sam.SamWithReceiverResolver
import org.jetbrains.kotlin.psi.KtModifierListOwner

class SamWithReceiverResolverExtension(
        private val annotations: List<String>
) : SamWithReceiverResolver, AnnotationBasedExtension {
    override fun getAnnotationFqNames(modifierListOwner: KtModifierListOwner?) = annotations

    override fun shouldConvertFirstSamParameterToReceiver(function: FunctionDescriptor): Boolean {
        return (function.containingDeclaration as? ClassDescriptor)?.hasSpecialAnnotation(null) ?: false
    }
}