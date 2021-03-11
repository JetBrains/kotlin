package custom.scriptDefinition

import kotlin.script.dependencies.*
import kotlin.script.templates.*
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import kotlin.script.experimental.location.*

class TestDependenciesResolver : ScriptDependenciesResolver {
    override fun resolve(
            script: ScriptContents,
            environment: Map<String, Any?>?,
            report: (ScriptDependenciesResolver.ReportSeverity, String, ScriptContents.Position?) -> Unit, previousDependencies: KotlinScriptExternalDependencies?
    ): Future<KotlinScriptExternalDependencies?> {
        return CompletableFuture.completedFuture(
                object : KotlinScriptExternalDependencies {
                    override val classpath: Iterable<File> = listOf(environment?.get("template-classes") as File)
                })

    }
}

@ScriptExpectedLocations([ScriptExpectedLocation.Everywhere])
@ScriptTemplateDefinition(TestDependenciesResolver::class, scriptFilePattern = "script.kts")
class Template: Base()

open class Base {
    val i = 3
    val str = ""
}