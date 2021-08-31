/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package example

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

public object ExampleConfigurationKeys {
    public val EXAMPLE_KEY: CompilerConfigurationKey<String> = CompilerConfigurationKey.create<String>("example argument")
    public val EXAMPLE_LEGACY_KEY: CompilerConfigurationKey<String> = CompilerConfigurationKey.create<String>("example legacy argument")
}

public class ExampleCommandLineProcessor : CommandLineProcessor {
    companion object {
        public val EXAMPLE_PLUGIN_ID: String = "example.plugin"
        public val EXAMPLE_OPTION: CliOption = CliOption("exampleKey", "<value>", "")
        public val EXAMPLE_LEGACY_OPTION: CliOption = CliOption("exampleLegacyKey", "<value>", "")
    }

    override val pluginId: String = EXAMPLE_PLUGIN_ID
    override val pluginOptions: Collection<CliOption> = listOf(EXAMPLE_OPTION, EXAMPLE_LEGACY_OPTION)

    override fun processOption(option: CliOption, value: String, configuration: CompilerConfiguration) {
        when (option) {
            EXAMPLE_OPTION -> configuration.put(ExampleConfigurationKeys.EXAMPLE_KEY, value)
            EXAMPLE_LEGACY_OPTION -> configuration.put(ExampleConfigurationKeys.EXAMPLE_LEGACY_KEY, value)
            else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
        }
    }
}

public class ExampleComponentRegistrar : ComponentRegistrar {
    public override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val exampleValue = configuration.get(ExampleConfigurationKeys.EXAMPLE_KEY)
        val exampleLegacyValue = configuration.get(ExampleConfigurationKeys.EXAMPLE_LEGACY_KEY)
        val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
        messageCollector.report(CompilerMessageSeverity.INFO, "Project component registration: $exampleValue")
        messageCollector.report(CompilerMessageSeverity.INFO, "Project component registration: $exampleLegacyValue")
    }
}
