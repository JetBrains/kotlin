/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.logging

import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker

internal class LoggingCompilerLookupTracker(private val kotlinLogger: KotlinLogger) : CompilerLookupTracker {

    override fun recordLookup(
        filePath: String,
        scopeFqName: String,
        scopeKind: CompilerLookupTracker.ScopeKind,
        name: String,
    ) {
        kotlinLogger.debug("Lookup: $filePath:$scopeFqName:$scopeKind:$name")
    }

    override fun clear() {
    }
}
