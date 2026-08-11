/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.ast

import org.jetbrains.kotlin.js.test.ast.directives.CheckContainsNoCallsDirective
import org.jetbrains.kotlin.js.test.ast.directives.ExpectGeneratedJsDirective
import org.jetbrains.kotlin.js.testOld.utils.ArgumentsHelper
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

internal object JsAstDirectives : SimpleDirectivesContainer() {

    @OptIn(SensitiveDirectiveAPI::class)
    private fun <T : ArgumentsHelper> directiveWithArguments(description: String, parser: (String) -> T) =
        valueDirective(description, DirectiveApplicability.Any, splitValuesOnSpaces = false, parser = parser)

    private fun directiveWithArguments(description: String) =
        directiveWithArguments(description, ::ArgumentsHelper)

    val EXPECT_GENERATED_JS by directiveWithArguments(
        "Checks the generated JS of a specific function against the specified file",
        ::ExpectGeneratedJsDirective,
    )

    val CHECK_CONTAINS_NO_CALLS by directiveWithArguments(
        "Checks that the specified function contains no calls",
        ::CheckContainsNoCallsDirective,
    )

    val CHECK_NOT_CALLED by directiveWithArguments("Checks that the specified function is never called")

    val FUNCTION_CALLED_TIMES by directiveWithArguments("Checks that the specified function is called the specified number of times")

    val PROPERTY_NOT_USED by directiveWithArguments("Checks that the specified property is not used in the given scope")

    val PROPERTY_NOT_READ_FROM by directiveWithArguments("Checks that the specified property is not read in the given scope")

    val PROPERTY_NOT_WRITTEN_TO by directiveWithArguments("Checks that the specified property is not written to in the given scope")

    val PROPERTY_READ_COUNT by directiveWithArguments("Checks that the specified property is read the specified number of times in the given scope")

    val PROPERTY_WRITE_COUNT by directiveWithArguments("Checks that the specified property is written the specified number of times in the given scope")

    val CHECK_CLASS_EXISTS by directiveWithArguments("Checks that the specified class exists")

    val CHECK_FUNCTION_EXISTS by directiveWithArguments("Checks that the specified function exists")

    val CHECK_CALLED_IN_SCOPE by directiveWithArguments("Checks that the specified function is called in the given scope")

    val CHECK_NOT_CALLED_IN_SCOPE by directiveWithArguments("Checks that the specified function is not called in the given scope")

    val CHECK_COMMENT_EXISTS by directiveWithArguments("Checks that the specified comment exists")

    val CHECK_LABELS_COUNT by directiveWithArguments("Checks that there is the specified number of labels with this name")

    val CHECK_VARS_COUNT by directiveWithArguments("Checks that the specified number of variables with this name exist")

    val CHECK_BREAKS_COUNT by directiveWithArguments("Checks that the specified number of 'break' statements exist")

    val CHECK_NULLS_COUNT by directiveWithArguments("Checks that the specified number of 'null' literals exist")

    val CHECK_NEW_COUNT by directiveWithArguments("Checks that the specified number of 'new' expressions exist")

    val CHECK_CASES_COUNT by directiveWithArguments("Checks that the specified number of 'case' branches exist in 'switch'")

    val CHECK_IF_COUNT by directiveWithArguments("Checks that the specified number of 'if' statements exist")

    val CHECK_SUPER_COUNT by directiveWithArguments("Checks that the specified number of 'super' qualifiers exist")

    val CHECK_STRING_LITERAL_COUNT by directiveWithArguments("Checks that the specified string literal occurs the specified number of times")

    val CHECK_NOT_REFERENCED by directiveWithArguments("Checks that the specified function is never referenced")

    val HAS_NO_CAPTURED_VARS by directiveWithArguments("Checks that the specified function doesn't capture any variables")
}
