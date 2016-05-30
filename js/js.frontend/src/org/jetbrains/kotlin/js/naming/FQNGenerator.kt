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
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils.*
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils.isCompanionObject
import org.jetbrains.kotlin.resolve.calls.tasks.isDynamic
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
import java.util.*

class FQNGenerator {
    private val cache: MutableMap<DeclarationDescriptor, FQNPart> = WeakHashMap()

    fun generate(descriptor: DeclarationDescriptor) = cache.getOrPut(descriptor) { generateCacheMiss(descriptor.original) }

    private fun generateCacheMiss(descriptor: DeclarationDescriptor): FQNPart {
        if (isNativeObject(descriptor) && isCompanionObject(descriptor)) {
            return generate(descriptor.containingDeclaration!!)
        }

        when (descriptor) {
            is ModuleDescriptor -> return FQNPart(listOf(descriptor.name.asString()), true, descriptor, descriptor)
            is PackageFragmentDescriptor -> {
                return if (!descriptor.name.isSpecial) {
                    FQNPart(descriptor.fqName.pathSegments().map { it.asString() }, true, descriptor,
                            descriptor.containingDeclaration)
                }
                else {
                    generate(descriptor.containingDeclaration)
                }
            }
            is FakeCallableDescriptorForObject -> return generate(descriptor.getReferencedDescriptor())
            is ConstructorDescriptor -> {
                if (descriptor.isPrimary || isNativeObject(descriptor)) {
                    return generate(descriptor.containingDeclaration)
                }
            }
            is CallableDescriptor ->
                if (DescriptorUtils.isDescriptorWithLocalVisibility(descriptor)) {
                    val name = getMangledName(getSuggestedName(descriptor), descriptor)
                    return FQNPart(listOf(name.first), false, descriptor, descriptor.containingDeclaration)
                }
        }

        val (localName, shared, parent) = getLocalName(descriptor)
        return FQNPart(listOf(localName), shared, descriptor, fixParent(parent))
    }

    private fun fixParent(parent: DeclarationDescriptor) = when (parent) {
        is PropertyDescriptor -> parent.containingDeclaration
        else -> parent
    }

    private fun getLocalName(descriptor: DeclarationDescriptor): LocalName {
        if (descriptor.isDynamic()) {
            return LocalName(descriptor.name.asString(), true, descriptor.containingDeclaration!!)
        }

        val parts = mutableListOf<String>()
        var current: DeclarationDescriptor = descriptor
        do {
            parts += getSuggestedName(current)
            var last = current
            current = current.containingDeclaration!!
            if (last is ConstructorDescriptor && !last.isPrimary) {
                last = current
                parts[parts.lastIndex] = getSuggestedName(current) + "_init"
                current = current.containingDeclaration!!
            }
        } while (DescriptorUtils.isDescriptorWithLocalVisibility(last) && current !is ClassDescriptor)

        parts.reverse()
        val (id, shared) = getMangledName(parts.joinToString("$"), descriptor)
        return LocalName(id, shared, current)
    }

    private data class LocalName(val id: String, val shared: Boolean, val parent: DeclarationDescriptor)

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

    private fun getMangledName(baseName: String, descriptor: DeclarationDescriptor): Pair<String, Boolean> {
        if (descriptor !is CallableDescriptor) {
            if (isNativeObject(descriptor) || isLibraryObject(descriptor)) {
                return Pair(getNameForAnnotatedObjectWithOverrides(descriptor) ?: descriptor.name.asString(), true)
            }
            return Pair(baseName, needsStableMangling(descriptor))
        }

        var resolvedDescriptor: CallableDescriptor = descriptor
        var overriddenDescriptor: CallableDescriptor? = descriptor
        while (overriddenDescriptor != null) {
            resolvedDescriptor = overriddenDescriptor
            if (isNativeObject(resolvedDescriptor) || isLibraryObject(resolvedDescriptor)) {
                val explicitName = getNameForAnnotatedObjectWithOverrides(resolvedDescriptor)
                if (explicitName != null) {
                    return Pair(explicitName, true)
                }
            }
            overriddenDescriptor = getOverriddenDescriptor(overriddenDescriptor)?.original
        }

        when {
            isNativeObject(resolvedDescriptor) || isLibraryObject(resolvedDescriptor) -> {
                return Pair(getNameForAnnotatedObjectWithOverrides(resolvedDescriptor) ?: resolvedDescriptor.name.asString(), true)
            }
        }

        val explicitName = getJsName(resolvedDescriptor)
        return when {
            explicitName != null -> Pair(explicitName, true)
            needsStableMangling(resolvedDescriptor) ->
                Pair(getStableMangledName(baseName, getArgumentTypesAsString(resolvedDescriptor)), true)
            else -> Pair(baseName, false)
        }
    }

    private fun getOverriddenDescriptor(functionDescriptor: CallableDescriptor): CallableDescriptor? {
        val overriddenDescriptors = functionDescriptor.overriddenDescriptors
        if (overriddenDescriptors.isEmpty()) {
            return null
        }

        //TODO: for now translator can't deal with multiple inheritance good enough
        return overriddenDescriptors.iterator().next()
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
        val suffix = if (forCalculateId.isEmpty()) "" else "_${mangledId(forCalculateId)}\$"
        return suggestedName + suffix
    }

    private fun needsStableMangling(descriptor: DeclarationDescriptor): Boolean {
        if (DescriptorUtils.isDescriptorWithLocalVisibility(descriptor)) return false
        if (descriptor is ClassOrPackageFragmentDescriptor) return true
        if (descriptor !is CallableMemberDescriptor) return false

        // Use stable mangling for overrides because we use stable mangling when any function inside a overridable declaration
        // for avoid clashing names when inheritance.
        if (DescriptorUtils.isOverride(descriptor)) return true

        val containingDeclaration = descriptor.containingDeclaration
        if (isNativeObject(containingDeclaration) || isLibraryObject(containingDeclaration)) return true

        return when (containingDeclaration) {
            is PackageFragmentDescriptor -> descriptor.visibility.isPublicAPI
            is ClassDescriptor -> {
                if (descriptor.modality == Modality.OPEN || descriptor.modality == Modality.ABSTRACT) {
                    return descriptor.visibility.isPublicAPI
                }

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

    companion object {
        @JvmStatic fun mangledId(forCalculateId: String): String {
            val absHashCode = Math.abs(forCalculateId.hashCode())
            return if (absHashCode != 0) Integer.toString(absHashCode, Character.MAX_RADIX) else ""
        }
    }
}
