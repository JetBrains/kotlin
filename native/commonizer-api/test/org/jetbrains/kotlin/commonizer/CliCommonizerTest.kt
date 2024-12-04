/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import org.jetbrains.kotlin.commonizer.utils.konanHome
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test

public class CliCommonizerTest {

    @get:Rule
    public val temporaryOutputDirectory: TemporaryFolder = TemporaryFolder()

    @Test
    public fun invokeCliWithEmptyArguments() {
        val commonizer = CliCommonizer(this::class.java.classLoader)
        commonizer.commonizeLibraries(
            konanHome = konanHome,
            inputLibraries = emptySet(),
            dependencyLibraries = emptySet(),
            outputTargets = setOf(CommonizerTarget(KonanTarget.LINUX_X64, KonanTarget.MACOS_X64)),
            outputDirectory = temporaryOutputDirectory.root,
            additionalSettings = emptyList(),
        )
    }
}
