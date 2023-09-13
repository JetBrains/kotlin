/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.group

import org.jetbrains.kotlin.konan.test.blackbox.support.TestCaseGroup
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCaseGroupId
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Settings

internal interface TestCaseGroupProvider {
    fun getTestCaseGroup(testCaseGroupId: TestCaseGroupId, settings: Settings): TestCaseGroup?
}