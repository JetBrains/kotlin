/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.translate.utils

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.naming.encodeSignature
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.module

// Unfortunately, our descriptor serializer can't serialize references to descriptors.
// We have to serialize entire class or package to serialize function or property.
// In case of declarations from external modules we end up with duplication of significant metadata.

// What we really need these descriptors is to determine which names from different fragments point to one declarations,
// so that merger could turn these names into one name. Actually, we need a way to get unique identifier for every
// declaration.

// This code does not generate unique identifiers, it's possible to pick two declarations with same strings. However,
// it's hard to do it unintentionally.
fun generateSignature(descriptor: DeclarationDescriptor): String? {
    if (DescriptorUtils.isDescriptorWithLocalVisibility(descriptor)) return null
    if (descriptor is DeclarationDescriptorWithVisibility && descriptor.visibility == Visibilities.PRIVATE &&
        !AnnotationsUtils.isNativeObject(descriptor) && !AnnotationsUtils.isLibraryObject(descriptor)
    ) {
        return null
    }
    return when (descriptor) {
        is CallableDescriptor -> {
            // Should correspond to inner name generation
            if (descriptor is ConstructorDescriptor && descriptor.isPrimary) {
                return generateSignature(descriptor.constructedClass)
            }

            val parent = generateSignature(descriptor.containingDeclaration) ?: return null
            if (descriptor !is VariableAccessorDescriptor && descriptor !is ConstructorDescriptor && descriptor.name.isSpecial) {
                return null
            }

            // Make distinction between functions with zero parameters and properties
            val separator = if (descriptor is FunctionDescriptor) "#" else "!"

            parent + separator + escape(descriptor.name.asString()) + "|" + encodeSignature(descriptor)
        }
        is PackageFragmentDescriptor -> {
            val module = descriptor.module.name.asString()
            val parts = sequenceOf(module) + descriptor.fqName.pathSegments().map { it.identifier }
            parts.joinToString(".") { escape(it) }
        }
        is ClassDescriptor -> {
            val parent = generateSignature(descriptor.containingDeclaration) ?: return null
            if (descriptor.name.isSpecial) return null
            parent + "$" + escape(descriptor.name.asString())
        }
        else -> return null
    }
}

private fun escape(s: String): String {
    val sb = StringBuilder()
    for (c in s) {
        val escapedChar = when (c) {
            '\\', '"', '.', '$', '#', '!', '<', '>', '|', '+', '-', ':', '*', '?' -> "\\$c"
            else -> c.toString()
        }
        sb.append(escapedChar)
    }
    return sb.toString()
}
