package org.jetbrains.kotlin.tools.projectWizard.templates

import org.jetbrains.kotlin.tools.projectWizard.Identificator
import org.jetbrains.kotlin.tools.projectWizard.SettingsOwner
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.entity.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.TargetConfigurationIR
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.StructurePlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version
import org.jetbrains.kotlin.tools.projectWizard.transformers.interceptors.InterceptionPoint
import org.jetbrains.kotlin.tools.projectWizard.transformers.interceptors.TemplateInterceptor
import java.nio.file.Path
import kotlin.properties.ReadOnlyProperty

interface TemplateEnvironment {
    val <V : Any, T : SettingType<V>> TemplateSetting<V, T>.reference: TemplateSettingReference<V, T>
}

class IdBasedTemplateEnvironment(
    private val template: Template,
    private val moduleId: Identificator
) : TemplateEnvironment {
    override val <V : Any, T : SettingType<V>> TemplateSetting<V, T>.reference
        get() = IdBasedTemplateSettingReference(template, moduleId, this)
}

class ModuleBasedTemplateEnvironment<Q : Template>(
    val template: Q,
    private val module: Module
) : TemplateEnvironment {
    override val <V : Any, T : SettingType<V>> TemplateSetting<V, T>.reference
        get() = ModuleBasedTemplateSettingReference(template, module, this)
}

fun <T> withSettingsOf(
    module: Module,
    template: Template = module.template!!,
    function: TemplateEnvironment.() -> T
): T = function(ModuleBasedTemplateEnvironment(template, module))

fun <T> withSettingsOf(
    identificator: Identificator,
    template: Template,
    function: TemplateEnvironment.() -> T
): T = function(IdBasedTemplateEnvironment(template, identificator))


abstract class Template : SettingsOwner {
    final override fun <V : Any, T : SettingType<V>> settingDelegate(
        create: (path: String) -> SettingBuilder<V, T>
    ): ReadOnlyProperty<Any, TemplateSetting<V, T>> = cached { name ->
        TemplateSetting(create(name).buildInternal())
    }

    abstract val title: String
    abstract val htmlDescription: String
    abstract val moduleTypes: Set<ModuleType>

    open fun isApplicableTo(module: Module): Boolean = true

    open val settings: List<TemplateSetting<*, *>> = emptyList()
    open val interceptionPoints: List<InterceptionPoint<Any>> = emptyList()

    open fun TaskRunningContext.getRequiredLibraries(module: ModuleIR): List<DependencyIR> = emptyList()

    //TODO: use setting reading context
    open fun TaskRunningContext.getIrsToAddToBuildFile(
        module: ModuleIR
    ): List<BuildSystemIR> = emptyList()

    open fun updateTargetIr(
        module: ModuleIR,
        targetConfigurationIR: TargetConfigurationIR
    ): TargetConfigurationIR = targetConfigurationIR

    open fun TaskRunningContext.getFileTemplates(module: ModuleIR): List<FileTemplateDescriptorWithPath> = emptyList()

    open fun createInterceptors(module: ModuleIR): List<TemplateInterceptor> = emptyList()

    fun TaskRunningContext.applyToSourceset(
        module: ModuleIR
    ): TaskResult<TemplateApplicationResult> {
        val librariesToAdd = getRequiredLibraries(module)
        val irsToAddToBuildFile = getIrsToAddToBuildFile(module)

        val targetsUpdater = when (module) {
            is SourcesetModuleIR -> { target: TargetConfigurationIR ->
                if (target.targetName == module.name) updateTargetIr(module, target)
                else target
            }
            else -> idFunction()
        }

        val result = TemplateApplicationResult(librariesToAdd, irsToAddToBuildFile, targetsUpdater)
        return result.asSuccess()
    }

    fun TaskRunningContext.settingsAsMap(module: Module): Map<String, Any> =
        withSettingsOf(module) {
            settings.associate { setting ->
                setting.path to setting.reference.settingValue
            }
        } + createDefaultSettings()


    private fun TaskRunningContext.createDefaultSettings() = mapOf(
        "projectName" to StructurePlugin::name.settingValue.capitalize()
    )

    @Suppress("UNCHECKED_CAST")
    final override fun <V : DisplayableSettingItem> dropDownSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        parser: Parser<V>,
        init: DropDownSettingType.Builder<V>.() -> Unit
    ): ReadOnlyProperty<Any, TemplateSetting<V, DropDownSettingType<V>>> =
        super.dropDownSetting(
            title,
            neededAtPhase,
            parser,
            init
        ) as ReadOnlyProperty<Any, TemplateSetting<V, DropDownSettingType<V>>>

    @Suppress("UNCHECKED_CAST")
    final override fun stringSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        init: StringSettingType.Builder.() -> Unit
    ): ReadOnlyProperty<Any, TemplateSetting<String, StringSettingType>> =
        super.stringSetting(
            title,
            neededAtPhase,
            init
        ) as ReadOnlyProperty<Any, TemplateSetting<String, StringSettingType>>

    @Suppress("UNCHECKED_CAST")
    final override fun booleanSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        init: BooleanSettingType.Builder.() -> Unit
    ): ReadOnlyProperty<Any, TemplateSetting<Boolean, BooleanSettingType>> =
        super.booleanSetting(
            title,
            neededAtPhase,
            init
        ) as ReadOnlyProperty<Any, TemplateSetting<Boolean, BooleanSettingType>>

    @Suppress("UNCHECKED_CAST")
    final override fun <V : Any> valueSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        parser: Parser<V>,
        init: ValueSettingType.Builder<V>.() -> Unit
    ): ReadOnlyProperty<Any, TemplateSetting<V, ValueSettingType<V>>> =
        super.valueSetting(
            title,
            neededAtPhase,
            parser,
            init
        ) as ReadOnlyProperty<Any, TemplateSetting<V, ValueSettingType<V>>>

    @Suppress("UNCHECKED_CAST")
    final override fun versionSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        init: VersionSettingType.Builder.() -> Unit
    ): ReadOnlyProperty<Any, TemplateSetting<Version, VersionSettingType>> =
        super.versionSetting(
            title,
            neededAtPhase,
            init
        ) as ReadOnlyProperty<Any, TemplateSetting<Version, VersionSettingType>>

    @Suppress("UNCHECKED_CAST")
    final override fun <V : Any> listSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        parser: Parser<V>,
        init: ListSettingType.Builder<V>.() -> Unit
    ): ReadOnlyProperty<Any, TemplateSetting<List<V>, ListSettingType<V>>> =
        super.listSetting(
            title,
            neededAtPhase,
            parser,
            init
        ) as ReadOnlyProperty<Any, TemplateSetting<List<V>, ListSettingType<V>>>


    @Suppress("UNCHECKED_CAST")
    final override fun pathSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        init: PathSettingType.Builder.() -> Unit
    ): ReadOnlyProperty<Any, TemplateSetting<Path, PathSettingType>> =
        super.pathSetting(
            title,
            neededAtPhase,
            init
        ) as ReadOnlyProperty<Any, TemplateSetting<Path, PathSettingType>>

    inline fun <reified E> enumSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        crossinline init: DropDownSettingType.Builder<E>.() -> Unit = {}
    ) where E : Enum<E>, E : DisplayableSettingItem = dropDownSetting<E>(title, neededAtPhase, enumParser()) {
        values = enumValues<E>().asList()
        init()
    }

    companion object {
        fun parser(sourcesetIdentificator: Identificator): Parser<Template> = mapParser { map, path ->
            val (id) = map.parseValue<String>(path, "id")
            val (template) = state.idToTemplate[id].toResult { TemplateNotFoundError(id) }
            val (settingsWithValues) = template.settings.mapComputeM { setting ->
                val (settingValue) = map[setting.path].toResult { ParseError("No value was found for a key `$path.${setting.path}`") }
                val reference = withSettingsOf(sourcesetIdentificator, template) { setting.reference }
                setting.type.parse(this, settingValue, setting.path).map { reference to it }
            }.sequence()
            updateState { it.withSettings(settingsWithValues) }
            template
        } or valueParserM { value, path ->
            val (id) = value.parseAs<String>(path)
            state.idToTemplate[id].toResult { TemplateNotFoundError(id) }
        }
    }
}

fun Template.settings(module: Module) = withSettingsOf(module) {
    settings.map { it.reference }
}

fun TaskRunningContext.applyTemplateToModule(
    template: Template?,
    module: ModuleIR
): TaskResult<TemplateApplicationResult> = when (template) {
    null -> TemplateApplicationResult.EMPTY.asSuccess()
    else -> with(template) {
        applyToSourceset(module)
    }
}


data class TemplateApplicationResult(
    val librariesToAdd: List<DependencyIR>,
    val irsToAddToBuildFile: List<BuildSystemIR>,
    val updateTarget: (TargetConfigurationIR) -> TargetConfigurationIR
) {
    companion object {
        val EMPTY = TemplateApplicationResult(
            librariesToAdd = emptyList(),
            irsToAddToBuildFile = emptyList(),
            updateTarget = { it }
        )
    }
}

fun List<TemplateApplicationResult>.fold() =
    fold(TemplateApplicationResult.EMPTY, TemplateApplicationResult::plus)

operator fun TemplateApplicationResult.plus(other: TemplateApplicationResult) =
    TemplateApplicationResult(
        librariesToAdd + other.librariesToAdd,
        irsToAddToBuildFile + other.irsToAddToBuildFile,
        updateTarget andThen other.updateTarget
    )