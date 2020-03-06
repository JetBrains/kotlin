package org.jetbrains.kotlin.tools.projectWizard.wizard

import org.jetbrains.kotlin.tools.projectWizard.core.context.ReadingContext
import org.jetbrains.kotlin.tools.projectWizard.core.context.WritingContext
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.context.SettingsWritingContext
import org.jetbrains.kotlin.tools.projectWizard.core.entity.*
import org.jetbrains.kotlin.tools.projectWizard.core.service.WizardService
import org.jetbrains.kotlin.tools.projectWizard.core.service.ServicesManager
import org.jetbrains.kotlin.tools.projectWizard.core.service.SettingSavingWizardService
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.withAllSubModules

abstract class Wizard(createPlugins: PluginsCreator, val servicesManager: ServicesManager, private val isUnitTestMode: Boolean) {
    val context = Context(createPlugins, EventManager())
    val valuesReadingContext = ReadingContext(context, servicesManager, isUnitTestMode)
    private val settingsWritingContext = SettingsWritingContext(context, servicesManager, isUnitTestMode)
    protected val plugins = context.plugins
    protected val pluginSettings = plugins.flatMap { it.declaredSettings }.distinctBy { it.path }

    inline fun <reified V : Any> settingValue(setting: PluginSettingReference<V, SettingType<V>>): V =
        with(valuesReadingContext) { setting.settingValue }

    private fun Context.checkAllRequiredSettingPresent(phases: Set<GenerationPhase>): TaskResult<Unit> =
        getUnspecifiedSettings(phases).let { unspecifiedSettings ->
            if (unspecifiedSettings.isEmpty()) UNIT_SUCCESS
            else Failure(RequiredSettingsIsNotPresentError(unspecifiedSettings.map { it.path }))
        }


    private fun initNonPluginDefaultValues() {
        with(settingsWritingContext) {
            KotlinPlugin::modules.reference.notRequiredSettingValue?.withAllSubModules(includeSourcesets = true)?.forEach { module ->
                with(module) { initDefaultValuesForSettings() }
            }
        }
    }

    protected fun initPluginSettingsDefaultValues() {
        with(settingsWritingContext) {
            for (setting in pluginSettings) {
                setting.reference.setSettingValueToItsDefaultIfItIsNotSetValue()
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun ReadingContext.validate(phases: Set<GenerationPhase>): TaskResult<Unit> =
        context.settingContext.allPluginSettings.map { setting ->
            val value = context.settingContext.pluginSettingValue(setting) ?: return@map ValidationResult.OK
            if (setting.neededAtPhase in phases && setting.isActive(this))
                (setting.validator as SettingValidator<Any>).validate(this, value)
            else ValidationResult.OK
        }.fold(ValidationResult.OK, ValidationResult::and).toResult()

    private fun ReadingContext.saveSettingValues(phases: Set<GenerationPhase>) {
        for (setting in pluginSettings) {
            if (setting.neededAtPhase !in phases) continue
            if (!setting.isSavable) continue
            if (!setting.isAvailable(valuesReadingContext)) continue
            val serializer = setting.type.serializer as? SerializerImpl<Any> ?: continue
            service<SettingSavingWizardService>().saveSettingValue(
                setting.path,
                serializer.toString(setting.reference.settingValue)
            )
        }
    }

    open fun apply(
        services: List<WizardService>,
        phases: Set<GenerationPhase>,
        onTaskExecuting: (PipelineTask) -> Unit = {}
    ): TaskResult<Unit> = computeM {
        initPluginSettingsDefaultValues()
        initNonPluginDefaultValues()
        context.checkAllRequiredSettingPresent(phases).ensure()
        val taskRunningContext = WritingContext(context, servicesManager.withServices(services), isUnitTestMode)
        taskRunningContext.validate(phases).ensure()
        taskRunningContext.saveSettingValues(phases)
        val (tasksSorted) = context.sortTasks().map { tasks ->
            tasks.groupBy { it.phase }.toList().sortedBy { it.first }.flatMap { it.second }
        }
        tasksSorted
            // We should take only one task of each type as all tasks with the same path are considered to be the same
            .distinctBy { it.path }
            .asSequence()
            .filter { task -> task.phase in phases }
            .filter { task -> task.isAvailable(taskRunningContext) }
            .map { task -> onTaskExecuting(task); task.action(taskRunningContext) }
            .sequenceFailFirst()
            .ignore()
    }
}