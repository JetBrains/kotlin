/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.group

import org.jetbrains.kotlin.konan.test.blackbox.support.settings.DisabledTestDataFiles
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.GeneratedSources
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.TestConfiguration
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.TestRoots

@Target(AnnotationTarget.CLASS)
@TestConfiguration(
    providerClass = ExtTestCaseGroupProvider::class,
    requiredSettings = [TestRoots::class, GeneratedSources::class, DisabledTestDataFiles::class]
)
annotation class UseExtTestCaseGroupProvider
