package custom.scriptDefinition

import java.io.File
import kotlin.script.dependencies.*
import kotlin.script.experimental.dependencies.*
import kotlin.script.templates.ScriptTemplateDefinition
import kotlin.script.experimental.location.ScriptExpectedLocation
import kotlin.script.experimental.location.ScriptExpectedLocations

var count = 0

class TestDependenciesResolver : DependenciesResolver {
    override fun resolve(
            scriptContents: ScriptContents,
            environment: Environment
    ): DependenciesResolver.ResolveResult {
        if (count == 0) {
            count++
            return ScriptDependencies.Empty.asSuccess()
        }
        return ScriptDependencies(classpath = listOf(environment["lib-classes"] as File)).asSuccess()
    }
}

@ScriptExpectedLocations([ScriptExpectedLocation.Everywhere])
@ScriptTemplateDefinition(TestDependenciesResolver::class, scriptFilePattern = "script.kts")
open class Template