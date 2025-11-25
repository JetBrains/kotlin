/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.config

object SwcConfig {
    fun getArgumentsWhen(
        inputDirectory: String,
        configPath: String,
        outputDirectory: String,
        environmentCode: String,
        fileExtension: String
    ) = buildList {
        add("compile")
        add(inputDirectory)
        add("--config-file")
        add(configPath)
        add("--env-name=${environmentCode}")
        add("--out-dir")
        add(outputDirectory)
        add("--out-file-extension=$fileExtension")
        add("--extensions=$fileExtension")
    }
}