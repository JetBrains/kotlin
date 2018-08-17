
import kotlin.script.experimental.annotations.*
import kotlin.script.experimental.api.*
import kotlin.script.experimental.util.*

object TestScriptWithSimpleEnvVarsDefinition : ScriptDefinition(
    {
        providedProperties("stringVar1" to String::class)
    })

@KotlinScript(extension = "2.kts", definition = TestScriptWithSimpleEnvVarsDefinition::class)
abstract class TestScriptWithSimpleEnvVars

