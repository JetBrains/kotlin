/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.jetbrains.kotlin.test.WithMuteInDatabase
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * Base class for all Kotlin Gradle plugin integration tests.
 */
@Tag("JUnit5")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WithMuteInDatabase
abstract class KGPBaseTest {
    open val defaultBuildOptions = BuildOptions()

    @TempDir
    lateinit var workingDir: Path
}