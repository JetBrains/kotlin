package test.kotlin.kdoc

import java.io.File
import kotlin.test.assertTrue
import org.jetbrains.jet.cli.jvm.K2JVMCompiler
import org.jetbrains.jet.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.kotlin.doc.highlighter.HtmlCompilerPlugin
import org.junit.Test

class HtmlVisitorTest {

    Test fun generateHtmlFromSource() {
        val src = "src/test/sample"
        var dir = File(".")
        if (!File(dir, src).exists()) {
            dir = File("kdoc")
            assertTrue(File(dir, src).exists(), "Cannot find file $src")
        }
        val srcDir = File(dir, src)
        val outDir = File(dir, "target/htmldocs")
        println("Generating source HTML to $outDir")

        val args = K2JVMCompilerArguments()
        args.kotlinHome = "../../../dist/kotlinc"
        args.freeArgs = listOf(srcDir.toString())
        args.destination = File(dir, "target/classes-htmldocs").toString()

        val compiler = K2JVMCompiler()
        compiler.getCompilerPlugins().add(HtmlCompilerPlugin())
        compiler.exec(System.out, args)
    }
}
