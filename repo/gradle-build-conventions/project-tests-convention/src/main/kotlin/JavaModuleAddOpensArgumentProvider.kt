/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.process.CommandLineArgumentProvider

/**
 * Provides `--add-opens` arguments for JDK > 8
 */
abstract class JavaModuleAddOpensArgumentProvider : CommandLineArgumentProvider {

    @get:Nested
    abstract val javaLauncher: Property<JavaLauncher>

    override fun asArguments(): Iterable<String> =
        if (javaLauncher.get().metadata.languageVersion.asInt() > 8) {
            listOf(
                "--add-opens", "java.base/java.io=ALL-UNNAMED",
                "--add-opens", "java.base/java.lang=ALL-UNNAMED",
                "--add-opens", "java.desktop/javax.swing=ALL-UNNAMED",
            )
        } else emptyList()
}
