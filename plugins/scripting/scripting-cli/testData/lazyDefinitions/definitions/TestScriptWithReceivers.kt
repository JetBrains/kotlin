
import kotlin.script.experimental.annotations.*
import kotlin.script.experimental.api.*
import kotlin.script.experimental.util.*

object TestScriptWithReceiversDefinition : ScriptDefinition(
    {
        implicitReceivers<String>()
    })

@KotlinScript(extension = "1.kts", definition = TestScriptWithReceiversDefinition::class)
abstract class TestScriptWithReceivers
