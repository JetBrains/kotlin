/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("MoveLambdaOutsideParentheses")

package org.jetbrains.kotlin.js.test.ast

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.test.ast.directives.*
import org.jetbrains.kotlin.js.testOld.utils.ArgumentsHelper
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.SensitiveDirectiveAPI
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.utils.bind

internal object JsAstDirectives : SimpleDirectivesContainer() {

    @OptIn(SensitiveDirectiveAPI::class)
    private fun <T : ArgumentsHelper> directiveWithArguments(description: String, parser: (String) -> T) =
        valueDirective(description, DirectiveApplicability.Any, splitValuesOnSpaces = false, parser = parser)

    val EXPECT_GENERATED_JS by directiveWithArguments(
        "Checks the generated JS of a specific function against the specified file",
        ::ExpectGeneratedJsDirective,
    )

    val CHECK_CONTAINS_NO_CALLS by directiveWithArguments(
        "Checks that the specified function contains no calls",
        ::CheckContainsNoCallsDirective,
    )

    val CHECK_NOT_CALLED by directiveWithArguments("Checks that the specified function is never called", ::CheckNotCalledDirective)

    val FUNCTION_CALLED_TIMES by directiveWithArguments(
        "Checks that the specified function is called the specified number of times",
        ::FunctionCalledTimesDirective,
    )

    val PROPERTY_NOT_USED by directiveWithArguments(
        "Checks that the specified property is not used in the given scope",
        { PropertyCountingDirective(it, expectedReadCount = 0, expectedWriteCount = 0) },
    )

    val PROPERTY_NOT_READ_FROM by directiveWithArguments(
        "Checks that the specified property is not read in the given scope",
        { PropertyCountingDirective(it, expectedReadCount = 0, expectedWriteCount = PropertyCountingDirective.ANY_COUNT) },
    )

    val PROPERTY_NOT_WRITTEN_TO by directiveWithArguments(
        "Checks that the specified property is not written to in the given scope",
        { PropertyCountingDirective(it, expectedReadCount = PropertyCountingDirective.ANY_COUNT, expectedWriteCount = 0) },
    )

    val PROPERTY_READ_COUNT by directiveWithArguments(
        "Checks that the specified property is read the specified number of times in the given scope",
        {
            PropertyCountingDirective(
                it,
                expectedReadCount = PropertyCountingDirective.FROM_ARGUMENT,
                expectedWriteCount = PropertyCountingDirective.ANY_COUNT,
            )
        },
    )

    val PROPERTY_WRITE_COUNT by directiveWithArguments(
        "Checks that the specified property is written the specified number of times in the given scope",
        {
            PropertyCountingDirective(
                it,
                expectedReadCount = PropertyCountingDirective.ANY_COUNT,
                expectedWriteCount = PropertyCountingDirective.FROM_ARGUMENT,
            )
        }
    )

    val CHECK_CLASS_EXISTS by directiveWithArguments(
        "Checks that the specified class exists",
        ::CheckDeclarationExistsDirective.bind(CheckDeclarationExistsDirective.DeclarationKind.CLASS),
    )

    val CHECK_FUNCTION_EXISTS by directiveWithArguments(
        "Checks that the specified function exists",
        ::CheckDeclarationExistsDirective.bind(CheckDeclarationExistsDirective.DeclarationKind.FUNCTION),
    )

    val CHECK_CALLED_IN_SCOPE by directiveWithArguments(
        "Checks that the specified function is called in the given scope",
        ::HasCallsDirective,
    )

    val CHECK_NOT_CALLED_IN_SCOPE by directiveWithArguments(
        "Checks that the specified function is not called in the given scope",
        { HasCallsDirective(it, inverted = true) },
    )

    val CHECK_COMMENT_EXISTS by directiveWithArguments(
        "Checks that the specified comment exists",
        ::CheckCommentExistsDirective,
    )

    val CHECK_LABELS_COUNT by directiveWithArguments(
        "Checks that there is the specified number of labels with this name",
        CountNodesDirective.counting<JsLabel>(),
    )

    val CHECK_VARS_COUNT by directiveWithArguments(
        "Checks that the specified number of variables with this name exist",
        CountNodesDirective.counting<JsVars.JsVar>(),
    )

    val CHECK_BREAKS_COUNT by directiveWithArguments(
        "Checks that the specified number of 'break' statements exist",
        CountNodesDirective.counting<JsBreak>(),
    )

    val CHECK_NULLS_COUNT by directiveWithArguments(
        "Checks that the specified number of 'null' literals exist",
        CountNodesDirective.counting<JsNullLiteral>(),
    )

    val CHECK_NEW_COUNT by directiveWithArguments(
        "Checks that the specified number of 'new' expressions exist",
        CountNodesDirective.counting<JsNew>(),
    )

    val CHECK_CASES_COUNT by directiveWithArguments(
        "Checks that the specified number of 'case' branches exist in 'switch'",
        CountNodesDirective.counting<JsCase>(),
    )

    val CHECK_IF_COUNT by directiveWithArguments(
        "Checks that the specified number of 'if' statements exist",
        CountNodesDirective.counting<JsIf>(),
    )

    val CHECK_SUPER_COUNT by directiveWithArguments(
        "Checks that the specified number of 'super' qualifiers exist",
        CountNodesDirective.counting<JsSuperRef>(),
    )

    val CHECK_STRING_LITERAL_COUNT by directiveWithArguments(
        "Checks that the specified string literal occurs the specified number of times",
        CountNodesDirective.counting<JsStringLiteral>(),
    )

    val HAS_NO_CAPTURED_VARS by directiveWithArguments(
        "Checks that the specified function doesn't capture any variables",
        ::CheckNoCapturedVarsDirective,
    )
}
