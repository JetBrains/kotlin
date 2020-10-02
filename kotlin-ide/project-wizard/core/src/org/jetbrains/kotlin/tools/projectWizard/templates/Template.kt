package org.jetbrains.kotlin.tools.projectWizard.templates

import org.jetbrains.kotlin.tools.projectWizard.Identificator
import org.jetbrains.kotlin.tools.projectWizard.SettingsOwner
import org.jetbrains.kotlin.tools.projectWizard.WizardRunConfiguration


import org.jetbrains.kotlin.tools.projectWizard.core.*

import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.*
import org.jetbrains.kotlin.tools.projectWizard.enumSettingImpl
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.TargetConfigurationIR
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.RunConfigurationsPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.StructurePlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModulesToIrConversionData
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

fun <V : Any, T : SettingType<V>> Reader.settingValue(module: Module, setting: TemplateSetting<V, T>): V? =
    withSettingsOf(module) {
        setting.reference.notRequiredSettingValue
    }


abstract class Template : SettingsOwner, EntitiesOwnerDescriptor, DisplayableSettingItem {
    final override fun <V : Any, T : SettingType<V>> settingDelegate(
        create: (path: String) -> SettingBuilder<V, T>
    ): ReadOnlyProperty<Any, TemplateSetting<V, T>> = cached { name ->
        TemplateSetting(create(name).buildInternal())
    }

    abstract val title: String
    abstract val description: String

    abstract fun isSupportedByModuleType(module: Module): Boolean

    override val text: String get() = title

    open fun isApplicableTo(
        reader: Reader,
        module: Module
    ): Boolean = true

    open val settings: List<TemplateSetting<*, *>> = emptyList()
    open val interceptionPoints: List<InterceptionPoint<Any>> = emptyList()

    fun SettingsWriter.initDefaultValuesFor(module: Module) {
        withSettingsOf(module) {
            settings.forEach { setting ->
                setting.reference.setSettingValueToItsDefaultIfItIsNotSetValue()
            }
        }
    }

    open fun Writer.getRequiredLibraries(module: ModuleIR): List<DependencyIR> = emptyList()

    //TODO: use setting reading context
    open fun Writer.getIrsToAddToBuildFile(
        module: ModuleIR
    ): List<BuildSystemIR> = emptyList()

    open fun Writer.runArbitratyTask(
        module: ModuleIR
    ): TaskResult<Unit> = UNIT_SUCCESS

    open fun updateTargetIr(
        module: ModuleIR,
        targetConfigurationIR: TargetConfigurationIR
    ): TargetConfigurationIR = targetConfigurationIR

    open fun Reader.getFileTemplates(module: ModuleIR): List<FileTemplateDescriptorWithPath> = emptyList()
    open fun Reader.getAdditionalSettings(module: Module): Map<String, Any> = emptyMap()

    open fun createInterceptors(module: ModuleIR): List<TemplateInterceptor> = emptyList()

    open fun Reader.createRunConfigurations(module: ModuleIR): List<WizardRunConfiguration> = emptyList()


    open fun Reader.updateModuleIR(module: ModuleIR): ModuleIR = module
    open fun Reader.updateBuildFileIRs(irs: List<BuildSystemIR>): List<BuildSystemIR> = irs

    open fun createRootBuildFileIrs(): List<BuildSystemIR> = emptyList()

    fun Writer.applyToSourceset(
        module: ModuleIR
    ): TaskResult<TemplateApplicationResult> = compute {

        val librariesToAdd = getRequiredLibraries(module)
        val irsToAddToBuildFile = getIrsToAddToBuildFile(module)
        runArbitratyTask(module).ensure()

        val targetsUpdater = when (module) {
            is MultiplatformModuleIR -> { target: TargetConfigurationIR ->
                if (target.targetName == module.name) updateTargetIr(module, target)
                else target
            }
            is FakeMultiplatformModuleIR -> {
                module.targets.map { expectedTarget ->
                    { configurationIR: TargetConfigurationIR ->
                        if (configurationIR.targetName == expectedTarget.name) updateTargetIr(expectedTarget, configurationIR)
                        else configurationIR
                    }
                }.reduce(::compose)
            }
            else -> idFunction()
        }

        RunConfigurationsPlugin.configurations.addValues(createRunConfigurations(module))

        TemplateApplicationResult(librariesToAdd, irsToAddToBuildFile, targetsUpdater) { updateModuleIR(it) }
    }

    fun Reader.settingsAsMap(module: Module): Map<String, Any> = mutableMapOf<String, Any>().apply {
        withSettingsOf(module) {
            settings.associateTo(this@apply) { setting ->
                setting.path to setting.reference.settingValue
            }
        }
        putAll(createDefaultSettings())
        putAll(getAdditionalSettings(module))
    }


    private fun Reader.createDefaultSettings() = mapOf(
        "projectName" to StructurePlugin.name.settingValue.capitalize()
    )

    override fun equals(other: Any?): Boolean =
        other.safeAs<Template>()?.id == id

    override fun hashCode(): Int = id.hashCode()

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

    @Suppress("UNCHECKED_CAST")
    inline fun <reified E> enumSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        crossinline init: DropDownSettingType.Builder<E>.() -> Unit = {}
    ): ReadOnlyProperty<Any, TemplateSetting<E, DropDownSettingType<E>>> where E : Enum<E>, E : DisplayableSettingItem =
        enumSettingImpl(title, neededAtPhase, init) as ReadOnlyProperty<Any, TemplateSetting<E, DropDownSettingType<E>>>

    companion object {
        fun parser(templateId: Identificator): Parser<Template> = mapParser { map, path ->
            val (id) = map.parseValue<String>(path, "id")
            val (template) = state.idToTemplate[id].toResult { TemplateNotFoundError(id) }
            val (settingsWithValues) = parseSettingsMap(
                path,
                map,
                template.settings.map { setting ->
                    val reference = withSettingsOf(templateId, template) { setting.reference }
                    reference to setting
                }
            )
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

fun Writer.applyTemplateToModule(
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
    val updateTarget: (TargetConfigurationIR) -> TargetConfigurationIR,
    val updateModuleIR: (ModuleIR) -> ModuleIR
) {
    companion object {
        val EMPTY = TemplateApplicationResult(
            librariesToAdd = emptyList(),
            irsToAddToBuildFile = emptyList(),
            updateTarget = { it },
            updateModuleIR = { it },
        )
    }
}

fun List<TemplateApplicationResult>.fold() =
    fold(TemplateApplicationResult.EMPTY, TemplateApplicationResult::plus)

operator fun TemplateApplicationResult.plus(other: TemplateApplicationResult) =
    TemplateApplicationResult(
        librariesToAdd + other.librariesToAdd,
        irsToAddToBuildFile + other.irsToAddToBuildFile,
        updateTarget andThen other.updateTarget,
        updateModuleIR andThen other.updateModuleIR,
    )