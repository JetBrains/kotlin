/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.assignment.plugin

import org.jetbrains.kotlin.assignment.plugin.k2.diagnostics.FirErrorsAssignmentPlugin
import org.jetbrains.kotlin.test.utils.verifyDiagnostics
import org.junit.jupiter.api.Test

class AssignmentPluginDiagnosticMessagesTest {
    @Test
    fun verifyMessages() {
        verifyDiagnostics(FirErrorsAssignmentPlugin)
    }
}
