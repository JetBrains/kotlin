/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import org.jetbrains.kotlin.backend.common.serialization.metadata.impl.ExportedForwardDeclarationsPackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.utils.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.scopes.ChainedMemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.alwaysTrue

internal fun mergeModules(
    storageManager: StorageManager,
    cacheRW: CirRootNode.ClassifiersCacheImpl,
    modules: List<ModuleDescriptor?>
): CirModuleNode {
    val node = buildModuleNode(storageManager, modules)

    val packageMemberScopesMap = CommonizedGroupMap<FqName, MemberScope>(modules.size)

    modules.forEachIndexed { index, module ->
        module?.collectNonEmptyPackageMemberScopes { packageFqName, memberScope ->
            packageMemberScopesMap[packageFqName.intern()][index] = memberScope
        }
    }

    val moduleName = modules.firstNonNull().name.intern()
    for ((packageFqName, packageMemberScopesGroup) in packageMemberScopesMap) {
        node.packages += mergePackages(storageManager, cacheRW, moduleName, packageFqName, packageMemberScopesGroup.toList())
    }

    return node
}

// collects member scopes for every non-empty package provided by this module
internal fun ModuleDescriptor.collectNonEmptyPackageMemberScopes(collector: (FqName, MemberScope) -> Unit) {
    // we don's need to process fragments from other modules which are the dependencies of this module, so
    // let's use the appropriate package fragment provider
    val packageFragmentProvider = this.packageFragmentProvider

    fun recurse(packageFqName: FqName) {
        if (packageFqName.isUnderStandardKotlinPackages || packageFqName.isUnderKotlinNativeSyntheticPackages)
            return

        val ownPackageFragments = packageFragmentProvider.getPackageFragments(packageFqName)
        val ownPackageMemberScopes = ownPackageFragments.asSequence()
            .filter { it !is ExportedForwardDeclarationsPackageFragmentDescriptor }
            .map { it.getMemberScope() }
            .filter { it != MemberScope.Empty }
            .toList(ownPackageFragments.size)

        if (ownPackageMemberScopes.isNotEmpty()) {
            // don't include subpackages into chained member scope
            val memberScope = ChainedMemberScope.create(
                "package member scope for $packageFqName in $name",
                ownPackageMemberScopes
            )
            collector(packageFqName, memberScope)
        }

        packageFragmentProvider.getSubPackagesOf(packageFqName, alwaysTrue()).toSet().map { recurse(it) }
    }

    recurse(FqName.ROOT)
}
