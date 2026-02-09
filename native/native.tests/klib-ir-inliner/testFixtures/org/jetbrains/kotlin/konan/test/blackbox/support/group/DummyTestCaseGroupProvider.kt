/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.group

import org.jetbrains.kotlin.konan.test.blackbox.support.TestCaseGroup
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCaseGroupId
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Settings
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.TestConfiguration
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.TestRoots

internal class DummyTestCaseGroupProvider: TestCaseGroupProvider {
    override fun getTestCaseGroup(
        testCaseGroupId: TestCaseGroupId,
        settings: Settings,
    ): TestCaseGroup? {
        TODO("Not yet implemented")
    }
}

@Target(AnnotationTarget.CLASS)
@TestConfiguration(
    providerClass = DummyTestCaseGroupProvider::class,
    requiredSettings = [TestRoots::class]
)
annotation class UseDummyTestCaseGroupProvider
