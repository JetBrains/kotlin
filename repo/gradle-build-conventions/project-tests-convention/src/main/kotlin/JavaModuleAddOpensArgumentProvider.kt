/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.process.CommandLineArgumentProvider

/**
 * Provides the arguments that keep the test JVM quiet about the parts of the JDK the tests rely on.
 *
 * All of them are warnings today and errors in some later release, and the repository has tests that
 * assert on clean output, so they are passed as soon as the runtime knows them.
 */
abstract class JavaModuleAddOpensArgumentProvider : CommandLineArgumentProvider {

    @get:Nested
    abstract val javaLauncher: Property<JavaLauncher>

    override fun asArguments(): Iterable<String> {
        val version = javaLauncher.get().metadata.languageVersion.asInt()
        return buildList {
            if (version > 8) {
                addAll(
                    listOf(
                        "--add-opens", "java.base/java.io=ALL-UNNAMED",
                        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
                        "--add-opens", "java.desktop/javax.swing=ALL-UNNAMED",
                    )
                )
            }
            if (version >= 24) {
                // JEP 472 and JEP 498.
                addAll(listOf("--enable-native-access=ALL-UNNAMED", "--sun-misc-unsafe-memory-access=allow"))
            }
        }
    }
}
