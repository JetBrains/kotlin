package org.jetbrains.kotlin.tools.projectWizard.core

import java.io.IOException

abstract class Error {
    abstract val message: String
}

abstract class ExceptionError : Error() {
    abstract val exception: Exception
    override val message: String
        get() = exception::class.simpleName!!.removeSuffix("Exception").splitByWords() +
                exception.message?.let { ": $it" }.orEmpty()

    companion object {
        private val wordRegex = "[A-Z][a-z0-9]+".toRegex()
        private fun String.splitByWords() =
            wordRegex.findAll(this).joinToString(separator = " ") { it.value }
    }
}

data class IOError(override val exception: IOException) : ExceptionError()

data class ExceptionErrorImpl(override val exception: Exception) : ExceptionError()

data class ParseError(override val message: String) : Error()

data class TemplateNotFoundError(val id: String) : Error() {
    override val message: String
        get() = "Template with an id `$id` is not found"
}

data class RequiredSettingsIsNotPresentError(val settingNames: List<String>) : Error() {
    override val message: String
        get() = buildString {
            append("The following required settings is not present: \n")
            settingNames.joinTo(this, "\n") { "   $it" }
        }
}

data class CircularTaskDependencyError(val taskName: String) : Error() {
    override val message: String
        get() = "$taskName task has circular dependencies"
}

data class BadSettingValueError(override val message: String) : Error()

data class ConfiguratorNotFoundError(val id: String) : Error() {
    override val message: String
        get() = "Module type `$id` was not found"
}

data class ValidationError(val validationMessage: String) : Error() {
    override val message: String
        get() = validationMessage.capitalize()
}

data class ProjectImportingError(override val message: String) : Error()