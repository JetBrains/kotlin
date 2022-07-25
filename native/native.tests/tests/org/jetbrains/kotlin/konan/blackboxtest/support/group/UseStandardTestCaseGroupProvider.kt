/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.group

import org.jetbrains.kotlin.konan.blackboxtest.support.settings.DisabledTestDataFiles
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.GeneratedSources
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.TestRoots
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.TestConfiguration

@Target(AnnotationTarget.CLASS)
@TestConfiguration(
    providerClass = StandardTestCaseGroupProvider::class,
    requiredSettings = [TestRoots::class, GeneratedSources::class, DisabledTestDataFiles::class]
)
annotation class UseStandardTestCaseGroupProvider
