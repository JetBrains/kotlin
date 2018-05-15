
import kotlin.script.experimental.annotations.*
import kotlin.script.experimental.api.*
import kotlin.script.experimental.util.*
import kotlin.script.experimental.misc.*
import kotlin.reflect.full.starProjectedType

object TestScriptWithReceiversConfiguration : ArrayList<Pair<TypedKey<*>, Any?>>(
    listOf(
        ScriptCompileConfigurationProperties.scriptImplicitReceivers(String::class.starProjectedType)
    )
)

@KotlinScript
@KotlinScriptFileExtension("1.kts")
@KotlinScriptDefaultCompilationConfiguration(TestScriptWithReceiversConfiguration::class)
abstract class TestScriptWithReceivers
