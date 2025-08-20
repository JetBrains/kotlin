/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.GradleException
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlin.text.trim

abstract class XcodeToolchainValueSource @Inject constructor(private val execOperations: ExecOperations) :
    ValueSource<String, ValueSourceParameters.None> {
    override fun obtain(): String? {
        val out = ByteArrayOutputStream()
        val execResult = execOperations.exec {
            commandLine("/usr/bin/xcrun")
            args = listOf("-f", "ld")
            this.standardOutput = out
        }
        if (execResult.exitValue != 0) {
            throw GradleException( """
                An error occurred during an xcrun execution. Make sure that Xcode and its command line tools are properly installed.
                Failed command: /usr/bin/xcrun -f ld
                Try running this command in Terminal and fix the errors by making Xcode (and its command line tools) configuration correct.
            """.trimIndent()
            )
        }
        return out.toString().trim().removeSuffix("/usr/bin/ld")
    }
}