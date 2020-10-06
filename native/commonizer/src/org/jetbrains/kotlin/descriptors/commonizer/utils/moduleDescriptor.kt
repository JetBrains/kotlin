/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.utils

import org.jetbrains.kotlin.backend.common.serialization.metadata.impl.ExportedForwardDeclarationsPackageFragmentDescriptor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.packageFragments
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.konan.util.KlibMetadataFactories
import org.jetbrains.kotlin.library.metadata.NativeTypeTransformer
import org.jetbrains.kotlin.library.metadata.NullFlexibleTypeDeserializer
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.konan.impl.KlibResolvedModuleDescriptorsFactoryImpl
import org.jetbrains.kotlin.storage.StorageManager

internal val ModuleDescriptor.packageFragmentProvider
    get() = (this as ModuleDescriptorImpl).packageFragmentProviderForModuleContentWithoutDependencies

internal fun createKotlinNativeForwardDeclarationsModule(
    storageManager: StorageManager,
    builtIns: KotlinBuiltIns
): ModuleDescriptorImpl =
    (NativeFactories.createDefaultKonanResolvedModuleDescriptorsFactory(NativeFactories.DefaultDeserializedDescriptorFactory) as KlibResolvedModuleDescriptorsFactoryImpl)
        .createForwardDeclarationsModule(
            builtIns = builtIns,
            storageManager = storageManager
        )

// similar to org.jetbrains.kotlin.descriptors.DescriptorUtilKt#resolveClassByFqName, but resolves also type aliases
internal fun ModuleDescriptor.resolveClassOrTypeAlias(classId: ClassId): ClassifierDescriptorWithTypeParameters? {
    val relativeClassName: FqName = classId.relativeClassName
    if (relativeClassName.isRoot)
        return null

    var memberScope: MemberScope = getPackage(classId.packageFqName).memberScope

    val classifierName = if ('.' in relativeClassName.asString()) {
        // resolve member scope of the nested class
        relativeClassName.pathSegments().reduce { first, second ->
            memberScope = (memberScope.getContributedClassifier(
                first,
                NoLookupLocation.FOR_ALREADY_TRACKED
            ) as? ClassDescriptor)?.unsubstitutedMemberScope ?: return null

            second
        }
    } else {
        relativeClassName.shortName()
    }

    return memberScope.getContributedClassifier(
        classifierName,
        NoLookupLocation.FOR_ALREADY_TRACKED
    ) as? ClassifierDescriptorWithTypeParameters
}

internal fun MutableMap<String, ModuleDescriptor?>.guessModuleByPackageFqName(packageFqName: FqName): ModuleDescriptor? {
    if (isEmpty()) return null

    val packageFqNameRaw = packageFqName.asString()
    if (containsKey(packageFqNameRaw)) {
        return this[packageFqNameRaw] // might return null if this is a previously cached result
    }

    fun guessByEnding(): ModuleDescriptor? {
        return entries
            .firstOrNull { (name, _) -> name.endsWith(packageFqNameRaw, ignoreCase = true) }
            ?.value
    }

    fun guessBySmartEnding(): ModuleDescriptor? {
        val packageFqNameFragments = packageFqNameRaw.split('.')
        if (packageFqNameFragments.size < 2) return null

        return entries.firstOrNull { (name, _) ->
            var startIndex = 0
            for (fragment in packageFqNameFragments) {
                val index = name.indexOf(fragment, startIndex = startIndex, ignoreCase = true)
                if (index < startIndex)
                    return@firstOrNull false
                else
                    startIndex = index + fragment.length
            }
            true
        }?.value
    }

    val candidate = guessByEnding() ?: guessBySmartEnding()
    this[packageFqNameRaw] = candidate // cache to speed-up the further look-ups
    return candidate
}

internal val ModuleDescriptor.hasSomethingUnderStandardKotlinPackages: Boolean
    get() {
        val packageFragmentProvider = packageFragmentProvider
        return STANDARD_KOTLIN_PACKAGE_FQNS.any { fqName ->
            packageFragmentProvider.packageFragments(fqName).any { packageFragment ->
                packageFragment !is ExportedForwardDeclarationsPackageFragmentDescriptor
                        && packageFragment.getMemberScope() != MemberScope.Empty
            }
        }
    }

internal val NativeFactories = KlibMetadataFactories(::KonanBuiltIns, NullFlexibleTypeDeserializer, NativeTypeTransformer())
