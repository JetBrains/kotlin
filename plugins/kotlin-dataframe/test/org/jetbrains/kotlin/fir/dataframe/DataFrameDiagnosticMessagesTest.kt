/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe

import org.jetbrains.kotlin.test.utils.verifyDiagnostics
import org.jetbrains.kotlinx.dataframe.plugin.extensions.FirDataFrameErrors
import org.jetbrains.kotlinx.dataframe.plugin.extensions.ImportedSchemasDiagnostics
import org.jetbrains.kotlinx.dataframe.plugin.extensions.SchemaInfoDiagnostics
import org.junit.Test

class DataFrameDiagnosticMessagesTest {
    @Test
    fun verifyMessages() {
        verifyDiagnostics(
            SchemaInfoDiagnostics,
            FirDataFrameErrors,
            ImportedSchemasDiagnostics,
        )
    }
}
