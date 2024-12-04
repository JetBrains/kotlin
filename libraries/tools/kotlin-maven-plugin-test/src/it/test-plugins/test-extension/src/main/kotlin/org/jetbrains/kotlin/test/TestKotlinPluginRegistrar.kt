/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import com.intellij.mock.*
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.*

object TestPluginKeys {
    val TestOption = CompilerConfigurationKey.create<String>("test option")
}

class TestCommandLineProcessor : CommandLineProcessor {
    companion object {
        val TestPluginId = "org.jetbrains.kotlin.test.test-plugin"
        val MyTestOption = CliOption("test-option", "", "", true, false)
    }

    override val pluginId: String = TestPluginId

    override val pluginOptions: Collection<CliOption> = listOf(MyTestOption)

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        when (option) {
            MyTestOption -> {
                configuration.put(TestPluginKeys.TestOption, value)
            }
            else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
        }
    }
}

class TestKotlinPluginRegistrar : CompilerPluginRegistrar() {
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val collector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        val option = configuration.get(TestPluginKeys.TestOption)!!

        collector.report(CompilerMessageSeverity.INFO, "Plugin applied")
        collector.report(CompilerMessageSeverity.INFO, "Option value: $option")
    }

    override val supportsK2: Boolean
        get() = true
}
