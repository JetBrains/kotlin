package org.jetbrains.kotlin.tools.projectWizard.core.entity

import org.jetbrains.kotlin.tools.projectWizard.core.Failure
import org.jetbrains.kotlin.tools.projectWizard.core.ReadingContext
import org.jetbrains.kotlin.tools.projectWizard.core.UNIT_SUCCESS
import org.jetbrains.kotlin.tools.projectWizard.core.ValidationError


inline class SettingValidator<V>(val validate: ReadingContext.(V) -> ValidationResult) {
    infix fun and(other: SettingValidator<V>) = SettingValidator<V> { value ->
        validate(value) and other.validate(this, value)
    }

    operator fun ReadingContext.invoke(value: V) = validate(value)
}

fun <V> settingValidator(validator: ReadingContext.(V) -> ValidationResult) =
    SettingValidator(validator)

fun <V> inValidatorContext(validator: ReadingContext.(V) -> SettingValidator<V>) =
    SettingValidator<V> { value ->
        validator(value).validate(this, value)
    }



object StringValidators {
    fun shouldNotBeBlank(name: String) = settingValidator { value: String ->
        if (value.isBlank()) ValidationResult.ValidationError("${name.capitalize()} should not be blank ")
        else ValidationResult.OK
    }

    fun shouldBeValidIdentifier(name: String, allowedExtraSymbols: Set<Char>) = settingValidator { value: String ->
        if (value.any { char -> !char.isLetterOrDigit() && char !in allowedExtraSymbols }) {
            val allowedExtraSymbolsStringified = allowedExtraSymbols
                .takeIf { it.isNotEmpty() }
                ?.joinToString(separator = ", ") { char -> "'$char'" }
                ?.let { chars -> ", and symbols: $chars" }
                .orEmpty()
            ValidationResult.ValidationError(
                "${name.capitalize()} should consist only of letters, digits$allowedExtraSymbolsStringified"
            )
        } else ValidationResult.OK
    }
}

fun List<ValidationResult>.fold() = fold(ValidationResult.OK, ValidationResult::and)


sealed class ValidationResult {
    abstract val isOk: Boolean

    object OK : ValidationResult() {
        override val isOk = true
    }

    data class ValidationError(val messages: List<String>, val target: Any? = null) : ValidationResult() {
        constructor(message: String, target: Any? = null) : this(listOf(message), target)

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

infix fun ValidationResult.isSpecificError(error: ValidationResult.ValidationError) =
    this is ValidationResult.ValidationError && messages.firstOrNull() == error.messages.firstOrNull()

fun ValidationResult.withTarget(target: Any) = when (this) {
    ValidationResult.OK -> this
    is ValidationResult.ValidationError -> copy(target = target)
}

fun ValidationResult.withTargetIfNull(target: Any) = when (this) {
    ValidationResult.OK -> this
    is ValidationResult.ValidationError -> if (this.target == null) copy(target = target) else this
}

fun ValidationResult.toResult() = when (this) {
    ValidationResult.OK -> UNIT_SUCCESS
    is ValidationResult.ValidationError -> Failure(messages.map { ValidationError(it) })
}


interface Validatable<out V> {
    val validator: SettingValidator<@UnsafeVariance V>
}

fun <V, Q : Validatable<Q>> ReadingContext.validateList(list: List<Q>) = settingValidator<V> {
    list.fold(ValidationResult.OK as ValidationResult) { result, value ->
        result and value.validator.validate(this, value).withTarget(value)
    }
}

