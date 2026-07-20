/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.test

import org.jetbrains.kotlin.konan.target.Family

/**
 * Restricts a Swift Export test class to the given native targets. Evaluated once per test by
 * [AbstractSwiftExportTest.assumeTestTargetEnabled]; a test whose `testTarget` is not allowed is
 * skipped (JUnit "ignored"), not failed.
 *
 * A target is enabled when its [Family] is listed in [families] **or** its canonical name
 * (e.g. `"macos_arm64"`) is listed in [targets]. Use [families] for family-wide constraints
 * (e.g. "any Apple mobile/desktop target") and [targets] to pin an exact target (e.g. suites that
 * consume prebuilt klibs that only exist for `macos_arm64` on the CI agents).
 *
 * `@Inherited` so a generated concrete suite picks up the annotation from its abstract base; a suite may
 * also carry its own annotation (e.g. added by a test generator, as the coroutines suites do), which
 * shadows the inherited one.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@java.lang.annotation.Inherited
annotation class EnabledOnNativeTargets(
    val families: Array<Family> = [],
    val targets: Array<String> = [],
)
