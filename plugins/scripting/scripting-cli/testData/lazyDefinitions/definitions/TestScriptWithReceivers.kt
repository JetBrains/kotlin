
import kotlin.script.experimental.annotations.*
import kotlin.script.experimental.api.*
import kotlin.script.experimental.util.*

object TestScriptWithReceiversDefinition : ScriptCompilationConfiguration(
    {
        implicitReceivers(String::class)
    })

@KotlinScript(extension = "1.kts", compilationConfiguration = TestScriptWithReceiversDefinition::class)
abstract class TestScriptWithReceivers
