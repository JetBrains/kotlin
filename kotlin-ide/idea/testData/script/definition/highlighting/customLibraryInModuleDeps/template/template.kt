package custom.scriptDefinition

import java.io.File
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.ScriptingHostConfigurationKeys
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.util.PropertiesCollection

@KotlinScript(
    fileExtension = "kts",
    compilationConfiguration = TemplateDefinition::class
)
open class Template(val args: Array<String>)

val ScriptingHostConfigurationKeys.getEnvironment by PropertiesCollection.key<() -> Map<String, Any?>?>()

object TemplateDefinition : ScriptCompilationConfiguration(
    {
        refineConfiguration {
            beforeCompiling { context ->
                val environment =
                    context.compilationConfiguration[ScriptCompilationConfiguration.hostConfiguration]?.let {
                        it[ScriptingHostConfiguration.getEnvironment]?.invoke()
                    }.orEmpty()

                context.compilationConfiguration.with {
                    dependencies(JvmDependency(environment["template-classes"] as File))
                }.asSuccess()
            }
        }
        ide {
            acceptedLocations(ScriptAcceptedLocation.Everywhere)
        }
    }
)