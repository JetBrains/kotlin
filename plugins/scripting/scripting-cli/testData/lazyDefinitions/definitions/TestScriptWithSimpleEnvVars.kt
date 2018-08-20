
import kotlin.script.experimental.annotations.*
import kotlin.script.experimental.api.*
import kotlin.script.experimental.util.*
import kotlin.script.experimental.misc.*
import kotlin.reflect.full.starProjectedType

object TestScriptWithSimpleEnvVarsConfiguration : ArrayList<Pair<TypedKey<*>, Any?>>(
    listOf(
        ScriptCompileConfigurationProperties.contextVariables("stringVar1" to String::class.starProjectedType)
    )
)

@KotlinScript
@KotlinScriptFileExtension("2.kts")
@KotlinScriptDefaultCompilationConfiguration(TestScriptWithSimpleEnvVarsConfiguration::class)
abstract class TestScriptWithSimpleEnvVars

