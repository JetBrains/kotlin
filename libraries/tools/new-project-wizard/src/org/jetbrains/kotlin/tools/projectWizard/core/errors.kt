package org.jetbrains.kotlin.tools.projectWizard.core

import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version
import org.jetbrains.kotlin.tools.projectWizard.settings.version.VersionRange
import java.io.IOException

abstract class Error {
    abstract val message: String
}

abstract class ExceptionError : Error() {
    abstract val exception: Exception
    override val message: String
        get() = exception.asString()
}

data class IOError(override val exception: IOException) : ExceptionError()

data class ExceptionErrorImpl(override val exception: Exception) : ExceptionError()

data class ParseError(override val message: String) : Error()

data class TemplateNotFoundError(val id: String) : Error() {
    override val message: String
        get() = "Template with an id `$id` is not found"
}

data class SettingNotFoundError(val settingName: String) : Error() {
    override val message: String
        get() = "Setting with name `$settingName` was not found"
}

data class RequiredSettingsIsNotPresentError(val settingNames: List<String>) : Error() {
    override val message: String
        get() = buildString {
            append("The following required settings is not present: \n")
            settingNames.joinTo(this, "\n") { "   $it" }
        }
}

data class BadApplicationExitCodeError(val applicationName: String, val exitCode: Int) : Error() {
    override val message: String
        get() = "Application $applicationName exited with code $exitCode"
}


data class CircularTaskDependencyError(val taskName: String) : Error() {
    override val message: String
        get() = "$taskName task has circular dependencies"
}

data class BadSettingValueError(override val message: String) : Error()

data class LibraryNotFoundError(val name: String) : Error() {
    override val message: String
        get() = "Library with name `$name` was not found"
}

data class ConfiguratorNotFoundError(val id: String) : Error() {
    override val message: String
        get() = "Module type `$id` was not found"
}

data class VersionIsNotInRangeError(val subject: String, val version: Version, val range: VersionRange) : Error() {
    override val message: String
        get() = "${subject.capitalize()}'s version $version is not in expected range $range"
}

data class ValidationError(val validationMessage: String) : Error() {
    override val message: String
        get() = "Validation error: $validationMessage"
}

data class InvalidSourceSetName(val name: String) : Error() {
    override val message: String
        get() = "Source set name `$name` is invalid"
}

data class ModuleNotFoundError(val path: String) : Error() {
    override val message: String
        get() = "Sourceset with a path `$path` was not found invalid"
}


fun Throwable.asString() =
    this::class.simpleName!!.removeSuffix("Exception").splitByWords() + message?.let { ": $it" }.orEmpty()

private val wordRegex = "[A-Z][a-z0-9]+".toRegex()
private fun String.splitByWords() =
    wordRegex.findAll(this).joinToString(separator = " ") { it.value }


