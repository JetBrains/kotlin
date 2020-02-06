/*
 * Copyright (C) 2020 Brian Norman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bnorm.power

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration

@AutoService(CommandLineProcessor::class)
class PowerAssertCommandLineProcessor : CommandLineProcessor {
  override val pluginId: String = "com.bnorm.kotlin-power-assert"

  override val pluginOptions: Collection<CliOption> = listOf(
    CliOption(
      optionName = "enabled",
      valueDescription = "<true|false>",
      description = "whether to enable the kotlin-power-assert plugin or not"
    )
  )

  override fun processOption(
    option: AbstractCliOption,
    value: String,
    configuration: CompilerConfiguration
  ) {
    return when (option.optionName) {
      "enabled" -> configuration.put(KEY_ENABLED, value.toBoolean())
      else -> error("Unexpected config option ${option.optionName}")
    }
  }
}
