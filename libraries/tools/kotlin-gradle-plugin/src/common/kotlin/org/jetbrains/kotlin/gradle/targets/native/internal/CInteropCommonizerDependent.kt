/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.associateWithClosure
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropIdentifier.Scope
import org.jetbrains.kotlin.tooling.core.UnsafeApi

internal class CInteropCommonizerDependent @UnsafeApi constructor(
    val target: SharedCommonizerTarget,
    val scopes: Set<Scope>,
    val interops: Set<CInteropIdentifier>
) {

    override fun equals(other: Any?): Boolean {
        if (other !is CInteropCommonizerDependent) return false
        if (this.target != other.target) return false
        if (this.scopes != other.scopes) return false
        if (this.interops != other.interops) return false
        return true
    }

    override fun hashCode(): Int {
        var result = target.hashCode()
        result = 31 * result + scopes.hashCode()
        result = 31 * result + interops.hashCode()
        return result
    }

    init {
        require(target.targets.isNotEmpty()) { "CInteropCommonizerDependent.target.targets.size can't be empty" }
        require(scopes.isNotEmpty()) { "CInteropCommonizerDependent.scopes can't be empty" }
        require(interops.isNotEmpty()) { "CInteropCommonizerDependent.interops can't be empty" }
    }

    companion object Factory
}

@OptIn(UnsafeApi::class)
internal fun CInteropCommonizerDependent.Factory.from(
    target: SharedCommonizerTarget,
    compilations: Set<KotlinNativeCompilation>
): CInteropCommonizerDependent? {
    target.targets.ifEmpty { return null }

    /*
     Filter out compilations that have their associate also in the set of compilations
     e.g. do not include '*test' if their main counterpart is also present.
     *test and *main compilations will be included when build authors declare a *Test dependsOn *Main source set relationship.
     This relationship should not be declared, but we try to be lenient towards it here.
      */
    val filteredCompilations = compilations.filter { compilation ->
        compilation.associateWithClosure.none { associateCompilation -> associateCompilation in compilations }
    }.ifEmpty { return null }.toSet()

    val scopes: Set<Scope> = filteredCompilations
        .map { compilation -> Scope.create(compilation) }.toSet()
        .ifEmpty { return null }

    val interops: Set<CInteropIdentifier> = filteredCompilations
        .flatMap { compilation -> compilation.cinterops.ifEmpty { return null } }
        .map { cinterop -> cinterop.identifier }.toSet()

    return CInteropCommonizerDependent(target, scopes, interops)
}

internal suspend fun CInteropCommonizerDependent.Factory.from(compilation: KotlinSharedNativeCompilation): CInteropCommonizerDependent? {
    return from(
        compilation.commonizerTarget.await() as? SharedCommonizerTarget ?: return null,
        compilation.getImplicitlyDependingNativeCompilations()
    )
}

internal suspend fun CInteropCommonizerDependent.Factory.from(sourceSet: KotlinSourceSet): CInteropCommonizerDependent? {
    return from(
        target = sourceSet.commonizerTarget.await() as? SharedCommonizerTarget ?: return null,
        compilations = sourceSet.internal.compilations
            .filterIsInstance<KotlinNativeCompilation>().toSet()
    )
}

internal suspend fun CInteropCommonizerDependent.Factory.fromAssociateCompilations(sourceSet: KotlinSourceSet): CInteropCommonizerDependent? {
    return from(
        target = sourceSet.commonizerTarget.await() as? SharedCommonizerTarget ?: return null,
        compilations = sourceSet.internal.compilations
            .filterIsInstance<KotlinNativeCompilation>()
            .flatMap { compilation -> compilation.associateWithClosure }
            .filterIsInstance<KotlinNativeCompilation>()
            .toSet()
    )
}
