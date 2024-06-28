/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.descriptors

import org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isUnit

/**
 * Implementation of given method.
 *
 * TODO: this method is actually a part of resolve and probably duplicates another one
 */
@InternalKotlinNativeApi
fun <T : CallableMemberDescriptor> T.resolveFakeOverride(allowAbstract: Boolean = false): T {
    if (this.kind.isReal) {
        return this
    } else {
        val overridden = OverridingUtil.getOverriddenDeclarations(this)
        val filtered = OverridingUtil.filterOutOverridden(overridden)
        // TODO: is it correct to take first?
        @Suppress("UNCHECKED_CAST")
        return filtered.first { allowAbstract || it.modality != Modality.ABSTRACT } as T
    }
}

@InternalKotlinNativeApi
val ClassDescriptor.isArray: Boolean
    get() = this.fqNameSafe.asString() in arrayTypes


@InternalKotlinNativeApi
val ClassDescriptor.isInterface: Boolean
    get() = (this.kind == ClassKind.INTERFACE)

@InternalKotlinNativeApi
fun ClassDescriptor.isUnit() = this.defaultType.isUnit()

@InternalKotlinNativeApi
fun ClassDescriptor.isNothing() = this.defaultType.isNothing()


@InternalKotlinNativeApi
val <T : CallableMemberDescriptor> T.allOverriddenDescriptors: List<T>
    get() {
        val result = mutableListOf<T>()
        fun traverse(descriptor: T) {
            result.add(descriptor)
            @Suppress("UNCHECKED_CAST")
            descriptor.overriddenDescriptors.forEach { traverse(it as T) }
        }
        traverse(this)
        return result
    }

@InternalKotlinNativeApi
val ClassDescriptor.contributedMethods: List<FunctionDescriptor>
    get() = unsubstitutedMemberScope.contributedMethods

@InternalKotlinNativeApi
val MemberScope.contributedMethods: List<FunctionDescriptor>
    get() {
        val contributedDescriptors = this.getContributedDescriptors()

        val functions = contributedDescriptors.filterIsInstance<FunctionDescriptor>()

        val properties = contributedDescriptors.filterIsInstance<PropertyDescriptor>()
        val getters = properties.mapNotNull { it.getter }
        val setters = properties.mapNotNull { it.setter }

        return functions + getters + setters
    }

fun ClassDescriptor.isAbstract() = this.modality == Modality.SEALED || this.modality == Modality.ABSTRACT

@InternalKotlinNativeApi
val FunctionDescriptor.target: FunctionDescriptor
    get() = (if (modality == Modality.ABSTRACT) this else resolveFakeOverride()).original

@InternalKotlinNativeApi
fun DeclarationDescriptor.findPackageView(): PackageViewDescriptor {
    val packageFragment = this.findPackage()
    return packageFragment.module.getPackage(packageFragment.fqName)
}

@InternalKotlinNativeApi
fun DeclarationDescriptor.allContainingDeclarations(): List<DeclarationDescriptor> {
    var list = mutableListOf<DeclarationDescriptor>()
    var current = this.containingDeclaration
    while (current != null) {
        list.add(current)
        current = current.containingDeclaration
    }
    return list
}

private fun getPackagesFqNames(module: ModuleDescriptor): Set<FqName> {
    val result = mutableSetOf<FqName>()
    val packageFragmentProvider = (module as? ModuleDescriptorImpl)?.packageFragmentProviderForModuleContentWithoutDependencies

    fun getSubPackages(fqName: FqName) {
        result.add(fqName)
        val subPackages = packageFragmentProvider?.getSubPackagesOf(fqName) { true }
            ?: module.getSubPackagesOf(fqName) { true }
        subPackages.forEach { getSubPackages(it) }
    }

    getSubPackages(FqName.ROOT)
    return result
}

fun ModuleDescriptor.getPackageFragments(): List<PackageFragmentDescriptor> =
    getPackagesFqNames(this).flatMap {
        getPackage(it).fragments.filter { it.module == this }.toSet()
    }

val ClassDescriptor.enumEntries: List<ClassDescriptor>
    get() {
        assert(this.kind == ClassKind.ENUM_CLASS)
        return this.unsubstitutedMemberScope.getContributedDescriptors()
            .filterIsInstance<ClassDescriptor>()
            .filter { it.kind == ClassKind.ENUM_ENTRY }
    }

@InternalKotlinNativeApi
val DeclarationDescriptor.isExpectMember: Boolean
    get() = this is MemberDescriptor && this.isExpect

@InternalKotlinNativeApi
val arrayTypes = setOf(
    "kotlin.Array",
    "kotlin.ByteArray",
    "kotlin.CharArray",
    "kotlin.ShortArray",
    "kotlin.IntArray",
    "kotlin.LongArray",
    "kotlin.FloatArray",
    "kotlin.DoubleArray",
    "kotlin.BooleanArray",
    "kotlin.native.ImmutableBlob",
    "kotlin.native.internal.NativePtrArray"
)

@InternalKotlinNativeApi
val arraysWithFixedSizeItems = setOf(
    "kotlin.ByteArray",
    "kotlin.CharArray",
    "kotlin.ShortArray",
    "kotlin.IntArray",
    "kotlin.LongArray",
    "kotlin.FloatArray",
    "kotlin.DoubleArray",
    "kotlin.BooleanArray"
)