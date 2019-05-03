
import kotlin.script.experimental.annotations.*
import kotlin.script.experimental.api.*
import kotlin.script.experimental.util.*

object TestScriptWithSimpleEnvVarsDefinition : ScriptCompilationConfiguration(
    {
        providedProperties("stringVar1" to String::class)
    })

@KotlinScript(fileExtension = "2.kts", compilationConfiguration = TestScriptWithSimpleEnvVarsDefinition::class)
abstract class TestScriptWithSimpleEnvVars

