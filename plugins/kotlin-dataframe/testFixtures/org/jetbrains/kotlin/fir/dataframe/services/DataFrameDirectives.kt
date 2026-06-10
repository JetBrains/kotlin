/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe.services

import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object DataFrameDirectives : SimpleDirectivesContainer() {
    val WITH_SCHEMA_READER by directive(
        description = """
        Adds library declarations for @DataSchemaSource
    """.trimIndent()
    )

    val DUMP_SCHEMAS by directive(
        description = "Whether checkers should report schemas as info warnings"
    )

    val DISABLE_TEST_UTILS by directive(
        description = "Do not add files in TestUtilsSourceProvider, helps to avoid unnecessary declarations in IR/bytecode listings"
    )

    val WITHOUT_HANDLE_EXTENSION_PROPERTY_EXCEPTIONS by directive(
        description = "Simulate older KDF runtime versions without org.jetbrains.kotlinx.dataframe.exceptions.handleExtensionPropertyException"
    )
}
