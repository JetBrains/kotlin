package org.jetbrains.kotlin.doc

import java.io.File
import java.io.PrintStream
import org.jetbrains.jet.cli.jvm.K2JVMCompilerArguments
import org.jetbrains.jet.cli.jvm.K2JVMCompiler
import org.jetbrains.jet.cli.jvm.compiler.CompileEnvironmentConfiguration
import org.jetbrains.kotlin.doc.highlighter.HtmlCompilerPlugin

/**
* Main for running the KDocCompiler
*/
fun main(args: Array<String?>): Unit {
    K2JVMCompiler.doMain(KDocCompiler(), args);
}

/**
 * A version of the [[K2JVMCompiler]] which includes the [[KDoc]] compiler plugin and allows
 * command line validation or for the configuration to be provided via [[KDocArguments]]
 */
class KDocCompiler() : K2JVMCompiler() {

    protected override fun configureEnvironment(configuration : CompileEnvironmentConfiguration?, arguments : K2JVMCompilerArguments?) {
        super.configureEnvironment(configuration, arguments)
        val coreEnvironment = configuration?.getEnvironment()
        if (coreEnvironment != null) {
            val kdoc = KDoc()

            if (arguments is KDocArguments) {
                kdoc.config = arguments.apply()
            }
            val plugins = configuration?.getCompilerPlugins().orEmpty()
/*
            val sourcePlugin = HtmlCompilerPlugin()
            plugins.add(sourcePlugin)
*/
            plugins.add(kdoc);
        }
    }

    protected override fun createArguments() : K2JVMCompilerArguments? {
        return KDocArguments()
    }

    protected override fun usage(target : PrintStream?) {
        target?.println("Usage: KDocCompiler -docOutput <docOutputDir> [-output <outputDir>|-jar <jarFileName>] [-stdlib <path to runtime.jar>] [-src <filename or dirname>|-module <module file>] [-includeRuntime]");
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
