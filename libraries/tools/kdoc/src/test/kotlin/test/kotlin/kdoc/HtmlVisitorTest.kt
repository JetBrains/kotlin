package test.kotlin.kdoc

import java.io.File
import kotlin.test.assertTrue
import org.jetbrains.jet.cli.jvm.K2JVMCompiler
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

        val compiler = K2JVMCompiler()
        compiler.getCompilerPlugins().add(HtmlCompilerPlugin())
        compiler.exec(
                System.out,
                "-kotlin-home", "../../../dist/kotlinc",
                "-d", File(dir, "target/classes-htmldocs").toString(),
                srcDir.toString()
        )
    }
}
