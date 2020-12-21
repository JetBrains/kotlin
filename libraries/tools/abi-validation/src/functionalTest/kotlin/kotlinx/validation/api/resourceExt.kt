/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation.api

import java.io.File

internal fun readFileList(fileName: String): String {
    val resource = BaseKotlinGradleTest::class.java.classLoader.getResource(fileName)
            ?: throw IllegalStateException("Could not find resource '$fileName'")
    return File(resource.toURI()).readText()
}
