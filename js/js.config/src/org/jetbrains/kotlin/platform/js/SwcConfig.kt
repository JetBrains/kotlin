/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.platform.js

import org.jetbrains.kotlin.js.config.ModuleKind

object SwcConfig {
    public fun getArgumentsWhen(
        inputDirectoryOrFiles: List<String>,
        configPath: String,
        outputDirectory: String,
        environmentCode: String,
        fileExtension: String
    ): List<String> = buildList {
        add("compile")
        inputDirectoryOrFiles.forEach(::add)
        add("--config-file")
        add(configPath)
        add("--env-name=${environmentCode}")
        add("--out-dir")
        add(outputDirectory)
        add("--out-file-extension=$fileExtension")
        add("--extensions=$fileExtension")
    }

    public fun getConfigWhen(
        sourceMapEnabled: Boolean,
        target: String,
        includeExternalHelpers: Boolean,
        moduleKind: ModuleKind
    ): Map<String, Any> = buildMap {
        set("\$schema", "https://swc.rs/schema.json")
        set("sourceMaps", sourceMapEnabled)
        set("inputSourceMap", sourceMapEnabled)
        set("exclude", arrayOf(".*\\.d\\.m?ts$"))
        set("jsc", buildMap<String, Any> {
            set("parser", buildMap {
                set("syntax", "ecmascript")
                set("dynamicImport", true)
                set("functionBind", true)
                set("importMeta", true)
            })
            set("loose", true)
            set("externalHelpers", includeExternalHelpers)
            set("target", target)
        })
        set("module", buildMap {
            set("resolveFully", true)
            set("type", if (moduleKind === ModuleKind.ES) "nodenext" else moduleKind.type)
            set("outFileExtension", moduleKind.jsExtension)
        })
    }
}