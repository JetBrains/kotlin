package org.jetbrains.kotlin.tools.projectWizard.wizard.core

import org.jetbrains.kotlin.tools.projectWizard.core.ExceptionError
import org.yaml.snakeyaml.parser.ParserException

data class YamlParsingError(override val exception: ParserException) : ExceptionError()