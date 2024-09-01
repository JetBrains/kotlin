/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.konan

import org.gradle.api.internal.file.FileOperations
import org.gradle.api.logging.Logger
import org.gradle.process.ExecOperations
import java.nio.file.Files

/** Kotlin/Native compiler runner */
internal class KonanCliCompilerRunner(
        fileOperations: FileOperations,
        execOperations: ExecOperations,
        logger: Logger,
        isolatedClassLoadersService: KonanCliRunnerIsolatedClassLoadersService,
        konanHome: String,
) : KonanCliRunner("konanc", fileOperations, execOperations, logger, isolatedClassLoadersService, konanHome) {
    override fun transformArgs(args: List<String>): List<String> {
        val argFile = Files.createTempFile(/* prefix = */ "konancArgs", /* suffix = */ ".lst").toFile().apply { deleteOnExit() }
        argFile.printWriter().use { w ->
            for (arg in args) {
                val escapedArg = arg
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                w.println("\"$escapedArg\"")
            }
        }

        return listOf(toolName, "@${argFile.absolutePath}")
    }
}