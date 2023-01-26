/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

import org.jetbrains.kotlin.cli.common.isWindows
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsGenerationGranularity
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.ModuleFragmentToNameMapper
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.extension
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.safeName
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator.Companion.getJsArtifactSimpleName
import org.jetbrains.kotlin.utils.addToStdlib.runIf

class JsIrModuleToPath(private val testServices: TestServices, private val moduleKind: ModuleKind) : ModuleFragmentToNameMapper {
    override fun getNameForModule(fragment: IrModuleFragment, granularity: JsGenerationGranularity): String? {
        val path = when (granularity) {
            JsGenerationGranularity.PER_FILE ->
                "../${getJsArtifactSimpleName(testServices, fragment.safeName)}_v5/index${moduleKind.extension}"

            JsGenerationGranularity.PER_MODULE -> runIf(moduleKind == ModuleKind.ES) {
                "./${getJsArtifactSimpleName(testServices, fragment.safeName)}_v5.mjs"
            }
            else -> null
        }
        return if (isWindows) path?.minify() else path
    }
}