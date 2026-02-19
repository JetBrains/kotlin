/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.tsexport

import org.jetbrains.kotlin.js.artifacts.CachedTestFunctionsWithTheirPackage
import org.jetbrains.kotlin.js.artifacts.JsArtifactProducer
import org.jetbrains.kotlin.js.artifacts.PerFileGenerator

private typealias TsModules = JsArtifactProducer.ArtifactModules<TypeScriptModuleArtifact>

internal object TsArtifactProducer : JsArtifactProducer<ProcessedModule, FileArtifactKey, TypeScriptModuleArtifact, Nothing> {
    override fun singleModuleToArtifact(
        module: ProcessedModule,
        mainModule: ProcessedModule,
    ): TypeScriptModuleArtifact = TypeScriptModuleArtifact(
        externalModuleName = module.jsOutputName,
        exportModel = module.declarationsGroupedByFile.values.flatten()
    )

    override fun makePerFileGenerator(mainModule: ProcessedModule): PerFileGenerator<ProcessedModule, FileArtifactKey, TsModules, Nothing> {
        return object : PerFileGenerator<ProcessedModule, FileArtifactKey, TsModules, Nothing> {
            override val mainModuleName: String
                get() = "" // Not needed for TypeScript generation

            override val ProcessedModule.isMain: Boolean
                get() = library == mainModule.library

            override val ProcessedModule.fileList: Iterable<FileArtifactKey>
                get() = declarationsGroupedByFile.keys

            override val TsModules.artifactName: String
                get() = this.mainModule.externalModuleName

            override val TsModules.hasEffect: Boolean
                get() = false // TODO: Required for generating the proxy artifact

            override val TsModules.hasExport: Boolean
                get() = true

            override val TsModules.packageFqn: String
                get() = this.mainModule.packageFqn!!

            override val TsModules.mainFunction: String?
                get() = null // TODO: Required for generating the proxy artifact

            override fun TsModules.takeTestEnvironmentOwnership(): Nothing? = null

            override val Nothing.testFunctionTag: String
                get() = this

            override val Nothing.suiteFunctionTag: String
                get() = this

            override fun List<TsModules>.merge(): TsModules = single() // TypeScript artifacts should already be merged

            override fun FileArtifactKey.generateArtifact(module: ProcessedModule): TsModules? {
                val exportModel = module.declarationsGroupedByFile[this] ?: return null
                return TsModules(
                    TypeScriptModuleArtifact(
                        externalModuleName = getExternalModuleName(module),
                        exportModel,
                        packageFqn = packageFqName.asString(),
                    ),
                )
            }

            private fun FileArtifactKey.getExternalModuleName(module: ProcessedModule): String = buildString {
                append(module.jsOutputName)
                append("/")
                val prefix = packageFqName.asString().replace('.', '/')
                append(prefix)
                if (prefix.isNotEmpty()) {
                    append("/")
                }
                append(fileName)
            }

            override fun ProcessedModule.generateProxyArtifact(
                mainFunctionTag: String?,
                suiteFunctionTag: String?,
                testFunctions: CachedTestFunctionsWithTheirPackage,
                moduleNameForEffects: String?,
            ): TsModules {
                TODO("Not yet implemented")
            }
        }
    }
}