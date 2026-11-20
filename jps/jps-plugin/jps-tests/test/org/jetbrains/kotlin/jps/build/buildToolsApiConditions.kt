/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.build

import org.jetbrains.kotlin.compilerRunner.btapi.JpsBtaToolchainLoader
import org.junit.jupiter.api.condition.DisabledIfSystemProperty
import org.junit.jupiter.api.condition.EnabledIfSystemProperty

/**
 * The test runs only in the `test` task, where Kotlin is compiled through the legacy compiler runner.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@DisabledIfSystemProperty(
    named = JpsBtaToolchainLoader.IMPL_HOME_PROPERTY,
    matches = ".+",
    disabledReason = "Not supported when compiling through the Build Tools API",
)
annotation class LegacyRunnerOnly

/**
 * The test runs only in the `testWithBuildToolsApi` task, where Kotlin is compiled through the Build Tools API.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@EnabledIfSystemProperty(
    named = JpsBtaToolchainLoader.IMPL_HOME_PROPERTY,
    matches = ".+",
    disabledReason = "Checks the compilation through the Build Tools API",
)
annotation class BuildToolsApiOnly
