/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.konan

import org.jetbrains.kotlin.commonizer.*
import org.jetbrains.kotlin.commonizer.repository.Repository
import java.io.File

internal class CopyUnconsumedModulesAsIsConsumer(
    private val repository: Repository,
    private val destination: File,
    private val targets: Set<LeafCommonizerTarget>,
    private val outputLayout: CommonizerOutputLayout,
) : ResultsConsumer {

    private val consumedTargets = mutableSetOf<LeafCommonizerTarget>()

    override fun targetConsumed(parameters: CommonizerParameters, target: CommonizerTarget) {
        if (target is LeafCommonizerTarget) {
            consumedTargets += target
        }
    }

    override fun allConsumed(parameters: CommonizerParameters, status: ResultsConsumer.Status) {
        when (status) {
            ResultsConsumer.Status.NOTHING_TO_DO -> targets.forEach { target -> copyTargetAsIs(parameters, target) }
            ResultsConsumer.Status.DONE -> targets.minus(consumedTargets).forEach { target -> copyTargetAsIs(parameters, target) }
        }
    }

    private fun copyTargetAsIs(parameters: CommonizerParameters, target: LeafCommonizerTarget) {
        val libraries = repository.getLibraries(target)
        val librariesDestination = outputLayout.getTargetDirectory(destination, target)
        librariesDestination.mkdirs() // always create an empty directory even if there is nothing to copy
        libraries.map { it.library.libraryFile.absolutePath }.map(::File).forEach { libraryFile ->
            libraryFile.copyRecursively(destination.resolve(libraryFile.name))
        }

        parameters.logger?.progress("Copied ${libraries.size} libraries for ${target.prettyName}")
    }
}
