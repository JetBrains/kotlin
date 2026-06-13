/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.test

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.konan.test.blackbox.support.group.UseExtTestCaseGroupProvider
import org.jetbrains.kotlin.test.TestMetadata

@TestMetadata("native/swift/swift-export-standalone-integration-tests/external/testData/generation")
@TestDataPath("\$PROJECT_ROOT")
@UseExtTestCaseGroupProvider
@Suppress("JUnitTestCaseWithNoTests")
class ExternalProjectGenerationTests : AbstractExternalProjectGenerationTest()
