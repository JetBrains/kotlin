package org.jetbrains.kotlin.tools.projectWizard.core

import org.jetbrains.kotlin.tools.projectWizard.core.entity.*
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.*
import org.jetbrains.kotlin.tools.projectWizard.core.service.ServicesManager
import org.jetbrains.kotlin.tools.projectWizard.core.service.SettingSavingWizardService
import org.jetbrains.kotlin.tools.projectWizard.core.service.WizardService
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties


class Context private constructor(
    private val servicesManager: ServicesManager,
    private val isUnitTestMode: Boolean,
    private val settingContext: SettingContext,
    private val propertyContext: PropertyContext,
    private val taskContext: TaskContext
) {

    private lateinit var plugins: List<Plugin>

    private val settingWritingContext = SettingsWriter()

    private val pluginSettings by lazy(LazyThreadSafetyMode.NONE) {
        plugins.flatMap(Plugin::settings).distinctBy(PluginSetting<*, *>::path)
    }

    fun <T> read(reader: Reader.() -> T): T =
        settingWritingContext.reader()

    fun <T> write(writer: Writer.() -> T): T =
        settingWritingContext.writer()

    fun <T> writeSettings(writer: SettingsWriter.() -> T): T =
        settingWritingContext.writer()


    constructor(
        pluginsCreator: PluginsCreator,
        servicesManager: ServicesManager,
        isUnitTestMode: Boolean
    ) : this(
        servicesManager,
        isUnitTestMode,
        SettingContext(),
        PropertyContext(),
        TaskContext()
    ) {
        plugins = pluginsCreator(this).onEach(::initPlugin)
    }

    fun withAdditionalServices(services: List<WizardService>) =
        Context(
            servicesManager.withAdditionalServices(services),
            isUnitTestMode,
            settingContext,
            propertyContext,
            taskContext
        ).also {
            it.plugins = plugins
        }


    fun <V : Any, T : SettingType<V>> pluginSettingDelegate(
        create: (path: String) -> SettingBuilder<V, T>
    ): ReadOnlyProperty<Any, PluginSetting<V, T>> =
        settingContext.settingDelegate(create)

    fun <T : Any> propertyDelegate(
        init: Property.Builder<T>.() -> Unit,
        defaultValue: T
    ) = entityDelegate(propertyContext) { name ->
        Property.Builder(name, defaultValue).apply(init).build()
    }

    fun pipelineTaskDelegate(
        phase: GenerationPhase,
        init: PipelineTask.Builder.() -> Unit
    ) = entityDelegate(taskContext) { name ->
        PipelineTask.Builder(name, phase).apply(init).build()
    }

    fun <A, B : Any> task1Delegate(
        init: Task1.Builder<A, B>.() -> Unit
    ) = entityDelegate(taskContext) { name ->
        Task1.Builder<A, B>(name).apply(init).build()
    }


    private fun initPlugin(plugin: Plugin) {
        for (entityReference in plugin::class.memberProperties) {
            val type = entityReference.returnType.classifier.safeAs<KClass<*>>() ?: continue
            if (type.isSubclassOf(Entity::class)) {
                when (val entity = entityReference.getter.call(plugin)) {
                    is Property<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        propertyContext[entityReference as PropertyReference<Any>] = entity.defaultValue
                    }
                }
            }
        }
    }


    private val pipelineLineTasks: List<PipelineTask>
        get() = plugins
            .flatMap { it.pipelineTasks }

    private fun task(reference: PipelineTaskReference) =
        taskContext.getEntity(reference) as? PipelineTask ?: error(reference.path)

    private val dependencyList: Map<PipelineTask, List<PipelineTask>>
        get() {
            val dependeeMap = pipelineLineTasks.flatMap { task ->
                task.after.map { after -> task to task(after) }
            }

            val dependencyMap = pipelineLineTasks.flatMap { task ->
                task.before.map { before -> task(before) to task }
            }

            return (dependeeMap + dependencyMap)
                .groupBy { it.first }
                .mapValues {
                    it.value.map { it.second }
                }
        }

    fun sortTasks(): TaskResult<List<PipelineTask>> =
        TaskSorter().sort(pipelineLineTasks, dependencyList)


    open inner class Reader {
        val plugins: List<Plugin>
            get() = this@Context.plugins

        val pluginSettings: List<PluginSetting<*, *>>
            get() = this@Context.pluginSettings

        val isUnitTestMode: Boolean
            get() = this@Context.isUnitTestMode

        inline fun <reified S : WizardService> service(noinline filter: (S) -> Boolean = { true }): S =
            serviceByClass(S::class, filter)


        fun <S : WizardService> serviceByClass(klass: KClass<S>, filter: (S) -> Boolean = { true }): S =
            servicesManager.serviceByClass(klass, filter) ?: error("Service ${klass.simpleName} was not found")

        @Suppress("UNCHECKED_CAST")
        val <T : Any> PropertyReference<T>.propertyValue: T
            get() = propertyContext[this] as T

        val <V : Any, T : SettingType<V>> SettingReference<V, T>.settingValue: V
            get() = settingContext[this] ?: error("No value is present for setting `$this`")

        inline val <reified V : Any> KProperty1<out Plugin, PluginSetting<V, SettingType<V>>>.settingValue: V
            get() = reference.settingValue

        inline fun <reified V : Any> KProperty1<out Plugin, PluginSetting<V, SettingType<V>>>.settingValue(): V =
            this.reference.settingValue

        fun <V : Any, T : SettingType<V>> SettingReference<V, T>.settingValue(): V =
            settingContext[this] ?: error("No value is present for setting `$this`")

        val <V : Any, T : SettingType<V>> SettingReference<V, T>.notRequiredSettingValue: V?
            get() = settingContext[this]

        fun <V : Any, T : SettingType<V>> SettingReference<V, T>.notRequiredSettingValue(): V? =
            settingContext[this]

        val <V : Any, T : SettingType<V>> PluginSettingReference<V, T>.pluginSetting: Setting<V, T>
            get() = settingContext.getPluginSetting(this)

        private fun <V : Any> Setting<V, SettingType<V>>.getSavedValueForSetting(): V? {
            if (!isSavable || this !is PluginSetting<*, *>) return null
            val serializer = type.serializer.safeAs<SettingSerializer.Serializer<V>>() ?: return null
            val savedValue = service<SettingSavingWizardService>().getSettingValue(path) ?: return null
            return serializer.fromString(savedValue)
        }

        val <V : Any> SettingReference<V, SettingType<V>>.savedOrDefaultValue: V?
            get() = setting.getSavedValueForSetting() ?: when (val defaultValue = setting.defaultValue) {
                is SettingDefaultValue.Value -> defaultValue.value
                is SettingDefaultValue.Dynamic<V> -> defaultValue.getter(this@Reader, this)
                null -> null
            }

        val <V : Any, T : SettingType<V>> SettingReference<V, T>.setting: Setting<V, T>
            get() = with(this) { getSetting() }

        fun <V : Any, T : SettingType<V>> SettingReference<V, T>.validate() =
            setting.validator.validate(this@Reader, settingValue)

        inline operator fun <T> invoke(reader: Reader.() -> T): T = reader()
    }

    open inner class Writer : Reader() {
        @Deprecated("Allows to get SettingsWriter where it is not supposed to be")
        val unsafeSettingWriter: SettingsWriter
            get() = settingWritingContext

        val eventManager: EventManager
            get() = settingContext.eventManager

        fun <A, B : Any> Task1Reference<A, B>.execute(value: A): TaskResult<B> {
            @Suppress("UNCHECKED_CAST")
            val task = taskContext.getEntity(this) as Task1<A, B>
            return task.action(this@Writer, value)
        }

        fun <T : Any> PropertyReference<T>.update(
            updater: suspend ComputeContext<*>.(T) -> TaskResult<T>
        ): TaskResult<Unit> = compute {
            val (newValue) = updater(propertyValue)
            propertyContext[this@update] = newValue
        }

        fun <T : Any> PropertyReference<List<T>>.addValues(
            vararg values: T
        ): TaskResult<Unit> = update { oldValues -> success(oldValues + values) }

        fun <T : Any> PropertyReference<List<T>>.addValues(
            values: List<T>
        ): TaskResult<Unit> = update { oldValues -> success(oldValues + values) }

        @JvmName("write")
        inline operator fun <T> invoke(writer: Writer.() -> T): T = writer()
    }

    open inner class SettingsWriter : Writer() {
        fun <V : Any, T : SettingType<V>> SettingReference<V, T>.setValue(newValue: V) {
            settingContext[this] = newValue
        }

        fun <V : Any, T : SettingType<V>> SettingReference<V, T>.setSettingValueToItsDefaultIfItIsNotSetValue() {
            val defaultValue = savedOrDefaultValue ?: return
            if (notRequiredSettingValue == null) {
                setValue(defaultValue)
            }
        }

        @JvmName("writeSettings")
        inline operator fun <T> invoke(writer: SettingsWriter.() -> T): T = writer()
    }
}

fun Reader.getUnspecifiedSettings(phases: Set<GenerationPhase>): List<AnySetting> {
    val required = plugins
        .flatMap { plugin ->
            plugin.settings.mapNotNull { setting ->
                if (setting.neededAtPhase !in phases) return@mapNotNull null
                if (setting.isRequired) setting else null
            }
        }.toSet()
    val provided = pluginSettings.map(PluginSetting<*, *>::path).toSet()
    return required.filterNot { it.path in provided }
}


typealias Reader = Context.Reader
typealias Writer = Context.Writer
typealias SettingsWriter = Context.SettingsWriter