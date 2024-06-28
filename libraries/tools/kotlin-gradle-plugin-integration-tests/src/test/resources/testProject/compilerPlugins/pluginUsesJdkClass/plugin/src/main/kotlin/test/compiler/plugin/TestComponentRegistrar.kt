/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.compiler.plugin

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import java.sql.SQLException

@OptIn(ExperimentalCompilerApi::class)
class TestComponentRegistrar : CompilerPluginRegistrar() {
    override fun ExtensionStorage.registerExtensions(
        configuration: CompilerConfiguration
    ) {
        // Validates plugin is able to use JDK platform classes
        try {
            throw SQLException()
        } catch (ex: SQLException) {
            // no-op
        }
        try {
            Class.forName("org.gradle.launcher.bootstrap.EntryPoint")
            throw Exception("Gradle classes are accessible within the isolated classpath")
        } catch (e: ClassNotFoundException) {
            // no-op
        } catch (t: Throwable) {
            throw Exception("Gradle classes are accessible within the isolated classpath")
        }
    }

    override val supportsK2: Boolean = true
}
