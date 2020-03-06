package org.jetbrains.kotlin.tools.projectWizard.core.entity

import org.jetbrains.kotlin.tools.projectWizard.Identificator
import org.jetbrains.kotlin.tools.projectWizard.core.context.ReadingContext
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.ModuleConfigurator
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version
import org.jetbrains.kotlin.tools.projectWizard.templates.Template
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

sealed class SettingReference<out V : Any, out T : SettingType<V>> {
    abstract val path: String
    abstract val type: KClass<out T>

    abstract fun ReadingContext.getSetting(): Setting<V, T>

    final override fun toString() = path
    final override fun equals(other: Any?) = other.safeAs<SettingReference<*, *>>()?.path == path
    final override fun hashCode() = path.hashCode()
}

data class PluginSettingReference<out V : Any, out T : SettingType<V>>(
    override val path: String,
    override val type: KClass<@UnsafeVariance T>
) : SettingReference<V, T>() {

    constructor(kProperty: KProperty1<out Plugin, PluginSetting<V, T>>, type: KClass<T>) :
            this(kProperty.path, type)

    @Suppress("UNCHECKED_CAST")
    constructor(setting: PluginSetting<V, T>) :
            this(setting.path, setting.type::class as KClass<T>)

    override fun ReadingContext.getSetting(): Setting<V, T> = pluginSetting
}

inline val <V : Any, reified T : SettingType<V>> PluginSetting<V, T>.reference: PluginSettingReference<V, T>
    get() = PluginSettingReference(path, T::class)

sealed class ModuleConfiguratorSettingReference<V : Any, T : SettingType<V>> : SettingReference<V, T>() {
    abstract val descriptor: ModuleConfigurator
    abstract val moduleId: Identificator
    abstract val setting: ModuleConfiguratorSetting<V, T>

    override val path: String
        get() = "${descriptor.id}/$moduleId/${setting.path}"

    override val type: KClass<out T>
        get() = setting.type::class

    override fun ReadingContext.getSetting(): Setting<V, T> = setting
    abstract val module: Module?
}

data class ModuleBasedConfiguratorSettingReference<V : Any, T : SettingType<V>>(
    override val descriptor: ModuleConfigurator,
    override val module: Module,
    override val setting: ModuleConfiguratorSetting<V, T>
) : ModuleConfiguratorSettingReference<V, T>() {
    override val moduleId: Identificator
        get() = module.identificator
}

data class IdBasedConfiguratorSettingReference<V : Any, T : SettingType<V>>(
    override val descriptor: ModuleConfigurator,
    override val moduleId: Identificator,
    override val setting: ModuleConfiguratorSetting<V, T>
) : ModuleConfiguratorSettingReference<V, T>() {
    override val module: Module? = null
}

sealed class TemplateSettingReference<V : Any, T : SettingType<V>> : SettingReference<V, T>() {
    abstract val descriptor: Template
    abstract val setting: TemplateSetting<V, T>
    abstract val sourcesetId: Identificator

    override val path: String
        get() = "${descriptor.id}/$sourcesetId/${setting.path}"

    override val type: KClass<out T>
        get() = setting.type::class

    override fun ReadingContext.getSetting(): Setting<V, T> = setting
    abstract val module: Module?
}

data class ModuleBasedTemplateSettingReference<V : Any, T : SettingType<V>>(
    override val descriptor: Template,
    override val module: Module,
    override val setting: TemplateSetting<V, T>
) : TemplateSettingReference<V, T>() {
    override val sourcesetId: Identificator
        get() = module.identificator
}

data class IdBasedTemplateSettingReference<V : Any, T : SettingType<V>>(
    override val descriptor: Template,
    override val sourcesetId: Identificator,
    override val setting: TemplateSetting<V, T>
) : TemplateSettingReference<V, T>() {
    override val module: Module? = null
}

inline val <V : Any, reified T : SettingType<V>> PluginSettingPropertyReference<V, T>.reference: PluginSettingReference<V, T>
    get() = PluginSettingReference(this, T::class)

typealias PluginSettingPropertyReference<V, T> = KProperty1<out Plugin, PluginSetting<V, T>>
typealias SettingPropertyReference<V, T> = KProperty1<out Plugin, Setting<V, T>>

class SettingContext(val onUpdated: (SettingReference<*, *>) -> Unit) {
    private val values = mutableMapOf<String, Any>()
    private val pluginSettings = mutableMapOf<String, PluginSetting<*, *>>()

    @Suppress("UNCHECKED_CAST")
    operator fun <V : Any, T : SettingType<V>> get(
        reference: SettingReference<V, T>
    ): V? = values[reference.path] as? V

    operator fun <V : Any, T : SettingType<V>> set(
        reference: SettingReference<V, T>,
        newValue: V
    ) {
        values[reference.path] = newValue
        onUpdated(reference)
    }


    val allPluginSettings: Collection<PluginSetting<*, *>>
        get() = pluginSettings.values

    @Suppress("UNCHECKED_CAST")
    fun <V : Any, T : SettingType<V>> getPluginSetting(pluginSettingReference: PluginSettingReference<V, T>) =
        pluginSettings[pluginSettingReference.path] as PluginSetting<V, T>

    @Suppress("UNCHECKED_CAST")
    fun <V : Any, T : SettingType<V>> getPluginSetting(pluginSettingReference: PluginSettingPropertyReference<V, T>) =
        pluginSettings[pluginSettingReference.path] as? PluginSetting<V, T>

    @Suppress("UNCHECKED_CAST")
    fun <V : Any, T : SettingType<V>> setPluginSetting(
        pluginSettingReference: PluginSettingPropertyReference<V, T>,
        setting: PluginSetting<V, T>
    ) {
        pluginSettings[pluginSettingReference.path] = setting
    }

    @Suppress("UNCHECKED_CAST")
    fun <V : Any, T : SettingType<V>> pluginSettingValue(setting: PluginSetting<V, T>): V? =
        values[setting.path] as? V
}


typealias AnySetting = Setting<*, *>

interface Setting<out V : Any, out T : SettingType<V>> : Entity, ActivityCheckerOwner, Validatable<V> {
    val title: String
    val defaultValue: SettingDefaultValue<V>?
    val isRequired: Boolean
    val isSavable: Boolean
    var neededAtPhase: GenerationPhase
    val type: T
}

data class InternalSetting<out V : Any, out T : SettingType<V>>(
    override val path: String,
    override val title: String,
    override val defaultValue: SettingDefaultValue<V>?,
    override val isAvailable: Checker,
    override val isRequired: Boolean,
    override val isSavable: Boolean,
    override var neededAtPhase: GenerationPhase,
    override val validator: SettingValidator<@UnsafeVariance V>,
    override val type: T
) : Setting<V, T>, EntityWithValue<V>()

sealed class SettingImpl<out V : Any, out T : SettingType<V>> : Setting<V, T>

class PluginSetting<out V : Any, out T : SettingType<V>>(
    internal: InternalSetting<V, T>
) : SettingImpl<V, T>(), Setting<V, T> by internal

class ModuleConfiguratorSetting<out V : Any, out T : SettingType<V>>(
    internal: InternalSetting<V, T>
) : SettingImpl<V, T>(), Setting<V, T> by internal

class TemplateSetting<out V : Any, out T : SettingType<V>>(
    internal: InternalSetting<V, T>
) : SettingImpl<V, T>(), Setting<V, T> by internal


sealed class SettingDefaultValue<out V : Any> {
    data class Value<V : Any>(val value: V) : SettingDefaultValue<V>()
    data class Dynamic<V : Any>(
        val getter: ReadingContext.(SettingReference<V, SettingType<V>>) -> V
    ) : SettingDefaultValue<V>()
}


abstract class SettingBuilder<V : Any, T : SettingType<V>>(
    private val path: String,
    private val title: String,
    private val neededAtPhase: GenerationPhase
) {
    var isAvailable: ReadingContext.() -> Boolean = { true }
    open var defaultValue: SettingDefaultValue<V>? = null
    var isSavable: Boolean = false
    var isRequired: Boolean? = null

    fun value(value: V) = SettingDefaultValue.Value(value)
    fun dynamic(getter: ReadingContext.(SettingReference<V, SettingType<V>>) -> V) = SettingDefaultValue.Dynamic(getter)

    protected var validator = SettingValidator<V> { ValidationResult.OK }

    fun validate(validator: SettingValidator<V>) {
        this.validator = this.validator and validator
    }

    fun validate(validator: ReadingContext.(V) -> ValidationResult) {
        this.validator = this.validator and settingValidator(validator)
    }


    abstract val type: T

    fun buildInternal() = InternalSetting(
        path = path,
        title = title,
        defaultValue = defaultValue,
        isAvailable = isAvailable,
        isRequired = isRequired ?: (defaultValue == null),
        isSavable = isSavable,
        neededAtPhase = neededAtPhase,
        validator = validator,
        type = type
    )
}


sealed class SettingSerializer<out V : Any>()

object NonSerializable : SettingSerializer<Nothing>()

data class SerializerImpl<V : Any>(
    val fromString: (String) -> V?,
    val toString: (V) -> String = Any::toString
) : SettingSerializer<V>()

sealed class SettingType<out V : Any> {
    abstract fun parse(context: ParsingContext, value: Any, name: String): TaskResult<V>
    open val serializer: SettingSerializer<V> = NonSerializable
}

object StringSettingType : SettingType<String>() {
    override fun parse(context: ParsingContext, value: Any, name: String) =
        value.parseAs<String>(name)

    override val serializer: SettingSerializer<String> = SerializerImpl(fromString = { it })

    class Builder(
        path: String,
        private val title: String,
        neededAtPhase: GenerationPhase
    ) : SettingBuilder<String, StringSettingType>(path, title, neededAtPhase) {
        fun shouldNotBeBlank() {
            validate(StringValidators.shouldNotBeBlank(title.capitalize()))
        }

        override val type = StringSettingType
    }
}

object BooleanSettingType : SettingType<Boolean>() {
    override fun parse(context: ParsingContext, value: Any, name: String) =
        value.parseAs<Boolean>(name)

    override val serializer: SettingSerializer<Boolean> = SerializerImpl(fromString = { it.toBoolean() })

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

    override val serializer: SettingSerializer<V> = SerializerImpl(fromString = { value ->
        ComputeContext.runInComputeContextWithState(ParsingState.EMPTY) {
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
            get() = DropDownSettingType(values, filter, parser)


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

typealias DropDownSettingTypeFilter <V> =
        ReadingContext.(SettingReference<V, DropDownSettingType<V>>, V) -> Boolean


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

    override val serializer: SettingSerializer<Path> = SerializerImpl(fromString = { Paths.get(it) })

    class Builder(
        path: String,
        private val title: String,
        neededAtPhase: GenerationPhase
    ) : SettingBuilder<Path, PathSettingType>(path, title, neededAtPhase) {

        init {
            validate { pathValue ->
                if (pathValue.toString().isBlank())
                    ValidationResult.ValidationError("${title.capitalize()} should not be blank")
                else ValidationResult.OK
            }
        }

        fun shouldExists() = validate { pathValue ->
            if (isUnitTestMode) return@validate ValidationResult.OK
            if (!Files.exists(pathValue))
                ValidationResult.ValidationError("File for ${title.capitalize()} should exists")
            else ValidationResult.OK
        }

        override val type = PathSettingType
    }
}
