/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing

import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTestDevServerService
import java.nio.file.Path

internal class FileBasedKotlinJsTestDevServer(
    private val filePath: Path
): KotlinJsTestDevServerService {
    private val baseUrl get() = filePath.toUri().toString()

    override fun <T> use(code: (baseUrl: String) -> T): T {
        return code(baseUrl)
    }
}
