package org.jetbrains.kotlin.tools.projectWizard.wizard

import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.entity.*
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.SettingSerializer
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.reference
import org.jetbrains.kotlin.tools.projectWizard.core.service.ServicesManager
import org.jetbrains.kotlin.tools.projectWizard.core.service.SettingSavingWizardService
import org.jetbrains.kotlin.tools.projectWizard.core.service.WizardService
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.withAllSubModules

abstract class Wizard(createPlugins: PluginsCreator, servicesManager: ServicesManager, isUnitTestMode: Boolean) {
    val context = Context(createPlugins, servicesManager, isUnitTestMode)

    private fun checkAllRequiredSettingPresent(phases: Set<GenerationPhase>): TaskResult<Unit> = context.read {
        getUnspecifiedSettings(phases).let { unspecifiedSettings ->
            if (unspecifiedSettings.isEmpty()) UNIT_SUCCESS
            else Failure(RequiredSettingsIsNotPresentError(unspecifiedSettings.map { it.path }))
        }
    }

    private fun initNonPluginDefaultValues() {
        context.writeSettings {
            KotlinPlugin.modules.notRequiredSettingValue
                ?.withAllSubModules(includeSourcesets = true)
                ?.forEach { module ->
                    with(module) { initDefaultValuesForSettings() }
                }
        }
    }

    protected fun initPluginSettingsDefaultValues() {
        context.writeSettings {
            for (setting in pluginSettings) {
                setting.reference.setSettingValueToItsDefaultIfItIsNotSetValue()
            }
        }
    }

    fun validate(phases: Set<GenerationPhase>): ValidationResult = context.read {
        pluginSettings.map { setting ->
            val value = setting.notRequiredSettingValue ?: return@map ValidationResult.OK
            if (setting.validateOnProjectCreation && setting.neededAtPhase in phases && setting.isActive(this))
                (setting.validator as SettingValidator<Any>).validate(this, value)
            else ValidationResult.OK
        }.fold()
    }

    private fun saveSettingValues(phases: Set<GenerationPhase>) = context.read {
        for (setting in pluginSettings) {
            if (setting.neededAtPhase !in phases) continue
            if (!setting.isSavable) continue
            if (!setting.isAvailable(this)) continue
            val serializer = setting.type.serializer as? SettingSerializer.Serializer<Any> ?: continue
            service<SettingSavingWizardService>().saveSettingValue(
                setting.path,
                serializer.toString(setting.settingValue)
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
        checkAllRequiredSettingPresent(phases).ensure()
        validate(phases).toResult().ensure()
        saveSettingValues(phases)
        val (tasksSorted) = context.sortTasks().map { tasks ->
            tasks.groupBy { it.phase }.toList().sortedBy { it.first }.flatMap { it.second }
        }

        context.withAdditionalServices(services).write {
            tasksSorted
                // We should take only one task of each type as all tasks with the same path are considered to be the same
                .distinctBy { it.path }
                .asSequence()
                .filter { task -> task.phase in phases }
                .filter { task -> task.isAvailable(this) }
                .map { task -> onTaskExecuting(task); task.action(this) }
                .sequenceFailFirst()
                .ignore()
        }
    }
}