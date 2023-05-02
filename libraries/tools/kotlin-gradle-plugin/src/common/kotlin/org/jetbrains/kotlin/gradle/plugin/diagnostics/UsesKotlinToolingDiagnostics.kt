/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal

internal interface UsesKotlinToolingDiagnostics : Task {
    @get:Internal
    val toolingDiagnosticsCollector: Property<KotlinToolingDiagnosticsCollector>

    @get:Internal
    val diagnosticRenderingOptions: Property<ToolingDiagnosticRenderingOptions>

    fun reportDiagnostic(diagnostic: ToolingDiagnostic) {
        toolingDiagnosticsCollector.get().report(this, diagnostic)
    }
}
