package example

import org.jetbrains.jet.config.CompilerConfigurationKey
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.jet.config.CompilerConfiguration
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import com.intellij.mock.MockProject
import org.jetbrains.jet.extensions.ExternalDeclarationsProvider
import org.jetbrains.jet.codegen.extensions.ExpressionCodegenExtension

public object ExampleConfigurationKeys {
    public val EXAMPLE_KEY: CompilerConfigurationKey<String> = CompilerConfigurationKey.create<String>("example argument")
}

public class ExampleCommandLineProcessor : CommandLineProcessor {
    class object {
        public val ANDROID_COMPILER_PLUGIN_ID: String = "example.plugin"
        public val EXAMPLE_OPTION: CliOption = CliOption("exampleKey", "<value>", "")
    }

    override val pluginId: String = ANDROID_COMPILER_PLUGIN_ID
    override val pluginOptions: Collection<CliOption> = listOf(EXAMPLE_OPTION)

    override fun processOption(option: CliOption, value: String, configuration: CompilerConfiguration) {
        when (option) {
            EXAMPLE_OPTION -> configuration.put(ExampleConfigurationKeys.EXAMPLE_KEY, value)
            else -> throw CliOptionProcessingException("Unknown option: ${option.name}")
        }
    }
}

public class ExampleComponentRegistrar : ComponentRegistrar {

    public override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val exampleArgument = configuration.get(ExampleConfigurationKeys.EXAMPLE_KEY)
        println("Project component registration: $exampleArgument")
    }
}