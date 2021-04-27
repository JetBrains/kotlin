/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.tree.deserializer

import org.jetbrains.kotlin.commonizer.*
import org.jetbrains.kotlin.commonizer.mergedtree.CirProvidedClassifiers
import org.jetbrains.kotlin.commonizer.metadata.CirTypeResolver
import org.jetbrains.kotlin.commonizer.tree.CirTreeRoot

internal class RootCirTreeDeserializer(
    private val moduleDeserializer: CirTreeModuleDeserializer
) {
    operator fun invoke(parameters: CommonizerParameters, targetProvider: TargetProvider): CirTreeRoot {

        val commonModuleNames = parameters.commonModuleNames(targetProvider)

        val commonModuleInfos = targetProvider.modulesProvider.loadModuleInfos()
            .filter { moduleInfo -> moduleInfo.name in commonModuleNames }

        val typeResolver = CirTypeResolver.create(
            providedClassifiers = CirProvidedClassifiers.of(
                CirProvidedClassifiers.by(targetProvider.modulesProvider),
                parameters.dependencyClassifiers(targetProvider.target)
            )
        )

        return CirTreeRoot(
            modules = commonModuleInfos.map { moduleInfo ->
                val metadata = targetProvider.modulesProvider.loadModuleMetadata(moduleInfo.name)
                moduleDeserializer(metadata, typeResolver)
            }
        )
    }
}

