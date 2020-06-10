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
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

typealias PluginReference = KClass<out Plugin>
typealias PluginsCreator = (Context) -> List<Plugin>


abstract class Plugin(override val context: Context) : EntityBase(),
    SettingsOwner,
    ContextOwner,
    EntitiesOwnerDescriptor,
    EntitiesOwner<Plugin> {
    override val descriptor get() = this
    override val id: String get() = path

    override fun <V : Any, T : SettingType<V>> settingDelegate(
        create: (path: String) -> SettingBuilder<V, T>
    ): ReadOnlyProperty<Any, PluginSetting<V, T>> = context.pluginSettingDelegate(create)

    val reference = this::class
    override val path = reference.path
    open val title: String = reference.name

    abstract val settings: List<PluginSetting<*, *>>
    abstract val pipelineTasks: List<PipelineTask>

    fun pipelineTask(phase: GenerationPhase, init: PipelineTask.Builder.() -> Unit) =
        context.pipelineTaskDelegate(phase, init)

    fun <A, B : Any> task1(init: Task1.Builder<A, B>.() -> Unit) =
        context.task1Delegate(init)

    fun <T : Any> property(defaultValue: T, init: Property.Builder<T>.() -> Unit = {}) =
        context.propertyDelegate(init, defaultValue)

    fun <T : Any> listProperty(vararg defaultValues: T, init: Property.Builder<List<T>>.() -> Unit = {}) =
        property(defaultValues.toList(), init)


    // setting types

    @Suppress("UNCHECKED_CAST")
    final override fun <V : DisplayableSettingItem> dropDownSetting(
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
    final override fun stringSetting(
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
    final override fun booleanSetting(
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
    final override fun <V : Any> valueSetting(
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
    final override fun versionSetting(
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
    final override fun <V : Any> listSetting(
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
    final override fun pathSetting(
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
}

val PluginReference.withParentPlugins
    get() = generateSequence(this) { klass ->
        klass.supertypes.firstOrNull { supertype ->
                supertype.classifier?.safeAs<KClass<Plugin>>()
                    ?.isSubclassOf(Plugin::class) == true
            }?.classifier
            ?.safeAs<KClass<Plugin>>()
            ?.takeIf { superClass ->
                superClass.simpleName != null
            }
    }

val PluginReference.name
    get() = simpleName
        ?.removeSuffix("Plugin")
        ?.decapitalize()
        .orEmpty()

val PluginReference.path
    get() = withParentPlugins.mapNotNull { klass ->
            klass.name.takeIf { it.isNotEmpty() }
        }.toList()
        .reversed()
        .joinToString(".")

