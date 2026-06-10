/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal

import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultCInteropSettings
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import java.io.File

/**
 * Collects the host module's own cinterops declared for re-export with the top-level
 * `swiftExport { reexportCinterop(...) }` DSL.
 */
internal fun collectReexportedHostCinterops(
    declarations: Provider<Map<String, String>>,
    mainCompilation: KotlinNativeCompilation,
): Provider<List<SwiftExportedModule>> = declarations.map { declared ->
    declared.mapNotNull { (cinteropName, objCModuleName) ->
        val interop = mainCompilation.cinteropOrNull(cinteropName) ?: return@mapNotNull null
        if (objCModuleName.isEmpty()) return@mapNotNull null
        val klib = mainCompilation.cinteropKlibFile(interop) ?: return@mapNotNull null
        SwiftExportedModule.CinteropReexported(objCModuleName, klib)
    }
}

/**
 * Search path arguments making the Objective-C modules of the re-exported host cinterops visible to
 * the Swift compilation of the generated code. The cinterop tool located the module through the
 * compiler options and include directories of the cinterop settings, so the same paths are forwarded.
 */
internal fun reexportedHostCinteropsSwiftcArgs(
    declarations: Provider<Map<String, String>>,
    mainCompilation: KotlinNativeCompilation,
): Provider<List<String>> = declarations.map { declared ->
    declared.keys
        .mapNotNull { mainCompilation.cinteropOrNull(it) }
        .flatMap { it.swiftcSearchPathArgs() }
}

private fun KotlinNativeCompilation.cinteropOrNull(cinteropName: String): DefaultCInteropSettings? =
    cinterops.findByName(cinteropName)

/**
 * Locates the klib of [interop] among [KotlinNativeCompilation.cinteropOutputs] by KGP's own naming
 * convention (`CInteropProcess.baseKlibName` = `<prefix>-cinterop-<name>`). The file collection is used
 * instead of the `CInteropProcess` task to keep the provider serializable for the configuration cache:
 * referencing the task here would capture it in the serialized state of the Swift Export task.
 */
private fun KotlinNativeCompilation.cinteropKlibFile(interop: DefaultCInteropSettings): File? =
    cinteropOutputs.files.singleOrNull { klib ->
        klib.nameWithoutExtension.substringAfterLast("-cinterop-") == interop.name
    }

private fun DefaultCInteropSettings.swiftcSearchPathArgs(): List<String> = buildList {
    includeDirs.allHeadersDirs.files.forEach { dir ->
        add("-I")
        add(dir.absolutePath)
    }

    val opts = compilerOpts
    var index = 0
    while (index < opts.size) {
        val opt = opts[index]
        when {
            opt == "-I" || opt == "-F" -> if (index + 1 < opts.size) {
                add(opt)
                add(opts[index + 1])
                index++
            }
            opt.startsWith("-I") || opt.startsWith("-F") -> add(opt)
            // Module maps are Clang importer options, not understood by the Swift driver directly.
            opt.startsWith("-fmodule-map-file=") -> {
                add("-Xcc")
                add(opt)
            }
        }
        index++
    }
}
