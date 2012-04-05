package test.kotlin.kdoc

import java.io.File
import kotlin.test.assertTrue
import org.jetbrains.jet.cli.CompilerArguments
import org.jetbrains.jet.cli.KotlinCompiler
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

        val args = CompilerArguments()
        args.setSrc(srcDir.toString())
        args.setOutputDir(File(dir, "target/classes-htmldocs").toString())
        args.getCompilerPlugins()?.add(HtmlCompilerPlugin())

        val compiler = KotlinCompiler()
        compiler.exec(System.out, args)
    }
}