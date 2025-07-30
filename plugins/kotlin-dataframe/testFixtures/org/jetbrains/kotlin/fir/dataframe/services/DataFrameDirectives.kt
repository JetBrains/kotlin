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
}