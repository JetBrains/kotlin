/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.utils

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.konan.library.KonanFactories.DefaultDeserializedDescriptorFactory
import org.jetbrains.kotlin.konan.library.KonanFactories.createDefaultKonanResolvedModuleDescriptorsFactory
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.konan.impl.KlibResolvedModuleDescriptorsFactoryImpl
import org.jetbrains.kotlin.storage.StorageManager

internal val ModuleDescriptor.packageFragmentProvider
    get() = (this as ModuleDescriptorImpl).packageFragmentProviderForModuleContentWithoutDependencies

internal fun createKotlinNativeForwardDeclarationsModule(
    storageManager: StorageManager,
    builtIns: KotlinBuiltIns
): ModuleDescriptorImpl =
    (createDefaultKonanResolvedModuleDescriptorsFactory(DefaultDeserializedDescriptorFactory) as KlibResolvedModuleDescriptorsFactoryImpl)
        .createForwardDeclarationsModule(
            builtIns = builtIns,
            storageManager = storageManager
        )

// similar to org.jetbrains.kotlin.descriptors.DescriptorUtilKt#resolveClassByFqName, but resolves also type aliases
internal fun ModuleDescriptor.resolveClassOrTypeAliasByFqName(
    fqName: FqName,
    lookupLocation: LookupLocation
): ClassifierDescriptorWithTypeParameters? {
    if (fqName.isRoot) return null

    (getPackage(fqName.parent()).memberScope.getContributedClassifier(
        fqName.shortName(),
        lookupLocation
    ) as? ClassifierDescriptorWithTypeParameters)?.let { return it }

    return (resolveClassOrTypeAliasByFqName(fqName.parent(), lookupLocation) as? ClassDescriptor)
        ?.unsubstitutedInnerClassesScope
        ?.getContributedClassifier(fqName.shortName(), lookupLocation) as? ClassifierDescriptorWithTypeParameters
}
