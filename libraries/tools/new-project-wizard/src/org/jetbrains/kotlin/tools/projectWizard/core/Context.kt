package org.jetbrains.kotlin.tools.projectWizard.core

import org.jetbrains.kotlin.tools.projectWizard.core.entity.*
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties

class Context(private val pluginsCreator: PluginsCreator, val eventManager: EventManager) {
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

    val settingContext = SettingContext(eventManager::fireListeners)
    val propertyContext = PropertyContext()
    val taskContext = TaskContext()
    val plugins = pluginsCreator(this).onEach(::initPlugin)

    @Suppress("UNUSED_PARAMETER")
    fun getUnspecifiedSettings(phases: Set<GenerationPhase>): List<AnySetting> {
        val required = plugins
            .flatMap { plugin ->
                plugin.declaredSettings.mapNotNull { setting ->
                    if (setting.neededAtPhase !in phases) return@mapNotNull null
                    if (setting.isRequired) setting else null
                }
            }.toSet()
        val provided = settingContext.allPluginSettings.map { it.path }.toSet()
        return required.filterNot { it.path in provided }
    }

    private val pipelineLineTasks: List<PipelineTask>
        get() = plugins
            .flatMap { it.declaredTasks.filterIsInstance<PipelineTask>() }

    private fun task(reference: PipelineTaskReference) =
        taskContext.getEntity(reference) as? PipelineTask
            ?: error(reference.path)

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

    fun copy() = Context(pluginsCreator, eventManager)

    fun sortTasks(): TaskResult<List<PipelineTask>> =
        TaskSorter().sort(pipelineLineTasks, dependencyList)
}
