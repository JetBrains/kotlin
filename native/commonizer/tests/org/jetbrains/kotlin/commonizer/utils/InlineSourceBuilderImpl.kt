/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.utils

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.commonizer.CompiledDependency
import org.jetbrains.kotlin.commonizer.mergedtree.CirFictitiousFunctionClassifiers
import org.jetbrains.kotlin.commonizer.mergedtree.CirProvidedClassifiers
import org.jetbrains.kotlin.commonizer.metadata.CirTypeResolver
import org.jetbrains.kotlin.commonizer.tree.CirTreeModule
import org.jetbrains.kotlin.commonizer.tree.defaultCirTreeModuleDeserializer

class InlineSourceBuilderImpl(private val disposable: Disposable) : InlineSourceBuilder {
    override fun createCirTree(module: InlineSourceBuilder.Module): CirTreeModule {
        val compiledDependenciesRoot = FileUtil.createTempDirectory(module.name, null)

        val dependencyToMetadata = mutableMapOf<InlineSourceBuilder.Module, CompiledDependency>()
        val (configuration, artifact) = serializeModuleAndAllDependenciesToMetadata(
            module, disposable, dependencyToMetadata, compiledDependenciesRoot
        )
        val moduleMetadata = artifact.metadata
        val stdlibNameAndMetadata = loadStdlibMetadata(configuration)

        val classifiers = listOf(
            CirFictitiousFunctionClassifiers,
            CirProvidedClassifiers.by(MockModulesProvider.create(moduleMetadata named module.name)),
            CirProvidedClassifiers.by(MockModulesProvider.create(stdlibNameAndMetadata)),
        ) + module.dependencies.map {
            val dependencyMetadata = dependencyToMetadata[it] ?: error("Dependency metadata should have been computed recursively")
            val provider = MockModulesProvider.create(dependencyMetadata)
            CirProvidedClassifiers.by(provider)
        }

        val typeResolver = CirTypeResolver.create(
            CirProvidedClassifiers.of(*classifiers.toTypedArray())
        )

        return defaultCirTreeModuleDeserializer(moduleMetadata, typeResolver)
    }

    override fun createMetadata(module: InlineSourceBuilder.Module): NamedMetadata =
        createModule(module, disposable).second.metadata named module.name
}
