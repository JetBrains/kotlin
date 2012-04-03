package org.jetbrains.kotlin.doc

import java.io.File
import java.io.PrintStream
import org.jetbrains.jet.cli.CompilerArguments
import org.jetbrains.jet.cli.KotlinCompiler
import org.jetbrains.jet.compiler.CompileEnvironment
import org.jetbrains.kotlin.doc.highlighter.HtmlCompilerPlugin

/**
* Main for running the KDocCompiler
*/
fun main(args: Array<String?>): Unit {
    KotlinCompiler.doMain(KDocCompiler(), args);
}

/**
 * A version of the [[KotlinCompiler]] which includes the [[KDoc]] compiler plugin and allows
 * command line validation or for the configuration to be provided via [[KDocArguments]]
 */
class KDocCompiler() : KotlinCompiler() {

    protected override fun configureEnvironment(environment : CompileEnvironment?, arguments : CompilerArguments?, errStream : PrintStream?) {
        super.configureEnvironment(environment, arguments, errStream)
        val coreEnvironment = environment?.getEnvironment()
        if (coreEnvironment != null) {
            val kdoc = KDoc()

            if (arguments is KDocArguments) {
                kdoc.config = arguments.apply()
            }
            val plugins = coreEnvironment.getCompilerPlugins().orEmpty()
/*
            val sourcePlugin = HtmlCompilerPlugin()
            plugins.add(sourcePlugin)
*/
            plugins.add(kdoc);
        }
    }

    protected override fun createArguments() : CompilerArguments? {
        return KDocArguments()
    }

    protected override fun usage(target : PrintStream?) {
        target?.println("Usage: KDocCompiler -docOutput <docOutputDir> [-output <outputDir>|-jar <jarFileName>] [-stdlib <path to runtime.jar>] [-src <filename or dirname>|-module <module file>] [-includeRuntime]");
    }
}

class KDocArguments() : CompilerArguments() {

    public var docConfig: KDocConfig = KDocConfig()

    /**
     * Applies the command line arguments to the given KDoc configuration
     */
    fun apply(): KDocConfig {
        // TODO...
        return docConfig
    }
}
