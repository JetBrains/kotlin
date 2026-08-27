/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

/**
 * Running tests inside the test federation knows two modes:
 * ## [Full]
 * All tests are executed. This happens if the tests belong to a [Domain] which was marked as 'affected'
 *
 * ## [Smoke]
 * Not all tests are executed.
 * All tests marked as '@SmokeTest' will be executed.
 * All contracts to affected [Domain]s will be executed.
 * For example,
 * If the current domain is [Domain.Gradle], but changes identified the [Domain.Compiler] as 'affected', then all tests marked as
 * `@CompilerContract` will be executed in addition to all smoke tests.
 */
enum class TestFederationMode {
    Full, Smoke;
}
