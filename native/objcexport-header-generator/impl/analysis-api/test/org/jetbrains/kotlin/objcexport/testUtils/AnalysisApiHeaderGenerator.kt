/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.testUtils

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.allDirectDependencies
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCHeader
import org.jetbrains.kotlin.backend.konan.testUtils.HeaderGenerator
import org.jetbrains.kotlin.objcexport.*
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.tooling.core.withClosure
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.io.File

class AnalysisApiHeaderGeneratorExtension : ParameterResolver {
    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.type == HeaderGenerator::class.java
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return AnalysisApiHeaderGenerator
    }
}

object AnalysisApiHeaderGenerator : HeaderGenerator {
    override fun generateHeaders(root: File, configuration: HeaderGenerator.Configuration): ObjCHeader {
        val session = createStandaloneAnalysisApiSession(
            kotlinSourceModuleName = defaultKotlinSourceModuleName,
            kotlinFiles = root.listFiles().orEmpty().filter { it.extension == "kt" },
            dependencyKlibs = configuration.dependencies
        )

        val (module, files) = session.modulesWithFiles.entries.single()
        return analyze(module) {
            val kaSession = this
            val exportedLibraries = module.withClosure<KaModule> { currentModule -> currentModule.allDirectDependencies().toList() }
                .filterIsInstance<KaLibraryModule>()
                .filter { libraryModule -> libraryModule.binaryRoots.first() in configuration.exportedDependencies }
                .toSet()

            val exportedLibraryFiles = exportedLibraries
                .flatMap { libraryModule -> libraryModule.readKtObjCExportFiles() }

            withKtObjCExportSession(
                KtObjCExportConfiguration(
                    frameworkName = configuration.frameworkName,
                ),
                moduleClassifier = { module ->
                    module == useSiteModule || module is KaLibraryModule && module in exportedLibraries
                }
            ) {
                with(ObjCExportContext(kaSession = kaSession, exportSession = this)) {
                    translateToObjCHeader(
                        files.map { it as KtFile }.map(::KtObjCExportFile) + exportedLibraryFiles,
                        withObjCBaseDeclarations = configuration.withObjCBaseDeclarationStubs
                    )
                }

            }
        }
    }
}
