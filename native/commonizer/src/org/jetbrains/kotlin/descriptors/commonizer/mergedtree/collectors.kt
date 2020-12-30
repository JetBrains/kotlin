/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import org.jetbrains.kotlin.backend.common.serialization.metadata.impl.ClassifierAliasingPackageFragmentDescriptor
import org.jetbrains.kotlin.backend.common.serialization.metadata.impl.ExportedForwardDeclarationsPackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.utils.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.scopes.ChainedMemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.utils.alwaysTrue

internal fun MemberScope.collectMembers(vararg collectors: (DeclarationDescriptor) -> Boolean) =
    getContributedDescriptors().forEach { member ->
        collectors.any { it(member) }
                // each member must be consumed, otherwise - error
                || error("Unhandled member declaration: $member")
    }

@Suppress("FunctionName")
private inline fun <reified T : DeclarationDescriptor> Collector(
    crossinline typedCollector: (T) -> Unit
): (DeclarationDescriptor) -> Boolean = { candidate ->
    if (candidate is T) {
        typedCollector(candidate)
        true
    } else
        false
}

@Suppress("FunctionName")
internal inline fun ClassCollector(
    crossinline typedCollector: (ClassDescriptor) -> Unit
): (DeclarationDescriptor) -> Boolean = Collector(typedCollector)

@Suppress("FunctionName")
internal inline fun TypeAliasCollector(
    crossinline typedCollector: (TypeAliasDescriptor) -> Unit
): (DeclarationDescriptor) -> Boolean = Collector(typedCollector)

@Suppress("FunctionName")
internal inline fun PropertyCollector(
    crossinline typedCollector: (PropertyDescriptor) -> Unit
): (DeclarationDescriptor) -> Boolean = Collector<PropertyDescriptor> { candidate ->
    if (candidate.kind.isReal) // omit fake overrides
        typedCollector(candidate)
}

@Suppress("FunctionName")
internal inline fun FunctionCollector(
    crossinline typedCollector: (SimpleFunctionDescriptor) -> Unit
): (DeclarationDescriptor) -> Boolean = Collector<SimpleFunctionDescriptor> { candidate ->
    if (candidate.kind.isReal
        && !candidate.isKniBridgeFunction()
        && !candidate.isDeprecatedTopLevelFunction()
        && !candidate.isIgnoredDarwinFunction()
    ) {
        typedCollector(candidate)
    }
}

// collects member scopes for every non-empty package provided by this module
internal fun ModuleDescriptor.collectNonEmptyPackageMemberScopes(
    probeRootPackageForEmptiness: Boolean = false, // false is the default as probing might be expensive and is not always necessary
    collector: (FqName, MemberScope) -> Unit
) {
    // we don's need to process fragments from other modules which are the dependencies of this module, so
    // let's use the appropriate package fragment provider
    val packageFragmentProvider = this.packageFragmentProvider

    fun recurse(packageFqName: FqName) {
        val probeForEmptiness = probeRootPackageForEmptiness && packageFqName.isRoot

        val ownPackageMemberScopes = packageFragmentProvider.packageFragments(packageFqName)
            .asSequence()
            .filter { it !is ExportedForwardDeclarationsPackageFragmentDescriptor && it !is ClassifierAliasingPackageFragmentDescriptor }
            .map { it.getMemberScope() }
            .filter { it != MemberScope.Empty }
            .filter { !probeForEmptiness || it.getContributedDescriptors().isNotEmpty() }
            .toList()

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
