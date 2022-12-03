/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.resolve

import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useImpl
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.js.analyze.JsNativeDiagnosticSuppressor
import org.jetbrains.kotlin.js.naming.NameSuggestion
import org.jetbrains.kotlin.js.resolve.diagnostics.*
import org.jetbrains.kotlin.resolve.PlatformConfiguratorBase
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker

object WasmPlatformConfigurator : PlatformConfiguratorBase(
    additionalDeclarationCheckers = listOf(
        JsNameChecker,
        JsExternalChecker,
        JsRuntimeAnnotationChecker,
        JsExportAnnotationChecker,
        JsExportDeclarationChecker,
    ),
    additionalCallCheckers = listOf(
        JsDefinedExternallyCallChecker,
    ),
) {
    override fun configureModuleComponents(container: StorageComponentContainer) {
        container.useInstance(NameSuggestion())
        container.useImpl<JsNameClashChecker>()
        container.useImpl<JsNameCharsChecker>()
        container.useImpl<JsReflectionAPICallChecker>()
        container.useImpl<JsNativeRttiChecker>()
        container.useImpl<JsReifiedNativeChecker>()
        container.useInstance(ExtensionFunctionToExternalIsInlinable)
        container.useInstance(JsNativeDiagnosticSuppressor)
    }

    override fun configureModuleDependentCheckers(container: StorageComponentContainer) {
        super.configureModuleDependentCheckers(container)
        container.useImpl<ExpectedActualDeclarationChecker>()
    }
}
