/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.konan

import org.jetbrains.kotlin.descriptors.commonizer.*
import org.jetbrains.kotlin.descriptors.commonizer.repository.Repository
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.util.Logger
import java.io.File

internal class CopyUnconsumedModulesAsIsConsumer(
    private val repository: Repository,
    private val destination: File,
    private val targets: Set<KonanTarget>,
    private val outputLayout: CommonizerOutputLayout,
    private val logger: Logger? = null
) : ResultsConsumer {

    private val consumedTargets = mutableSetOf<KonanTarget>()

    override fun targetConsumed(target: CommonizerTarget) {
        if (target is LeafCommonizerTarget) {
            consumedTargets.add(target.konanTarget)
        }
    }

    override fun allConsumed(status: ResultsConsumer.Status) {
        when (status) {
            ResultsConsumer.Status.NOTHING_TO_DO -> targets.forEach(::copyTargetAsIs)
            ResultsConsumer.Status.DONE -> targets.minus(consumedTargets).forEach(::copyTargetAsIs)
        }
    }

    private fun copyTargetAsIs(target: KonanTarget) {
        val commonizerTarget = CommonizerTarget(target)
        val libraries = repository.getLibraries(commonizerTarget)
        val librariesDestination = outputLayout.getTargetDirectory(destination, commonizerTarget)
        librariesDestination.mkdirs() // always create an empty directory even if there is nothing to copy
        libraries.map { it.library.libraryFile.absolutePath }.map(::File).forEach { libraryFile ->
            libraryFile.copyRecursively(destination.resolve(libraryFile.name))
        }

        logger?.log("Copied ${libraries.size} libraries for ${commonizerTarget.prettyName}")
    }
}
