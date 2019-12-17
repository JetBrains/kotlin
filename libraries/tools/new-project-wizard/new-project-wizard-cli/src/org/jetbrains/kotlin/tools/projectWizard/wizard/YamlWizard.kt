package org.jetbrains.kotlin.tools.projectWizard.wizard

import org.jetbrains.kotlin.tools.projectWizard.YamlSettingsParser
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.entity.PipelineTask
import org.jetbrains.kotlin.tools.projectWizard.core.entity.PluginSettingReference
import org.jetbrains.kotlin.tools.projectWizard.core.entity.reference
import org.jetbrains.kotlin.tools.projectWizard.core.service.*
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.StructurePlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.templates.TemplatesPlugin
import java.nio.file.Paths

class YamlWizard(
    private val yaml: String,
    private val path: String,
    createPlugins: (Context) -> List<Plugin>,
    isUnitTestMode: Boolean
) : Wizard(
    createPlugins,
    ServicesManager(Services.IDEA_INDEPENDENT_SERVICES) { services ->
        services.firstOrNull { it is IdeaIndependentWizardService }
    }, isUnitTestMode
) {
    override fun apply(
        services: List<WizardService>,
        phases: Set<GenerationPhase>,
        onTaskExecuting: (PipelineTask) -> Unit
    ): TaskResult<Unit> = computeM {
        super.apply(services, setOf(GenerationPhase.PREPARE), onTaskExecuting)
        val parsingData = with(valuesReadingContext) {
            ParsingState(TemplatesPlugin::templates.propertyValue, emptyMap())
        }
        val yamlParser = YamlSettingsParser(pluginSettings, parsingData)
        val (settingsValuesFromYaml) = yamlParser.parseYamlText(yaml)
        val settingValues = defaultPluginSettingValues + settingsValuesFromYaml
        settingValues.forEach { (path, value) -> context.settingContext[path] = value }
        context.settingContext[StructurePlugin::projectPath.reference] = Paths.get(path)
        super.apply(services, phases, onTaskExecuting)
    }

    private val defaultPluginSettingValues
        get() = pluginSettings.mapNotNull { setting ->
            val defaultValue = setting.defaultValue ?: return@mapNotNull null
            PluginSettingReference(setting) to defaultValue
        }.toMap()
}
