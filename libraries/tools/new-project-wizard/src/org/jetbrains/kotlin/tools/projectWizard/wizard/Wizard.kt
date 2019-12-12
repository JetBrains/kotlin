package org.jetbrains.kotlin.tools.projectWizard.wizard

import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.entity.*
import org.jetbrains.kotlin.tools.projectWizard.core.service.Service
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase

abstract class Wizard(createPlugins: PluginsCreator, initialServices: List<Service>) {
    val context = Context(createPlugins, EventManager())
    val valuesReadingContext = ValuesReadingContext(context, initialServices)
    protected val plugins = context.plugins
    protected val pluginSettings = plugins.flatMap { it.declaredSettings }.distinctBy { it.path }

    private fun Context.checkAllRequiredSettingPresent(phases: Set<GenerationPhase>): TaskResult<Unit> =
        getUnspecifiedSettings(phases).let { unspecifiedSettings ->
            if (unspecifiedSettings.isEmpty()) UNIT_SUCCESS
            else Failure(RequiredSettingsIsNotPresentError(unspecifiedSettings.map { it.path }))
        }

    @Suppress("UNUSED_PARAMETER")
    fun ValuesReadingContext.validate(phases: Set<GenerationPhase>): TaskResult<Unit> =
        context.settingContext.allPluginSettings.map { setting ->
            val value = context.settingContext.pluginSettingValue(setting) ?: return@map ValidationResult.OK
            when (setting.neededAtPhase) {
                in phases -> setting.validator.validate(this, value)
                else -> ValidationResult.OK
            }
        }.fold(ValidationResult.OK, ValidationResult::and).toResult()

    open fun apply(
        services: List<Service>,
        phases: Set<GenerationPhase>,
        onTaskExecuting: (PipelineTask) -> Unit = {}
    ): TaskResult<Unit> = computeM {
        context.checkAllRequiredSettingPresent(phases).ensure()
        val taskRunningContext = TaskRunningContext(context, services)
        taskRunningContext.validate(phases).ensure()
        val (tasksSorted) = context.sortTasks().map { tasks ->
            tasks.groupBy { it.phase }.toList().sortedBy { it.first }.flatMap { it.second }
        }
        tasksSorted
            // We should take only one task of each type as all tasks with the same path are considered to be the same
            .distinctBy { it.path }
            .asSequence()
            .filter { task -> task.phase in phases }
            .filter { task -> task.checker.check(taskRunningContext) }
            .map { task -> onTaskExecuting(task); task.action(taskRunningContext) }
            .sequenceFailFirst()
            .ignore()
    }
}