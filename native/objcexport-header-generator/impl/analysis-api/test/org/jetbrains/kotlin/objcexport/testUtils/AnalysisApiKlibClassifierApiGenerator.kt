/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.testUtils

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.export.utilities.isCompanion
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.allDirectDependencies
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolVisibility
import org.jetbrains.kotlin.analysis.api.symbols.typeParameters
import org.jetbrains.kotlin.backend.konan.testUtils.KlibClassifierApiGenerator
import org.jetbrains.kotlin.export.test.createStandaloneAnalysisApiSession
import org.jetbrains.kotlin.export.test.defaultKotlinSourceModuleName
import org.jetbrains.kotlin.objcexport.KtObjCExportFile
import org.jetbrains.kotlin.objcexport.readKtObjCExportFiles
import org.jetbrains.kotlin.tooling.core.withClosure
import java.nio.file.Path

object AnalysisApiKlibClassifierApiGenerator : KlibClassifierApiGenerator {
    override fun generate(dependencies: List<Path>): String {
        val session = createStandaloneAnalysisApiSession(
            kotlinSourceModuleName = defaultKotlinSourceModuleName,
            kotlinFiles = emptyList(),
            //kotlinFiles = root.listFiles().orEmpty().filter { it.extension == "kt" },
            dependencyKlibs = dependencies
        )

        val (module, files) = session.modulesWithFiles.entries.single()

        return analyze(module) {
            val kaSession = this
            val exportedLibraries = module.withClosure<KaModule> { currentModule -> currentModule.allDirectDependencies().toList() }
                .filterIsInstance<KaLibraryModule>()
                .filter { libraryModule -> libraryModule.binaryRoots.first() in dependencies }
                .toSet()

            val exportedLibraryFiles = exportedLibraries
                .flatMap { libraryModule -> libraryModule.readKtObjCExportFiles() }

            generateClassifierApi(exportedLibraryFiles, kaSession)
        }
    }

    @OptIn(KaExperimentalApi::class)
    fun generateClassifierApi(
        files: List<KtObjCExportFile>,
        analysisSession: KaSession,
    ): String {
        val imports = mutableSetOf<String>()
        val functions = mutableSetOf<String>()
        files.forEach { file ->
            with(file) {
                val file = analysisSession.resolve()
                file.classifierSymbols.forEach { classSymbol ->
                    val isAnnotation = classSymbol.classKind == KaClassKind.ANNOTATION_CLASS
                    if (classSymbol.visibility == KaSymbolVisibility.PUBLIC && !classSymbol.isCompanion && !isAnnotation) {
                        val pgk = classSymbol.classId?.packageFqName?.asString()
                        val className = classSymbol.classId?.asFqNameString()

                        val typeParams = if (classSymbol.typeParameters.isEmpty()) "" else {
                            "<" + classSymbol.typeParameters.map { "*" }.joinToString(", ") + ">"
                        }

                        imports.add("import $pgk.*")
                        functions.add("fun return_${className?.replace(".", "_")}(): $className$typeParams = null!!")
                    }

                }
            }
        }

        return buildString {
            append(imports.sorted().joinToString("\n"))
            append("\n\n")
            append(functions.sorted().joinToString("\n"))
        }
    }
}

//@OptIn
//fun generate(): Foo<*> = null!!

//class Foo<T>