/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.testUtils

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportProblemCollector
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCTopLevel
import org.jetbrains.kotlin.backend.konan.tests.ObjCExportBaseDeclarationsTest
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver


class Fe10BaseDeclarationsGeneratorExtension : ParameterResolver {
    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.type == ObjCExportBaseDeclarationsTest.BaseDeclarationsGenerator::class.java
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return Fe10BaseDeclarationsGenerator
    }
}

private object Fe10BaseDeclarationsGenerator : ObjCExportBaseDeclarationsTest.BaseDeclarationsGenerator {
    override fun invoke(topLevelPrefix: String): List<ObjCTopLevel> {
        val translator = ObjCExportTranslatorImpl(
            generator = null,
            mapper = createObjCExportMapper(),
            namer = createObjCExportNamer(configuration = createObjCExportNamerConfiguration(topLevelNamePrefix = topLevelPrefix)),
            problemCollector = ObjCExportProblemCollector.SILENT,
            objcGenerics = true
        )

        return translator.generateBaseDeclarations()
    }
}
