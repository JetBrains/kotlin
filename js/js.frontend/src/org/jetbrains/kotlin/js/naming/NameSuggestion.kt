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

import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils.getNameForAnnotatedObject
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils.isNativeObject
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils.isCompanionObject
import org.jetbrains.kotlin.resolve.calls.tasks.isDynamic
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.descriptorUtil.isEnumValueOfMethod
import java.util.*

/**
 * This class is responsible for generating names for declarations. It does not produce fully-qualified JS name, instead
 * it tries to generate a simple name and specify a scoping declaration. This information can be used by the front-end
 * to check whether names clash, by the code generator to place declarations to corresponding scopes and to produce
 * fully-qualified names for static declarations.
 *
 * A new instance of this class can be created for each request, however, it's recommended to use stable instance, since
 * [NameSuggestion] supports caching.
 */
class NameSuggestion {
    private val cache: MutableMap<DeclarationDescriptor, SuggestedName?> = WeakHashMap()

    /**
     * Generates names for declarations. Name consists of the following parts:
     *
     *   * Aliasing declaration, if the given `descriptor` does not have its own entity in JS.
     *   * Scoping declaration. Declarations are usually compiled to the hierarchy of nested JS objects,
     *     this attribute allows to find out where to put the declaration.
     *   * Simple name, which is a name that object must (or may) get on the generated JS.
     *   * Whether the name is stable. Stable names are visible to other modules and to native JS.
     *     Unstable names do not require particular name, so the code generator can invent any name
     *     which does not clash with anything; however, it may derive the name from the suggested name to
     *     improve readability and debugging experience.
     *
     * This method returns `null` for root declarations (modules and root packages).
     * It's guaranteed that a particular name is returned for any other declarations.
     *
     * Since packages in Kotlin do not always form hierarchy, suggested name is a list of strings. This
     * list consists of exactly one string for any declaration except for package. Package name lists
     * have at least one string.
     */
    fun suggest(descriptor: DeclarationDescriptor) = cache.getOrPut(descriptor) { generate(descriptor.original) }

    private fun generate(descriptor: DeclarationDescriptor): SuggestedName? {
        // Members of companion objects of classes are treated as static members of these classes
        if (isNativeObject(descriptor) && isCompanionObject(descriptor)) {
            return suggest(descriptor.containingDeclaration!!)
        }

        if (descriptor is FunctionDescriptor && descriptor.isSuspend) {
            descriptor.initialSignatureDescriptor?.let {
                return suggest(it)
            }
        }

        // Dynamic declarations always require stable names as defined in Kotlin source code
        if (descriptor.isDynamic()) {
            return SuggestedName(listOf(descriptor.name.asString()), true, descriptor, descriptor.containingDeclaration!!)
        }

        when (descriptor) {
            // Modules are root declarations, we don't produce declarations for them, therefore they can't clash
            is ModuleDescriptor -> return null

            is PackageFragmentDescriptor -> {
                return if (!descriptor.fqName.isRoot) {
                    SuggestedName(descriptor.fqName.pathSegments().map(Name::asString), true, descriptor,
                                  descriptor.containingDeclaration)
                }
                else {
                    // Root packages are similar to modules
                    null
                }
            }

            // It's a special case when an object has `invoke` operator defined, in this case we simply generate object itself
            is FakeCallableDescriptorForObject -> return suggest(descriptor.getReferencedObject())

            // For type alias constructor descriptors we generate references to underlying constructors
            is TypeAliasConstructorDescriptor -> return suggest(descriptor.underlyingConstructorDescriptor)

            // For primary constructors and constructors of native classes we generate references to containing classes
            is ConstructorDescriptor -> {
                if (descriptor.isPrimary || isNativeObject(descriptor)) {
                    return suggest(descriptor.containingDeclaration)
                }
            }

            // Local functions and variables are always private with their own names as suggested names
            is CallableDescriptor ->
                if (DescriptorUtils.isDescriptorWithLocalVisibility(descriptor)) {
                    val ownName = getNameForAnnotatedObject(descriptor) ?: getSuggestedName(descriptor)
                    var name = ownName
                    var scope = descriptor.containingDeclaration

                    // Local functions always lifted to the closest class or package when they are contained inside public inline function
                    if (descriptor is FunctionDescriptor) {
                        var container = descriptor.containingDeclaration
                        var liftedName = ownName
                        var hasInline = false
                        while (container is FunctionDescriptor) {
                            if (container.isInline && container.ownEffectiveVisibility.isPublicAPI) {
                                hasInline = true
                            }
                            liftedName = getSuggestedName(container) + "$" + liftedName
                            container = container.containingDeclaration
                        }
                        if (hasInline) {
                            scope = container
                            name = liftedName
                        }
                    }

                    return SuggestedName(listOf(name), false, descriptor, scope)
                }
        }

        return generateDefault(descriptor)
    }

    private fun generateDefault(descriptor: DeclarationDescriptor): SuggestedName {
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

        // For some strange reason we get FAKE_OVERRIDE for final functions called via subtype's receiver
        var current: DeclarationDescriptor = descriptor
        if (current is CallableMemberDescriptor && current.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
            val overridden = getOverridden(current) as CallableMemberDescriptor
            if (!overridden.isOverridableOrOverrides) {
                current = overridden
            }
        }
        val fixedDescriptor = current

        parts += if (fixedDescriptor is ConstructorDescriptor) {
            current = current.containingDeclaration!!
            getSuggestedName(current) + "_init"
        }
        else {
            getSuggestedName(fixedDescriptor)
        }
        if (current.containingDeclaration is FunctionDescriptor && current !is TypeParameterDescriptor) {
            val outerFunctionName = suggest(current.containingDeclaration as FunctionDescriptor)!!
            parts += outerFunctionName.names.single()
            current = outerFunctionName.scope
        }
        else {
            current = current.containingDeclaration!!
        }

        // Getters and setters have generation strategy similar to common declarations, except for they are declared as
        // members of classes/packages, not corresponding properties.
        if (current is PropertyDescriptor) {
            current = current.containingDeclaration
        }

        parts.reverse()
        val unmangledName = parts.joinToString("$")
        val (id, stable) = mangleNameIfNecessary(unmangledName, fixedDescriptor)
        return SuggestedName(listOf(id), stable, fixedDescriptor, current)
    }

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

    companion object {
        private fun mangleNameIfNecessary(baseName: String, descriptor: DeclarationDescriptor): NameAndStability {
            // If we have a callable descriptor (property or method) it can override method in a parent class.
            // Traverse to the topmost overridden method.
            // It does not matter which path to choose during traversal, since front-end must ensure
            // that names required by different overridden method do no differ.
            val overriddenDescriptor = if (descriptor is CallableDescriptor) {
                generateSequence(descriptor) { it.overriddenDescriptors.firstOrNull()?.original }.last()
            }
            else {
                descriptor
            }

            // If declaration is marked with either external, @native, @library or @JsName, return its stable name as is.
            val nativeName = getNameForAnnotatedObject(overriddenDescriptor)
            if (nativeName != null) return NameAndStability(nativeName, true)

            if (overriddenDescriptor is FunctionDescriptor) {
                when (overriddenDescriptor.fqNameUnsafe.asString()) {
                    "kotlin.CharSequence.get" -> return NameAndStability("charCodeAt", true)
                    "kotlin.Any.equals" -> return NameAndStability("equals", true)
                }
                val container = overriddenDescriptor.containingDeclaration
                if (container is ClassDescriptor && ReflectionTypes.isNumberedKPropertyOrKMutablePropertyType(container.defaultType)) {
                    val name = overriddenDescriptor.name.asString()
                    when (name) {
                        "get",
                        "set" -> return NameAndStability(name, true)
                    }
                }
            }
            else if (overriddenDescriptor is PropertyDescriptor) {
                when (overriddenDescriptor.fqNameUnsafe.asString()) {
                    "kotlin.reflect.KCallable.name" -> return NameAndStability("callableName", true)
                }
            }

            return mangleRegularNameIfNecessary(baseName, overriddenDescriptor)
        }

        private fun getOverridden(descriptor: CallableDescriptor): CallableDescriptor {
            return generateSequence(descriptor) { it.overriddenDescriptors.firstOrNull()?.original }.last()
        }

        private fun mangleRegularNameIfNecessary(baseName: String, descriptor: DeclarationDescriptor): NameAndStability {
            if (descriptor is ClassOrPackageFragmentDescriptor) {
                return NameAndStability(baseName, !DescriptorUtils.isDescriptorWithLocalVisibility(descriptor))
            }

            fun regularAndUnstable() = NameAndStability(baseName, false)

            if (descriptor !is CallableMemberDescriptor) {
                // Actually, only reified types get here, and it would be properly to put assertion here
                // However, it's better to generate wrong code than crash
                return regularAndUnstable()
            }

            fun mangledAndStable() = NameAndStability(getStableMangledName(baseName, encodeSignature(descriptor)), true)
            fun mangledInternal() = NameAndStability(getInternalMangledName(baseName, encodeSignature(descriptor)), true)
            fun mangledPrivate() = NameAndStability(getPrivateMangledName(baseName, descriptor), false)

            val effectiveVisibility = descriptor.ownEffectiveVisibility

            val containingDeclaration = descriptor.containingDeclaration
            return when (containingDeclaration) {
                is PackageFragmentDescriptor -> when {
                    effectiveVisibility.isPublicAPI -> mangledAndStable()

                    effectiveVisibility == Visibilities.INTERNAL -> mangledInternal()

                    else -> regularAndUnstable()
                }
                is ClassDescriptor -> when {
                    // valueOf() is created in the library with a mangled name for every enum class
                    descriptor is FunctionDescriptor && descriptor.isEnumValueOfMethod() -> mangledAndStable()

                    // Make all public declarations stable
                    effectiveVisibility == Visibilities.PUBLIC -> mangledAndStable()

                    descriptor.isOverridableOrOverrides -> mangledAndStable()

                    // Make all protected declarations of non-final public classes stable
                    effectiveVisibility == Visibilities.PROTECTED &&
                        !containingDeclaration.isFinalClass &&
                        containingDeclaration.visibility.isPublicAPI -> mangledAndStable()

                    effectiveVisibility == Visibilities.INTERNAL -> mangledInternal()

                    // Mangle (but make unstable) all non-public API of public classes
                    containingDeclaration.visibility.isPublicAPI && !containingDeclaration.isFinalClass -> mangledPrivate()

                    else -> regularAndUnstable()
                }
                else -> {
                    assert(containingDeclaration is CallableMemberDescriptor) {
                        "containingDeclaration for descriptor have unsupported type for mangling, " +
                        "descriptor: " + descriptor + ", containingDeclaration: " + containingDeclaration
                    }
                    regularAndUnstable()
                }
            }
        }

        data class NameAndStability(val name: String, val stable: Boolean)

        @JvmStatic fun getPrivateMangledName(baseName: String, descriptor: CallableDescriptor): String {
            val ownerName = descriptor.containingDeclaration.fqNameUnsafe.asString()
            return getStableMangledName(baseName, ownerName + ":" + encodeSignature(descriptor))
        }

        fun getInternalMangledName(suggestedName: String, forCalculateId: String): String {
            val suffix = "_${mangledId("internal:" + forCalculateId)}\$"
            return suggestedName + suffix
        }

        @JvmStatic fun getStableMangledName(suggestedName: String, forCalculateId: String): String {
            val suffix = if (forCalculateId.isEmpty()) "" else "_${mangledId(forCalculateId)}\$"
            return suggestedName + suffix
        }

        private fun mangledId(forCalculateId: String): String {
            val absHashCode = Math.abs(forCalculateId.hashCode())
            return if (absHashCode != 0) Integer.toString(absHashCode, Character.MAX_RADIX) else ""
        }

        private val DeclarationDescriptorWithVisibility.ownEffectiveVisibility
            get() = visibility.effectiveVisibility(this, checkPublishedApi = true).toVisibility()

        @JvmStatic fun sanitizeName(name: String): String {
            if (name.isEmpty()) return "_"

            val first = name.first().let { if (it.isES5IdentifierStart()) it else '_' }
            return first.toString() + name.drop(1).map { if (it.isES5IdentifierPart()) it else '_' }.joinToString("")
        }
    }
}

// See ES 5.1 spec: https://www.ecma-international.org/ecma-262/5.1/#sec-7.6
fun Char.isES5IdentifierStart() =
        Character.isLetter(this) ||   // Lu | Ll | Lt | Lm | Lo
        Character.getType(this).toByte() == Character.LETTER_NUMBER ||
        // Nl which is missing in Character.isLetter, but present in UnicodeLetter in spec
        this == '_' ||
        this == '$'

fun Char.isES5IdentifierPart() =
        isES5IdentifierStart() ||
        when (Character.getType(this).toByte()) {
            Character.NON_SPACING_MARK,
            Character.COMBINING_SPACING_MARK,
            Character.DECIMAL_DIGIT_NUMBER,
            Character.CONNECTOR_PUNCTUATION -> true
            else -> false
        } ||
        this == '\u200C' ||   // Zero-width non-joiner
        this == '\u200D'      // Zero-width joiner

fun String.isValidES5Identifier() =
        isNotEmpty() &&
        first().isES5IdentifierStart() &&
        drop(1).all { it.isES5IdentifierPart() }