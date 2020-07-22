package org.jetbrains.kotlin.tools.projectWizard.core

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

    companion object : SettingsOwner {
        fun pipelineTask(
            prefix: String,
            phase: GenerationPhase,
            init: PipelineTask.Builder.() -> Unit
        ): ReadOnlyProperty<Any, PipelineTask> =
            cached { name -> PipelineTask.Builder(withPrefix(prefix, name), phase).apply(init).build() }

        fun <A, B : Any> task1(
            prefix: String,
            init: Task1.Builder<A, B>.() -> Unit
        ): ReadOnlyProperty<Any, Task1<A, B>> = cached { name -> Task1.Builder<A, B>(withPrefix(prefix, name)).apply(init).build() }

        fun <T : Any> property(
            prefix: String,
            defaultValue: T,
            init: Property.Builder<T>.() -> Unit = {}
        ): ReadOnlyProperty<Any, Property<T>> =
            cached { name -> Property.Builder(withPrefix(prefix, name), defaultValue).apply(init).build() }

        fun <T : Any> listProperty(prefix: String, vararg defaultValues: T, init: Property.Builder<List<T>>.() -> Unit = {}) =
            property(prefix, defaultValues.toList(), init)

        private fun withPrefix(name: String, prefix: String): String = if (prefix.isNotEmpty()) "$prefix.$name" else name

        override fun <V : Any, T : SettingType<V>> settingDelegate(
            prefix: String,
            create: (path: String) -> SettingBuilder<V, T>
        ): ReadOnlyProperty<Any, PluginSetting<V, T>> = cached { name -> PluginSetting(create(withPrefix(name, prefix)).buildInternal()) }

        // setting types

        @Suppress("UNCHECKED_CAST")
        override fun <V : DisplayableSettingItem> dropDownSetting(
            title: String,
            neededAtPhase: GenerationPhase,
            parser: Parser<V>,
            prefix: String,
            init: DropDownSettingType.Builder<V>.() -> Unit
        ): ReadOnlyProperty<Any, PluginSetting<V, DropDownSettingType<V>>> =
            super.dropDownSetting(
                title,
                neededAtPhase,
                parser,
                prefix,
                init
            ) as ReadOnlyProperty<Any, PluginSetting<V, DropDownSettingType<V>>>

        @Suppress("UNCHECKED_CAST")
        override fun stringSetting(
            title: String,
            neededAtPhase: GenerationPhase,
            prefix: String,
            init: StringSettingType.Builder.() -> Unit
        ): ReadOnlyProperty<Any, PluginSetting<String, StringSettingType>> =
            super.stringSetting(
                title,
                neededAtPhase,
                prefix,
                init
            ) as ReadOnlyProperty<Any, PluginSetting<String, StringSettingType>>

        @Suppress("UNCHECKED_CAST")
        override fun booleanSetting(
            title: String,
            neededAtPhase: GenerationPhase,
            prefix: String,
            init: BooleanSettingType.Builder.() -> Unit
        ): ReadOnlyProperty<Any, PluginSetting<Boolean, BooleanSettingType>> =
            super.booleanSetting(
                title,
                neededAtPhase,
                prefix,
                init
            ) as ReadOnlyProperty<Any, PluginSetting<Boolean, BooleanSettingType>>

        @Suppress("UNCHECKED_CAST")
        override fun <V : Any> valueSetting(
            title: String,
            neededAtPhase: GenerationPhase,
            parser: Parser<V>,
            prefix: String,
            init: ValueSettingType.Builder<V>.() -> Unit
        ): ReadOnlyProperty<Any, PluginSetting<V, ValueSettingType<V>>> =
            super.valueSetting(
                title,
                neededAtPhase,
                parser,
                prefix,
                init
            ) as ReadOnlyProperty<Any, PluginSetting<V, ValueSettingType<V>>>

        @Suppress("UNCHECKED_CAST")
        override fun versionSetting(
            title: String,
            neededAtPhase: GenerationPhase,
            prefix: String,
            init: VersionSettingType.Builder.() -> Unit
        ): ReadOnlyProperty<Any, PluginSetting<Version, VersionSettingType>> =
            super.versionSetting(
                title,
                neededAtPhase,
                prefix,
                init
            ) as ReadOnlyProperty<Any, PluginSetting<Version, VersionSettingType>>

        @Suppress("UNCHECKED_CAST")
        override fun <V : Any> listSetting(
            title: String,
            neededAtPhase: GenerationPhase,
            parser: Parser<V>,
            prefix: String,
            init: ListSettingType.Builder<V>.() -> Unit
        ): ReadOnlyProperty<Any, PluginSetting<List<V>, ListSettingType<V>>> =
            super.listSetting(
                title,
                neededAtPhase,
                parser,
                prefix,
                init
            ) as ReadOnlyProperty<Any, PluginSetting<List<V>, ListSettingType<V>>>

        @Suppress("UNCHECKED_CAST")
        override fun pathSetting(
            title: String,
            neededAtPhase: GenerationPhase,
            prefix: String,
            init: PathSettingType.Builder.() -> Unit
        ): ReadOnlyProperty<Any, PluginSetting<Path, PathSettingType>> =
            super.pathSetting(title, neededAtPhase, prefix, init) as ReadOnlyProperty<Any, PluginSetting<Path, PathSettingType>>

        @Suppress("UNCHECKED_CAST")
        inline fun <reified E> enumSetting(
            title: String,
            neededAtPhase: GenerationPhase,
            prefix: String,
            crossinline init: DropDownSettingType.Builder<E>.() -> Unit = {}
        ): ReadOnlyProperty<Any, PluginSetting<E, DropDownSettingType<E>>> where E : Enum<E>, E : DisplayableSettingItem =
            enumSettingImpl(title, neededAtPhase, prefix, init) as ReadOnlyProperty<Any, PluginSetting<E, DropDownSettingType<E>>>
    }
}

