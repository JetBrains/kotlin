/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

import org.jetbrains.kotlin.descriptors.commonizer.utils.konanHome
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test

class CliCommonizerTest {

    @get:Rule
    val temporaryOutputDirectory = TemporaryFolder()

    @Test
    fun invokeCliWithEmptyArguments() {
        val commonizer = CliCommonizer(this::class.java.classLoader)
        commonizer.commonizeLibraries(
            konanHome = konanHome,
            inputLibraries = emptySet(),
            dependencyLibraries = emptySet(),
            outputCommonizerTarget = CommonizerTarget(KonanTarget.LINUX_X64, KonanTarget.MACOS_X64),
            outputDirectory = temporaryOutputDirectory.root
        )
    }
}
