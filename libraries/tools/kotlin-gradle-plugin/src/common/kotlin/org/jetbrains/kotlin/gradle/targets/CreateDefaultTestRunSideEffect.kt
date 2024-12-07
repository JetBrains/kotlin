/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets

import org.jetbrains.kotlin.gradle.plugin.KotlinTargetWithTests
import org.jetbrains.kotlin.gradle.plugin.addExtension

internal val CreateDefaultTestRunSideEffect = KotlinTargetSideEffect<KotlinTargetWithTests<*, *>> { target ->
    target.addExtension(target::testRuns.name, target.testRuns)
    target.testRuns.create(KotlinTargetWithTests.DEFAULT_TEST_RUN_NAME)
}
