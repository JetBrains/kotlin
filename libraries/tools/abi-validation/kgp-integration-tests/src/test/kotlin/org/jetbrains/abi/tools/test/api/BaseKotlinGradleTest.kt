/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.abi.tools.test.api

import org.junit.jupiter.api.io.TempDir
import java.io.File

public open class BaseKotlinGradleTest {
    @field:TempDir
    lateinit var testProjectDir: File

    internal val rootProjectDir: File get() = testProjectDir

    internal val rootProjectApiDump: File get() = rootProjectDir.resolve("$API_DIR/${rootProjectDir.name}.api")

    internal fun rootProjectAbiDump(project: String = rootProjectDir.name): File {
        return rootProjectDir.resolve("$API_DIR/$project.klib.api")
    }
}
