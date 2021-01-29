/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.utils

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.konan.util.KlibMetadataFactories
import org.jetbrains.kotlin.library.metadata.NativeTypeTransformer
import org.jetbrains.kotlin.library.metadata.NullFlexibleTypeDeserializer
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.konan.impl.KlibResolvedModuleDescriptorsFactoryImpl
import org.jetbrains.kotlin.storage.StorageManager

internal val ModuleDescriptor.packageFragmentProvider: PackageFragmentProvider
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

internal fun MemberScope.resolveClassOrTypeAlias(relativeClassName: FqName): ClassifierDescriptorWithTypeParameters? {
    var memberScope: MemberScope = this
    if (memberScope is MemberScope.Empty)
        return null

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

internal val NativeFactories = KlibMetadataFactories(::KonanBuiltIns, NullFlexibleTypeDeserializer, NativeTypeTransformer())
