/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.jetbrains.kotlin.testFederation.MustRunOnChangesInBuildToolsApi
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInCommonBackend
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInCompilerInfrastructure
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInFrontend
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInGradle
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInJs
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInJvm
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInNative
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInWasm

@RequiresOptIn(
    message = "Marking test with a broad impact annotation impacts aggregate performance. Please use this only for select smoke tests",
    level = RequiresOptIn.Level.ERROR,
)
annotation class ImpactsAggregatePerformance

@ImpactsAggregatePerformance
@MustRunOnChangesInFrontend
@MustRunOnChangesInCompilerInfrastructure
@MustRunOnChangesInCommonBackend
@MustRunOnChangesInNative
@MustRunOnChangesInJvm
@MustRunOnChangesInJs
@MustRunOnChangesInWasm
@MustRunOnChangesInBuildToolsApi
/**
 * Please use this sparingly to mark tests that are either:
 * - Broad smoke tests that cover a lot of ground across all compilations
 * - Tests that don't belong to Gradle team and should be moved out of KGP-IT test suite
 */
annotation class MustRunOnChangesInAnyCompiler(val reason: String)

@MustRunOnChangesInJs
@MustRunOnChangesInBuildToolsApi
annotation class MustRunOnChangesInJsIrCompiler
