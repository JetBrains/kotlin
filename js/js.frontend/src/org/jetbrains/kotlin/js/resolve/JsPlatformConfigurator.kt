/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.resolve

import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useImpl
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.js.analyze.JsNativeDiagnosticSuppressor
import org.jetbrains.kotlin.js.naming.JsNameSuggestion
import org.jetbrains.kotlin.js.naming.NameSuggestion
import org.jetbrains.kotlin.js.resolve.diagnostics.*
import org.jetbrains.kotlin.resolve.PlatformConfiguratorBase
import org.jetbrains.kotlin.resolve.calls.checkers.LateinitIntrinsicApplicabilityChecker
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker
import org.jetbrains.kotlin.types.DynamicTypesAllowed

object JsPlatformConfigurator : PlatformConfiguratorBase(
    DynamicTypesAllowed(),
    additionalDeclarationCheckers = listOf(
        NativeInvokeChecker(), NativeGetterChecker(), NativeSetterChecker(),
        JsNameChecker, JsModuleChecker, JsExternalFileChecker,
        JsExternalChecker, JsInheritanceChecker, JsMultipleInheritanceChecker,
        JsExternalInheritorOnlyChecker,
        JsRuntimeAnnotationChecker,
        JsDynamicDeclarationChecker,
        JsExportAnnotationChecker,
    ),
    additionalCallCheckers = listOf(
        JsModuleCallChecker,
        JsDynamicCallChecker,
        JsDefinedExternallyCallChecker,
        LateinitIntrinsicApplicabilityChecker(isWarningInPre19 = true),
        JsExternalArgumentCallChecker
    ),
) {
    override fun configureModuleComponents(container: StorageComponentContainer) {
        container.useInstance(JsNameSuggestion())
        container.useImpl<JsCallChecker>()
        container.useImpl<JsTypeSpecificityComparator>()
        container.useImpl<JsNameClashChecker>()
        container.useImpl<JsIdentifierChecker>()
        container.useImpl<JsNameCharsChecker>()
        container.useImpl<JsBuiltinNameClashChecker>()
        container.useInstance(JsModuleClassLiteralChecker)
        container.useImpl<JsReflectionAPICallChecker>()
        container.useImpl<JsNativeRttiChecker>()
        container.useImpl<JsReifiedNativeChecker>()
        container.useInstance(ExtensionFunctionToExternalIsInlinable)
        container.useInstance(JsQualifierChecker)
        container.useInstance(JsNativeDiagnosticSuppressor)
        container.useInstance(JsExportDeclarationChecker(includeUnsignedNumbers = false))
    }

    override fun configureModuleDependentCheckers(container: StorageComponentContainer) {
        super.configureModuleDependentCheckers(container)
        container.useImpl<ExpectedActualDeclarationChecker>()
    }
}
