/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.js.resolve.diagnostics.JsCallChecker
import org.jetbrains.kotlin.js.resolve.diagnostics.NativeInnerClassChecker
import org.jetbrains.kotlin.resolve.IdentifierChecker
import org.jetbrains.kotlin.resolve.OverloadFilter
import org.jetbrains.kotlin.resolve.PlatformConfigurator
import org.jetbrains.kotlin.resolve.scopes.SyntheticScopes
import org.jetbrains.kotlin.types.DynamicTypesAllowed

object JsPlatformConfigurator : PlatformConfigurator(
        DynamicTypesAllowed(),
        additionalDeclarationCheckers = listOf(NativeInvokeChecker(), NativeGetterChecker(), NativeSetterChecker(),
                                               NativeInnerClassChecker()),
        additionalCallCheckers = listOf(),
        additionalTypeCheckers = listOf(),
        additionalSymbolUsageValidators = listOf(),
        additionalAnnotationCheckers = listOf(),
        identifierChecker = IdentifierChecker.DEFAULT,
        overloadFilter = OverloadFilter.DEFAULT
) {
    override fun configure(container: StorageComponentContainer) {
        super.configure(container)

        container.useImpl<JsCallChecker>()
        container.useInstance(SyntheticScopes.Empty)
        container.useInstance(JsTypeSpecificityComparator)
    }
}
