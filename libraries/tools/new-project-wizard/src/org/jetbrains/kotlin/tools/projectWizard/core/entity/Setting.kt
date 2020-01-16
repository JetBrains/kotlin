package org.jetbrains.kotlin.tools.projectWizard.core.entity

import org.jetbrains.kotlin.tools.projectWizard.Identificator
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.ModuleConfigurator
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Sourceset
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version
import org.jetbrains.kotlin.tools.projectWizard.templates.Template
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

sealed class SettingReference<out V : Any, out T : SettingType<V>> {
    abstract val path: String
    abstract val type: KClass<out T>

    abstract fun Context.getSetting(): Setting<V, T>

    final override fun toString() = path
    final override fun equals(other: Any?) = other.safeAs<SettingReference<*, *>>()?.path == path
    final override fun hashCode() = path.hashCode()
}

data class PluginSettingReference<V : Any, T : SettingType<V>>(
    override val path: String,
    override val type: KClass<T>
) : SettingReference<V, T>() {

    constructor(kProperty: KProperty1<out Plugin, PluginSetting<V, T>>, type: KClass<T>) :
            this(kProperty.path, type)

    @Suppress("UNCHECKED_CAST")
    constructor(setting: PluginSetting<V, T>) :
            this(setting.path, setting.type::class as KClass<T>)

    override fun Context.getSetting(): Setting<V, T> =
        settingContext.getPluginSetting(this@PluginSettingReference)
}

data class ModuleConfiguratorSettingReference<V : Any, T : SettingType<V>>(
    val descriptor: ModuleConfigurator,
    val moduleId: Identificator,
    val setting: ModuleConfiguratorSetting<V, T>
) : SettingReference<V, T>() {
    constructor(descriptor: ModuleConfigurator, module: Module, setting: ModuleConfiguratorSetting<V, T>) :
            this(descriptor, module.identificator, setting)

    override val path: String
        get() = "${descriptor.id}/$moduleId/${setting.path}"

    override val type: KClass<out T>
        get() = setting.type::class

    override fun Context.getSetting(): Setting<V, T> = setting
}

sealed class TemplateSettingReference<V : Any, T : SettingType<V>> : SettingReference<V, T>() {
    abstract val descriptor: Template
    abstract val setting: TemplateSetting<V, T>
    abstract val sourcesetId: Identificator

    override val path: String
        get() = "${descriptor.id}/$sourcesetId/${setting.path}"

    override val type: KClass<out T>
        get() = setting.type::class

    override fun Context.getSetting(): Setting<V, T> = setting
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

    fun initPluginSettings(settings: List<PluginSetting<*, *>>) {
        for (setting in settings) {
            setting.defaultValue?.let { values[setting.path] = it }
        }
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
    val defaultValue: V?
    val isRequired: Boolean
    var neededAtPhase: GenerationPhase
    val type: T

}

data class InternalSetting<out V : Any, out T : SettingType<V>>(
    override val path: String,
    override val title: String,
    override val defaultValue: V?,
    override val activityChecker: Checker,
    override val isRequired: Boolean,
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


abstract class SettingBuilder<V : Any, T : SettingType<V>>(
    private val path: String,
    private val title: String,
    private val neededAtPhase: GenerationPhase
) {
    var checker: Checker = Checker.ALWAYS_AVAILABLE
    var defaultValue: V? = null

    protected var validator = SettingValidator<V> { ValidationResult.OK }

    fun validate(validator: SettingValidator<V>) {
        this.validator = this.validator and validator
    }

    fun validate(validator: ValuesReadingContext.(V) -> ValidationResult) {
        this.validator = this.validator and settingValidator(validator)
    }


    abstract val type: T

    fun buildInternal() = InternalSetting(
        path = path,
        title = title,
        defaultValue = defaultValue,
        activityChecker = checker,
        isRequired = defaultValue == null,
        neededAtPhase = neededAtPhase,
        validator = validator,
        type = type
    )
}


sealed class SettingType<out V : Any> {
    abstract fun parse(context: ParsingContext, value: Any, name: String): TaskResult<V>
}

object StringSettingType : SettingType<String>() {
    override fun parse(context: ParsingContext, value: Any, name: String) =
        value.parseAs<String>(name)

    class Builder(
        path: String,
        private val title: String,
        neededAtPhase: GenerationPhase
    ) : SettingBuilder<String, StringSettingType>(path, title, neededAtPhase) {
        fun shouldNotBeBlank() = validate { value: String ->
            if (value.isBlank()) ValidationResult.ValidationError("`${title.capitalize()}` should not be blank ")
            else ValidationResult.OK
        }

        override val type = StringSettingType
    }
}

object BooleanSettingType : SettingType<Boolean>() {
    override fun parse(context: ParsingContext, value: Any, name: String) =
        value.parseAs<Boolean>(name)

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
    }
}

typealias DropDownSettingTypeFilter<V> = ValuesReadingContext.(SettingReference<V, DropDownSettingType<V>>, V) -> Boolean


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
                if (value is Validatable<*>) value.validator.validate(this, value)
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
                        is Validatable<*> -> value.validator.validate(this, value)
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
