/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.test.WithMuteInDatabase
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * Base class for all Kotlin Gradle plugin integration tests.
 *
 * @[TestDataPath] helps DevKit IDE Plugin to find "related testData".
 * It provides various IDE-assistance (e.g. "NavigateToTestData"-actions and gutter icons)
 * For the test named 'testFoo' the "related testData" will be checked at:
 *      '<argument-of-@TestDataPath>/foo'
 *
 * If the test uses a name that doesn't correspond to the testdata, you can add
 *      @org.jetbrains.kotlin.test.TestMetadata("<path relative to resources/testProject">)
 *
 */
@Tag("JUnit5")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WithMuteInDatabase
@TestDataPath("\$CONTENT_ROOT/resources/testProject")
@OsCondition
abstract class KGPBaseTest {
    open val defaultBuildOptions = BuildOptions()

    @TempDir
    lateinit var workingDir: Path
}
