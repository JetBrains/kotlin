/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtSdkModule
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.builder.buildImport
import org.jetbrains.kotlin.sir.builder.buildModule
import org.jetbrains.kotlin.sir.providers.SirModuleProvider
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.utils.withSirAnalyse

private const val KOTLIN_RUNTIME_MODULE_NAME: String = "KotlinRuntime"

public class SirModuleProviderImpl(
    private val ktAnalysisSession: KtAnalysisSession,
    private val sirSession: SirSession,
) : SirModuleProvider {

    private val seenModule: MutableMap<KtModule, SirModule> = mutableMapOf()

    override fun KtModule.sirModule(): SirModule = withSirAnalyse(sirSession, ktAnalysisSession) {
        seenModule.getOrPut(this@sirModule) {
            buildModule {
                name = moduleName()
                // imports should be reworked - KT-66727
                declarations += buildImport {
                    moduleName = bridgeModuleName
                }
                declarations += buildImport {
                    moduleName = KOTLIN_RUNTIME_MODULE_NAME
                }
            }.apply {
                declarations.forEach { it.parent = this }
            }
        }
    }

    // TODO: Postprocess to make sure that module name is a correct Swift name
    private fun KtModule.moduleName(): String {
        return when (this) {
            is KtSourceModule -> this.moduleName
            is KtSdkModule -> this.sdkName
            is KtLibraryModule -> this.libraryName
            else -> error(this)
        }
    }
}
