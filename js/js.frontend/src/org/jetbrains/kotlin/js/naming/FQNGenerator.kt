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

/**
 * This class is responsible for generating names for declarations. It does not produce fully-qualified JS name, instead
 * it tries to generate a simple name and specify a scoping declaration. This information can be used by the front-end
 * to check whether names clash, by the code generator to place declarations to corresponding scopes and to produce
 * fully-qualified names for static declarations.
 *
 * A new instance of this class can be created for each request, however, it's recommended to use shared instance, since
 * [FQNGenerator] supports caching.
 */
class FQNGenerator {
    private val cache: MutableMap<DeclarationDescriptor, FQNPart?> = WeakHashMap()

    /**
     * Generates names for declarations. Name consist of the following parts:
     *
     *   * Aliasing declaration, if the given `descriptor` does not have its own entity in JS.
     *   * Scoping declaration. Declarations are usually compiled to the hierarchy of nested JS objects,
     *     this attribute allows to find out where to put the declaration.
     *   * Simple name, which is a name that object must (or may) get on the generated JS.
     *   * Whether the name is "shared". Shared names are visible to other modules and to native JS.
     *     In order to preserve ABI, these names must be persistent. Private names do not require particular
     *     name, so the code generator can invent any name which does not clash with anything; however,
     *     it may derive the name from the suggested name to improve readability and debugging.
     *
     * This method returns `null` for root declarations (modules and root packages).
     * It's guaranteed that a particular name is returned for any other declarations.
     *
     * Since packages in Kotlin do not always form hierarchy, suggested name is a list of strings. This
     * list consists of exactly one string for any declaration except for package. Package name lists
     * have at least one string.
     */
    fun generate(descriptor: DeclarationDescriptor) = cache.getOrPut(descriptor) { generateCacheMiss(descriptor.original) }

    private fun generateCacheMiss(descriptor: DeclarationDescriptor): FQNPart? {
        // Members of companion objects of classes are treated as static members of these classes
        if (isNativeObject(descriptor) && isCompanionObject(descriptor)) {
            return generate(descriptor.containingDeclaration!!)
        }

        when (descriptor) {
            // Modules are root declarations, we don't produce declarations for them, therefore they can't clash
            is ModuleDescriptor -> return null

            is PackageFragmentDescriptor -> {
                return if (!descriptor.name.isSpecial) {
                    FQNPart(descriptor.fqName.pathSegments().map { it.asString() }, true, descriptor, descriptor.containingDeclaration)
                }
                else {
                    // Root packages are similar to modules
                    null
                }
            }

            // It's a special case when an object has `invoke` operator defined, in this case we simply generate object itself
            is FakeCallableDescriptorForObject -> return generate(descriptor.getReferencedDescriptor())

            // For primary constructors and constructors of native classes we generate references to containing classes
            is ConstructorDescriptor -> {
                if (descriptor.isPrimary || isNativeObject(descriptor)) {
                    return generate(descriptor.containingDeclaration)
                }
            }

            // Local functions and variables are always private with their own names as suggested names
            is CallableDescriptor ->
                if (DescriptorUtils.isDescriptorWithLocalVisibility(descriptor)) {
                    val name = getMangledName(getSuggestedName(descriptor), descriptor)
                    return FQNPart(listOf(name.first), false, descriptor, descriptor.containingDeclaration)
                }
        }

        val (localName, shared, parent) = getLocalName(descriptor)
        return FQNPart(listOf(localName), shared, descriptor, fixParent(parent))
    }

    // Getters and setters have generation strategy similar to common declarations, except for they are declared as
    // members of classes/packages, not corresponding properties.
    private fun fixParent(parent: DeclarationDescriptor) = when (parent) {
        is PropertyDescriptor -> parent.containingDeclaration
        else -> parent
    }

    private fun getLocalName(descriptor: DeclarationDescriptor): LocalName {
        // Dynamic declarations always require shared names as defined in Kotlin source code
        if (descriptor.isDynamic()) {
            return LocalName(descriptor.name.asString(), true, descriptor.containingDeclaration!!)
        }

        // For any non-local declaration suggest its own suggested name and put it in scope of its containing declaration.
        // For local declaration get a sequence for names of all containing functions and join their names with '$' symbol,
        // and use container of topmost function, i.e.
        //
        //     class A {
        //         fun foo() {
        //             fun bar() {
        //                 fun baz() { ... }
        //             }
        //         }
        //     }
        //
        // `baz` gets name 'foo$bar$baz$' scoped in `A` class.
        //
        // The exception are secondary constructors which get suggested name with '_init' suffix and are put in
        // the class's parent scope.
        //
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
        } while (current is FunctionDescriptor)

        parts.reverse()
        val (id, shared) = getMangledName(parts.joinToString("$"), descriptor)
        return LocalName(id, shared, current)
    }

    private data class LocalName(val id: String, val shared: Boolean, val parent: DeclarationDescriptor)

    // For regular names suggest its string representation
    // For property accessors suggest name of a property with 'get_' and 'set_' prefixes
    // For anonymous declarations (i.e. lambdas and object expressions) suggest 'f'
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
                if (descriptor.modality == Modality.OPEN || descriptor.modality == Modality.ABSTRACT ||
                    containingDeclaration.modality == Modality.OPEN || containingDeclaration.modality == Modality.ABSTRACT
                ) {
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
