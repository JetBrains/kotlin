package org.jetbrains.kotlin.tools.projectWizard.core

import org.jetbrains.kotlin.tools.projectWizard.PropertiesOwner
import org.jetbrains.kotlin.tools.projectWizard.SettingsOwner
import org.jetbrains.kotlin.tools.projectWizard.core.entity.*
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.*
import org.jetbrains.kotlin.tools.projectWizard.enumSettingImpl
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version
import java.nio.file.Path
import kotlin.properties.ReadOnlyProperty

typealias PluginsCreator = (Context) -> List<Plugin>


abstract class Plugin(override val context: Context) : EntityBase(),
    ContextOwner,
    EntitiesOwnerDescriptor,
    EntitiesOwner<Plugin> {
    override val descriptor get() = this
    override val id: String get() = path

    val reference = this::class
    abstract override val path: String

    abstract val properties: List<Property<*>>
    abstract val settings: List<PluginSetting<*, *>>
    abstract val pipelineTasks: List<PipelineTask>
}

abstract class PluginSettingsOwner : SettingsOwner, PropertiesOwner {
    abstract val pluginPath: String

    // properties
    override fun <T : Any> propertyDelegate(
        create: (path: String) -> PropertyBuilder<T>
    ): ReadOnlyProperty<Any, Property<T>> =
        cached { name -> PluginProperty(create(withPluginPath(name)).build()) }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> property(
        defaultValue: T,
        init: PropertyBuilder<T>.() -> Unit,
    ): ReadOnlyProperty<Any, PluginProperty<T>> =
        super.property(defaultValue, init) as ReadOnlyProperty<Any, PluginProperty<T>>

    override fun <T : Any> listProperty(
        vararg defaultValues: T,
        init: PropertyBuilder<List<T>>.() -> Unit,
    ): ReadOnlyProperty<Any, PluginProperty<List<T>>> =
        property(defaultValues.toList(), init)

    // pippeline tasks

    fun pipelineTask(
        phase: GenerationPhase,
        init: PipelineTask.Builder.() -> Unit
    ): ReadOnlyProperty<Any, PipelineTask> =
        cached { name -> PipelineTask.Builder(withPluginPath(name), phase).apply(init).build() }

    // task1

    fun <A, B : Any> task1(
        init: Task1.Builder<A, B>.() -> Unit
    ): ReadOnlyProperty<Any, Task1<A, B>> = cached { name -> Task1.Builder<A, B>(withPluginPath(name)).apply(init).build() }

    // settings

    override fun <V : Any, T : SettingType<V>> settingDelegate(
        create: (path: String) -> SettingBuilder<V, T>
    ): ReadOnlyProperty<Any, PluginSetting<V, T>> = cached { name -> PluginSetting(create(withPluginPath(name)).buildInternal()) }

    // setting types

    @Suppress("UNCHECKED_CAST")
    override fun <V : DisplayableSettingItem> dropDownSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        parser: Parser<V>,
        init: DropDownSettingType.Builder<V>.() -> Unit
    ): ReadOnlyProperty<Any, PluginSetting<V, DropDownSettingType<V>>> =
        super.dropDownSetting(
            title,
            neededAtPhase,
            parser,
            init
        ) as ReadOnlyProperty<Any, PluginSetting<V, DropDownSettingType<V>>>

    @Suppress("UNCHECKED_CAST")
    override fun stringSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        init: StringSettingType.Builder.() -> Unit
    ): ReadOnlyProperty<Any, PluginSetting<String, StringSettingType>> =
        super.stringSetting(
            title,
            neededAtPhase,
            init
        ) as ReadOnlyProperty<Any, PluginSetting<String, StringSettingType>>

    @Suppress("UNCHECKED_CAST")
    override fun booleanSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        init: BooleanSettingType.Builder.() -> Unit
    ): ReadOnlyProperty<Any, PluginSetting<Boolean, BooleanSettingType>> =
        super.booleanSetting(
            title,
            neededAtPhase,
            init
        ) as ReadOnlyProperty<Any, PluginSetting<Boolean, BooleanSettingType>>

    @Suppress("UNCHECKED_CAST")
    override fun <V : Any> valueSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        parser: Parser<V>,
        init: ValueSettingType.Builder<V>.() -> Unit
    ): ReadOnlyProperty<Any, PluginSetting<V, ValueSettingType<V>>> =
        super.valueSetting(
            title,
            neededAtPhase,
            parser,
            init
        ) as ReadOnlyProperty<Any, PluginSetting<V, ValueSettingType<V>>>

    @Suppress("UNCHECKED_CAST")
    override fun versionSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        init: VersionSettingType.Builder.() -> Unit
    ): ReadOnlyProperty<Any, PluginSetting<Version, VersionSettingType>> =
        super.versionSetting(
            title,
            neededAtPhase,
            init
        ) as ReadOnlyProperty<Any, PluginSetting<Version, VersionSettingType>>

    @Suppress("UNCHECKED_CAST")
    override fun <V : Any> listSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        parser: Parser<V>,
        init: ListSettingType.Builder<V>.() -> Unit
    ): ReadOnlyProperty<Any, PluginSetting<List<V>, ListSettingType<V>>> =
        super.listSetting(
            title,
            neededAtPhase,
            parser,
            init
        ) as ReadOnlyProperty<Any, PluginSetting<List<V>, ListSettingType<V>>>

    @Suppress("UNCHECKED_CAST")
    override fun pathSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        init: PathSettingType.Builder.() -> Unit
    ): ReadOnlyProperty<Any, PluginSetting<Path, PathSettingType>> =
        super.pathSetting(title, neededAtPhase, init) as ReadOnlyProperty<Any, PluginSetting<Path, PathSettingType>>

    @Suppress("UNCHECKED_CAST")
    inline fun <reified E> enumSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        crossinline init: DropDownSettingType.Builder<E>.() -> Unit = {}
    ): ReadOnlyProperty<Any, PluginSetting<E, DropDownSettingType<E>>> where E : Enum<E>, E : DisplayableSettingItem =
        enumSettingImpl(title, neededAtPhase, init) as ReadOnlyProperty<Any, PluginSetting<E, DropDownSettingType<E>>>

    // utils

    private fun withPluginPath(name: String): String = "$pluginPath.$name"
}

