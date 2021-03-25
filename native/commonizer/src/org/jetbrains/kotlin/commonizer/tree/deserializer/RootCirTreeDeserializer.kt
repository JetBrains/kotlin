/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.tree.deserializer

import org.jetbrains.kotlin.commonizer.CommonizerParameters
import org.jetbrains.kotlin.commonizer.TargetProvider
import org.jetbrains.kotlin.commonizer.getCommonModuleNames
import org.jetbrains.kotlin.commonizer.mergedtree.CirFictitiousFunctionClassifiers
import org.jetbrains.kotlin.commonizer.mergedtree.CirProvidedClassifiers
import org.jetbrains.kotlin.commonizer.metadata.CirTypeResolver
import org.jetbrains.kotlin.commonizer.tree.CirTreeRoot

internal class RootCirTreeDeserializer(
    private val moduleDeserializer: ModuleCirTreeDeserializer
) {
    operator fun invoke(parameters: CommonizerParameters, targetProvider: TargetProvider): CirTreeRoot {

        val commonModuleInfos = targetProvider.modulesProvider.loadModuleInfos()
            .filter { moduleInfo -> moduleInfo.name in parameters.getCommonModuleNames() }

        val typeResolver = CirTypeResolver.create(
            providedClassifiers = CirProvidedClassifiers.of(
                CirProvidedClassifiers.by(targetProvider.modulesProvider),
                CirProvidedClassifiers.by(targetProvider.dependencyModulesProvider),
                CirProvidedClassifiers.of(
                    CirFictitiousFunctionClassifiers,
                    CirProvidedClassifiers.by(parameters.commonDependencyModulesProvider)
                )
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
