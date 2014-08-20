package test.kotlin.kdoc

import java.io.File
import kotlin.test.assertTrue
import org.jetbrains.jet.cli.common.ExitCode
import org.jetbrains.kotlin.doc.KDocCompiler
import org.junit.Assert
import org.junit.Test

class KDocTest {
    Test fun generateKDocForStandardLibrary() {
        var moduleName = "ApiDocsModule.kt"
        var dir = "."
        if (!File(moduleName).exists()) {
            dir = "kdoc"
            moduleName = dir + "/" + moduleName
            assertTrue(File(moduleName).exists(), "Cannot find file $moduleName")
        }
        val outDir = File(dir, "target/apidocs")
        println("Generating library KDocs to $outDir")

        val compiler = KDocCompiler()
        val r = compiler.exec(
                System.out,
                "-kotlin-home", "../../../dist/kotlinc",
                "-d", "target/classes-stdlib",
                "-no-stdlib",
                "-classpath", "../runtime/target/kotlin-runtime-0.1-SNAPSHOT.jar${File.pathSeparator}../../lib/junit-4.9.jar",
                "../../stdlib/src", "../../kunit/src/main/kotlin", "../../kotlin-jdbc/src/main/kotlin"
        )
        Assert.assertEquals(ExitCode.OK, r)
    }
}
