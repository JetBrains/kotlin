/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.resolve

import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useImpl
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.js.naming.NameSuggestion
import org.jetbrains.kotlin.js.resolve.ExtensionFunctionToExternalIsInlinable
import org.jetbrains.kotlin.js.resolve.diagnostics.*
import org.jetbrains.kotlin.resolve.PlatformConfiguratorBase
import org.jetbrains.kotlin.resolve.calls.checkers.LateinitIntrinsicApplicabilityChecker
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker
import org.jetbrains.kotlin.wasm.analyze.WasmDiagnosticSuppressor
import org.jetbrains.kotlin.wasm.resolve.diagnostics.*

// TODO: Review the list of used K/JS checkers.
//       Refactor useful checkers into common module.
object WasmPlatformConfigurator : PlatformConfiguratorBase(
    additionalDeclarationCheckers = listOf(
        JsNameChecker, JsModuleChecker, JsExternalFileChecker,
        JsExternalChecker, WasmExternalInheritanceChecker,
        JsRuntimeAnnotationChecker,
        JsExportAnnotationChecker,
        JsExportDeclarationChecker,
        WasmExternalDeclarationChecker,
        WasmImportAnnotationChecker,
        WasmJsFunAnnotationChecker,
        WasmJsInteropTypesChecker,
    ),
    additionalCallCheckers = listOf(
        JsModuleCallChecker,
        JsDefinedExternallyCallChecker,
        LateinitIntrinsicApplicabilityChecker(isWarningInPre19 = true)
    ),
) {
    override fun configureModuleComponents(container: StorageComponentContainer) {
        container.useInstance(NameSuggestion())
        container.useImpl<WasmJsCallChecker>()
        container.useImpl<JsNameClashChecker>()
        container.useImpl<JsNameCharsChecker>()
        container.useInstance(JsModuleClassLiteralChecker)
        container.useImpl<JsReflectionAPICallChecker>()
        container.useImpl<JsNativeRttiChecker>()
        container.useImpl<JsReifiedNativeChecker>()
        container.useInstance(ExtensionFunctionToExternalIsInlinable)
        container.useInstance(JsQualifierChecker)
        container.useInstance(WasmDiagnosticSuppressor)
    }

    override fun configureModuleDependentCheckers(container: StorageComponentContainer) {
        super.configureModuleDependentCheckers(container)
        container.useImpl<ExpectedActualDeclarationChecker>()
    }
}
