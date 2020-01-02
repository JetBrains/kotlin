package org.jetbrains.kotlin.tools.projectWizard

import YamlParsingError
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.entity.*
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.parser.ParserException
import java.nio.file.Path


class YamlSettingsParser(settings: List<PluginSetting<Any, *>>, private val parsingState: ParsingState) {
    private val settingByName = settings.associateBy { it.path }

    fun parseYamlText(yaml: String): TaskResult<Map<SettingReference<*, *>, Any>> {
        val yamlObject = try {
            Success(Yaml().load<Any>(yaml) ?: emptyMap<String, Any>())
        } catch (e: ParserException) {
            Failure(YamlParsingError(e))
        } catch (e: Exception) {
            Failure(ExceptionErrorImpl(e))
        }
        return yamlObject.flatMap { map ->
            if (map is Map<*, *>) {
                val result = ComputeContext.runInComputeContextWithState(parsingState) {
                    parseSettingValues(map, "")
                }
                result.map { (pluginSettings, newState) ->
                    pluginSettings + newState.settingValues
                }
            } else Failure(
                BadSettingValueError("Settings file should be a map of settings")
            )
        }
    }

    fun parseYamlFile(file: Path) = computeM {
        val (yaml) = safe { file.toFile().readText() }
        parseYamlText(yaml)
    }

    private fun ParsingContext.parseSettingValues(
        data: Any,
        settingPath: String
    ): TaskResult<Map<PluginSettingReference<*, *>, Any>> =
        if (settingPath in settingByName) compute {
            val setting = settingByName.getValue(settingPath)
            val (parsed) = setting.type.parse(this, data, settingPath)
            listOf(PluginSettingReference(setting) to parsed)
        } else {
            when (data) {
                is Map<*, *> -> {
                    data.entries.mapCompute { (name, value) ->
                        if (value == null) fail(BadSettingValueError("No value was found for a key `$settingPath`"))
                        val prefix = if (settingPath.isEmpty()) "" else "$settingPath."
                        val (children) = parseSettingValues(value, "$prefix$name")
                        children.entries.map { (key, value) -> key to value }
                    }.sequence().map { it.flatten() }
                }
                else -> Failure(
                    BadSettingValueError("No value was found for a key `$settingPath`")
                )
            }
        }.map { it.toMap() }
}