package custom.scriptDefinition

import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*

@KotlinScript(
    displayName = "Definition for tests",
    fileExtension = "kts",
    compilationConfiguration = TemplateDefinition::class
)
open class Template(val args: Array<String>)

@Suppress("UNCHECKED_CAST")
object TemplateDefinition : ScriptCompilationConfiguration(
    {
        baseClass(Base::class)
        ide {
            acceptedLocations(ScriptAcceptedLocation.Everywhere)
        }
    }
)

open class Base {
    val i = 3
    val str = ""
}