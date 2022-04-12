/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.compiler.plugins.kotlin

import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.Renderers
import org.jetbrains.kotlin.diagnostics.rendering.Renderers.RENDER_TYPE_WITH_ANNOTATIONS

class ComposeErrorMessages : DefaultErrorMessages.Extension {
    private val MAP =
        DiagnosticFactoryToRendererMap(
            "Compose"
        )
    override fun getMap() = MAP

    init {
        MAP.put(
            ComposeErrors.COMPOSABLE_INVOCATION,
            "@Composable invocations can only happen from the context of a @Composable function"
        )

        MAP.put(
            ComposeErrors.COMPOSABLE_EXPECTED,
            "Functions which invoke @Composable functions must be marked with the @Composable " +
                "annotation"
        )

        MAP.put(
            ComposeErrors.COMPOSABLE_FUNCTION_REFERENCE,
            "Function References of @Composable functions are not currently supported"
        )

        MAP.put(
            ComposeErrors.CAPTURED_COMPOSABLE_INVOCATION,
            "Composable calls are not allowed inside the {0} parameter of {1}",
            Renderers.NAME,
            Renderers.COMPACT
        )

        MAP.put(
            ComposeErrors.MISSING_DISALLOW_COMPOSABLE_CALLS_ANNOTATION,
            "Parameter {0} cannot be inlined inside of lambda argument {1} of {2} " +
                "without also being annotated with @DisallowComposableCalls",
            Renderers.NAME,
            Renderers.NAME,
            Renderers.NAME
        )

        MAP.put(
            ComposeErrors.NONREADONLY_CALL_IN_READONLY_COMPOSABLE,
            "Composables marked with @ReadOnlyComposable can only call other @ReadOnlyComposable " +
                "composables"
        )

        MAP.put(
            ComposeErrors.COMPOSABLE_PROPERTY_BACKING_FIELD,
            "Composable properties are not able to have backing fields"
        )

        MAP.put(
            ComposeErrors.CONFLICTING_OVERLOADS,
            "Conflicting overloads: {0}",
            CommonRenderers.commaSeparated(
                Renderers.FQ_NAMES_IN_TYPES_WITH_ANNOTATIONS
            )
        )

        MAP.put(
            ComposeErrors.COMPOSABLE_VAR,
            "Composable properties are not able to have backing fields"
        )
        MAP.put(
            ComposeErrors.COMPOSABLE_SUSPEND_FUN,
            "Composable function cannot be annotated as suspend"
        )
        MAP.put(
            ComposeErrors.ABSTRACT_COMPOSABLE_DEFAULT_PARAMETER_VALUE,
            "Abstract Composable functions cannot have parameters with default values"
        )
        MAP.put(
            ComposeErrors.COMPOSABLE_FUN_MAIN,
            "Composable main functions are not currently supported"
        )
        MAP.put(
            ComposeErrors.ILLEGAL_TRY_CATCH_AROUND_COMPOSABLE,
            "Try catch is not supported around composable function invocations."
        )
        MAP.put(
            ComposeErrors.TYPE_MISMATCH,
            "Type inference failed. Expected type mismatch: inferred type is {1} but {0}" +
                " was expected",
            RENDER_TYPE_WITH_ANNOTATIONS,
            RENDER_TYPE_WITH_ANNOTATIONS
        )
        MAP.put(
            ComposeErrors.COMPOSE_APPLIER_CALL_MISMATCH,
            "Calling a {0} composable function where a {1} composable was expected",
            Renderers.TO_STRING,
            Renderers.TO_STRING
        )
        MAP.put(
            ComposeErrors.COMPOSE_APPLIER_PARAMETER_MISMATCH,
            "A {0} composable parameter was provided where a {1} composable was expected",
            Renderers.TO_STRING,
            Renderers.TO_STRING
        )
        MAP.put(
            ComposeErrors.COMPOSE_APPLIER_DECLARATION_MISMATCH,
            "The composition target of an override must match the ancestor target"
        )
    }
}