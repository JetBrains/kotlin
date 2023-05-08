/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin.k2

import androidx.compose.compiler.plugins.kotlin.ComposeClassIds
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCallableReferenceAccessChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirPropertyAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirFunctionTypeKindExtension
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class ComposeFirExtensionRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::ComposableFunctionTypeKindExtension
        +::ComposeFirCheckersExtension
    }
}

class ComposableFunctionTypeKindExtension(
    session: FirSession
) : FirFunctionTypeKindExtension(session) {
    override fun FunctionTypeKindRegistrar.registerKinds() {
        registerKind(ComposableFunction, KComposableFunction)
    }
}

object ComposableFunction : FunctionTypeKind(
    FqName.topLevel(Name.identifier("androidx.compose.runtime.internal")),
    "ComposableFunction",
    ComposeClassIds.Composable,
    isReflectType = false
) {
    override val prefixForTypeRender: String
        get() = "@Composable"

    override fun reflectKind(): FunctionTypeKind = KComposableFunction
}

object KComposableFunction : FunctionTypeKind(
    FqName.topLevel(Name.identifier("androidx.compose.runtime.internal")),
    "KComposableFunction",
    ComposeClassIds.Composable,
    isReflectType = true
) {
    override fun nonReflectKind(): FunctionTypeKind = ComposableFunction
}

class ComposeFirCheckersExtension(session: FirSession) : FirAdditionalCheckersExtension(session) {
    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val functionCheckers: Set<FirFunctionChecker> =
            setOf(ComposableFunctionChecker)

        override val propertyCheckers: Set<FirPropertyChecker> =
            setOf(ComposablePropertyChecker)
    }

    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
        override val functionCallCheckers: Set<FirFunctionCallChecker> =
            setOf(ComposableFunctionCallChecker)

        override val propertyAccessExpressionCheckers: Set<FirPropertyAccessExpressionChecker> =
            setOf(ComposablePropertyAccessExpressionChecker)

        override val callableReferenceAccessCheckers: Set<FirCallableReferenceAccessChecker> =
            setOf(ComposableCallableReferenceChecker)
    }
}
