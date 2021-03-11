package org.jetbrains.kotlin.tools.projectWizard.wizard

import org.jetbrains.kotlin.tools.projectWizard.YamlSettingsParser
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.entity.PipelineTask
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.PluginSetting
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.SettingReference
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.reference
import org.jetbrains.kotlin.tools.projectWizard.core.service.IdeaIndependentWizardService
import org.jetbrains.kotlin.tools.projectWizard.core.service.Services
import org.jetbrains.kotlin.tools.projectWizard.core.service.ServicesManager
import org.jetbrains.kotlin.tools.projectWizard.core.service.WizardService
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.StructurePlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.templates.TemplatesPlugin
import java.nio.file.Path

class YamlWizard(
    private val yaml: String,
    private val projectPath: Path,
    createPlugins: (Context) -> List<Plugin>,
    servicesManager: ServicesManager = CLI_SERVICES_MANAGER,
    isUnitTestMode: Boolean
) : Wizard(createPlugins, servicesManager, isUnitTestMode) {

    override fun apply(
        services: List<WizardService>,
        phases: Set<GenerationPhase>,
        onTaskExecuting: (PipelineTask) -> Unit
    ): TaskResult<Unit> = computeM {
        val (settingsValuesFromYaml) = context.read { parseYaml(yaml, pluginSettings) }

        context.writeSettings {
            settingsValuesFromYaml.forEach { (reference, value) -> reference.setValue(value) }
            StructurePlugin.projectPath.reference.setValue(projectPath)
        }

        super.apply(services, phases, onTaskExecuting)
    }

    companion object {
        val CLI_SERVICES_MANAGER = ServicesManager(Services.IDEA_INDEPENDENT_SERVICES) { services ->
            services.firstOrNull { it is IdeaIndependentWizardService }
        }
    }
}

fun Reader.parseYaml(
    yaml: String,
    pluginSettings: List<PluginSetting<*, *>>
): TaskResult<Map<SettingReference<*, *>, Any>> {
    val parsingData = ParsingState(TemplatesPlugin.templates.propertyValue, emptyMap())
    val yamlParser = YamlSettingsParser(pluginSettings, parsingData)
    return yamlParser.parseYamlText(yaml)
}
