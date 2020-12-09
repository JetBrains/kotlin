/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.api

import org.jetbrains.kotlin.commonizer.api.utils.konanHome
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
        commonizer(
            konanHome = konanHome,
            targetLibraries = emptySet(),
            dependencyLibraries = emptySet(),
            outputHierarchy = CommonizerTarget(KonanTarget.LINUX_X64, KonanTarget.MACOS_X64),
            outputDirectory = temporaryOutputDirectory.root
        )
    }

}
