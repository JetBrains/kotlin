package org.jetbrains.kotlin.tools.projectWizard.core.entity

import org.jetbrains.kotlin.tools.projectWizard.core.Failure
import org.jetbrains.kotlin.tools.projectWizard.core.UNIT_SUCCESS
import org.jetbrains.kotlin.tools.projectWizard.core.ValidationError
import org.jetbrains.kotlin.tools.projectWizard.core.ValuesReadingContext

inline class SettingValidator<V>(val validate: ValuesReadingContext.(V) -> ValidationResult) {
    infix fun and(other: SettingValidator<V>) = SettingValidator<V> { value ->
        validate(value) and other.validate(this, value)
    }

    operator fun ValuesReadingContext.invoke(value: V) = validate(value)
}

fun <V> settingValidator(validator: ValuesReadingContext.(V) -> ValidationResult) =
    SettingValidator(validator)

fun <V> inValidatorContext(validator: ValuesReadingContext.(V) -> SettingValidator<V>) =
    SettingValidator<V> { value ->
        validator(value).validate(this, value)
    }



typealias StringValidator = SettingValidator<String>

object StringValidators {
    fun shouldNotBeBlank(name: String) = settingValidator { value: String ->
        if (value.isBlank()) ValidationResult.ValidationError("${name.capitalize()} should not be blank ")
        else ValidationResult.OK
    }

    fun shouldBeValidIdentifier(name: String) = settingValidator { value: String ->
        if (value.any { !it.isLetterOrDigit() && it != '_' })
            ValidationResult.ValidationError(
                "${name.capitalize()} should consist only of letters, digits, and underscores"
            )
        else ValidationResult.OK
    }
}

fun List<ValidationResult>.fold() = fold(ValidationResult.OK, ValidationResult::and)


sealed class ValidationResult {
    abstract val isOk: Boolean

    object OK : ValidationResult() {
        override val isOk = true
    }

    class ValidationError(val messages: List<String>) : ValidationResult() {
        constructor(message: String) : this(listOf(message))

        override val isOk = false
    }

    infix fun and(other: ValidationResult) = when {
        this is OK -> other
        this is ValidationError && other is ValidationError -> ValidationError(messages + other.messages)
        else -> this
    }

    companion object {
        fun create(condition: Boolean, message: String) =
            if (condition) OK else ValidationError(message)
    }
}

fun ValidationResult.toResult() = when (this) {
    ValidationResult.OK -> UNIT_SUCCESS
    is ValidationResult.ValidationError -> Failure(messages.map { ValidationError(it) })
}


interface Validatable<out V> {
    val validator: SettingValidator<@UnsafeVariance V>
}

fun <V, Q : Validatable<Q>> ValuesReadingContext.validateList(list: List<Q>) = settingValidator<V> {
    list.fold(ValidationResult.OK as ValidationResult) { result, value ->
        result and value.validator.validate(this, value)
    }
}

