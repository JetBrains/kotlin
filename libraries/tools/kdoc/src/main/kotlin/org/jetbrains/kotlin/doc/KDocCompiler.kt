package org.jetbrains.kotlin.doc

import java.io.PrintStream
import org.jetbrains.jet.cli.common.CLICompiler
import org.jetbrains.jet.cli.common.CLIConfigurationKeys
import org.jetbrains.jet.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.jet.cli.jvm.K2JVMCompiler
import org.jetbrains.jet.config.CompilerConfiguration

/**
* Main for running the KDocCompiler
*/
fun main(args: Array<String>): Unit {
    CLICompiler.doMain(KDocCompiler(), args);
}

/**
 * A version of the [[K2JVMCompiler]] which includes the [[KDoc]] compiler plugin and allows
 * command line validation or for the configuration to be provided via [[KDocArguments]]
 */
class KDocCompiler() : K2JVMCompiler() {

    protected override fun configureEnvironment(configuration : CompilerConfiguration, arguments : K2JVMCompilerArguments) {
        super.configureEnvironment(configuration, arguments)
        configuration.add(CLIConfigurationKeys.COMPILER_PLUGINS, KDoc(arguments as KDocArguments))
    }

    protected override fun createArguments() : K2JVMCompilerArguments {
        return KDocArguments()
    }

    protected override fun usage(target : PrintStream) {
        target.println("Usage: KDocCompiler -docOutput <docOutputDir> [-output <outputDir>|-jar <jarFileName>] [-stdlib <path to runtime.jar>] [-src <filename or dirname>|-module <module file>] [-includeRuntime]");
    }
}

class KDocArguments() : K2JVMCompilerArguments() {

    public var docConfig: KDocConfig = KDocConfig()

    /**
     * Applies the command line arguments to the given KDoc configuration
     */
    fun apply(): KDocConfig {
        // TODO...
        return docConfig
    }
}
