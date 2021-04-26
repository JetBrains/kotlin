/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.core.entity.settings

import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.core.*

import org.jetbrains.kotlin.tools.projectWizard.core.entity.*
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

sealed class SettingType<out V : Any> {
    abstract fun parse(context: ParsingContext, value: Any, name: String): TaskResult<V>
    open val serializer: SettingSerializer<V> = SettingSerializer.None
}

object StringSettingType : SettingType<String>() {
    override fun parse(context: ParsingContext, value: Any, name: String) =
        value.parseAs<String>(name)

    override val serializer: SettingSerializer<String> =
        SettingSerializer.Serializer(fromString = { it })

    class Builder(
        path: String,
        val title: String,
        neededAtPhase: GenerationPhase
    ) : SettingBuilder<String, StringSettingType>(path, title, neededAtPhase) {
        fun shouldNotBeBlank() {
            validate(StringValidators.shouldNotBeBlank(title.replaceFirstChar(Char::uppercaseChar)))
        }

        override val type = StringSettingType
    }
}

object BooleanSettingType : SettingType<Boolean>() {
    override fun parse(context: ParsingContext, value: Any, name: String) =
        value.parseAs<Boolean>(name)

    override val serializer: SettingSerializer<Boolean> =
        SettingSerializer.Serializer(fromString = { it.toBoolean() })

    class Builder(
        path: String,
        title: String,
        neededAtPhase: GenerationPhase
    ) : SettingBuilder<Boolean, BooleanSettingType>(path, title, neededAtPhase) {
        override val type = BooleanSettingType
    }

}

class DropDownSettingType<V : DisplayableSettingItem>(
    val values: List<V>,
    val filter: DropDownSettingTypeFilter<V>,
    val parser: Parser<V>
) : SettingType<V>() {
    override fun parse(context: ParsingContext, value: Any, name: String): TaskResult<V> = with(context) {
        computeM {
            parser.parse(this, value, name)
        }
    }

    override val serializer: SettingSerializer<V> =
        SettingSerializer.Serializer(fromString = { value ->
            ComputeContext.runInComputeContextWithState(
                ParsingState.EMPTY
            ) {
                parser.parse(this, value, "")
            }.asNullable?.first
        })

    class Builder<V : DisplayableSettingItem>(
        path: String,
        title: String,
        neededAtPhase: GenerationPhase,
        private val parser: Parser<V>
    ) : SettingBuilder<V, DropDownSettingType<V>>(path, title, neededAtPhase) {
        var values = emptyList<V>()

        var filter: DropDownSettingTypeFilter<V> = { _, _ -> true }

        override val type
            get() = DropDownSettingType(
                values,
                filter,
                parser
            )


        init {
            defaultValue = dynamic { reference ->
                values.first {
                    @Suppress("UNCHECKED_CAST")
                    filter(reference as SettingReference<V, DropDownSettingType<V>>, it)
                }
            }
        }
    }
}

class ValueSettingType<V : Any>(
    private val parser: Parser<V>
) : SettingType<V>() {
    override fun parse(context: ParsingContext, value: Any, name: String): TaskResult<V> = with(context) {
        computeM {
            parser.parse(this, value, name)
        }
    }

    class Builder<V : Any>(
        private val path: String,
        title: String,
        neededAtPhase: GenerationPhase,
        private val parser: Parser<V>
    ) : SettingBuilder<V, ValueSettingType<V>>(path, title, neededAtPhase) {
        init {
            validate { value ->
                if (value is Validatable<*>) (value.validator as SettingValidator<Any>).validate(this, value)
                else ValidationResult.OK
            }
        }

        override val type
            get() = ValueSettingType(parser)
    }
}

object VersionSettingType : SettingType<Version>() {
    override fun parse(context: ParsingContext, value: Any, name: String): TaskResult<Version> = with(context) {
        computeM {
            Version.parser.parse(this, value, name)
        }
    }

    class Builder(
        path: String,
        title: String,
        neededAtPhase: GenerationPhase
    ) : SettingBuilder<Version, VersionSettingType>(path, title, neededAtPhase) {
        override val type
            get() = VersionSettingType
    }
}

class ListSettingType<V : Any>(private val parser: Parser<V>) : SettingType<List<V>>() {
    override fun parse(context: ParsingContext, value: Any, name: String): TaskResult<List<V>> = with(context) {
        computeM {
            val (list) = value.parseAs<List<*>>(name)
            list.mapComputeM { parser.parse(this, it, name) }.sequence()
        }
    }

    class Builder<V : Any>(
        path: String,
        title: String,
        neededAtPhase: GenerationPhase,
        parser: Parser<V>
    ) : SettingBuilder<List<V>, ListSettingType<V>>(path, title, neededAtPhase) {
        init {
            validate { values ->
                values.fold(ValidationResult.OK as ValidationResult) { result, value ->
                    result and when (value) {
                        is Validatable<*> -> (value.validator as SettingValidator<Any>).validate(this, value).withTargetIfNull(value)
                        else -> ValidationResult.OK
                    }
                }
            }
        }

        override val type = ListSettingType(parser)
    }
}

object PathSettingType : SettingType<Path>() {
    override fun parse(context: ParsingContext, value: Any, name: String): TaskResult<Path> = with(context) {
        computeM {
            pathParser.parse(this, value, name)
        }
    }

    override val serializer: SettingSerializer<Path> =
        SettingSerializer.Serializer(fromString = { Paths.get(it) })

    class Builder(
        path: String,
        private val title: String,
        neededAtPhase: GenerationPhase
    ) : SettingBuilder<Path, PathSettingType>(path, title, neededAtPhase) {

        init {
            validate { pathValue ->
                if (pathValue.toString().isBlank())
                    ValidationResult.ValidationError(
                        KotlinNewProjectWizardBundle.message(
                            "validation.should.not.be.blank",
                            title.replaceFirstChar(Char::uppercaseChar)
                        )
                    )
                else ValidationResult.OK
            }
        }

        fun shouldExists() = validate { pathValue ->
            if (isUnitTestMode) return@validate ValidationResult.OK
            if (!Files.exists(pathValue))
                ValidationResult.ValidationError(
                    KotlinNewProjectWizardBundle.message("validation.file.should.exists", title.replaceFirstChar(Char::uppercaseChar))
                )
            else ValidationResult.OK
        }

        override val type = PathSettingType
    }
}

typealias DropDownSettingTypeFilter <V> = Reader.(SettingReference<V, DropDownSettingType<V>>, V) -> Boolean