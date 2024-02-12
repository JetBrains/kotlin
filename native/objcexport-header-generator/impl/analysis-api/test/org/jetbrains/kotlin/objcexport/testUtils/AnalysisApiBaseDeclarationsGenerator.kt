/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.testUtils

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCTopLevel
import org.jetbrains.kotlin.backend.konan.tests.ObjCExportBaseDeclarationsTest
import org.jetbrains.kotlin.objcexport.KtObjCExportConfiguration
import org.jetbrains.kotlin.objcexport.KtObjCExportSession
import org.jetbrains.kotlin.objcexport.objCBaseDeclarations
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

class AnalysisApiBaseDeclarationsGeneratorExtension : ParameterResolver {
    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.type == ObjCExportBaseDeclarationsTest.BaseDeclarationsGenerator::class.java
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return AnalysisApiBaseDeclarationsGenerator
    }
}

object AnalysisApiBaseDeclarationsGenerator : ObjCExportBaseDeclarationsTest.BaseDeclarationsGenerator {
    override fun invoke(topLevelPrefix: String): List<ObjCTopLevel> {
        return KtObjCExportSession(KtObjCExportConfiguration(topLevelPrefix)) {
            objCBaseDeclarations()
        }
    }
}