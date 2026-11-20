/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport.deprecation

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.SyntheticPropertyDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.impl.DescriptorDerivedFromTypeAlias
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.deprecation.DEPRECATED_FUNCTION_KEY
import org.jetbrains.kotlin.resolve.deprecation.DescriptorBasedDeprecationInfo
import org.jetbrains.kotlin.resolve.deprecation.isFulfilled
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedMemberDescriptor
import org.jetbrains.kotlin.storage.MemoizedFunctionToNotNull
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.utils.SmartList

@K1Deprecation
internal class DeprecationResolver(
    storageManager: StorageManager,
    private val languageVersionSettings: LanguageVersionSettings,
    private val deprecationSettings: DeprecationSettings
) {
    private data class DeprecationInfo(
        val deprecations: List<DescriptorBasedDeprecationInfo>,
        val hasInheritedDeprecations: Boolean,
        val hiddenInheritedDeprecations: List<DescriptorBasedDeprecationInfo> = emptyList()
    ) {
        companion object {
            val EMPTY = DeprecationInfo(emptyList(), hasInheritedDeprecations = false, emptyList())
        }
    }

    private val deprecations: MemoizedFunctionToNotNull<DeclarationDescriptor, DeprecationInfo> =
        storageManager.createMemoizedFunction { descriptor ->
            computeDeprecation(descriptor)
        }

    private fun computeDeprecation(descriptor: DeclarationDescriptor): DeprecationInfo {
        val deprecations = descriptor.getOwnDeprecations()
        return when {
            deprecations.isNotEmpty() -> DeprecationInfo(deprecations, hasInheritedDeprecations = false)
            descriptor is PropertyAccessorDescriptor && descriptor.correspondingProperty is SyntheticPropertyDescriptor -> {
                // This branch is necessary only for Java getters with JDKMemberStatus.NOT_CONSIDERED status
                // Currently (Mar 2024, JDK 21) we don't know such methods, but they may appear in future
                // See jawl.somethingNonExisting line (must have deprecation)
                // in the test compiler/testData/diagnostics/tests/testWithModifiedMockJdk/notConsideredGetter.kt
                val syntheticProperty = descriptor.correspondingProperty as SyntheticPropertyDescriptor
                val originalMethod =
                    if (descriptor is PropertyGetterDescriptor) syntheticProperty.getMethod else syntheticProperty.setMethod

                @Suppress("FoldInitializerAndIfToElvis") // Wait until KTIJ-26450 is fixed
                if (originalMethod == null) return DeprecationInfo.EMPTY
                val originalMethodDeprecationInfo = deprecations(originalMethod)

                // Limiting these new (they didn't exist before 1.9.10) deprecations only to WARNING and forcePropagationToOverrides
                // (i.e., for overrides of NOT_CONSIDERED JDK members)
                // is deliberate once we would like to reduce the scope of affected usages because otherwise
                // it might be a big unexpected breaking change for users who are enabled -Werror flag.
                val filteredDeprecations =
                    originalMethodDeprecationInfo.deprecations.filter {
                        it.deprecationLevel == WARNING && it.forcePropagationToOverrides
                    }
                originalMethodDeprecationInfo.copy(deprecations = filteredDeprecations)
            }
            descriptor is CallableMemberDescriptor -> {
                if (descriptor is SyntheticPropertyDescriptor && descriptor.setMethod == null &&
                    // KT-65235: hide such properties only for List.getFirst() and List.getLast()
                    descriptor.name.asString() in LIST_DEPRECATED_PROPERTIES
                ) {
                    // Here we make synthetic read-only properties equivalent to their getter in terms of deprecation.
                    // Mostly it's done because of KT-65235.
                    // About the case with read-write properties (setMethod != null), it was decided not to touch them.
                    // Normally this case should depend on a fact if we are now doing read or write (we don't know it here).
                    // Also, we don't know important use-cases with hidden read-write synthetic properties,
                    // so we decided not to break things here.
                    val getterDeprecations = descriptor.getMethod.getOwnDeprecations()
                    if (getterDeprecations.isNotEmpty()) {
                        return DeprecationInfo(getterDeprecations, hasInheritedDeprecations = false)
                    }
                }
                val inheritedDeprecations = listOfNotNull(deprecationByOverridden(descriptor))
                when (inheritedDeprecations.isNotEmpty()) {
                    true -> when (languageVersionSettings.supportsFeature(StopPropagatingDeprecationThroughOverrides)) {
                        true -> DeprecationInfo(
                            inheritedDeprecations.filter { it.forcePropagationToOverrides },
                            hasInheritedDeprecations = true,
                            inheritedDeprecations
                        )
                        false -> DeprecationInfo(inheritedDeprecations, hasInheritedDeprecations = true)
                    }
                    false -> DeprecationInfo.EMPTY
                }
            }
            else -> DeprecationInfo.EMPTY
        }
    }

    fun getDeprecations(descriptor: DeclarationDescriptor): List<DescriptorBasedDeprecationInfo> =
        deprecations(descriptor.original).deprecations

    fun isDeprecatedHidden(descriptor: DeclarationDescriptor): Boolean =
        getDeprecations(descriptor).any { it.deprecationLevel == HIDDEN }

    private fun KotlinType.deprecationsByConstituentTypes(): List<DescriptorBasedDeprecationInfo> =
        SmartList<DescriptorBasedDeprecationInfo>().also { deprecations ->
            TypeUtils.contains(this) { type ->
                type.constructor.declarationDescriptor?.let {
                    deprecations.addAll(getDeprecations(it))
                }
                false
            }
        }

    private fun deprecationByOverridden(root: CallableMemberDescriptor): DescriptorBasedDeprecationInfo? {
        val visited = HashSet<CallableMemberDescriptor>()
        val deprecations = LinkedHashSet<DescriptorBasedDeprecationInfo>()
        var hasUndeprecatedOverridden = false

        fun traverse(node: CallableMemberDescriptor) {
            if (node in visited) return

            visited.add(node)

            val deprecationsByAnnotation = node.getOwnDeprecations()
            val overriddenDescriptors = node.original.overriddenDescriptors
            when {
                deprecationsByAnnotation.isNotEmpty() -> {
                    deprecations.addAll(deprecationsByAnnotation)
                }
                overriddenDescriptors.isEmpty() -> {
                    hasUndeprecatedOverridden = true
                    return
                }
                else -> {
                    overriddenDescriptors.forEach(::traverse)
                }
            }
        }

        traverse(root)

        if (deprecations.isEmpty()) return null
        if (hasUndeprecatedOverridden && deprecations.none { it.forcePropagationToOverrides }) return null

        // We might've filtered out not-propagating deprecations already in the initializer of `deprecationsByAnnotation` in the code above.
        // But it would lead to treating Java overridden as not-deprecated at all that works controversially in case of mixed J/K override:
        // interface J {
        //      @Deprecated
        //      void foo();
        // }
        //
        // interface K {
        //      @Deprecated("")
        //      fun foo();
        // }
        //
        // class K1 : K, J {
        //      // We'd probably better treating it as deprecated
        //      // Basically, it's just a corner case and we may change the behavior if it's too annoying
        //      override fun foo() {}
        // }
        //
        // Also, we don't ignore non-propagating deprecations in case of fake overrides
        // Because we don't want to depend on the choice of the base descriptor
        if (root.kind.isReal && deprecations.none(DescriptorBasedDeprecationInfo::propagatesToOverrides)) return null

        return DeprecatedByOverridden(deprecations)
    }

    private fun DeclarationDescriptor.getOwnDeprecations(): List<DescriptorBasedDeprecationInfo> {
        // This is a temporary workaround before @DeprecatedSinceKotlin is introduced, see KT-23575
        if (shouldSkipDeprecationOnKotlinIoReadBytes(this, languageVersionSettings)) {
            return emptyList()
        }

        val result = SmartList<DescriptorBasedDeprecationInfo>()

        addDeprecationIfPresent(result)

        when (this) {
            is TypeAliasDescriptor -> expandedType.deprecationsByConstituentTypes().mapTo(result) { deprecation ->
                when (deprecation) {
                    is DeprecatedByAnnotation -> DeprecatedTypealiasByAnnotation(this, deprecation)
                    else -> deprecation
                }
            }

            is DescriptorDerivedFromTypeAlias ->
                result.addAll(typeAliasDescriptor.getOwnDeprecations())

            is PropertyAccessorDescriptor ->
                correspondingProperty.addDeprecationIfPresent(result)
        }

        return result.distinct()
    }

    private fun DeclarationDescriptor.addDeprecationIfPresent(result: MutableList<DescriptorBasedDeprecationInfo>) {
        val annotation = annotations.findAnnotation(StandardNames.FqNames.deprecated) ?: annotations.findAnnotation(JAVA_DEPRECATED)
        if (annotation != null) {
            val deprecatedByAnnotation =
                DeprecatedByAnnotation.create(
                    annotation, annotations.findAnnotation(StandardNames.FqNames.deprecatedSinceKotlin),
                    this, deprecationSettings.propagatedToOverrides(annotation),
                    languageVersionSettings.apiVersion
                )
            if (deprecatedByAnnotation != null) {
                val deprecation = when {
                    this is TypeAliasConstructorDescriptor ->
                        DeprecatedTypealiasByAnnotation(typeAliasDescriptor, deprecatedByAnnotation)

                    else -> deprecatedByAnnotation
                }
                result.add(deprecation)
            }
        }

        for (deprecation in getDeprecationByVersionRequirement(this)) {
            result.add(deprecation)
        }
        getDeprecationFromUserData(this)?.let(result::add)
    }

    private fun shouldSkipDeprecationOnKotlinIoReadBytes(
        descriptor: DeclarationDescriptor, languageVersionSettings: LanguageVersionSettings
    ): Boolean =
        descriptor.name.asString() == "readBytes" &&
                (descriptor.containingDeclaration as? PackageFragmentDescriptor)?.fqName?.asString() == "kotlin.io" &&
                descriptor is FunctionDescriptor &&
                descriptor.valueParameters.singleOrNull()?.type?.let(KotlinBuiltIns::isInt) == true &&
                languageVersionSettings.apiVersion < ApiVersion.KOTLIN_1_3

    private fun getDeprecationFromUserData(target: DeclarationDescriptor): DescriptorBasedDeprecationInfo? =
        (target as? CallableDescriptor)?.getUserData(DEPRECATED_FUNCTION_KEY)

    private fun getDeprecationByVersionRequirement(target: DeclarationDescriptor): List<DeprecatedByVersionRequirement> {
        val versionRequirements =
            (target as? DeserializedMemberDescriptor)?.versionRequirements
                ?: (target as? DeserializedClassDescriptor)?.versionRequirements
                ?: return emptyList()

        return versionRequirements.mapNotNull { versionRequirement ->
            if (!versionRequirement.isFulfilled(this.languageVersionSettings))
                DeprecatedByVersionRequirement(versionRequirement, target)
            else
                null
        }
    }

    companion object {
        val JAVA_DEPRECATED = FqName("java.lang.Deprecated")

        val LIST_DEPRECATED_PROPERTIES = listOf("first", "last")
    }
}
