/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.testUtils

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCHeader
import org.jetbrains.kotlin.backend.konan.testUtils.HeaderGenerator
import org.jetbrains.kotlin.objcexport.KtObjCExportConfiguration
import org.jetbrains.kotlin.objcexport.KtObjCExportSession
import org.jetbrains.kotlin.objcexport.translateToObjCHeader
import org.jetbrains.kotlin.psi.KtFile
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
        val session = createStandaloneAnalysisApiSession(root.listFiles().orEmpty().filter { it.extension == "kt" })
        val (module, files) = session.modulesWithFiles.entries.single()
        return analyze(module) {
            KtObjCExportSession(
                KtObjCExportConfiguration(
                    frameworkName = configuration.frameworkName,
                    generateBaseDeclarationStubs = configuration.generateBaseDeclarationStubs
                )
            ) {
                translateToObjCHeader(files.map { it as KtFile })
            }
        }
    }
}
