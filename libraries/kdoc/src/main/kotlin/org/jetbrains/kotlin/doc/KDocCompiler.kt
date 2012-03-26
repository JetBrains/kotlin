package org.jetbrains.kotlin.doc

import org.jetbrains.jet.cli.KotlinCompiler
import org.jetbrains.jet.compiler.CompileEnvironment
import org.jetbrains.jet.cli.CompilerArguments
import java.io.PrintStream
import java.io.File

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

    override fun configureEnvironment(environment : CompileEnvironment?, arguments : CompilerArguments?, errStream : PrintStream?) {
        // TODO lets clear the docOutput as a temporary hack while
        // KotlinCompiler has a KDoc hook...
        val docOutputDir = if (arguments != null) {
            val answer = arguments.docOutputDir
            arguments.docOutputDir = null
            answer
        } else null

        super.configureEnvironment(environment, arguments, errStream)
        val coreEnvironment = environment?.getMyEnvironment()
        if (coreEnvironment != null) {
            // now lets add the KDoc plugin
            val outDir = File(docOutputDir ?: "target/apidocs")
            val kdoc = KDoc(outDir)

            if (arguments is KDocArguments) {
                kdoc.config = arguments.apply()
            }
            coreEnvironment.getCompilerPlugins().orEmpty().add(kdoc);
        }
    }

    override fun createArguments() : CompilerArguments? {
        return KDocArguments()
    }

    override fun usage(target : PrintStream?) {
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


    /**
     * Add more configurations here when value attributes supported
     * see: KT-1522

    @Argument(value = "docOutput", description = "KDoc output directory")
    public String docOutputDir;
    */
}
