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

import org.jetbrains.kotlin.builtins.PlatformToKotlinClassMap
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useImpl
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.js.analyze.JsNativeDiagnosticSuppressor
import org.jetbrains.kotlin.js.naming.NameSuggestion
import org.jetbrains.kotlin.js.resolve.diagnostics.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.checkers.ReifiedTypeParameterSubstitutionChecker
import org.jetbrains.kotlin.resolve.calls.components.SamConversionTransformer
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker
import org.jetbrains.kotlin.resolve.lazy.DelegationFilter
import org.jetbrains.kotlin.resolve.scopes.SyntheticScopes
import org.jetbrains.kotlin.types.DynamicTypesAllowed

object JsPlatformConfigurator : PlatformConfigurator(
        DynamicTypesAllowed(),
        additionalDeclarationCheckers = listOf(
                NativeInvokeChecker(), NativeGetterChecker(), NativeSetterChecker(),
                JsNameChecker, JsModuleChecker, JsExternalFileChecker,
                JsExternalChecker, JsInheritanceChecker, JsMultipleInheritanceChecker,
                JsRuntimeAnnotationChecker,
                JsDynamicDeclarationChecker,
                ExpectedActualDeclarationChecker
        ),
        additionalCallCheckers = listOf(
                ReifiedTypeParameterSubstitutionChecker(),
                JsModuleCallChecker,
                JsDynamicCallChecker,
                JsDefinedExternallyCallChecker
        ),
        additionalTypeCheckers = listOf(),
        additionalClassifierUsageCheckers = listOf(),
        additionalAnnotationCheckers = listOf(),
        identifierChecker = JsIdentifierChecker,
        overloadFilter = OverloadFilter.Default,
        platformToKotlinClassMap = PlatformToKotlinClassMap.EMPTY,
        delegationFilter = DelegationFilter.Default,
        overridesBackwardCompatibilityHelper = OverridesBackwardCompatibilityHelper.Default,
        declarationReturnTypeSanitizer = DeclarationReturnTypeSanitizer.Default
) {
    override fun configureModuleComponents(container: StorageComponentContainer) {
        container.useInstance(NameSuggestion())
        container.useImpl<JsCallChecker>()
        container.useInstance(SyntheticScopes.Empty)
        container.useInstance(SamConversionTransformer.Empty)
        container.useInstance(JsTypeSpecificityComparator)
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
}
