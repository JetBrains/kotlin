/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation.api

import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File

internal open class BaseKotlinGradleTest {
    @Rule
    @JvmField
    internal val testProjectDir: TemporaryFolder = TemporaryFolder()
    internal lateinit var apiDump: File

    @Before
    fun setup() {
        apiDump = testProjectDir.newFolder("api")
                .toPath()
                .resolve("${testProjectDir.root.name}.api")
                .toFile()
                .apply {
                    createNewFile()
                }
    }
}
