/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.commonizer.LeafCommonizerTarget
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.commonizer.allLeaves
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.sources.awaitPlatformCompilations
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.utils.Future
import org.jetbrains.kotlin.gradle.utils.future
import org.jetbrains.kotlin.gradle.utils.futureExtension
import org.jetbrains.kotlin.tooling.core.UnsafeApi

internal val KotlinSourceSet.commonizerTarget: Future<CommonizerTarget?> by futureExtension {
    inferCommonizerTarget(internal.awaitPlatformCompilations())
}

internal val KotlinSourceSet.sharedCommonizerTarget: Future<SharedCommonizerTarget?>
    get() = project.future { commonizerTarget.await() as? SharedCommonizerTarget }

internal val KotlinCompilation<*>.commonizerTarget: Future<CommonizerTarget?> by futureExtension {
    KotlinPluginLifecycle.Stage.AfterFinaliseRefinesEdges.await()
    @OptIn(UnsafeApi::class)
    inferCommonizerTarget(this)
}

@UnsafeApi("Use sourceSet.commonizerTarget instead")
internal fun inferCommonizerTarget(sourceSet: KotlinSourceSet): CommonizerTarget? {
    val platformCompilations = sourceSet.internal.compilations
        .filter { compilation -> compilation !is KotlinMetadataCompilation }
    return inferCommonizerTarget(platformCompilations)
}


private fun inferCommonizerTarget(compilations: Iterable<KotlinCompilation<*>>): CommonizerTarget? {
    @OptIn(UnsafeApi::class)
    val allCompilationLeafTargets = compilations
        .filter { compilation -> compilation !is KotlinMetadataCompilation }
        .map { compilation -> inferCommonizerTarget(compilation) ?: return null }
        .allLeaves()

    return when {
        allCompilationLeafTargets.isEmpty() -> null
        allCompilationLeafTargets.size == 1 -> allCompilationLeafTargets.single()
        else -> SharedCommonizerTarget(allCompilationLeafTargets)
    }
}

@UnsafeApi("Use compilation.commonizerTarget instead")
internal fun inferCommonizerTarget(compilation: KotlinCompilation<*>): CommonizerTarget? {
    if (compilation is KotlinNativeCompilation) {
        return LeafCommonizerTarget(compilation.konanTarget)
    }

    if (compilation is KotlinSharedNativeCompilation) {
        return inferCommonizerTarget(compilation.defaultSourceSet) ?: CommonizerTarget(compilation.konanTargets)
    }

    return null
}
