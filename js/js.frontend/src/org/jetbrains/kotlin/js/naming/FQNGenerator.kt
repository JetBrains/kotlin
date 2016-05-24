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

package org.jetbrains.kotlin.js.naming

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.js.descriptorUtils.isEnumValueOfMethod
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils.isLibraryObject
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils.isNativeObject
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils.isCompanionObject

class FQNGenerator(private val participants: List<FQNPartipcipant> = listOf()) {
    private val cache = mutableMapOf<DeclarationDescriptor, List<FQNPart>>()

    fun generate(descriptor: DeclarationDescriptor) = cache.getOrPut(descriptor) { generateCacheMiss(descriptor) }

    private fun generateCacheMiss(descriptor: DeclarationDescriptor): List<FQNPart> {
        for (participant in participants) {
            val result = participant.participate(descriptor, this)
            if (result != null) {
                return result
            }
        }

        when (descriptor) {
            is ModuleDescriptor -> return listOf(FQNPart(descriptor.name.asString(), FQNPartType.MODULE, descriptor))
            is PackageFragmentDescriptor -> {
                val result = generate(descriptor.containingDeclaration).toMutableList()
                if (!descriptor.name.isSpecial) {
                    result += descriptor.fqName.pathSegments().map { FQNPart(it.asString(), FQNPartType.PUBLIC, descriptor) }
                }
                return result
            }
            is ConstructorDescriptor -> if (descriptor.isPrimary) return generate(descriptor.containingDeclaration)
            is CallableMemberDescriptor ->
                if (DescriptorUtils.isDescriptorWithLocalVisibility(descriptor)) {
                    val name = getMangledName(getSuggestedName(descriptor), descriptor)
                    return listOf(FQNPart(name, FQNPartType.PRIVATE, descriptor))
                }
        }

        val (localName, shared, parent) = getLocalName(descriptor)
        val localPart = FQNPart(localName, if (shared) FQNPartType.PUBLIC else FQNPartType.PRIVATE, descriptor)

        val qualifier = if (parent != null) generate(parent) else listOf()
        return qualifier + localPart
    }

    private fun getLocalName(descriptor: DeclarationDescriptor): LocalName {
        val parts = mutableListOf<String>()
        var current: DeclarationDescriptor? = descriptor
        do {
            current!!
            parts += getSuggestedName(current)
            var last = current
            current = current.containingDeclaration
            if (last is ConstructorDescriptor && !last.isPrimary) {
                last = current
                parts[parts.lastIndex] = getSuggestedName(current!!) + "_init"
                current = current.containingDeclaration
            }
        } while (current != null && DescriptorUtils.isDescriptorWithLocalVisibility(last) && current !is ClassDescriptor)

        parts.reverse()
        return LocalName(getMangledName(parts.joinToString("$"), descriptor), needsStableMangling(descriptor), current)
    }

    private data class LocalName(val id: String, val shared: Boolean, val parent: DeclarationDescriptor?)

    private fun getSuggestedName(descriptor: DeclarationDescriptor): String {
        val name = descriptor.name
        return if (name.isSpecial) {
            when (descriptor) {
                is PropertyGetterDescriptor -> "get_" + getSuggestedName(descriptor.correspondingProperty)
                is PropertySetterDescriptor -> "set_" + getSuggestedName(descriptor.correspondingProperty)
                else -> "f"
            }
        }
        else {
            name.asString()
        }
    }

    private fun getMangledName(baseName: String, descriptor: DeclarationDescriptor): String {
        if (needsStableMangling(descriptor)) {
            return if (descriptor is CallableMemberDescriptor) {
                getStableMangledName(baseName, getArgumentTypesAsString(descriptor))
            }
            else {
                baseName
            }
        }

        return baseName
    }

    private fun getArgumentTypesAsString(descriptor: CallableDescriptor): String {
        val argTypes = StringBuilder()

        val receiverParameter = descriptor.extensionReceiverParameter
        if (receiverParameter != null) {
            argTypes.append(receiverParameter.type.getJetTypeFqName(true)).append(".")
        }

        argTypes.append(descriptor.valueParameters.joinToString(",") { it.type.getJetTypeFqName(true) })

        return argTypes.toString()
    }

    private fun getStableMangledName(suggestedName: String, forCalculateId: String): String {
        val absHashCode = Math.abs(forCalculateId.hashCode())
        val suffix = if (absHashCode == 0) "" else "_" + Integer.toString(absHashCode, Character.MAX_RADIX) + "$"
        return suggestedName + suffix
    }

    private fun needsStableMangling(descriptor: DeclarationDescriptor): Boolean {
        if (descriptor is ClassOrPackageFragmentDescriptor) return true
        if (descriptor !is CallableMemberDescriptor) return false

        // Use stable mangling for overrides because we use stable mangling when any function inside a overridable declaration
        // for avoid clashing names when inheritance.
        if (DescriptorUtils.isOverride(descriptor)) return true

        val containingDeclaration = descriptor.containingDeclaration

        return when (containingDeclaration) {
            is PackageFragmentDescriptor -> descriptor.visibility.isPublicAPI
            is ClassDescriptor -> {
                // Use stable mangling when it's inside an overridable declaration to avoid clashing names on inheritance.
                if (!containingDeclaration.isFinalOrEnum) return true

                // valueOf() is created in the library with a mangled name for every enum class
                if (descriptor is FunctionDescriptor && descriptor.isEnumValueOfMethod()) return true

                // Don't use stable mangling when it inside a non-public API declaration.
                if (!containingDeclaration.visibility.isPublicAPI) return false

                // Ignore the `protected` visibility because it can be use outside a containing declaration
                // only when the containing declaration is overridable.
                if (descriptor.visibility === Visibilities.PUBLIC) return true

                return false
            }
            else -> {
                assert(containingDeclaration is CallableMemberDescriptor) {
                    "containingDeclaration for descriptor have unsupported type for mangling, " +
                    "descriptor: " + descriptor + ", containingDeclaration: " + containingDeclaration
                }
                false
            }
        }
    }

    object NativeParticipant : FQNPartipcipant {
        override fun participate(descriptor: DeclarationDescriptor, generator: FQNGenerator): List<FQNPart>? {
            return when {
                isNativeObject(descriptor) && isCompanionObject(descriptor) -> {
                    generator.generate(descriptor.containingDeclaration!!)
                }
                descriptor is PropertyAccessorDescriptor && isNativeObject(descriptor.correspondingProperty) -> {
                    generator.generate(descriptor.correspondingProperty)
                }
                isNativeObject(descriptor) || isLibraryObject(descriptor) -> {
                    if (descriptor is ConstructorDescriptor) {
                        generator.generate(descriptor.containingDeclaration)
                    }
                    else {
                        val name = AnnotationsUtils.getNameForAnnotatedObjectWithOverrides(descriptor)
                        val qualifier = when {
                            descriptor is CallableDescriptor && DescriptorUtils.isDescriptorWithLocalVisibility(descriptor) -> listOf()
                            descriptor is ClassDescriptor && descriptor.containingDeclaration is PackageFragmentDescriptor -> listOf()
                            descriptor is ClassDescriptor && isNativeObject(descriptor) &&
                                    !isNativeObject(descriptor.containingDeclaration) -> listOf()
                            else -> generator.generate(descriptor.containingDeclaration!!)
                        }
                        qualifier + listOf(FQNPart(name ?: descriptor.name.asString(), FQNPartType.PUBLIC, descriptor))
                    }
                }
                else -> null
            }
        }
    }
}
