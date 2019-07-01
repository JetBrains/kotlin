/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.jetbrains.kotlin.resolve.deprecation.CoroutineCompatibilitySupport
import org.jetbrains.kotlin.types.DynamicTypesAllowed

object JsPlatformConfigurator : PlatformConfiguratorBase(
        DynamicTypesAllowed(),
        additionalDeclarationCheckers = listOf(
                NativeInvokeChecker(), NativeGetterChecker(), NativeSetterChecker(),
                JsNameChecker, JsModuleChecker, JsExternalFileChecker,
                JsExternalChecker, JsInheritanceChecker, JsMultipleInheritanceChecker,
                JsRuntimeAnnotationChecker,
                JsDynamicDeclarationChecker
        ),
        additionalCallCheckers = listOf(
                JsModuleCallChecker,
                JsDynamicCallChecker,
                JsDefinedExternallyCallChecker
        ),
        identifierChecker = JsIdentifierChecker
) {
    override fun configureModuleComponents(container: StorageComponentContainer) {
        container.useInstance(NameSuggestion())
        container.useImpl<JsCallChecker>()
        container.useImpl<JsTypeSpecificityComparator>()
        container.useImpl<JsNameClashChecker>()
        container.useImpl<JsNameCharsChecker>()
        container.useImpl<JsBuiltinNameClashChecker>()
        container.useInstance(JsModuleClassLiteralChecker)
        container.useImpl<JsReflectionAPICallChecker>()
        container.useImpl<JsNativeRttiChecker>()
        container.useImpl<JsReifiedNativeChecker>()
        container.useInstance(ExtensionFunctionToExternalIsInlinable)
        container.useInstance(JsQualifierChecker)
        container.useInstance(JsNativeDiagnosticSuppressor)
        container.useInstance(CoroutineCompatibilitySupport.DISABLED)
    }

    override fun configureModuleDependentCheckers(container: StorageComponentContainer) {
        super.configureModuleDependentCheckers(container)
        container.useImpl<ExpectedActualDeclarationChecker>()
    }
}
